package frootloops.versus.mod.environment.worldgen.aquifer;

import frootloops.versus.mod.environment.worldgen.PvWorldgen;
import frootloops.versus.mod.environment.worldgen.WorldgenTestData;
import frootloops.versus.mod.environment.worldgen.density.AquiferFloodedness;
import frootloops.versus.mod.environment.worldgen.density.AquiferSpread;
import frootloops.versus.mod.environment.worldgen.density.DensityOps;
import org.junit.jupiter.api.Test;

import java.util.Random;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * The Java aquifer functions ({@link AquiferFormulas}, through the router's {@link AquiferFloodedness} and
 * {@link AquiferSpread}) against the JSON they replaced, kept as the tests' reference ({@link WorldgenTestData#REFERENCE}):
 * they must give the same doubles everywhere.
 */
class AquiferPortTest {

    private static final long SEED = 8675309L;
    /** Band edges of the JSON, and the blocks next to them. */
    private static final int[] EDGE_YS = {-33, -32, -31, -5, -4, -3, -2, -1, 0, 1, 7, 8, 9, 15, 16, 17, 23, 24, 25, 31, 32, 33,
            39, 40, 41, 47, 48, 49, 53, 54, 55, 63, 64, 65};

    @Test
    void yValueIsVanillasY() {
        RandomState config = WorldgenTestData.noiseConfig(SEED);
        DensityFunction y = WorldgenTestData.seeded(config, "minecraft:y");
        for (int blockY = -64; blockY < 320; blockY++) {
            assertEquals(y.compute(new DensityFunction.SinglePointContext(0, blockY, 0)), DensityOps.yValue(blockY), 0.0, "y " + blockY);
        }
    }

    @Test
    void javaGivesTheJsonsValues() {
        RandomState config = WorldgenTestData.noiseConfig(SEED);
        // a router slot decoded from JSON is a registry holder around the function
        DensityFunction floodedness = PvWorldgen.unwrap(config.router().fluidLevelFloodednessNoise());
        DensityFunction spread = PvWorldgen.unwrap(config.router().fluidLevelSpreadNoise());
        assertInstanceOf(AquiferFloodedness.class, floodedness);
        assertInstanceOf(AquiferSpread.class, spread);
        DensityFunction jsonFloodedness = WorldgenTestData.seeded(config, WorldgenTestData.REFERENCE + ":overworld/aquifer_fluid_level_floodedness");
        DensityFunction jsonSpread = WorldgenTestData.seeded(config, WorldgenTestData.REFERENCE + ":overworld/aquifer_fluid_level_spread");

        Random random = new Random(SEED);
        int compared = 0, nonZero = 0;
        for (int i = 0; i < 20000; i++) {
            int x = random.nextInt(8000) - 4000, z = random.nextInt(8000) - 4000;
            int y = i % 2 == 0 ? EDGE_YS[random.nextInt(EDGE_YS.length)] : random.nextInt(110) - 40;
            DensityFunction.FunctionContext pos = new DensityFunction.SinglePointContext(x, y, z);
            double expectedFloodedness = jsonFloodedness.compute(pos), expectedSpread = jsonSpread.compute(pos);
            assertEquals(expectedFloodedness, floodedness.compute(pos), 0.0, () -> "floodedness at " + x + "," + y + "," + z);
            assertEquals(expectedSpread, spread.compute(pos), 0.0, () -> "spread at " + x + "," + y + "," + z);
            compared++;
            if (expectedFloodedness != 0.0 || expectedSpread != 0.0) nonZero++;
        }
        System.out.println("[port] F and S equal the JSON at " + compared + " points, " + nonZero + " of them not both zero");
    }
}
