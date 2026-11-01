package frootloops.versus.mod.items_and_effects.blight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Blight's numbers: white hearts, stacking doses, the regeneration cap and when all hearts are white. */
class BlightTest {

    @Test
    void anAmplifierWhitensOneHeartMoreThanItself() {
        assertEquals(1, Blight.whiteHearts(0));
        assertEquals(4, Blight.whiteHearts(3));
    }

    @Test
    void eachDoseAddsItsHeartsToThoseHad() {
        // A first dose: one white heart (amplifier 0); a second: two (1); a stronger one on top adds its own
        assertEquals(1, Blight.stackedAmplifier(0, 0));
        assertEquals(2, Blight.stackedAmplifier(1, 0));
        assertEquals(5, Blight.stackedAmplifier(2, 2));
    }

    @Test
    void aDoseRefreshesTheEffectButNeverShortensIt() {
        // A two-minute dose on a minute left: two minutes; rotten flesh's minute on a minute and a half: as it was
        assertEquals(2400, Blight.stackedDuration(1200, 2400));
        assertEquals(1800, Blight.stackedDuration(1800, 1200));
        // An endless one stays endless
        assertEquals(-1, Blight.stackedDuration(-1, 2400));
    }

    @Test
    void foodRegeneratesOnlyUpToTheWhiteHearts() {
        assertEquals(20.0F, Blight.regenCap(20.0F, 0));
        assertEquals(14.0F, Blight.regenCap(20.0F, 3));
        assertEquals(0.0F, Blight.regenCap(20.0F, 12));
    }

    @Test
    void whiteHeartsStartAtTheEndOfTheBar() {
        // Ten hearts, three white: the last three
        assertFalse(Blight.isWhite(6, 10, 3));
        assertTrue(Blight.isWhite(7, 10, 3));
        assertTrue(Blight.isWhite(9, 10, 3));
        // Absorption hearts, after the bar's, are never white
        assertFalse(Blight.isWhite(10, 10, 3));
        assertFalse(Blight.isWhite(0, 10, 0));
    }

    @Test
    void allHeartsWhiteTurnsUndead() {
        assertFalse(Blight.allWhite(20.0F, 9));
        assertTrue(Blight.allWhite(20.0F, 10));
        assertTrue(Blight.allWhite(20.0F, 14));
        // Half a heart counts as one
        assertFalse(Blight.allWhite(21.0F, 10));
        assertTrue(Blight.allWhite(21.0F, 11));
        assertFalse(Blight.allWhite(20.0F, 0));
    }

    @Test
    void theWorldGreysWithTheWhiteHeartsAndTheTimeLeft() {
        // 20 health: 10 hearts. No Blight, no grey; all white with time to spare, all grey
        assertEquals(0.0F, Blight.greyStrength(-1, 20.0F, 0), 1.0E-6F);
        assertEquals(1.0F, Blight.greyStrength(9, 20.0F, 2400), 1.0E-6F);
        // As grey as the hearts are white: half, a tenth
        assertEquals(0.5F, Blight.greyStrength(4, 20.0F, 2400), 1.0E-6F);
        assertEquals(0.1F, Blight.greyStrength(0, 20.0F, 2400), 1.0E-6F);
        // All white, wearing off in its last half minute: the grey fades with it
        assertEquals(0.5F, Blight.greyStrength(9, 20.0F, 300), 1.0E-6F);
        // Endless: as grey as its hearts
        assertEquals(1.0F, Blight.greyStrength(9, 20.0F, -1), 1.0E-6F);
    }
}
