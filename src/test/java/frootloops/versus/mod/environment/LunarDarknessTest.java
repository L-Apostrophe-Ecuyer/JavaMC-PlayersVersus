package frootloops.versus.mod.environment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Nights darken with the moon: a new moon's midnight is all but black, a full moon's as vanilla's, days untouched. */
class LunarDarknessTest {
    /** Vanilla's sky factor by night and by day (the minecraft:day timeline's visual/sky_light_factor). */
    private static final float NIGHT = 0.24F, DAY = 1.0F;
    /** When the moon is highest (a little after midnight) on the first night (the full moon's) and the fifth (the new moon's). */
    private static final long FULL_MOON_HIGH = 18750L, NEW_MOON_HIGH = 18750L + 4L * 24000L;

    @Test
    void aNewMoonHighInTheSkyLeavesItAllButBlack() {
        assertTrue(LunarDarkness.skyFactor(NIGHT, NEW_MOON_HIGH) < 0.03F, "" + LunarDarkness.skyFactor(NIGHT, NEW_MOON_HIGH));
        // Later in the cycle, the same: the moon turns every 192000 ticks
        assertEquals(LunarDarkness.skyFactor(NIGHT, NEW_MOON_HIGH), LunarDarkness.skyFactor(NIGHT, NEW_MOON_HIGH + 5L * 192000L), 1.0E-6F);
    }

    @Test
    void aFullMoonsNightIsVanillas() {
        assertEquals(NIGHT, LunarDarkness.skyFactor(NIGHT, FULL_MOON_HIGH), 0.01F);
    }

    @Test
    void theLessMoonTheDarker() {
        float previous = LunarDarkness.skyFactor(NIGHT, FULL_MOON_HIGH);
        for (int night = 1; night <= 4; night++) {
            float now = LunarDarkness.skyFactor(NIGHT, FULL_MOON_HIGH + night * 24000L);
            assertTrue(now < previous, "night " + night + " (" + now + ") isn't darker than the one before (" + previous + ")");
            previous = now;
        }
        // And lighter again as it waxes
        for (int night = 5; night <= 8; night++) {
            float now = LunarDarkness.skyFactor(NIGHT, FULL_MOON_HIGH + night * 24000L);
            assertTrue(now > previous, "night " + night + " (" + now + ") isn't lighter than the one before (" + previous + ")");
            previous = now;
        }
    }

    @Test
    void itDeepensAsTheMoonRises() {
        float dusk = LunarDarkness.skyFactor(NIGHT, NEW_MOON_HIGH - 5000L);
        float evening = LunarDarkness.skyFactor(NIGHT, NEW_MOON_HIGH - 2500L);
        float high = LunarDarkness.skyFactor(NIGHT, NEW_MOON_HIGH);
        assertTrue(dusk > evening && evening > high, dusk + " > " + evening + " > " + high);
    }

    @Test
    void daysAreUntouched() {
        for (long t = 0; t < 192000L; t += 1000L) assertEquals(DAY, LunarDarkness.skyFactor(DAY, t));
    }

    @Test
    void itNeverGoesBelowNothing() {
        for (long t = 0; t < 192000L; t += 250L) assertTrue(LunarDarkness.skyFactor(0.05F, t) >= 0.0F);
    }
}
