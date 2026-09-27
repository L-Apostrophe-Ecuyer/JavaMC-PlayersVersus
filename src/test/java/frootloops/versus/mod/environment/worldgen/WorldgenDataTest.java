package frootloops.versus.mod.environment.worldgen;

import frootloops.versus.mod.environment.worldgen.aquifer.PvAquiferDecision;
import frootloops.versus.mod.environment.worldgen.aquifer.PvAquiferRules;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.noise.NoiseRouter;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.ToDoubleFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Loads this mod's real worldgen data and checks what the rest of the code assumes about it. */
class WorldgenDataTest {

    static final long SEED = 8675309L;

    @Test
    void settingsMatchTheCode() {
        ChunkGeneratorSettings settings = WorldgenTestData.pvSettings();
        System.out.printf(Locale.ROOT, "[data] players-versus:overworld min_y %d height %d sea_level %d aquifers %s ore_veins %s%n",
                settings.generationShapeConfig().minimumY(), settings.generationShapeConfig().height(), settings.seaLevel(),
                settings.aquifers(), settings.oreVeins());
        assertTrue(PvWorldgen.isPvGenerator(settings), "the router's fluid_level_floodedness has no players-versus:aquifer_floodedness");
        assertEquals(PvWorldgenConstants.SEA_LEVEL, settings.seaLevel(), "sea_level in the noise settings and PvWorldgenConstants.SEA_LEVEL differ");
    }

    /** {@link WorldgenTestData#seeded} must give the exact values of {@link NoiseConfig}'s router. */
    @Test
    void seedingMatchesNoiseConfig() {
        NoiseConfig config = WorldgenTestData.noiseConfig(SEED);
        NoiseRouter router = config.getNoiseRouter();
        assertEqualValues("depth", WorldgenTestData.seeded(config, "players-versus:overworld/depth"), router.depth());
        assertEqualValues("floodedness", WorldgenTestData.seeded(config, "players-versus:overworld/aquifer_fluid_level_floodedness"), router.fluidLevelFloodednessNoise());
        assertEqualValues("spread", WorldgenTestData.seeded(config, "players-versus:overworld/aquifer_fluid_level_spread"), router.fluidLevelSpreadNoise());
    }

    private static void assertEqualValues(String name, DensityFunction ours, DensityFunction router) {
        Random random = new Random(SEED);
        for (int i = 0; i < 2000; i++) {
            DensityFunction.NoisePos pos = new DensityFunction.UnblendedNoisePos(random.nextInt(60000) - 30000, random.nextInt(384) - 64, random.nextInt(60000) - 30000);
            double expected = router.sample(pos);
            assertEquals(expected, ours.sample(pos), 0.0, () -> name + " at " + pos.blockX() + "," + pos.blockY() + "," + pos.blockZ());
        }
    }

    /**
     * What today's aquifer would place in open space (as for a carved block) around the smoke test's region, by y band,
     * and what one raw sample of each input costs. A reference for the lattice aquifer of Phase 2.
     */
    @Test
    void aquiferBaseline() {
        NoiseConfig config = WorldgenTestData.noiseConfig(SEED);
        NoiseRouter router = config.getNoiseRouter();
        int[][] bands = {{-31, -4}, {-3, 7}, {8, 31}, {32, 47}, {48, 63}};
        for (int[] band : bands) {
            Map<PvAquiferDecision, Integer> counts = new EnumMap<>(PvAquiferDecision.class);
            int total = 0;
            for (int x = 1408; x < 1808; x += 4) {
                for (int z = 1408; z < 1808; z += 4) {
                    for (int y = band[0]; y <= band[1]; y += 2) {
                        DensityFunction.NoisePos pos = new DensityFunction.UnblendedNoisePos(x, y, z);
                        PvAquiferDecision decision = PvAquiferRules.decide(pos, 0.0, false, router.fluidLevelFloodednessNoise(), router.fluidLevelSpreadNoise());
                        counts.merge(decision, 1, Integer::sum);
                        total++;
                    }
                }
            }
            StringBuilder line = new StringBuilder(String.format(Locale.ROOT, "[aquifer] y %d..%d:", band[0], band[1]));
            for (Map.Entry<PvAquiferDecision, Integer> entry : counts.entrySet()) {
                line.append(String.format(Locale.ROOT, " %s %.2f%%", entry.getKey(), 100.0 * entry.getValue() / total));
            }
            System.out.println(line);
        }
        time("floodedness", pos -> router.fluidLevelFloodednessNoise().sample(pos));
        time("spread", pos -> router.fluidLevelSpreadNoise().sample(pos));
        time("depth", pos -> router.depth().sample(pos));
        DensityFunction entrances = WorldgenTestData.seeded(config, "players-versus:overworld/caves/entrances");
        time("entrances", entrances::sample);
        DensityFunction noodle = WorldgenTestData.seeded(config, "players-versus:overworld/caves/noodle");
        time("noodle", noodle::sample);
    }

    private static void time(String name, ToDoubleFunction<DensityFunction.NoisePos> function) {
        double sink = 0;
        for (int round = 0; round < 2; round++) {  // the first round warms up the JIT
            long start = System.nanoTime();
            int samples = 0;
            for (int x = 0; x < 64; x++) {
                for (int z = 0; z < 64; z++) {
                    for (int y = -28; y < 64; y += 4) {
                        sink += function.applyAsDouble(new DensityFunction.UnblendedNoisePos(1600 + x * 4, y, 1600 + z * 4));
                        samples++;
                    }
                }
            }
            if (round == 1) {
                System.out.printf(Locale.ROOT, "[aquifer] raw %s: %.0f ns per sample (%d samples)%n", name, (System.nanoTime() - start) / (double) samples, samples);
            }
        }
        assertTrue(Double.isFinite(sink));
    }
}
