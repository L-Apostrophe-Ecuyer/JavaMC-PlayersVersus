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
import java.util.TreeMap;
import java.util.TreeSet;
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

    /**
     * How far inside a range the probes stay (0.005): slice edges are other slices' edges too, where the nearest entry
     * is a tie; and the mountainsides' limit can be as close as 0.01 to vanilla's slice edge at -0.375.
     */
    private static final long EDGE = 50;
    /** Erosion just above the mountainsides' limit. */
    private static final long ABOVE_MOUNTAINS = PvBiomeLayout.MOUNTAIN_EROSION_MAX + EDGE;

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
        assertEquals(Map.of(Biomes.LUSH_CAVES, 1, Biomes.DRIPSTONE_CAVES, 1, Biomes.SULFUR_CAVES, 1, Biomes.ICE_CAVES, 1,
                Biomes.DEEP_DARK, 1), other);
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
            // The pieces of vanilla's slices; the mod's own surface entries (the wider mushroom fields) lie over them
            if (PvBiomeLayout.isPoint(entry.parameters().depth(), 0.0F) && !entry.rule().equals("added-surface")) {
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
            if (h.erosion().max() <= ABOVE_MOUNTAINS) continue;
            long temperature = mid(h.temperature());
            if (temperature >= Climate.quantizeCoord(-0.55F) && temperature <= Climate.quantizeCoord(-0.375F)) continue;
            // erosion above the mountainside limit and temperature outside the frozen band: only the humid rule applies
            // slice edges are other slices' edges too, where the nearest entry is a tie: stay strictly inside
            Climate.TargetPoint point = point(temperature, Climate.quantizeCoord(0.29F), mid(h.continentalness()),
                    Math.max(mid(h.erosion()), ABOVE_MOUNTAINS), mid(h.weirdness()));
            assertEquals(CustomOverworldBiomes.DARK_BIRCH_FOREST, layout.findValue(point), "at " + point);
            checked++;
        }
        assertTrue(checked > 0, "no birch forest slice crosses humidity 0.29");
    }

    /**
     * The weirdness rules: where the weirdness is positive and the temperature negative, birch forests turn into sparse
     * dappled forest, and nearer the taigas into dappled taiga, old growth birch into vanilla's dappled forest, and dark
     * forest into dark taiga; vanilla's dappled forests turn into flower forests. Checked inside vanilla's slices away
     * from the transitions that come first (mountainside, the humid band for birch and dark forest, the frozen band for
     * birch), and every new biome must turn up. Also prints where vanilla puts these biomes.
     */
    @Test
    void weirdColdForestsChange() {
        Climate.ParameterList<ResourceKey<Biome>> layout = entries(toPairs(PvBiomeLayout.build()));
        Map<String, Integer> checked = new TreeMap<>();
        Map<String, Set<String>> placement = new TreeMap<>();
        for (Pair<ParameterPoint, ResourceKey<Biome>> entry : vanilla) {
            ParameterPoint h = entry.getFirst();
            ResourceKey<Biome> biome = entry.getSecond();
            if (!PvBiomeLayout.isPoint(h.depth(), 0.0F)) continue;
            boolean birch = biome == Biomes.BIRCH_FOREST, oldGrowth = biome == Biomes.OLD_GROWTH_BIRCH_FOREST;
            boolean dark = biome == Biomes.DARK_FOREST, dappled = CustomOverworldBiomes.DAPPLED_FOREST.equals(biome);
            if (!birch && !oldGrowth && !dark && !dappled && biome != Biomes.FLOWER_FOREST) continue;
            placement.computeIfAbsent(biome.identifier().getPath(), k -> new TreeSet<>()).add(String.format(Locale.ROOT,
                    "T %s H %s W %s", range(h.temperature()), range(h.humidity()), range(h.weirdness())));
            Long erosion = inside(h.erosion(), Climate.unquantizeCoord(ABOVE_MOUNTAINS), 1.0F);
            if (dappled) {
                Climate.TargetPoint point = point(mid(h.temperature()), mid(h.humidity()), mid(h.continentalness()), mid(h.erosion()), mid(h.weirdness()));
                assertEquals(Biomes.FLOWER_FOREST, layout.findValue(point), "dappled forest at " + point);
                checked.merge("dappled forest -> flower forest", 1, Integer::sum);
                continue;
            }
            Long weirdness = inside(h.weirdness(), PvBiomeLayout.DAPPLED_WEIRDNESS, 1.0F);
            if (!birch && !oldGrowth && !dark || erosion == null || weirdness == null) continue;
            Long humidity = birch || dark ? either(inside(h.humidity(), -1.0F, 0.275F), inside(h.humidity(), 0.35F, 1.0F))
                    : Long.valueOf(mid(h.humidity()));
            if (humidity == null) continue;
            float cold = PvBiomeLayout.DAPPLED_COLD;
            Long coldPoint = birch ? either(inside(h.temperature(), -0.375F, cold), inside(h.temperature(), -1.0F, Math.min(cold, -0.55F)))
                    : inside(h.temperature(), -1.0F, cold);
            Long coolPoint = inside(h.temperature(), cold, PvBiomeLayout.DAPPLED_COOL);
            // The cold old growth birch on level ground (erosion 0.05 to 0.3) is the rocky birch forest
            boolean level = erosion >= Climate.quantizeCoord(PvBiomeLayout.ROCKY_EROSION_MIN) && erosion < Climate.quantizeCoord(0.3F);
            ResourceKey<Biome> coldTarget = birch ? CustomOverworldBiomes.DAPPLED_TAIGA
                    : dark ? CustomOverworldBiomes.DARK_TAIGA_FOREST : level ? CustomOverworldBiomes.ROCKY_BIRCH_FOREST : CustomOverworldBiomes.DAPPLED_FOREST;
            ResourceKey<Biome> coolTarget = birch ? CustomOverworldBiomes.SPARSE_DAPPLED_FOREST
                    : dark ? CustomOverworldBiomes.DARK_TAIGA_FOREST : CustomOverworldBiomes.DAPPLED_FOREST;
            String name = birch ? "birch forest" : dark ? "dark forest" : "old growth birch forest";
            if (coldPoint != null) {
                Climate.TargetPoint point = point(coldPoint, humidity, mid(h.continentalness()), erosion, weirdness);
                assertEquals(coldTarget, layout.findValue(point), name + " at " + point);
                checked.merge(name + " cold -> " + coldTarget.identifier().getPath(), 1, Integer::sum);
            }
            if (coolPoint != null) {
                Climate.TargetPoint point = point(coolPoint, humidity, mid(h.continentalness()), erosion, weirdness);
                assertEquals(coolTarget, layout.findValue(point), name + " at " + point);
                checked.merge(name + " cool -> " + coolTarget.identifier().getPath(), 1, Integer::sum);
            }
        }
        placement.forEach((biome, slices) -> System.out.printf(Locale.ROOT, "[dappled] vanilla %s, %d surface slices: %s%n", biome, slices.size(), slices));
        System.out.println("[dappled] checked slices: " + checked);
        assertTrue(checked.containsKey("dappled forest -> flower forest"), "vanilla has no dappled forest at the surface");
        assertTrue(checked.containsKey("birch forest cold -> dappled_taiga"), "no birch forest turns into dappled taiga");
        assertTrue(checked.containsKey("birch forest cool -> sparse_dappled_forest"), "no birch forest turns into sparse dappled forest");
        assertTrue(checked.keySet().stream().anyMatch(key -> key.startsWith("dark forest ")), "no dark forest turns into dark taiga");
    }

    /**
     * The warm band: the cool edge of savannas and jungles (from their start at 0.2 up to {@link PvBiomeLayout#WARM_BAND_MAX})
     * turns into the transition to what lies beyond it, and the rest of each slice keeps its biome. Probed above the
     * mountainsides' erosion and away from the humid band.
     */
    @Test
    void warmBandTakesTheCoolEdgeOfWarmBiomes() {
        Climate.ParameterList<ResourceKey<Biome>> layout = entries(toPairs(PvBiomeLayout.build()));
        long inBand = Climate.quantizeCoord(0.25F), beyond = Climate.quantizeCoord(0.4F);
        Map<String, Integer> checked = new TreeMap<>();
        for (Pair<ParameterPoint, ResourceKey<Biome>> entry : vanilla) {
            ParameterPoint h = entry.getFirst();
            ResourceKey<Biome> biome = entry.getSecond();
            if (!PvBiomeLayout.isPoint(h.depth(), 0.0F) || h.erosion().max() <= ABOVE_MOUNTAINS) continue;
            boolean savanna = biome == Biomes.SAVANNA || biome == Biomes.SAVANNA_PLATEAU;
            boolean jungle = biome == Biomes.JUNGLE || biome == Biomes.SPARSE_JUNGLE || biome == Biomes.BAMBOO_JUNGLE;
            if (!savanna && !jungle) continue;
            assertTrue(h.temperature().min() == Climate.quantizeCoord(0.2F), () -> biome + " no longer starts at 0.2: " + h);
            boolean wet = h.humidity().min() >= Climate.quantizeCoord(PvBiomeLayout.WARM_HUMIDITY_SPLIT);
            long erosion = Math.max(mid(h.erosion()), ABOVE_MOUNTAINS);
            // The level stretches of a savanna's cool edge are rocky plains
            boolean level = erosion >= Climate.quantizeCoord(PvBiomeLayout.ROCKY_EROSION_MIN);
            ResourceKey<Biome> expected = savanna ? (level ? CustomOverworldBiomes.ROCKY_PLAINS : CustomOverworldBiomes.TUSCAN_GROVE)
                    : wet || biome == Biomes.BAMBOO_JUNGLE ? CustomOverworldBiomes.DARK_JUNGLE_FOREST : CustomOverworldBiomes.WARM_OAK_FOREST;
            Climate.TargetPoint band = point(inBand, mid(h.humidity()), mid(h.continentalness()), erosion, mid(h.weirdness()));
            Climate.TargetPoint core = point(beyond, mid(h.humidity()), mid(h.continentalness()), erosion, mid(h.weirdness()));
            assertEquals(expected, layout.findValue(band), biome.identifier().getPath() + " band at " + band);
            assertEquals(biome, layout.findValue(core), biome.identifier().getPath() + " core at " + core);
            checked.merge(biome.identifier().getPath() + " -> " + expected.identifier().getPath(), 1, Integer::sum);
        }
        System.out.println("[warm] checked slices: " + checked);
        for (ResourceKey<Biome> target : List.of(CustomOverworldBiomes.TUSCAN_GROVE, CustomOverworldBiomes.WARM_OAK_FOREST, CustomOverworldBiomes.DARK_JUNGLE_FOREST)) {
            assertTrue(checked.keySet().stream().anyMatch(key -> key.endsWith(target.identifier().getPath())), "nothing turns into " + target);
        }
    }

    /** The level stretches (erosion 0.05 and more) of the savannas' cool edge are rocky plains; the Tuscan grove keeps the rest. */
    @Test
    void rockyPlainsTakeTheLevelCoolEdgeOfSavannas() {
        Climate.ParameterList<ResourceKey<Biome>> layout = entries(toPairs(PvBiomeLayout.build()));
        long band = Climate.quantizeCoord(0.25F), beyond = Climate.quantizeCoord(0.4F);
        int rocky = 0, grove = 0;
        for (Pair<ParameterPoint, ResourceKey<Biome>> entry : vanilla) {
            ParameterPoint h = entry.getFirst();
            ResourceKey<Biome> biome = entry.getSecond();
            if (!PvBiomeLayout.isPoint(h.depth(), 0.0F) || (biome != Biomes.SAVANNA && biome != Biomes.SAVANNA_PLATEAU)) continue;
            Long level = inside(h.erosion(), PvBiomeLayout.ROCKY_EROSION_MIN, 1.0F);
            Long hilly = inside(h.erosion(), Climate.unquantizeCoord(ABOVE_MOUNTAINS), PvBiomeLayout.ROCKY_EROSION_MIN);
            if (level != null) {
                assertEquals(CustomOverworldBiomes.ROCKY_PLAINS, layout.findValue(point(band, mid(h.humidity()), mid(h.continentalness()), level, mid(h.weirdness()))),
                        "the level cool edge of " + h);
                assertEquals(biome, layout.findValue(point(beyond, mid(h.humidity()), mid(h.continentalness()), level, mid(h.weirdness()))), "the savanna beyond " + h);
                rocky++;
            }
            if (hilly != null) {
                assertEquals(CustomOverworldBiomes.TUSCAN_GROVE, layout.findValue(point(band, mid(h.humidity()), mid(h.continentalness()), hilly, mid(h.weirdness()))),
                        "the hilly cool edge of " + h);
                grove++;
            }
        }
        assertTrue(rocky > 0 && grove > 0, "both: " + rocky + " rocky, " + grove + " grove");
    }

    /**
     * Just warmer than the warm band (temperature 0.3 to 0.36), the level jungle is rocky: out of the 3D-noise terrain's
     * climate (humidity 0 to 0.3 with erosion 0.3 to 0.6).
     */
    @Test
    void rockyJungleLiesBetweenTheWarmBandAndTheJungle() {
        Climate.ParameterList<ResourceKey<Biome>> layout = entries(toPairs(PvBiomeLayout.build()));
        long rockyBand = Climate.quantizeCoord(0.33F), beyond = Climate.quantizeCoord(0.45F);
        int rocky = 0;
        for (Pair<ParameterPoint, ResourceKey<Biome>> entry : vanilla) {
            ParameterPoint h = entry.getFirst();
            ResourceKey<Biome> biome = entry.getSecond();
            boolean jungle = biome == Biomes.JUNGLE || biome == Biomes.SPARSE_JUNGLE || biome == Biomes.BAMBOO_JUNGLE;
            if (!PvBiomeLayout.isPoint(h.depth(), 0.0F) || !jungle) continue;
            long humidity = mid(h.humidity());
            boolean dry = humidity < Climate.quantizeCoord(0.3F);
            Long level = dry ? either(inside(h.erosion(), PvBiomeLayout.ROCKY_EROSION_MIN, 0.3F), inside(h.erosion(), 0.6F, 1.0F))
                    : inside(h.erosion(), PvBiomeLayout.ROCKY_EROSION_MIN, 1.0F);
            if (level == null) continue;
            assertEquals(CustomOverworldBiomes.ROCKY_JUNGLE, layout.findValue(point(rockyBand, humidity, mid(h.continentalness()), level, mid(h.weirdness()))),
                    "the level jungle just past the warm band, " + h);
            assertEquals(biome, layout.findValue(point(beyond, humidity, mid(h.continentalness()), level, mid(h.weirdness()))), "the jungle beyond " + h);
            if (dry) {
                Long noise = inside(h.erosion(), 0.3F, 0.6F);
                if (noise != null) assertFalse(layout.findValue(point(rockyBand, humidity, mid(h.continentalness()), noise, mid(h.weirdness())))
                        == CustomOverworldBiomes.ROCKY_JUNGLE, "no rocky jungle in the 3D-noise climate, " + h);
            }
            rocky++;
        }
        assertTrue(rocky > 0, "no jungle slice turns rocky");
    }

    /** The cold side of old growth birch forests (temperature below -0.075), level (erosion 0.05 to 0.3), is the rocky birch forest. */
    @Test
    void rockyBirchForestTakesTheColdLevelOldGrowthBirch() {
        Climate.ParameterList<ResourceKey<Biome>> layout = entries(toPairs(PvBiomeLayout.build()));
        long cold = Climate.quantizeCoord(-0.11F), cool = Climate.quantizeCoord(-0.03F);
        int rocky = 0;
        for (Pair<ParameterPoint, ResourceKey<Biome>> entry : vanilla) {
            ParameterPoint h = entry.getFirst();
            if (!PvBiomeLayout.isPoint(h.depth(), 0.0F) || entry.getSecond() != Biomes.OLD_GROWTH_BIRCH_FOREST) continue;
            Long level = inside(h.erosion(), PvBiomeLayout.ROCKY_EROSION_MIN, 0.3F);
            if (level == null) continue;
            long weirdness = Math.max(mid(h.weirdness()), Climate.quantizeCoord(PvBiomeLayout.DAPPLED_WEIRDNESS) + EDGE);
            assertEquals(CustomOverworldBiomes.ROCKY_BIRCH_FOREST, layout.findValue(point(cold, mid(h.humidity()), mid(h.continentalness()), level, weirdness)),
                    "the cold level old growth birch of " + h);
            assertFalse(layout.findValue(point(cool, mid(h.humidity()), mid(h.continentalness()), level, weirdness)) == CustomOverworldBiomes.ROCKY_BIRCH_FOREST,
                    "only the cold side, " + h);
            rocky++;
        }
        assertTrue(rocky > 0, "no old growth birch turns rocky");
    }

    /** Every swamp has a rim of humid oak forest where its erosion begins, up to {@link PvBiomeLayout#SWAMP_RIM_MAX}. */
    @Test
    void swampsHaveAHumidOakRim() {
        Climate.ParameterList<ResourceKey<Biome>> layout = entries(toPairs(PvBiomeLayout.build()));
        long rim = Climate.quantizeCoord((0.55F + PvBiomeLayout.SWAMP_RIM_MAX) / 2), heart = Climate.quantizeCoord(0.8F);
        int checked = 0;
        for (Pair<ParameterPoint, ResourceKey<Biome>> entry : vanilla) {
            ParameterPoint h = entry.getFirst();
            if (entry.getSecond() != Biomes.SWAMP || !PvBiomeLayout.isPoint(h.depth(), 0.0F)) continue;
            assertEquals(Climate.quantizeCoord(0.55F), h.erosion().min(), "swamps no longer start at erosion 0.55");
            Climate.TargetPoint atRim = point(mid(h.temperature()), mid(h.humidity()), mid(h.continentalness()), rim, mid(h.weirdness()));
            Climate.TargetPoint inside = point(mid(h.temperature()), mid(h.humidity()), mid(h.continentalness()), heart, mid(h.weirdness()));
            assertEquals(CustomOverworldBiomes.HUMID_OAK_FOREST, layout.findValue(atRim), "rim at " + atRim);
            assertEquals(Biomes.SWAMP, layout.findValue(inside), "swamp at " + inside);
            checked++;
        }
        assertTrue(checked > 0, "no swamp slice at the surface");
    }

    /** The cold half of every swamp is a cold swamp, which has no rim; the warmer half keeps its swamp and its rim. */
    @Test
    void theColdHalfOfSwampsIsColdSwamp() {
        Climate.ParameterList<ResourceKey<Biome>> layout = entries(toPairs(PvBiomeLayout.build()));
        long cold = Climate.quantizeCoord(-0.3F), mild = Climate.quantizeCoord(0.0F);
        long rim = Climate.quantizeCoord((0.55F + PvBiomeLayout.SWAMP_RIM_MAX) / 2), heart = Climate.quantizeCoord(0.8F);
        int checked = 0;
        for (Pair<ParameterPoint, ResourceKey<Biome>> entry : vanilla) {
            ParameterPoint h = entry.getFirst();
            if (entry.getSecond() != Biomes.SWAMP || !PvBiomeLayout.isPoint(h.depth(), 0.0F)) continue;
            for (long erosion : new long[]{rim, heart}) {
                assertEquals(CustomOverworldBiomes.COLD_SWAMP, layout.findValue(point(cold, mid(h.humidity()), mid(h.continentalness()), erosion, mid(h.weirdness()))),
                        "a cold swamp at erosion " + erosion);
            }
            assertEquals(Biomes.SWAMP, layout.findValue(point(mild, mid(h.humidity()), mid(h.continentalness()), heart, mid(h.weirdness()))), "a mild swamp");
            checked++;
        }
        assertTrue(checked > 0, "no swamp slice at the surface");
    }

    /**
     * The dark forest's warm side (temperature {@link PvBiomeLayout#WARM_DARK_FOREST_MIN} up to its 0.2) is the warm dark
     * forest, between the dark forest and the dark jungle forest; the humid band keeps its strip of dark birch forest; the
     * cool side is the dark forest still.
     */
    @Test
    void theDarkForestsWarmSideIsTheWarmDarkForest() {
        Climate.ParameterList<ResourceKey<Biome>> layout = entries(toPairs(PvBiomeLayout.build()));
        int checked = 0;
        for (Pair<ParameterPoint, ResourceKey<Biome>> entry : vanilla) {
            ParameterPoint h = entry.getFirst();
            if (entry.getSecond() != Biomes.DARK_FOREST || !PvBiomeLayout.isPoint(h.depth(), 0.0F) || h.erosion().max() <= ABOVE_MOUNTAINS) continue;
            Long warm = inside(h.temperature(), 0.08F, 0.19F), cool = inside(h.temperature(), -0.14F, -0.01F), dry = inside(h.humidity(), 0.4F, 1.0F);
            Long strip = inside(h.humidity(), 0.31F, 0.34F);
            long erosion = Math.max(mid(h.erosion()), ABOVE_MOUNTAINS), continentalness = mid(h.continentalness()), weirdness = mid(h.weirdness());
            if (warm == null || cool == null || dry == null) continue;
            assertEquals(CustomOverworldBiomes.WARM_DARK_FOREST, layout.findValue(point(warm, dry, continentalness, erosion, weirdness)), "the warm side at " + h);
            ResourceKey<Biome> coolSide = layout.findValue(point(cool, dry, continentalness, erosion, weirdness));
            assertTrue(coolSide != CustomOverworldBiomes.WARM_DARK_FOREST, "the cool side is warm at " + h);
            if (strip != null) {
                assertEquals(CustomOverworldBiomes.DARK_BIRCH_FOREST, layout.findValue(point(warm, strip, continentalness, erosion, weirdness)), "the humid strip at " + h);
            }
            checked++;
        }
        assertTrue(checked > 0, "no dark forest slice checked");
    }

    /**
     * The warm swamps' drier part (humidity {@link PvBiomeLayout#WETLAND_HUMIDITY_MAX} and under) past the rim is the grassy
     * wetlands; the wetter part stays swamp, the rim humid oak forest whatever the humidity, the cold half cold swamp.
     */
    @Test
    void theDrierWarmSwampIsGrassyWetlands() {
        Climate.ParameterList<ResourceKey<Biome>> layout = entries(toPairs(PvBiomeLayout.build()));
        long rim = Climate.quantizeCoord((0.55F + PvBiomeLayout.SWAMP_RIM_MAX) / 2), heart = Climate.quantizeCoord(0.8F);
        long dry = Climate.quantizeCoord(-0.4F), wet = Climate.quantizeCoord(0.1F), cold = Climate.quantizeCoord(-0.3F);
        int checked = 0;
        for (Pair<ParameterPoint, ResourceKey<Biome>> entry : vanilla) {
            ParameterPoint h = entry.getFirst();
            if (entry.getSecond() != Biomes.SWAMP || !PvBiomeLayout.isPoint(h.depth(), 0.0F)) continue;
            long warm = inside(h.temperature(), -0.1F, 0.19F), continentalness = mid(h.continentalness()), weirdness = mid(h.weirdness());
            assertEquals(CustomOverworldBiomes.GRASSY_WETLANDS, layout.findValue(point(warm, dry, continentalness, heart, weirdness)), "the drier swamp at " + h);
            assertEquals(Biomes.SWAMP, layout.findValue(point(warm, wet, continentalness, heart, weirdness)), "the wetter swamp at " + h);
            assertEquals(CustomOverworldBiomes.HUMID_OAK_FOREST, layout.findValue(point(warm, dry, continentalness, rim, weirdness)), "the drier rim at " + h);
            assertEquals(CustomOverworldBiomes.COLD_SWAMP, layout.findValue(point(cold, dry, continentalness, heart, weirdness)), "the cold half, dry, at " + h);
            checked++;
        }
        assertTrue(checked > 0, "no swamp slice at the surface");
    }

    /** The mangrove swamp's rim (erosion up to {@link PvBiomeLayout#SWAMP_RIM_MAX}) and its drier part are the mangrove marsh. */
    @Test
    void theMangroveSwampsRimAndDrierPartAreMarsh() {
        Climate.ParameterList<ResourceKey<Biome>> layout = entries(toPairs(PvBiomeLayout.build()));
        long rim = Climate.quantizeCoord((0.55F + PvBiomeLayout.SWAMP_RIM_MAX) / 2), heart = Climate.quantizeCoord(0.8F);
        long dry = Climate.quantizeCoord(-0.4F), wet = Climate.quantizeCoord(0.2F);
        int checked = 0;
        for (Pair<ParameterPoint, ResourceKey<Biome>> entry : vanilla) {
            ParameterPoint h = entry.getFirst();
            if (entry.getSecond() != Biomes.MANGROVE_SWAMP || !PvBiomeLayout.isPoint(h.depth(), 0.0F)) continue;
            long temperature = mid(h.temperature()), continentalness = mid(h.continentalness()), weirdness = mid(h.weirdness());
            assertEquals(Biomes.MANGROVE_SWAMP, layout.findValue(point(temperature, wet, continentalness, heart, weirdness)), "the wetter mangrove swamp at " + h);
            assertEquals(CustomOverworldBiomes.MANGROVE_MARSH, layout.findValue(point(temperature, dry, continentalness, heart, weirdness)), "the drier part at " + h);
            assertEquals(CustomOverworldBiomes.MANGROVE_MARSH, layout.findValue(point(temperature, wet, continentalness, rim, weirdness)), "the rim at " + h);
            checked++;
        }
        assertTrue(checked > 0, "no mangrove swamp slice at the surface");
    }

    /** Above freezing, no windswept biome nor old growth birch forest is left at the surface. */
    @Test
    void noWindsweptNorOldGrowthBirchAboveFreezing() {
        Climate.ParameterList<ResourceKey<Biome>> layout = entries(toPairs(PvBiomeLayout.build()));
        Set<ResourceKey<Biome>> gone = Set.of(Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST,
                Biomes.WINDSWEPT_SAVANNA, Biomes.OLD_GROWTH_BIRCH_FOREST);
        Random random = new Random(20261107L);
        Map<String, Integer> found = new TreeMap<>();
        for (int i = 0; i < 100_000; i++) {
            float temperature = PvBiomeLayout.TEMPERATE_MIN + 0.005F + (1.0F - PvBiomeLayout.TEMPERATE_MIN - 0.005F) * random.nextFloat();
            Climate.TargetPoint point = Climate.target(temperature, uniform(random), uniform(random), uniform(random), 0.0F, uniform(random));
            ResourceKey<Biome> biome = layout.findValue(point);
            found.merge(biome.identifier().getPath(), 1, Integer::sum);
            assertFalse(gone.contains(biome), () -> biome + " above freezing at " + point);
        }
        // and they are still there below it, where they were asked to stay
        boolean windsweptBelow = false;
        for (int i = 0; i < 100_000 && !windsweptBelow; i++) {
            Climate.TargetPoint point = Climate.target(-0.1F - 0.3F * random.nextFloat(), uniform(random), uniform(random), uniform(random), 0.0F, uniform(random));
            windsweptBelow = gone.contains(layout.findValue(point));
        }
        assertTrue(windsweptBelow, "no windswept biome left below freezing either");
        assertTrue(found.containsKey("tuscan_grove") && found.containsKey("warm_oak_forest") && found.containsKey("dark_jungle_forest"),
                "a warm transition is missing above freezing: " + found);
    }

    private static final Set<ResourceKey<Biome>> WINDSWEPT = Set.of(Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST,
            Biomes.WINDSWEPT_SAVANNA);

    /** In the 3D-noise zone, below 0: the windswept biome that fits (the spec's table): gravelly hills, hills, forest. */
    private static ResourceKey<Biome> windsweptThatFits(float temperature, float humidity) {
        if (humidity >= 0.1F) return Biomes.WINDSWEPT_FOREST;
        return temperature < -0.15F ? Biomes.WINDSWEPT_GRAVELLY_HILLS : Biomes.WINDSWEPT_HILLS;
    }

    /**
     * What lies beside a windswept slice outside the zone, below 0 (vanilla's middle biomes by temperature and humidity;
     * from -0.15 up the temperate rule's: hills and savannas to plains, forests to forest).
     */
    private static ResourceKey<Biome> besideWindswept(ResourceKey<Biome> windswept, float temperature, float humidity) {
        if (temperature < -0.45F) return humidity < 0.1F ? Biomes.SNOWY_PLAINS : humidity < 0.3F ? Biomes.SNOWY_TAIGA : Biomes.TAIGA;
        if (temperature < -0.15F) {
            return humidity < -0.1F ? Biomes.PLAINS : humidity < 0.1F ? Biomes.FOREST : humidity < 0.3F ? Biomes.TAIGA : Biomes.OLD_GROWTH_SPRUCE_TAIGA;
        }
        return windswept == Biomes.WINDSWEPT_FOREST ? Biomes.FOREST : Biomes.PLAINS;
    }

    /**
     * The windswept biomes are the rugged terrain's (the 3D-noise zone): where vanilla puts one, inside the zone where its
     * window is at half strength or more (humidity 0.025 to 0.275, continentalness 0.075 or more) and below 0, it is the
     * windswept biome that fits; anywhere else it is what lies beside it.
     */
    @Test
    void windsweptSlicesFollowTheZone() {
        Climate.ParameterList<ResourceKey<Biome>> layout = entries(toPairs(PvBiomeLayout.build()));
        Map<String, Integer> checked = new TreeMap<>();
        for (Pair<ParameterPoint, ResourceKey<Biome>> entry : vanilla) {
            ParameterPoint h = entry.getFirst();
            ResourceKey<Biome> biome = entry.getSecond();
            if (!PvBiomeLayout.isPoint(h.depth(), 0.0F) || !WINDSWEPT.contains(biome)) continue;
            Long cool = inside(h.temperature(), -1.0F, -0.02F), inland = inside(h.continentalness(), 0.085F, 1.0F);
            if (cool == null) continue;
            long erosion = mid(h.erosion()), weirdness = mid(h.weirdness());
            float t = Climate.unquantizeCoord(cool);
            Long inBox = inside(h.humidity(), 0.04F, 0.26F);
            if (inBox != null && inland != null) {
                Climate.TargetPoint point = point(cool, inBox, inland, erosion, weirdness);
                ResourceKey<Biome> expected = windsweptThatFits(t, Climate.unquantizeCoord(inBox));
                assertEquals(expected, layout.findValue(point), biome.identifier().getPath() + " in the zone at " + point);
                checked.merge(biome.identifier().getPath() + " -> " + expected.identifier().getPath(), 1, Integer::sum);
            }
            // Outside the box: too dry or too humid, or off the coast
            List<Long> outside = new ArrayList<>();
            Long dry = inside(h.humidity(), -1.0F, 0.015F), wet = inside(h.humidity(), 0.285F, 1.0F);
            if (dry != null) outside.add(dry);
            if (wet != null) outside.add(wet);
            for (long humidity : outside) {
                if (inland == null) continue;
                Climate.TargetPoint point = point(cool, humidity, inland, erosion, weirdness);
                ResourceKey<Biome> expected = besideWindswept(biome, t, Climate.unquantizeCoord(humidity));
                assertEquals(expected, layout.findValue(point), biome.identifier().getPath() + " outside the zone at " + point);
                checked.merge(biome.identifier().getPath() + " -> " + expected.identifier().getPath(), 1, Integer::sum);
            }
            // Off the coast: where the window is under half strength (continentalness 0.03 to 0.075), the terrain is still plain
            Long coast = inside(h.continentalness(), -1.0F, 0.065F);
            if (coast != null) {
                long humidity = inBox != null ? inBox : mid(h.humidity());
                Climate.TargetPoint point = point(cool, humidity, coast, erosion, weirdness);
                ResourceKey<Biome> expected = besideWindswept(biome, t, Climate.unquantizeCoord(humidity));
                assertEquals(expected, layout.findValue(point), biome.identifier().getPath() + " off the coast at " + point);
                checked.merge(biome.identifier().getPath() + " -> " + expected.identifier().getPath(), 1, Integer::sum);
            }
        }
        System.out.println("[windswept] checked slices: " + checked);
        for (ResourceKey<Biome> fits : List.of(Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_GRAVELLY_HILLS, Biomes.WINDSWEPT_FOREST)) {
            assertTrue(checked.keySet().stream().anyMatch(key -> key.endsWith("-> " + fits.identifier().getPath())), "nowhere is " + fits);
        }
        for (ResourceKey<Biome> beside : List.of(Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.TAIGA, Biomes.PLAINS, Biomes.FOREST, Biomes.OLD_GROWTH_SPRUCE_TAIGA)) {
            assertTrue(checked.keySet().stream().anyMatch(key -> key.endsWith("-> " + beside.identifier().getPath())), "no windswept slice turns into " + beside);
        }
    }

    /** Over random climates, a windswept biome is only ever in the zone's box, below 0, or on a mountainside. */
    @Test
    void windsweptBiomesStandOnlyInTheZone() {
        Climate.ParameterList<ResourceKey<Biome>> layout = entries(toPairs(PvBiomeLayout.build()));
        Random random = new Random(20261112L);
        int found = 0;
        for (int i = 0; i < 400_000; i++) {
            float temperature = uniform(random), humidity = uniform(random), continentalness = uniform(random), erosion = uniform(random), weirdness = uniform(random);
            ResourceKey<Biome> biome = layout.findValue(Climate.target(temperature, humidity, continentalness, erosion, 0.0F, weirdness));
            // The taigas' mountainsides have a windswept forest of their own (the mountainside rule): not this rule's
            if (!WINDSWEPT.contains(biome) || erosion < -0.3F) continue;
            found++;
            String where = String.format(Locale.ROOT, "%s at T %.3f H %.3f C %.3f E %.3f", biome.identifier().getPath(), temperature, humidity, continentalness, erosion);
            assertTrue(temperature < 0.003F, where);
            assertTrue(humidity >= 0.022F && humidity <= 0.278F, where);
            assertTrue(continentalness >= 0.072F, where);
            assertTrue(erosion >= 0.447F && erosion <= 0.553F, where);
        }
        assertTrue(found >= 200, "only " + found + " windswept points found");
    }

    /** The middle of the part of {@code range} between {@code from} and {@code to}, or null if it has no width there. */
    private static Long inside(Climate.Parameter range, float from, float to) {
        long lo = Math.max(range.min(), Climate.quantizeCoord(from)), hi = Math.min(range.max(), Climate.quantizeCoord(to));
        return lo < hi ? (lo + hi) / 2 : null;
    }

    private static Long either(Long first, Long second) {
        return first != null ? first : second;
    }

    private static String range(Climate.Parameter range) {
        return String.format(Locale.ROOT, "%.2f..%.2f", Climate.unquantizeCoord(range.min()), Climate.unquantizeCoord(range.max()));
    }

    /** Q4: a mountainside covers only erosion below its limit; the forest keeps the rest of its slice. */
    @Test
    void mountainsideStopsAtItsErosionLimit() {
        Climate.ParameterList<ResourceKey<Biome>> layout = entries(toPairs(PvBiomeLayout.build()));
        long limit = PvBiomeLayout.MOUNTAIN_EROSION_MAX;
        long inland = Climate.quantizeCoord(0.1F);
        int checked = 0;
        for (Pair<ParameterPoint, ResourceKey<Biome>> entry : vanilla) {
            ParameterPoint h = entry.getFirst();
            if (entry.getSecond() != Biomes.FOREST || !PvBiomeLayout.isPoint(h.depth(), 0.0F)) continue;
            if (h.erosion().min() >= limit - EDGE || h.erosion().max() <= limit + EDGE) continue;
            if (h.continentalness().max() <= inland) continue;
            if (h.weirdness().min() >= Climate.quantizeCoord(-0.3F) && h.weirdness().max() <= Climate.quantizeCoord(0.3F)) continue;
            long temperature = mid(h.temperature());
            if (temperature >= Climate.quantizeCoord(-0.55F) && temperature <= Climate.quantizeCoord(-0.375F)) continue;
            long continentalness = Math.max(mid(h.continentalness()), inland);
            ResourceKey<Biome> below = layout.findValue(point(temperature, mid(h.humidity()), continentalness, limit - EDGE, mid(h.weirdness())));
            ResourceKey<Biome> above = layout.findValue(point(temperature, mid(h.humidity()), continentalness, limit + EDGE, mid(h.weirdness())));
            assertTrue(below == CustomOverworldBiomes.MOUNTAINSIDE_FOREST || below == CustomOverworldBiomes.MOUNTAINSIDE_FOREST_WARM, "below: " + below);
            assertEquals(Biomes.FOREST, above, "above");
            checked++;
        }
        assertTrue(checked > 0, "no forest slice crosses the mountainside erosion limit outside river valleys");
    }

    /**
     * How far the new layout moves biomes compared with the old mixin, over random climate points at several depths.
     * The surface changes by design (Q2, Q4, Q7), and Pale Grotto is a new cave addition. Other caves must not change.
     * Where two entries are exactly as near, the search tree's layout decides, so a different pick that ties in either
     * layout isn't counted as a change.
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
                if (after == CustomOverworldBiomes.PALE_GROTTO || after == CustomOverworldBiomes.MUSHROOM_CAVES) continue;
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

    /**
     * The mushroom caves lie under the mushroom fields from the surface caves down, whatever the rest of the climate,
     * and nowhere else; the islands themselves stay mushroom fields.
     */
    @Test
    void mushroomCavesLieUnderTheMushroomFields() {
        List<PvBiomeLayout.Entry> built = PvBiomeLayout.build();
        Climate.ParameterList<ResourceKey<Biome>> layout = entries(toPairs(built));
        Random random = new Random(20261027L);
        for (int i = 0; i < 4_000; i++) {
            float temperature = uniform(random), humidity = uniform(random), erosion = uniform(random), weirdness = uniform(random);
            // Inland of the deep ocean: the caves near an island's shores can still be the islands' (vanilla's caves leave
            // the coldest climates far from every cave entry)
            float island = -1.19F + 0.13F * random.nextFloat(), inland = -0.5F + 1.5F * random.nextFloat();
            float depth = 0.12F + 0.86F * random.nextFloat();
            Climate.TargetPoint under = Climate.target(temperature, humidity, island, erosion, depth, weirdness);
            assertEquals(CustomOverworldBiomes.MUSHROOM_CAVES, layout.findValue(under),
                    () -> "under the mushroom fields at " + under + ", nearest " + nearestEntries(built, under));
            assertEquals(Biomes.MUSHROOM_FIELDS, layout.findValue(Climate.target(temperature, humidity, island, erosion, 0.0F, weirdness)),
                    "on the mushroom fields");
            assertTrue(layout.findValue(Climate.target(temperature, humidity, inland, erosion, depth, weirdness)) != CustomOverworldBiomes.MUSHROOM_CAVES,
                    "mushroom caves away from the islands, at continentalness " + inland);
        }
    }

    /** The entries nearest to the point, with their rules and parameters, for a failure's message. */
    private static List<String> nearestEntries(List<PvBiomeLayout.Entry> layout, Climate.TargetPoint point) {
        long best = layout.stream().mapToLong(e -> squaredDistance(e.parameters(), point)).min().orElse(0L);
        return layout.stream().filter(e -> squaredDistance(e.parameters(), point) == best)
                .map(e -> e.biome().identifier().getPath() + " (" + e.rule() + ") " + e.parameters()).toList();
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
