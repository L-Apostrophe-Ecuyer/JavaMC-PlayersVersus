package frootloops.versus.mod.environment.worldgen.density;

import frootloops.versus.mod.environment.worldgen.TestGame;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.CORRIDOR_BIAS;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.CORRIDOR_ENTRANCES;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.CORRIDOR_FLARE_BIAS;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.CORRIDOR_FLARE_FROM;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.CORRIDOR_ZONE_TAPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The noodle's height bias with the flooded corridors, and the form C2ME's compiler gets ({@link PvCorridorBias} in the layers). */
class PvNoodleTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    @Test
    void corridorBias() {
        // outside the layers (y -3..23), or far from the caves, the bias is today's
        assertFalse(PvNoodle.inCorridorLayers(-4));
        assertTrue(PvNoodle.inCorridorLayers(-3));
        assertTrue(PvNoodle.inCorridorLayers(23));
        assertFalse(PvNoodle.inCorridorLayers(24));
        assertEquals(PvNoodle.bias(24), PvNoodle.corridorBias(24, -1.0));
        assertEquals(PvNoodle.bias(10), PvNoodle.corridorBias(10, CORRIDOR_ENTRANCES));
        // in the zone: the corridors' bias, lower nearer the caves
        assertEquals(CORRIDOR_BIAS, PvNoodle.corridorBias(10, CORRIDOR_FLARE_FROM), 1e-12);
        assertEquals(CORRIDOR_FLARE_BIAS, PvNoodle.corridorBias(10, 0.0), 1e-12);
        assertEquals(CORRIDOR_FLARE_BIAS, PvNoodle.corridorBias(10, -0.5), 1e-12);
        double halfway = CORRIDOR_ENTRANCES - CORRIDOR_ZONE_TAPER / 2;
        assertEquals((PvNoodle.bias(10) + CORRIDOR_BIAS) / 2, PvNoodle.corridorBias(10, halfway), 1e-12);
    }

    /**
     * {@link PvNoodle#corridorBiasFunction} gives {@link PvNoodle#corridorBias}'s doubles, at every height and entrance value
     * tried: vanilla's bias below and above the layers, {@link PvCorridorBias} in them.
     */
    @Test
    void corridorBiasFunctionIsTheSame() {
        int compared = 0;
        for (int y = -10; y <= 30; y++) {
            for (int step = -40; step <= 60; step++) {
                double entrances = step * 0.01 + (step % 7) * 1.37e-4;
                DensityFunction function = PvNoodle.corridorBiasFunction(DensityFunctionTypes.constant(entrances));
                double expected = PvNoodle.corridorBias(y, entrances);
                DensityFunction.NoisePos pos = new DensityFunction.UnblendedNoisePos(0, y, 0);
                double actual = function.sample(pos);
                int height = y;
                assertEquals(expected, actual, 0.0, () -> "y " + height + ", entrances " + entrances);
                // C2ME's min and max nodes skip a side by these bounds, so they must hold
                PvCorridorBias corridor = new PvCorridorBias(DensityFunctionTypes.constant(entrances));
                assertEquals(expected, corridor.sample(pos), 0.0);
                assertTrue(actual >= corridor.minValue() && actual <= corridor.maxValue(), () -> "bounds at y " + height + ", entrances " + entrances);
                assertTrue(actual >= function.minValue() && actual <= function.maxValue());
                compared++;
            }
        }
        assertEquals(41 * 101, compared);
    }
}
