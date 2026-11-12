package frootloops.versus.mod.environment.worldgen.terrain;

import frootloops.versus.mod.environment.worldgen.PvWorldgenConstants;
import frootloops.versus.mod.environment.worldgen.terrain.TerrainSamples.Column;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 3D-noise zone's window ({@code players-versus:overworld/noise_zone}): where the shattered terrain is, from 0 to 1,
 * a smooth box of humidity, erosion and continentalness.
 */
class NoiseZoneTest {

    private static List<Column> columns;

    @BeforeAll
    static void sample() {
        columns = TerrainSamples.random(7L, 40_000);
    }

    /** From 0 at {@code from} to 1 at {@code to} (either way round), smooth: the window's edge. */
    private static double smoothstep(double x, double from, double to) {
        double t = Math.clamp((x - from) / (to - from), 0.0, 1.0);
        return t * t * (3 - 2 * t);
    }

    private static double window(double humidity, double erosion, double continentalness) {
        return smoothstep(humidity, PvWorldgenConstants.NOISE_ZONE_HUMIDITY_MIN, PvWorldgenConstants.NOISE_ZONE_HUMIDITY_MIN + PvWorldgenConstants.NOISE_ZONE_HUMIDITY_FADE)
                * smoothstep(humidity, PvWorldgenConstants.NOISE_ZONE_HUMIDITY_MAX, PvWorldgenConstants.NOISE_ZONE_HUMIDITY_MAX - PvWorldgenConstants.NOISE_ZONE_HUMIDITY_FADE)
                * smoothstep(erosion, PvWorldgenConstants.NOISE_ZONE_EROSION_MIN, PvWorldgenConstants.NOISE_ZONE_EROSION_MIN + PvWorldgenConstants.NOISE_ZONE_EROSION_FADE)
                * smoothstep(erosion, PvWorldgenConstants.NOISE_ZONE_EROSION_MAX, PvWorldgenConstants.NOISE_ZONE_EROSION_MAX - PvWorldgenConstants.NOISE_ZONE_EROSION_FADE)
                * smoothstep(continentalness, PvWorldgenConstants.NOISE_ZONE_CONTINENTALNESS_MIN,
                        PvWorldgenConstants.NOISE_ZONE_CONTINENTALNESS_MIN + PvWorldgenConstants.NOISE_ZONE_CONTINENTALNESS_FADE);
    }

    @Test
    void theBoxIsTheUsersOwn() {
        assertEquals(0.0, PvWorldgenConstants.NOISE_ZONE_HUMIDITY_MIN);
        assertEquals(0.3, PvWorldgenConstants.NOISE_ZONE_HUMIDITY_MAX);
        assertEquals(0.3, PvWorldgenConstants.NOISE_ZONE_EROSION_MIN);
        assertEquals(0.6, PvWorldgenConstants.NOISE_ZONE_EROSION_MAX);
        assertEquals(100.0, PvWorldgenConstants.NOISE_CAP_FROM_Y);
        assertEquals(128.0, PvWorldgenConstants.NOISE_CAP_TO_Y);
    }

    @Test
    void theWindowIsZeroOutsideTheBox() {
        int outside = 0;
        for (Column c : columns) {
            assertTrue(c.zone() >= 0 && c.zone() <= 1, "the window is " + c.zone() + " at " + c.x() + " " + c.z());
            boolean in = c.humidity() >= PvWorldgenConstants.NOISE_ZONE_HUMIDITY_MIN && c.humidity() < PvWorldgenConstants.NOISE_ZONE_HUMIDITY_MAX
                    && c.erosion() >= PvWorldgenConstants.NOISE_ZONE_EROSION_MIN && c.erosion() < PvWorldgenConstants.NOISE_ZONE_EROSION_MAX
                    && c.continentalness() >= PvWorldgenConstants.NOISE_ZONE_CONTINENTALNESS_MIN;
            if (!in) {
                outside++;
                assertEquals(0.0, c.zone(), 1e-6, "outside the box at " + c);
            }
        }
        assertTrue(outside > 30_000, "only " + outside + " columns outside the box");
    }

    @Test
    void theWindowIsOneWellInside() {
        int inside = 0;
        for (Column c : columns) {
            if (c.humidity() >= 0.05 && c.humidity() <= 0.25 && c.erosion() >= 0.36 && c.erosion() <= 0.54 && c.continentalness() >= 0.12) {
                inside++;
                assertEquals(1.0, c.zone(), 1e-5, "well inside the box at " + c);
            }
        }
        assertTrue(inside >= 100, "only " + inside + " columns well inside the box");
    }

    @Test
    void theWindowMatchesItsFormula() {
        int fades = 0, full = 0;
        for (Column c : columns) {
            assertEquals(window(c.humidity(), c.erosion(), c.continentalness()), c.zone(), 1e-3, "at " + c);
            if (c.zone() > 0.001 && c.zone() < 0.999) fades++;
            if (c.zone() >= 0.999) full++;
        }
        assertTrue(fades >= 100, "only " + fades + " columns in the fades");
        assertTrue(full >= 100, "only " + full + " columns inside");
    }
}
