package frootloops.versus.mod.environment.worldgen.density;

import frootloops.versus.mod.environment.worldgen.TestGame;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_BED;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_FADE;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_HALF_WIDTH;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_TOP;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_VALLEY_MAX_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_WIDENING;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_Y;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The high river's valley ({@link PvHighRiver}) on made-up inputs: its shape, and the aquifer's water against it. */
class PvHighRiverTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    private static PvHighRiver river(double channel, double depth, double terrain) {
        return new PvHighRiver(DensityFunctionTypes.constant(channel), DensityFunctionTypes.constant(depth), DensityFunctionTypes.constant(terrain));
    }

    private static double valley(double channel, double depth, double terrain, int y) {
        return river(channel, depth, terrain).sample(new DensityFunction.UnblendedNoisePos(0, y, 0));
    }

    @Test
    void activityFadesWhereTheGroundRisesHigh() {
        assertEquals(1.0, PvHighRiver.activity(-0.5));
        assertEquals(1.0, PvHighRiver.activity(HIGH_RIVER_TOP));
        assertEquals(0.5, PvHighRiver.activity(HIGH_RIVER_TOP + HIGH_RIVER_FADE / 2), 1e-12);
        assertEquals(0.0, PvHighRiver.activity(HIGH_RIVER_TOP + HIGH_RIVER_FADE));
        assertEquals(0.0, PvHighRiver.activity(1.0));
    }

    @Test
    void theValleyWidensUpwardsAndTheBedNarrowsDown() {
        assertEquals(HIGH_RIVER_HALF_WIDTH, PvHighRiver.fullHalfWidth(HIGH_RIVER_Y));
        assertEquals(HIGH_RIVER_HALF_WIDTH + 10 * HIGH_RIVER_WIDENING, PvHighRiver.fullHalfWidth(HIGH_RIVER_Y + 10), 1e-12);
        assertEquals(HIGH_RIVER_HALF_WIDTH / (HIGH_RIVER_BED + 1), PvHighRiver.fullHalfWidth(HIGH_RIVER_Y - HIGH_RIVER_BED), 1e-12);
        assertEquals(0.0, PvHighRiver.fullHalfWidth(HIGH_RIVER_Y - HIGH_RIVER_BED - 1));
        assertEquals(0.0, PvHighRiver.fullHalfWidth(HIGH_RIVER_VALLEY_MAX_Y));
        for (int y = PvHighRiver.MIN_Y; y < PvHighRiver.MAX_Y - 1; y++) {
            assertTrue(PvHighRiver.fullHalfWidth(y) < PvHighRiver.fullHalfWidth(y + 1), "at y " + y);
        }
    }

    @Test
    void theValleyOpensOnlyInsideItsHalfWidth() {
        // the middle of a full river: open from the bed's bottom to the valley's top, and nowhere else
        assertEquals(Double.POSITIVE_INFINITY, valley(0.0, 0.0, 1.0, PvHighRiver.MIN_Y - 1));
        assertEquals(-PvHighRiver.fullHalfWidth(PvHighRiver.MIN_Y), valley(0.0, 0.0, 1.0, PvHighRiver.MIN_Y));
        assertEquals(-HIGH_RIVER_HALF_WIDTH, valley(0.0, 0.0, 1.0, HIGH_RIVER_Y));
        assertEquals(-PvHighRiver.fullHalfWidth(PvHighRiver.MAX_Y - 1), valley(0.0, 0.0, 1.0, PvHighRiver.MAX_Y - 1));
        assertEquals(Double.POSITIVE_INFINITY, valley(0.0, 0.0, 1.0, PvHighRiver.MAX_Y));
        // off to the side: open higher up, where the valley is wider, not at the surface
        double side = HIGH_RIVER_HALF_WIDTH + 2.5 * HIGH_RIVER_WIDENING;
        assertEquals(Double.POSITIVE_INFINITY, valley(side, 0.0, 1.0, HIGH_RIVER_Y + 2));
        assertTrue(valley(side, 0.0, 1.0, HIGH_RIVER_Y + 3) < 0.0);
        assertTrue(valley(-side, 0.0, 1.0, HIGH_RIVER_Y + 3) < 0.0, "the river runs along the noise's zeros, on both sides");
        // no river where the ground rises too high
        assertEquals(Double.POSITIVE_INFINITY, valley(0.0, HIGH_RIVER_TOP + HIGH_RIVER_FADE, 1.0, HIGH_RIVER_Y + 5));
    }

    @Test
    void theSurfaceAndBedOpenOnlyWhereTheTerrainWasSolid() {
        for (int y = PvHighRiver.MIN_Y; y <= HIGH_RIVER_Y; y++) {
            assertEquals(Double.POSITIVE_INFINITY, valley(0.0, 0.0, -0.1, y), "at y " + y);
            assertEquals(Double.POSITIVE_INFINITY, valley(0.0, 0.0, 0.0, y), "at y " + y);
            assertTrue(valley(0.0, 0.0, 1e-9, y) < 0.0, "at y " + y);
        }
        // above the surface the valley is cut whatever the terrain was
        assertTrue(valley(0.0, 0.0, -0.1, HIGH_RIVER_Y + 1) < 0.0);
    }

    /**
     * The aquifer's water ({@link PvHighRiver#waterAt}) is exactly where the valley opens a block at or under the
     * surface, over a grid of inputs that crosses every threshold.
     */
    @Test
    void waterIsWhereTheValleyOpens() {
        int compared = 0, water = 0;
        for (int y = PvHighRiver.MIN_Y - 2; y <= HIGH_RIVER_Y + 2; y++) {
            for (int c = -80; c <= 80; c++) {
                double channel = c * 0.0005 + (c % 3) * 1.1e-5;
                for (int d = -4; d <= 12; d++) {
                    double depth = d * 0.01;
                    for (double terrain : new double[]{-0.2, 0.0, 1e-12, 0.3}) {
                        double valley = valley(channel, depth, terrain, y);
                        boolean waterAt = PvHighRiver.waterAt(y, channel, depth, terrain);
                        if (y <= HIGH_RIVER_Y) {
                            assertEquals(valley < 0.0, waterAt, "y " + y + ", channel " + channel + ", depth " + depth + ", terrain " + terrain);
                        } else {
                            assertFalse(waterAt);
                        }
                        if (waterAt) water++;
                        compared++;
                    }
                }
            }
        }
        assertTrue(water > 100, "the grid never reached the river: " + water + " of " + compared);
    }

    @Test
    void boundsHold() {
        PvHighRiver river = river(0.0, 0.0, 1.0);
        for (int y = PvHighRiver.MIN_Y - 1; y <= PvHighRiver.MAX_Y; y++) {
            double valley = river.sample(new DensityFunction.UnblendedNoisePos(0, y, 0));
            assertTrue(valley >= river.minValue() && valley <= river.maxValue(), "at y " + y);
        }
    }
}
