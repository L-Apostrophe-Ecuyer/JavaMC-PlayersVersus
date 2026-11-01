package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A watchling's static: how it builds while looked at and clears while not (what counts as looking: GazeTest). */
class WatchlingStareTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    @Test
    void theStaticFillsTheScreenAfterStareTicksOfLooking() {
        float stare = 0.0F;
        for (int tick = 0; tick < VersusSettings.Watchlings.STARE_TICKS - 1; tick++) stare = WatchlingStare.next(stare, true);
        assertTrue(stare < 1.0F, "full before its time: " + stare);
        assertTrue(WatchlingStare.next(stare, true) >= 0.999F, "not full after its time");
    }

    @Test
    void lookingAwayClearsItAndNeverBelowNothing() {
        assertEquals(0.5F - 1.0F / VersusSettings.Watchlings.CLEAR_TICKS, WatchlingStare.next(0.5F, false), 1.0E-6F);
        assertEquals(0.0F, WatchlingStare.next(0.0F, false));
    }

    @Test
    void beingNearBuildsStaticLookingOrNot() {
        double near = VersusSettings.Watchlings.NEAR_RANGE, kill = VersusSettings.Watchlings.KILL_RANGE;
        assertEquals(0.0F, WatchlingStare.proximity(near + 1.0));
        assertEquals(0.0F, WatchlingStare.proximity(near));
        assertTrue(WatchlingStare.proximity(near - 1.0) > 0.0F, "no pressure just inside the near range");
        assertEquals(VersusSettings.Watchlings.PROXIMITY_RATE, WatchlingStare.proximity(kill), 1.0E-6F);
        assertEquals(VersusSettings.Watchlings.PROXIMITY_RATE, WatchlingStare.proximity(0.5), 1.0E-6F);
        // Not looking, but at its feet: the static still builds
        assertTrue(WatchlingStare.next(0.0F, WatchlingStare.rate(false, kill)) > 0.0F);
        // Not looking and far off: it clears instead
        assertEquals(0.0F, WatchlingStare.next(0.0F, WatchlingStare.rate(false, near + 10.0)));
    }

    @Test
    void theNearerTheFasterAndAStareAddsToIt() {
        double far = WatchlingStare.proximity(8.0), middle = WatchlingStare.proximity(6.0), close = WatchlingStare.proximity(4.0);
        assertTrue(far < middle && middle < close, far + ", " + middle + ", " + close);
        assertEquals(WatchlingStare.rate(false, 4.0) + 1.0F, WatchlingStare.rate(true, 4.0), 1.0E-6F);
        assertEquals(1.0F, WatchlingStare.rate(true, 40.0), 1.0E-6F);
    }

    @Test
    void tooCloseKillsFasterThanAStare() {
        float stare = 0.0F, close = 0.0F;
        int ticks = 0;
        while (close < 1.0F) {
            close = WatchlingStare.next(close, WatchlingStare.rate(false, VersusSettings.Watchlings.KILL_RANGE));
            stare = WatchlingStare.next(stare, true);
            ticks++;
        }
        assertTrue(ticks < VersusSettings.Watchlings.STARE_TICKS, "too close took " + ticks + " ticks, no faster than a stare");
        assertTrue(stare < 1.0F);
    }

    @Test
    void theStaticClearsSlowly() {
        // From full to nothing takes CLEAR_TICKS, and that's a good while longer than the stare took to fill
        float stare = 1.0F;
        int ticks = 0;
        while (stare > 0.0F) {
            stare = WatchlingStare.next(stare, false);
            ticks++;
        }
        assertTrue(ticks >= VersusSettings.Watchlings.CLEAR_TICKS - 1);
        assertTrue(VersusSettings.Watchlings.CLEAR_TICKS >= 4 * 60, "the static clears in under 12 seconds");
    }

    @Test
    void eachSightingBringsItNearerButNeverTooNear() {
        double distance = 40.0;
        for (int sighting = 0; sighting < 30; sighting++) {
            double next = WatchlingStare.approach(distance, 0.5);
            assertTrue(next <= distance, "it went further off");
            assertTrue(next >= VersusSettings.Watchlings.CLOSEST - 1.0E-9, "it came nearer than the closest");
            distance = next;
        }
        assertEquals(VersusSettings.Watchlings.CLOSEST, distance, 1.0E-9);
        // Within the closest already, it doesn't back off
        assertEquals(2.0, WatchlingStare.approach(2.0, 1.0), 1.0E-9);
        // Every roll comes nearer
        assertTrue(WatchlingStare.approach(30.0, 0.0) < WatchlingStare.approach(30.0, 1.0));
        assertTrue(WatchlingStare.approach(30.0, 1.0) < 30.0);
    }

    @Test
    void itHuntsForNinetySecondsToThreeMinutes() {
        assertEquals(90 * 20, VersusSettings.Watchlings.VANISH_MIN);
        assertEquals(180 * 20, VersusSettings.Watchlings.VANISH_MAX);
        // And creeps nearer a few times in that time
        assertTrue(VersusSettings.Watchlings.STALK_MAX < VersusSettings.Watchlings.VANISH_MIN / 2);
    }
}
