package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A hunting watchling's gaze holds a stare just begun: only while the static rises and is still thin; then a view near
 * its eyes is drawn onto them, harder the nearer, and never past them; one turned away, not at all.
 */
class WatchlingLureTest {
    private static final double CONE = VersusSettings.Watchlings.LURE_DEGREES;

    @Test
    void itGripsOnlyAStareJustBegun() {
        float thin = VersusSettings.Watchlings.LURE_STATIC / 2;
        assertTrue(WatchlingLure.grips(true, 0.0F));
        assertTrue(WatchlingLure.grips(true, thin));
        // Unseen (the static not rising), it doesn't grip, however thin the static
        assertFalse(WatchlingLure.grips(false, 0.0F));
        assertFalse(WatchlingLure.grips(false, thin));
        // A stare already thick with static is left to the one staring
        assertFalse(WatchlingLure.grips(true, VersusSettings.Watchlings.LURE_STATIC));
        assertFalse(WatchlingLure.grips(true, 0.9F));
    }

    @Test
    void aViewTurnedAwayIsLeftAlone() {
        assertEquals(0.0, WatchlingLure.strength(CONE, 1.0));
        assertEquals(0.0, WatchlingLure.strength(90.0, 1.0));
        assertEquals(0.0, WatchlingLure.strength(180.0, 1.0));
    }

    @Test
    void theNearerTheViewTheHarderItsDrawn() {
        double edge = WatchlingLure.strength(CONE * 0.9, 0.5);
        double halfway = WatchlingLure.strength(CONE * 0.5, 0.5);
        double near = WatchlingLure.strength(CONE * 0.1, 0.5);
        assertTrue(edge > 0.0 && edge < halfway && halfway < near, edge + " " + halfway + " " + near);
        // At the edge of the cone it's hardly felt: a hundredth of the most
        assertTrue(edge < 0.02 * WatchlingLure.strength(0.0, 0.5));
    }

    @Test
    void theNearerItStandsTheHarderItDraws() {
        assertTrue(WatchlingLure.strength(10.0, 0.0) < WatchlingLure.strength(10.0, 1.0));
        assertEquals(1.0, WatchlingLure.closeness(0.0));
        assertEquals(0.0, WatchlingLure.closeness(VersusSettings.Watchlings.LURE_RANGE));
        assertEquals(0.0, WatchlingLure.closeness(VersusSettings.Watchlings.LURE_RANGE * 2));
    }

    @Test
    void theTurnGoesStraightAtItAndNeverPast() {
        float[] small = WatchlingLure.turn(3.0F, 4.0F, 1.0);
        assertEquals(0.6F, small[0], 1.0E-5F);
        assertEquals(0.8F, small[1], 1.0E-5F);
        float[] whole = WatchlingLure.turn(3.0F, -4.0F, 50.0);
        assertEquals(3.0F, whole[0], 1.0E-5F);
        assertEquals(-4.0F, whole[1], 1.0E-5F);
        float[] none = WatchlingLure.turn(0.0F, 0.0F, 5.0);
        assertEquals(0.0F, none[0]);
        assertEquals(0.0F, none[1]);
    }
}
