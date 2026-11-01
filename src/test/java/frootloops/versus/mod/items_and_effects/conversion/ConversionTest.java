package frootloops.versus.mod.items_and_effects.conversion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Conversion's numbers: white hearts, stacking doses, the regeneration cap and when all hearts are white. */
class ConversionTest {

    @Test
    void anAmplifierWhitensOneHeartMoreThanItself() {
        assertEquals(1, Conversion.whiteHearts(0));
        assertEquals(4, Conversion.whiteHearts(3));
    }

    @Test
    void eachDoseAddsItsHeartsToThoseHad() {
        // A first dose: one white heart (amplifier 0); a second: two (1); a stronger one on top adds its own
        assertEquals(1, Conversion.stackedAmplifier(0, 0));
        assertEquals(2, Conversion.stackedAmplifier(1, 0));
        assertEquals(5, Conversion.stackedAmplifier(2, 2));
    }

    @Test
    void aDoseRefreshesTheEffectButNeverShortensIt() {
        // A two-minute dose on a minute left: two minutes; rotten flesh's minute on a minute and a half: as it was
        assertEquals(2400, Conversion.stackedDuration(1200, 2400));
        assertEquals(1800, Conversion.stackedDuration(1800, 1200));
        // An endless one stays endless
        assertEquals(-1, Conversion.stackedDuration(-1, 2400));
    }

    @Test
    void foodRegeneratesOnlyUpToTheWhiteHearts() {
        assertEquals(20.0F, Conversion.regenCap(20.0F, 0));
        assertEquals(14.0F, Conversion.regenCap(20.0F, 3));
        assertEquals(0.0F, Conversion.regenCap(20.0F, 12));
    }

    @Test
    void whiteHeartsStartAtTheEndOfTheBar() {
        // Ten hearts, three white: the last three
        assertFalse(Conversion.isWhite(6, 10, 3));
        assertTrue(Conversion.isWhite(7, 10, 3));
        assertTrue(Conversion.isWhite(9, 10, 3));
        // Absorption hearts, after the bar's, are never white
        assertFalse(Conversion.isWhite(10, 10, 3));
        assertFalse(Conversion.isWhite(0, 10, 0));
    }

    @Test
    void allHeartsWhiteTurnsUndead() {
        assertFalse(Conversion.allWhite(20.0F, 9));
        assertTrue(Conversion.allWhite(20.0F, 10));
        assertTrue(Conversion.allWhite(20.0F, 14));
        // Half a heart counts as one
        assertFalse(Conversion.allWhite(21.0F, 10));
        assertTrue(Conversion.allWhite(21.0F, 11));
        assertFalse(Conversion.allWhite(20.0F, 0));
    }

    @Test
    void theWorldGreysWithTheWhiteHeartsAndTheTimeLeft() {
        // 20 health: 10 hearts. No Conversion, no grey; all white with time to spare, all grey (step 4)
        assertEquals(0, Conversion.greyStep(-1, 20.0F, 0));
        assertEquals(4, Conversion.greyStep(9, 20.0F, 2400));
        // Half white: half grey (step 2); a heart: a little (step 1)
        assertEquals(2, Conversion.greyStep(4, 20.0F, 2400));
        assertEquals(1, Conversion.greyStep(0, 20.0F, 2400));
        // All white, wearing off in its last half minute: the grey fades with it
        assertEquals(2, Conversion.greyStep(9, 20.0F, 300));
        // Endless: as grey as its hearts
        assertEquals(4, Conversion.greyStep(9, 20.0F, -1));
    }
}
