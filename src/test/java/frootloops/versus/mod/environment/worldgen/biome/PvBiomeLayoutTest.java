package frootloops.versus.mod.environment.worldgen.biome;

import com.mojang.datafixers.util.Pair;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes;
import frootloops.versus.mod.environment.worldgen.biome.PvBiomeLayout.Box;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.biome.source.util.MultiNoiseUtil.NoiseHypercube;
import net.minecraft.world.biome.source.util.VanillaBiomeParameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvBiomeLayoutTest {

    private static List<Pair<NoiseHypercube, RegistryKey<Biome>>> vanilla;

    @BeforeAll
    static void bootstrap() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        vanilla = new ArrayList<>();
        new VanillaBiomeParameters().writeOverworldBiomeParameters(vanilla::add);
    }

    /** Guards against anything (a mixin, another mod) changing vanilla's layout, which every world type shares. */
    @Test
    void vanillaLayoutHasNoPlayersVersusBiomes() {
        for (Pair<NoiseHypercube, RegistryKey<Biome>> entry : vanilla) {
            assertFalse(entry.getSecond().getValue().getNamespace().equals("players-versus"), () -> "vanilla layout contains " + entry.getSecond());
        }
    }

    /** {@link PvBiomeLayout#build} relies on this shape of vanilla's list. */
    @Test
    void vanillaLayoutHasTheExpectedShape() {
        int surface = 0, deep = 0;
        Map<RegistryKey<Biome>, Integer> other = new HashMap<>();
        for (int i = 0; i < vanilla.size(); i++) {
            NoiseHypercube h = vanilla.get(i).getFirst();
            if (PvBiomeLayout.isPoint(h.depth(), 0.0F)) {
                surface++;
                NoiseHypercube twin = vanilla.get(i + 1).getFirst();
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
        assertEquals(Map.of(BiomeKeys.LUSH_CAVES, 1, BiomeKeys.DRIPSTONE_CAVES, 1, BiomeKeys.DEEP_DARK, 1), other);
    }

    /** The replacement dripstone keeps the depth the old mixin gave it by passing continentalness as depth. */
    @Test
    void deepDripstoneKeepsItsOldDepth() {
        NoiseHypercube dripstone = vanilla.stream().filter(e -> e.getSecond() == BiomeKeys.DRIPSTONE_CAVES).findFirst().orElseThrow().getFirst();
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
        for (Pair<NoiseHypercube, RegistryKey<Biome>> entry : vanilla) {
            if (PvBiomeLayout.isPoint(entry.getFirst().depth(), 0.0F)) vanillaVolume += volume(Box.of(entry.getFirst()));
        }
        System.out.printf(Locale.ROOT, "[layout] players versus: %d entries, %d at the surface%n", layout.size(), surfaceEntries);
        assertEquals(vanillaVolume, layoutVolume, vanillaVolume * 1e-12, "surface pieces don't add up to vanilla's slices");

        // pieces of one slice never overlap and stay inside it
        for (Pair<NoiseHypercube, RegistryKey<Biome>> entry : vanilla) {
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
                    assertNull(pieces.get(a).intersect(pieces.get(b)), "overlapping pieces in slice " + entry.getSecond().getValue());
                }
            }
            assertEquals(volume(slice), sum, volume(slice) * 1e-9, "pieces don't cover slice " + entry.getSecond().getValue());
        }
    }

    /** Q2: both edges of the humid transition use the humid map (birch forest turns into dark birch forest). */
    @Test
    void humidTransitionUsesTheHumidMapOnBothEdges() {
        MultiNoiseUtil.Entries<RegistryKey<Biome>> layout = entries(toPairs(PvBiomeLayout.build()));
        int checked = 0;
        for (Pair<NoiseHypercube, RegistryKey<Biome>> entry : vanilla) {
            NoiseHypercube h = entry.getFirst();
            if (entry.getSecond() != BiomeKeys.BIRCH_FOREST || !PvBiomeLayout.isPoint(h.depth(), 0.0F)) continue;
            if (h.humidity().min() > MultiNoiseUtil.toLong(0.29F) || h.humidity().max() < MultiNoiseUtil.toLong(0.29F)) continue;
            if (h.erosion().max() < MultiNoiseUtil.toLong(-0.4F)) continue;
            long temperature = mid(h.temperature());
            if (temperature >= MultiNoiseUtil.toLong(-0.55F) && temperature <= MultiNoiseUtil.toLong(-0.375F)) continue;
            // erosion above the mountainside limit and temperature outside the frozen band: only the humid rule applies
            // slice edges are other slices' edges too, where the nearest entry is a tie: stay strictly inside
            MultiNoiseUtil.NoiseValuePoint point = point(temperature, MultiNoiseUtil.toLong(0.29F), mid(h.continentalness()),
                    Math.max(mid(h.erosion()), MultiNoiseUtil.toLong(-0.4F)), mid(h.weirdness()));
            assertEquals(CustomOverworldBiomes.DARK_BIRCH_FOREST, layout.get(point), "at " + point);
            checked++;
        }
        assertTrue(checked > 0, "no birch forest slice crosses humidity 0.29");
    }

    /** Q4: a mountainside covers only erosion below -0.475; the forest keeps the rest of its slice. */
    @Test
    void mountainsideStopsAtItsErosionLimit() {
        MultiNoiseUtil.Entries<RegistryKey<Biome>> layout = entries(toPairs(PvBiomeLayout.build()));
        long limit = MultiNoiseUtil.toLong(-0.475F);
        long inland = MultiNoiseUtil.toLong(0.1F);
        int checked = 0;
        for (Pair<NoiseHypercube, RegistryKey<Biome>> entry : vanilla) {
            NoiseHypercube h = entry.getFirst();
            if (entry.getSecond() != BiomeKeys.FOREST || !PvBiomeLayout.isPoint(h.depth(), 0.0F)) continue;
            if (h.erosion().min() >= limit - 100 || h.erosion().max() <= limit + 100) continue;
            if (h.continentalness().max() <= inland) continue;
            if (h.weirdness().min() >= MultiNoiseUtil.toLong(-0.3F) && h.weirdness().max() <= MultiNoiseUtil.toLong(0.3F)) continue;
            long temperature = mid(h.temperature());
            if (temperature >= MultiNoiseUtil.toLong(-0.55F) && temperature <= MultiNoiseUtil.toLong(-0.375F)) continue;
            long continentalness = Math.max(mid(h.continentalness()), inland);
            RegistryKey<Biome> below = layout.get(point(temperature, mid(h.humidity()), continentalness, limit - 100, mid(h.weirdness())));
            RegistryKey<Biome> above = layout.get(point(temperature, mid(h.humidity()), continentalness, limit + 100, mid(h.weirdness())));
            assertTrue(below == CustomOverworldBiomes.MOUNTAINSIDE_FOREST || below == CustomOverworldBiomes.MOUNTAINSIDE_FOREST_WARM, "below: " + below);
            assertEquals(BiomeKeys.FOREST, above, "above");
            checked++;
        }
        assertTrue(checked > 0, "no forest slice crosses erosion -0.475 outside river valleys");
    }

    /**
     * How far the new layout moves biomes compared with the old mixin, over random climate points at several depths.
     * The surface changes by design (Q2, Q4, Q7); caves must not change.
     */
    @Test
    void newLayoutStaysCloseToTheOldOne() {
        MultiNoiseUtil.Entries<RegistryKey<Biome>> oldLayout = entries(OldBiomeLayout.build());
        MultiNoiseUtil.Entries<RegistryKey<Biome>> newLayout = entries(toPairs(PvBiomeLayout.build()));
        // Depths between the layout's depth-range edges; points exactly on an edge are ties decided by the search tree.
        float[] depths = {0.0F, 0.05F, 0.12F, 0.17F, 0.22F, 0.3F, 0.45F, 0.7F, 0.85F, 0.95F, 1.05F};
        Random random = new Random(8675309L);
        int samples = 40_000;
        for (float depth : depths) {
            Map<String, Integer> changes = new HashMap<>();
            int same = 0;
            for (int i = 0; i < samples; i++) {
                MultiNoiseUtil.NoiseValuePoint point = MultiNoiseUtil.createNoiseValuePoint(
                        uniform(random), uniform(random), uniform(random), uniform(random), depth, uniform(random));
                RegistryKey<Biome> before = oldLayout.get(point);
                RegistryKey<Biome> after = newLayout.get(point);
                if (before == after) same++;
                else changes.merge(before.getValue().getPath() + " -> " + after.getValue().getPath(), 1, Integer::sum);
            }
            double agreement = (double) same / samples;
            System.out.printf(Locale.ROOT, "[layout] depth %.2f: %.2f%% unchanged; top changes %s%n", depth, 100 * agreement, top(changes, 8, samples));
            // random points can still land on a climate-axis edge (values are whole ten-thousandths), hence the small margin
            if (depth >= 0.3F) assertTrue(agreement > 0.9995, "caves changed at depth " + depth);
            else assertTrue(agreement > 0.8, "surface changed too much at depth " + depth);
        }
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
    private static MultiNoiseUtil.NoiseValuePoint point(long temperature, long humidity, long continentalness, long erosion, long weirdness) {
        return new MultiNoiseUtil.NoiseValuePoint(temperature, humidity, continentalness, erosion, 0L, weirdness);
    }

    private static long mid(MultiNoiseUtil.ParameterRange range) {
        return (range.min() + range.max()) / 2;
    }

    private static double volume(Box box) {
        double volume = 1;
        for (int axis = 0; axis < Box.AXES; axis++) volume *= (double) box.max[axis] - (double) box.min[axis];
        return volume;
    }

    private static List<Pair<NoiseHypercube, RegistryKey<Biome>>> toPairs(List<PvBiomeLayout.Entry> layout) {
        return layout.stream().map(e -> Pair.of(e.parameters(), e.biome())).toList();
    }

    private static MultiNoiseUtil.Entries<RegistryKey<Biome>> entries(List<Pair<NoiseHypercube, RegistryKey<Biome>>> list) {
        return new MultiNoiseUtil.Entries<>(list);
    }
}
