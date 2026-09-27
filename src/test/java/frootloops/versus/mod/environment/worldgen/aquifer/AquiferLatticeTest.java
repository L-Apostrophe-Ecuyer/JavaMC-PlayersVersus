package frootloops.versus.mod.environment.worldgen.aquifer;

import frootloops.versus.mod.environment.worldgen.WorldgenTestData;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.noise.NoiseRouter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.BASIN_MAX_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.BASIN_MIN_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.SEA_BAND_MIN_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.SEA_LEVEL;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The aquifer ({@link PvAquifer}: smooth inputs from lattices, F and S per block) against exact per-block values, on
 * this mod's real data: for every block of a few chunks, what it places if the block is open (as for a carved block),
 * and what each costs. For comparison it also measures interpolating F and S whole, which commit 373b78f tried: both
 * step inside their bands, so that moves the steps.
 */
class AquiferLatticeTest {

    private static final int[][] BANDS = {{-31, -4}, {-3, 7}, {8, 31}, {32, 47}, {48, 63}};
    private static final AquiferSampler.FluidLevelSampler NO_FLUID_LEVELS = (x, y, z) -> {
        throw new UnsupportedOperationException("the test passes lavaLevel itself");
    };

    @Test
    void latticeInputsKeepDecisions() {
        NoiseConfig config = WorldgenTestData.noiseConfig(8675309L);
        NoiseRouter router = config.getNoiseRouter();
        DensityFunction floodedness = router.fluidLevelFloodednessNoise();
        DensityFunction spread = router.fluidLevelSpreadNoise();
        AquiferInputs inputs = AquiferInputs.of(config, WorldgenTestData.pvSettings());

        int[] total = new int[BANDS.length], same = new int[BANDS.length], sameWhole = new int[BANDS.length];
        Map<String, Integer> changes = new HashMap<>();
        long aquiferNanos = 0, exactNanos = 0;
        int[] latticePoints = new int[4];
        int chunks = 0;
        for (int chunkX = 99; chunkX <= 101; chunkX++) {
            for (int chunkZ = 99; chunkZ <= 101; chunkZ++) {
                ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
                PvAquifer aquifer = new PvAquifer(inputs, router.depth(), chunk, NO_FLUID_LEVELS);
                Lattice wholeFloodedness = new Lattice(floodedness, chunk, SEA_BAND_MIN_Y, SEA_LEVEL);
                Lattice wholeSpread = new Lattice(spread, chunk, BASIN_MIN_Y, BASIN_MAX_Y);
                chunks++;
                for (int band = 0; band < BANDS.length; band++) {
                    for (int y = BANDS[band][0]; y <= BANDS[band][1]; y++) {
                        for (int x = chunk.getStartX(); x <= chunk.getEndX(); x++) {
                            for (int z = chunk.getStartZ(); z <= chunk.getEndZ(); z++) {
                                DensityFunction.NoisePos pos = new DensityFunction.UnblendedNoisePos(x, y, z);
                                long start = System.nanoTime();
                                PvAquiferDecision decision = aquifer.decide(pos, 0.0, false);
                                long middle = System.nanoTime();
                                PvAquiferDecision exact = PvAquiferRules.decide(pos, 0.0, false, floodedness::sample, spread::sample);
                                exactNanos += System.nanoTime() - middle;
                                aquiferNanos += middle - start;
                                PvAquiferDecision whole = PvAquiferRules.decide(pos, 0.0, false, wholeFloodedness, wholeSpread);
                                total[band]++;
                                if (decision == exact) same[band]++;
                                else changes.merge(exact + " -> " + decision, 1, Integer::sum);
                                if (whole == exact) sameWhole[band]++;
                            }
                        }
                    }
                }
                int[] points = aquifer.latticeSamples();
                for (int i = 0; i < points.length; i++) latticePoints[i] += points[i];
            }
        }

        int all = Arrays.stream(total).sum(), allSame = Arrays.stream(same).sum();
        for (int band = 0; band < BANDS.length; band++) {
            System.out.printf(Locale.ROOT, "[aquifer] y %d..%d: %.2f%% of decisions unchanged (interpolating F and S whole: %.2f%%)%n",
                    BANDS[band][0], BANDS[band][1], 100.0 * same[band] / total[band], 100.0 * sameWhole[band] / total[band]);
        }
        System.out.println("[aquifer] top changes (exact -> aquifer): " + changes.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(8)
                .map(entry -> String.format(Locale.ROOT, "%s %.3f%%", entry.getKey(), 100.0 * entry.getValue() / all))
                .collect(Collectors.joining(", ")));
        int finalChunks = chunks;
        System.out.printf(Locale.ROOT, "[aquifer] per chunk: aquifer %.1f ms, lattice points depth %d, continentalness %d, entrances %d, basin %d;"
                        + " exact %.1f ms for %d blocks (raw functions, outside any chunk cache)%n",
                aquiferNanos / 1e6 / chunks, latticePoints[0] / finalChunks, latticePoints[1] / finalChunks, latticePoints[2] / finalChunks,
                latticePoints[3] / finalChunks, exactNanos / 1e6 / chunks, all / chunks);
        for (int band = 0; band < BANDS.length; band++) {
            double agreement = (double) same[band] / total[band];
            assertTrue(agreement > 0.97, String.format(Locale.ROOT, "y %d..%d: only %.2f%% of decisions unchanged", BANDS[band][0], BANDS[band][1], 100 * agreement));
        }
        assertTrue((double) allSame / all > 0.99, "fewer than 99% of decisions unchanged overall");
    }
}
