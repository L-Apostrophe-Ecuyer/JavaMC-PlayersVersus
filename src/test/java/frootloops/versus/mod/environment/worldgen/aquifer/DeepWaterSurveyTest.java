package frootloops.versus.mod.environment.worldgen.aquifer;

import frootloops.versus.mod.environment.worldgen.WorldgenTestData;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Random;

import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.SEA_BAND_MIN_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.SEA_WATER_MIN_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.SEA_WATER_THRESHOLD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Everything below y -8 stays dry (the refactor plan, Section 10, question 7): where the sea's floodedness F would
 * still make water down there, under oceans, with exact values at random open blocks. The smoke region has none; F's
 * coast term can pass the water threshold in entrance caves under deep ocean, down to the sea band's floor.
 */
class DeepWaterSurveyTest {

    private static final long SEED = 8675309L;
    /** Vanilla's deep ocean: continentalness below -0.455; ocean below -0.19. */
    private static final double DEEP_OCEAN = -0.455, OCEAN = -0.19;

    @Test
    void waterBelowTheSeaWaterFloor() {
        NoiseConfig config = WorldgenTestData.noiseConfig(SEED);
        ChunkGeneratorSettings settings = WorldgenTestData.pvSettings();
        AquiferInputs inputs = AquiferInputs.of(config, settings);
        DensityFunction continents = config.getNoiseRouter().continents(), finalDensity = config.getNoiseRouter().finalDensity();
        Random random = new Random(SEED);
        int columns = 0, deepColumns = 0;
        long open = 0, wet = 0, openAbove = 0, wetAbove = 0;
        for (int attempt = 0; attempt < 200000 && columns < 4000; attempt++) {
            int x = random.nextInt(40000) - 20000, z = random.nextInt(40000) - 20000;
            double continentalness = continents.sample(new DensityFunction.UnblendedNoisePos(x, 0, z));
            if (continentalness >= OCEAN) continue;
            columns++;
            if (continentalness < DEEP_OCEAN) deepColumns++;
            for (int y = SEA_BAND_MIN_Y + 1; y < SEA_WATER_MIN_Y + 8; y++) {
                DensityFunction.NoisePos pos = new DensityFunction.UnblendedNoisePos(x, y, z);
                if (finalDensity.sample(pos) > 0.0) continue;
                boolean water = inputs.floodedness().sample(pos) > SEA_WATER_THRESHOLD;
                if (y < SEA_WATER_MIN_Y) {
                    open++;
                    if (water) wet++;
                } else {
                    openAbove++;
                    if (water) wetAbove++;
                }
            }
        }
        System.out.printf(Locale.ROOT, "[deep water] %d ocean columns (%d deep ocean), exact values: open blocks in y %d..%d %d, with F"
                        + " above the water threshold %d (%.2f%%); in y %d..%d %d, with F above it %d (%.2f%%)%n",
                columns, deepColumns, SEA_BAND_MIN_Y + 1, SEA_WATER_MIN_Y - 1, open, wet, open == 0 ? 0.0 : 100.0 * wet / open,
                SEA_WATER_MIN_Y, SEA_WATER_MIN_Y + 7, openAbove, wetAbove, openAbove == 0 ? 0.0 : 100.0 * wetAbove / openAbove);
        assertEquals(4000, columns, "not enough ocean columns found");
        assertTrue(open > 0, "no open blocks below the sea water floor under oceans");
        // what the rules make of F above the water threshold down there: a band, not water
        DensityFunction.NoisePos deep = new DensityFunction.UnblendedNoisePos(0, SEA_WATER_MIN_Y - 1, 0);
        assertEquals(PvAquiferDecision.SEA_BARRIER, PvAquiferRules.atPosition(deep, pos -> 1.0, pos -> 0.0));
    }
}
