package frootloops.versus.mod.environment.worldgen.aquifer;

import frootloops.versus.mod.environment.worldgen.TerrainPass;
import frootloops.versus.mod.environment.worldgen.WorldgenTestData;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PvAquifer} in vanilla's terrain pass ({@link TerrainPass}), on this mod's real data:
 * <ul>
 *   <li>its {@link Lattice}s on the cell grid give what the pass's {@code interpolated} gives, bit for bit;</li>
 *   <li>it answers the terrain pass and carvers alike (before Phase 2b they got different F and S);</li>
 *   <li>its decisions against the terrain pass's before Phase 2b, when the router's F and S were the JSON that stays
 *   in the mod's data, by band, with each change put down to F or S.</li>
 * </ul>
 */
class AquiferTerrainPassTest {

    private static final long SEED = 8675309L;
    /** The sea band is y -31..63 ({@code y > SEA_BAND_MIN_Y}), the basin band y -3..31. */
    private static final int[][] BANDS = {{-31, -4}, {-3, 7}, {8, 31}, {32, 47}, {48, 63}};
    private static final AquiferSampler.FluidLevelSampler NO_FLUID_LEVELS = (x, y, z) -> {
        throw new UnsupportedOperationException("the test passes lavaLevel itself");
    };

    @Test
    void latticesGiveTheTerrainPassInterpolation() {
        NoiseConfig config = WorldgenTestData.noiseConfig(SEED);
        ChunkGeneratorSettings settings = WorldgenTestData.pvSettings();
        AquiferInputs inputs = AquiferInputs.of(config, settings);
        GenerationShapeConfig shape = settings.generationShapeConfig();
        int minY = shape.minimumY(), maxY = shape.minimumY() + shape.height();
        for (ChunkPos chunk : List.of(new ChunkPos(100, 100), new ChunkPos(-7, 3))) {
            TerrainPass pass = new TerrainPass(config, settings, chunk, NO_FLUID_LEVELS);
            // what the aquifer's lattices sample: the chunk's router depth, the settings' entrances
            Lattice depth = new Lattice(pass.register(config.getNoiseRouter().depth()), chunk, minY, maxY, PvAquifer.CELL_HEIGHT);
            Lattice entrances = new Lattice(inputs.floodedness().entrances(), chunk, minY, maxY, PvAquifer.CELL_HEIGHT);
            // what the JSON F interpolated
            DensityFunction interpolatedDepth = pass.register(interpolated(config, "players-versus:overworld/depth"));
            DensityFunction interpolatedEntrances = pass.register(interpolated(config, "players-versus:overworld/caves/entrances"));
            Mismatches depthMismatches = new Mismatches("depth"), entrancesMismatches = new Mismatches("entrances");
            pass.run((x, y, z, pos) -> {
                depthMismatches.compare(x, y, z, interpolatedDepth.sample(pos), depth.at(x, y, z));
                entrancesMismatches.compare(x, y, z, interpolatedEntrances.sample(pos), entrances.at(x, y, z));
            });
            System.out.println("[terrain pass] chunk " + chunk.x + "," + chunk.z + ": " + depthMismatches + "; " + entrancesMismatches);
            assertEquals(0, depthMismatches.count, depthMismatches::toString);
            assertEquals(0, entrancesMismatches.count, entrancesMismatches::toString);
            assertTrue(depthMismatches.compared > 16 * 16 * 300, "the pass covered " + depthMismatches.compared + " blocks");
        }
    }

