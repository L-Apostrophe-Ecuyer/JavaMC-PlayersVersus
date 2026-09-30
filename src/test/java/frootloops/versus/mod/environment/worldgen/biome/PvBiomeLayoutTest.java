package frootloops.versus.mod.environment.worldgen.biome;

import com.mojang.datafixers.util.Pair;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes;
import frootloops.versus.mod.environment.worldgen.TestGame;
import frootloops.versus.mod.environment.worldgen.biome.PvBiomeLayout.Box;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Climate.ParameterPoint;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvBiomeLayoutTest {

    private static List<Pair<ParameterPoint, ResourceKey<Biome>>> vanilla;

    @BeforeAll
    static void bootstrap() {
        TestGame.start();
        vanilla = new ArrayList<>();
        new OverworldBiomeBuilder().addBiomes(vanilla::add);
    }

    /** Guards against anything (a mixin, another mod) changing vanilla's layout, which every world type shares. */
    @Test
    void vanillaLayoutHasNoPlayersVersusBiomes() {
        for (Pair<ParameterPoint, ResourceKey<Biome>> entry : vanilla) {
            assertFalse(entry.getSecond().identifier().getNamespace().equals("players-versus"), () -> "vanilla layout contains " + entry.getSecond());
        }
    }

    /** {@link PvBiomeLayout#build} relies on this shape of vanilla's list. */
    @Test
    void vanillaLayoutHasTheExpectedShape() {
        int surface = 0, deep = 0;
        Map<ResourceKey<Biome>, Integer> other = new HashMap<>();
        for (int i = 0; i < vanilla.size(); i++) {
            ParameterPoint h = vanilla.get(i).getFirst();
            if (PvBiomeLayout.isPoint(h.depth(), 0.0F)) {
                surface++;
                ParameterPoint twin = vanilla.get(i + 1).getFirst();
                assertTrue(PvBiomeLayout.isPoint(twin.depth(), 1.0F), "surface entry " + i + " has no depth-1 twin");
                assertEquals(vanilla.get(i).getSecond(), vanilla.get(i + 1).getSecond());
                assertEquals(Box.of(h).toHypercube(twin.depth(), h.offset()), twin);
            } else if (PvBiomeLayout.isPoint(h.depth(), 1.0F)) {
                deep++;
            } else {
                other.merge(vanilla.get(i).getSecond(), 1, Integer::sum);
            }
        }
        System.out.printf(Locale.ROOT, "[layout] vanilla: %d entries, %d surface slices, other %s%n", vanilla.size(), surface, other);
        assertEquals(surface, deep);
        assertEquals(Map.of(Biomes.LUSH_CAVES, 1, Biomes.DRIPSTONE_CAVES, 1, Biomes.DEEP_DARK, 1), other);
    }

    /** The replacement dripstone keeps the depth the old mixin gave it by passing continentalness as depth. */
    @Test
    void deepDripstoneKeepsItsOldDepth() {
        ParameterPoint dripstone = vanilla.stream().filter(e -> e.getSecond() == Biomes.DRIPSTONE_CAVES).findFirst().orElseThrow().getFirst();
        assertEquals(dripstone.continentalness(), PvBiomeLayout.DEEP_DRIPSTONE_DEPTH);
    }

    @Test
    void piecesPartitionEachSurfaceSlice() {
        List<PvBiomeLayout.Entry> layout = PvBiomeLayout.build();
        int surfaceEntries = 0;
        double vanillaVolume = 0, layoutVolume = 0;
        for (PvBiomeLayout.Entry entry : layout) {
            if (PvBiomeLayout.isPoint(entry.parameters().depth(), 0.0F)) {
                surfaceEntries++;
                layoutVolume += volume(Box.of(entry.parameters()));
            }
        }
        for (Pair<ParameterPoint, ResourceKey<Biome>> entry : vanilla) {
            if (PvBiomeLayout.isPoint(entry.getFirst().depth(), 0.0F)) vanillaVolume += volume(Box.of(entry.getFirst()));
        }
        System.out.printf(Locale.ROOT, "[layout] players versus: %d entries, %d at the surface%n", layout.size(), surfaceEntries);
        assertEquals(vanillaVolume, layoutVolume, vanillaVolume * 1e-12, "surface pieces don't add up to vanilla's slices");

        // pieces of one slice never overlap and stay inside it
        for (Pair<ParameterPoint, ResourceKey<Biome>> entry : vanilla) {
            if (!PvBiomeLayout.isPoint(entry.getFirst().depth(), 0.0F)) continue;
            Box slice = Box.of(entry.getFirst());
            List<Box> pieces = new ArrayList<>();
            for (PvBiomeLayout.Entry candidate : layout) {
                if (!PvBiomeLayout.isPoint(candidate.parameters().depth(), 0.0F)) continue;
                Box box = Box.of(candidate.parameters());
                Box inside = box.intersect(slice);
                if (inside != null && volume(inside) == volume(box)) pieces.add(box);
            }
            double sum = 0;
            for (int a = 0; a < pieces.size(); a++) {
                sum += volume(pieces.get(a));
                for (int b = a + 1; b < pieces.size(); b++) {
                    assertNull(pieces.get(a).intersect(pieces.get(b)), "overlapping pieces in slice " + entry.getSecond().identifier());
                }
            }
            assertEquals(volume(slice), sum, volume(slice) * 1e-9, "pieces don't cover slice " + entry.getSecond().identifier());
        }
    }

    /** Q2: both edges of the humid transition use the humid map (birch forest turns into dark birch forest). */
    @Test
    void humidTransitionUsesTheHumidMapOnBothEdges() {
        Climate.ParameterList<ResourceKey<Biome>> layout = entries(toPairs(PvBiomeLayout.build()));
        int checked = 0;
        for (Pair<ParameterPoint, ResourceKey<Biome>> entry : vanilla) {
            ParameterPoint h = entry.getFirst();
            if (entry.getSecond() != Biomes.BIRCH_FOREST || !PvBiomeLayout.isPoint(h.depth(), 0.0F)) continue;
            if (h.humidity().min() > Climate.quantizeCoord(0.29F) || h.humidity().max() < Climate.quantizeCoord(0.29F)) continue;
            if (h.erosion().max() < Climate.quantizeCoord(-0.4F)) continue;
            long temperature = mid(h.temperature());
            if (temperature >= Climate.quantizeCoord(-0.55F) && temperature <= Climate.quantizeCoord(-0.375F)) continue;
            // erosion above the mountainside limit and temperature outside the frozen band: only the humid rule applies
            // slice edges are other slices' edges too, where the nearest entry is a tie: stay strictly inside
            Climate.TargetPoint point = point(temperature, Climate.quantizeCoord(0.29F), mid(h.continentalness()),
                    Math.max(mid(h.erosion()), Climate.quantizeCoord(-0.4F)), mid(h.weirdness()));
            assertEquals(CustomOverworldBiomes.DARK_BIRCH_FOREST, layout.findValue(point), "at " + point);
            checked++;
        }
        assertTrue(checked > 0, "no birch forest slice crosses humidity 0.29");
    }

    /** Q4: a mountainside covers only erosion below -0.475; the forest keeps the rest of its slice. */
    @Test
    void mountainsideStopsAtItsErosionLimit() {
        Climate.ParameterList<ResourceKey<Biome>> layout = entries(toPairs(PvBiomeLayout.build()));
        long limit = Climate.quantizeCoord(-0.475F);
        long inland = Climate.quantizeCoord(0.1F);
        int checked = 0;
        for (Pair<ParameterPoint, ResourceKey<Biome>> entry : vanilla) {
            ParameterPoint h = entry.getFirst();
            if (entry.getSecond() != Biomes.FOREST || !PvBiomeLayout.isPoint(h.depth(), 0.0F)) continue;
            if (h.erosion().min() >= limit - 100 || h.erosion().max() <= limit + 100) continue;
            if (h.continentalness().max() <= inland) continue;
            if (h.weirdness().min() >= Climate.quantizeCoord(-0.3F) && h.weirdness().max() <= Climate.quantizeCoord(0.3F)) continue;
            long temperature = mid(h.temperature());
            if (temperature >= Climate.quantizeCoord(-0.55F) && temperature <= Climate.quantizeCoord(-0.375F)) continue;
            long continentalness = Math.max(mid(h.continentalness()), inland);
            ResourceKey<Biome> below = layout.findValue(point(temperature, mid(h.humidity()), continentalness, limit - 100, mid(h.weirdness())));
            ResourceKey<Biome> above = layout.findValue(point(temperature, mid(h.humidity()), continentalness, limit + 100, mid(h.weirdness())));
            assertTrue(below == CustomOverworldBiomes.MOUNTAINSIDE_FOREST || below == CustomOverworldBiomes.MOUNTAINSIDE_FOREST_WARM, "below: " + below);
            assertEquals(Biomes.FOREST, above, "above");
            checked++;
        }
        assertTrue(checked > 0, "no forest slice crosses erosion -0.475 outside river valleys");
    }

    /**
     * How far the new layout moves biomes compared with the old mixin, over random climate points at several depths.
     * The surface changes by design (Q2, Q4, Q7); caves must not change. Where two entries are exactly as near, the
     * search tree's layout decides, so a different pick that ties in either layout isn't counted as a change.
     */
    @Test
    void newLayoutStaysCloseToTheOldOne() {
        List<Pair<ParameterPoint, ResourceKey<Biome>>> oldList = OldBiomeLayout.build();
        List<Pair<ParameterPoint, ResourceKey<Biome>>> newList = toPairs(PvBiomeLayout.build());
        Climate.ParameterList<ResourceKey<Biome>> oldLayout = entries(oldList);
        Climate.ParameterList<ResourceKey<Biome>> newLayout = entries(newList);
        float[] depths = {0.0F, 0.05F, 0.12F, 0.17F, 0.22F, 0.3F, 0.45F, 0.7F, 0.85F, 0.95F, 1.05F};
        Random random = new Random(8675309L);
        int samples = 40_000;
        for (float depth : depths) {
            Map<String, Integer> changes = new HashMap<>();
            int changed = 0, ties = 0;
            for (int i = 0; i < samples; i++) {
                Climate.TargetPoint point = Climate.target(
                        uniform(random), uniform(random), uniform(random), uniform(random), depth, uniform(random));
                ResourceKey<Biome> before = oldLayout.findValue(point);
                ResourceKey<Biome> after = newLayout.findValue(point);
                if (before == after) continue;
                if (nearest(oldList, point).contains(after) || nearest(newList, point).contains(before)) {
                    ties++;
                    continue;
                }
                changed++;
                changes.merge(before.identifier().getPath() + " -> " + after.identifier().getPath(), 1, Integer::sum);
            }
            double agreement = 1.0 - (double) changed / samples;
            System.out.printf(Locale.ROOT, "[layout] depth %.2f: %.2f%% unchanged (%d ties); top changes %s%n",
                    depth, 100 * agreement, ties, top(changes, 8, samples));
            if (depth >= 0.3F) assertEquals(0, changed, "caves changed at depth " + depth + ": " + changes);
            else assertTrue(agreement > 0.8, "surface changed too much at depth " + depth);
        }
    }

    /** Every biome whose entry is nearest to the point, by the same squared distance the search tree minimizes. */
    private static Set<ResourceKey<Biome>> nearest(List<Pair<ParameterPoint, ResourceKey<Biome>>> list, Climate.TargetPoint point) {
        long best = Long.MAX_VALUE;
        Set<ResourceKey<Biome>> biomes = new HashSet<>();
        for (Pair<ParameterPoint, ResourceKey<Biome>> entry : list) {
            long distance = squaredDistance(entry.getFirst(), point);
            if (distance < best) {
                best = distance;
                biomes.clear();
            }
            if (distance == best) biomes.add(entry.getSecond());
        }
        return biomes;
    }

    private static long squaredDistance(ParameterPoint h, Climate.TargetPoint p) {
        return square(axis(h.temperature(), p.temperature())) + square(axis(h.humidity(), p.humidity()))
                + square(axis(h.continentalness(), p.continentalness())) + square(axis(h.erosion(), p.erosion()))
                + square(axis(h.depth(), p.depth())) + square(axis(h.weirdness(), p.weirdness())) + square(h.offset());
    }

    private static long axis(Climate.Parameter range, long value) {
        return value < range.min() ? range.min() - value : value > range.max() ? value - range.max() : 0L;
    }

    private static long square(long value) {
        return value * value;
    }

    private static float uniform(Random random) {
        return random.nextFloat() * 2.4F - 1.2F;
    }

    private static String top(Map<String, Integer> changes, int count, int samples) {
        return changes.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(count)
                .map(e -> String.format(Locale.ROOT, "%s %.2f%%", e.getKey(), 100.0 * e.getValue() / samples))
                .toList().toString();
    }

    /** A surface point (depth 0). */
    private static Climate.TargetPoint point(long temperature, long humidity, long continentalness, long erosion, long weirdness) {
        return new Climate.TargetPoint(temperature, humidity, continentalness, erosion, 0L, weirdness);
    }

    private static long mid(Climate.Parameter range) {
        return (range.min() + range.max()) / 2;
    }

    private static double volume(Box box) {
        double volume = 1;
        for (int axis = 0; axis < Box.AXES; axis++) volume *= (double) box.max[axis] - (double) box.min[axis];
        return volume;
    }

    private static List<Pair<ParameterPoint, ResourceKey<Biome>>> toPairs(List<PvBiomeLayout.Entry> layout) {
        return layout.stream().map(e -> Pair.of(e.parameters(), e.biome())).toList();
    }

    private static Climate.ParameterList<ResourceKey<Biome>> entries(List<Pair<ParameterPoint, ResourceKey<Biome>>> list) {
        return new Climate.ParameterList<>(list);
    }
}
