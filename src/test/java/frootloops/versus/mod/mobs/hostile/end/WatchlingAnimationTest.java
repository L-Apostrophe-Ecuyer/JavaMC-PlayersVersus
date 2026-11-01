package frootloops.versus.mod.mobs.hostile.end;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How a watchling moves: its jaw and head ease into place, and a strike sweeps out fast and comes back slower. */
class WatchlingAnimationTest {

    @Test
    void easingReachesItsTargetInSteadyStepsAndNeverOvershoots() {
        float jaw = 0.0F;
        int ticks = 0;
        while (jaw < 1.0F) {
            float next = WatchlingAnimation.ease(jaw, 1.0F, 1.0F / WatchlingAnimation.JAW_TICKS);
            assertTrue(next > jaw && next <= 1.0F, jaw + " -> " + next);
            jaw = next;
            ticks++;
        }
        assertEquals(WatchlingAnimation.JAW_TICKS, ticks);
        assertEquals(1.0F, WatchlingAnimation.ease(1.0F, 1.0F, 0.25F));
        // And back down, stopping at nothing
        assertEquals(0.0F, WatchlingAnimation.ease(0.1F, 0.0F, 0.25F));
    }

    @Test
    void aSmoothStepStartsAndEndsGently() {
        assertEquals(0.0F, WatchlingAnimation.smooth(0.0F));
        assertEquals(1.0F, WatchlingAnimation.smooth(1.0F));
        assertEquals(0.5F, WatchlingAnimation.smooth(0.5F), 1.0E-6F);
        assertTrue(WatchlingAnimation.smooth(0.1F) < 0.1F && WatchlingAnimation.smooth(0.9F) > 0.9F);
    }

    @Test
    void aStrikeSweepsOutFastAndComesBackSlower() {
        assertEquals(0.0F, WatchlingAnimation.strike(0.0F));
        assertEquals(0.0F, WatchlingAnimation.strike(1.0F));
        float peak = 0.0F, peakAt = 0.0F;
        for (int i = 1; i < 100; i++) {
            float swing = i / 100.0F, out = WatchlingAnimation.strike(swing);
            assertTrue(out > 0.0F && out <= 1.0F, swing + ": " + out);
            if (out > peak) {
                peak = out;
                peakAt = swing;
            }
        }
        assertTrue(peak > 0.99F, "it never swings all the way: " + peak);
        assertTrue(peakAt < 0.35F, "it peaks late, at " + peakAt);
    }

    @Test
    void theHeadLiftsOffItsJawByLessThanTheJawIsTall() {
        // The top of the jaw stays inside the lifted head, so the mouth never shows a gap above it
        assertTrue(WatchlingAnimation.JAW_LIFT < WatchlingAnimation.JAW_HEIGHT);
        assertTrue(WatchlingAnimation.JAW_HEIGHT < WatchlingAnimation.HEAD_HEIGHT);
    }
}
