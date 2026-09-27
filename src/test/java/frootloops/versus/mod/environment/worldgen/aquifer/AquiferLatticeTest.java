package frootloops.versus.mod.environment.worldgen.aquifer;

import frootloops.versus.mod.environment.worldgen.WorldgenTestData;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.noise.NoiseRouter;
import org.junit.jupiter.api.Test;

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
 * The lattice aquifer against exact per-block values, on this mod's real data: for every block of a few chunks,
 * what the aquifer would place if the block were open (as for a carved block), with lattice values and with exact
 * ones, and what each costs.
 */
class AquiferLatticeTest {

    private static final int[][] BANDS = {{-31, -4}, {-3, 7}, {8, 31}, {32, 47}, {48, 63}};

    @Test
    void latticeDecisionsMatchExactOnes() {
        NoiseConfig config = WorldgenTestData.noiseConfig(8675309L);
        NoiseRouter router = config.getNoiseRouter();
        DensityFunction floodedness = router.fluidLevelFloodednessNoise();
        DensityFunction spread = router.fluidLevelSpreadNoise();

        int[] total = new int[BANDS.length], same = new int[BANDS.length];
        Map<String, Integer> changes = new HashMap<>();
        long latticeNanos = 0, exactNanos = 0;
        int latticePoints = 0, chunks = 0;
        for (int chunkX = 99; chunkX <= 101; chunkX++) {
            for (int chunkZ = 99; chunkZ <= 101; chunkZ++) {
                ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
                Lattice floodednessLattice = new Lattice(floodedness, chunk, SEA_BAND_MIN_Y, SEA_LEVEL);
                Lattice spreadLattice = new Lattice(spread, chunk, BASIN_MIN_Y, BASIN_MAX_Y);
                chunks++;
                for (int band = 0; band < BANDS.length; band++) {
                    for (int y = BANDS[band][0]; y <= BANDS[band][1]; y++) {
                        for (int x = chunk.getStartX(); x <= chunk.getEndX(); x++) {
                            for (int z = chunk.getStartZ(); z <= chunk.getEndZ(); z++) {
                                DensityFunction.NoisePos pos = new DensityFunction.UnblendedNoisePos(x, y, z);
                                long start = System.nanoTime();
                                PvAquiferDecision lattice = PvAquiferRules.decide(pos, 0.0, false, floodednessLattice, spreadLattice);
                                long middle = System.nanoTime();
                                PvAquiferDecision exact = PvAquiferRules.decide(pos, 0.0, false, floodedness::sample, spread::sample);
                                exactNanos += System.nanoTime() - middle;
                                latticeNanos += middle - start;
                                total[band]++;
                                if (lattice == exact) same[band]++;
                                else changes.merge(exact + " -> " + lattice, 1, Integer::sum);
                            }
                        }
                    }
                }
                latticePoints += floodednessLattice.samples() + spreadLattice.samples();
            }
        }

        int all = 0, allSame = 0;
        for (int band = 0; band < BANDS.length; band++) {
            System.out.printf(Locale.ROOT, "[lattice] y %d..%d: %.2f%% of decisions unchanged%n",
                    BANDS[band][0], BANDS[band][1], 100.0 * same[band] / total[band]);
            all += total[band];
            allSame += same[band];
        }
        int finalAll = all;
        System.out.println("[lattice] top changes (exact -> lattice): " + changes.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(8)
                .map(entry -> String.format(Locale.ROOT, "%s %.2f%%", entry.getKey(), 100.0 * entry.getValue() / finalAll))
                .collect(Collectors.joining(", ")));
        System.out.printf(Locale.ROOT, "[lattice] per chunk: lattice %.1f ms (%d points sampled), exact %.1f ms (%d blocks)%n",
                latticeNanos / 1e6 / chunks, latticePoints / chunks, exactNanos / 1e6 / chunks, all / chunks);
        assertTrue((double) allSame / all > 0.9, "the lattice changes more than 10% of decisions");
    }
}