    @Test
    void aquiferKeepsTheTerrainPassDecisions() {
        NoiseConfig config = WorldgenTestData.noiseConfig(SEED);
        ChunkGeneratorSettings settings = WorldgenTestData.pvSettings();
        int bands = BANDS.length;
        int[] total = new int[bands], same = new int[bands], sameForCarvers = new int[bands], beforeSameForCarvers = new int[bands];
        int[] sameWithNewF = new int[bands], sameWithNewS = new int[bands];
        int[] spreadChangesByLocalX = new int[16];
        Map<String, Integer> changes = new HashMap<>();
        Mismatches floodedness = new Mismatches("F"), spread = new Mismatches("S"), spreadAtCorners = new Mismatches("S at cell corners");
        int chunks = 0, latticePoints = 0;
        for (int chunkX = 99; chunkX <= 101; chunkX++) {
            for (int chunkZ = 99; chunkZ <= 101; chunkZ++) {
                ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
                TerrainPass pass = new TerrainPass(config, settings, chunk, NO_FLUID_LEVELS);
                PvAquifer aquifer = assertInstanceOf(PvAquifer.class, pass.aquifer(), "ChunkNoiseSamplerMixin didn't make the aquifer");
                // the router's F and S before Phase 2b
                DensityFunction jsonF = pass.register(WorldgenTestData.seeded(config, "players-versus:overworld/aquifer_fluid_level_floodedness"));
                DensityFunction jsonS = pass.register(WorldgenTestData.seeded(config, "players-versus:overworld/aquifer_fluid_level_spread"));
                pass.run((x, y, z, pos) -> {
                    int band = band(y);
                    if (band < 0) return;
                    DensityFunction.NoisePos carverPos = new DensityFunction.UnblendedNoisePos(x, y, z);
                    PvAquiferDecision now = aquifer.decide(pos, 0.0, false);
                    PvAquiferDecision before = PvAquiferRules.decide(pos, 0.0, false, jsonF::sample, jsonS::sample);
                    total[band]++;
                    if (now == before) same[band]++;
                    else changes.merge(before + " -> " + now, 1, Integer::sum);
                    if (now == aquifer.decide(carverPos, 0.0, false)) sameForCarvers[band]++;
                    if (before == PvAquiferRules.decide(carverPos, 0.0, false, jsonF::sample, jsonS::sample)) beforeSameForCarvers[band]++;
                    if (before == PvAquiferRules.decide(pos, 0.0, false, aquifer::floodedness, jsonS::sample)) sameWithNewF[band]++;
                    if (before == PvAquiferRules.decide(pos, 0.0, false, jsonF::sample, aquifer::spread)) sameWithNewS[band]++;

                    floodedness.compare(x, y, z, jsonF.sample(pos), aquifer.floodedness(pos));
                    if (y > -4 && y < 32) {
                        double spreadBefore = jsonS.sample(pos), spreadNow = aquifer.spread(pos);
                        if (spread.compare(x, y, z, spreadBefore, spreadNow)) spreadChangesByLocalX[x - chunk.getStartX()]++;
                        if (Math.floorMod(x, 4) == 0 && Math.floorMod(y, PvAquifer.CELL_HEIGHT) == 0 && Math.floorMod(z, 4) == 0) {
                            spreadAtCorners.compare(x, y, z, spreadBefore, spreadNow);
                        }
                    }
                });
                chunks++;
                for (int points : aquifer.latticeSamples()) latticePoints += points;
            }
        }

        int all = Arrays.stream(total).sum();
        for (int band = 0; band < bands; band++) {
            System.out.printf(Locale.ROOT, "[terrain pass] y %d..%d: %.3f%% of decisions as before (with only F new %.3f%%, with only S new %.3f%%);"
                            + " carvers get the terrain pass's decision %.3f%% now, %.3f%% before%n",
                    BANDS[band][0], BANDS[band][1], percent(same[band], total[band]), percent(sameWithNewF[band], total[band]),
                    percent(sameWithNewS[band], total[band]), percent(sameForCarvers[band], total[band]), percent(beforeSameForCarvers[band], total[band]));
        }
        System.out.println("[terrain pass] changes: " + changes.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(10)
                .map(entry -> String.format(Locale.ROOT, "%s %.3f%%", entry.getKey(), percent(entry.getValue(), all)))
                .collect(Collectors.joining(", ")));
        System.out.println("[terrain pass] " + floodedness + "; " + spread + "; " + spreadAtCorners);
        System.out.println("[terrain pass] S changes by x within the chunk: " + Arrays.toString(spreadChangesByLocalX));
        System.out.println("[terrain pass] " + latticePoints / chunks + " lattice points per chunk");

        for (int band = 0; band < bands; band++) {
            assertEquals(total[band], sameForCarvers[band], "y " + BANDS[band][0] + ".." + BANDS[band][1] + ": carvers and the terrain pass disagree");
            double agreement = (double) sameWithNewF[band] / total[band];
            assertTrue(agreement > 0.99, String.format(Locale.ROOT, "y %d..%d: F alone changed %.2f%% of decisions",
                    BANDS[band][0], BANDS[band][1], 100 * (1 - agreement)));
        }
    }

    private static DensityFunction interpolated(NoiseConfig config, String function) {
        return WorldgenTestData.parse(config, "{\"type\": \"minecraft:interpolated\", \"argument\": \"" + function + "\"}");
    }

    private static int band(int y) {
        for (int band = 0; band < BANDS.length; band++) {
            if (y >= BANDS[band][0] && y <= BANDS[band][1]) return band;
        }
        return -1;
    }

    private static double percent(int part, int whole) {
        return 100.0 * part / whole;
    }

    /** Where two computations of one value differ. */
    private static final class Mismatches {
        private final String name;
        private final List<String> examples = new ArrayList<>();
        int compared, count;
        double maxDifference;

        Mismatches(String name) {
            this.name = name;
        }

        /** @return whether they differ */
        boolean compare(int x, int y, int z, double expected, double actual) {
            this.compared++;
            if (expected == actual || Double.isNaN(expected) && Double.isNaN(actual)) return false;
            this.count++;
            this.maxDifference = Math.max(this.maxDifference, Math.abs(expected - actual));
            if (this.examples.size() < 4) this.examples.add(x + "," + y + "," + z + ": " + expected + " vs " + actual);
            return true;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "%s differs at %d of %d blocks (by up to %.3g)%s", this.name, this.count, this.compared,
                    this.maxDifference, this.examples.isEmpty() ? "" : ", e.g. " + this.examples);
        }
    }
}
