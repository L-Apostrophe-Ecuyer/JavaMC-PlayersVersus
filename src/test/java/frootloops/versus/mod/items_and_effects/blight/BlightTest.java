package frootloops.versus.mod.items_and_effects.blight;

import frootloops.versus.VersusSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Blight's numbers: blighted hearts, stacking doses, the regeneration cap, being fully blighted and its damage. */
class BlightTest {

    @Test
    void anAmplifierBlightsOneHeartMoreThanItself() {
        assertEquals(1, Blight.blightedHearts(0));
        assertEquals(4, Blight.blightedHearts(3));
    }

    @Test
    void eachDoseAddsItsHeartsToThoseHad() {
        // A first dose: one blighted heart (amplifier 0); a second: two (1); a stronger one on top adds its own
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
    void foodRegeneratesOnlyUpToTheBlightedHearts() {
        assertEquals(20.0F, Blight.regenCap(20.0F, 0));
        assertEquals(14.0F, Blight.regenCap(20.0F, 3));
        assertEquals(0.0F, Blight.regenCap(20.0F, 12));
    }

    @Test
    void blightedHeartsStartAtTheEndOfTheBar() {
        // Ten hearts, three blighted: the last three
        assertFalse(Blight.isBlighted(6, 10, 3));
        assertTrue(Blight.isBlighted(7, 10, 3));
        assertTrue(Blight.isBlighted(9, 10, 3));
        // Absorption hearts, after the bar's, are never blighted
        assertFalse(Blight.isBlighted(10, 10, 3));
        assertFalse(Blight.isBlighted(0, 10, 0));
    }

    @Test
    void asManyBlightedHeartsAsTheBarHasIsFullyBlighted() {
        assertFalse(Blight.fullyBlighted(20.0F, 9));
        assertTrue(Blight.fullyBlighted(20.0F, 10));
        assertTrue(Blight.fullyBlighted(20.0F, 14));
        // Max health / 2: half a heart counts as one (21 health is 10.5 hearts, met by 11)
        assertFalse(Blight.fullyBlighted(21.0F, 10));
        assertTrue(Blight.fullyBlighted(21.0F, 11));
        assertFalse(Blight.fullyBlighted(20.0F, 0));
    }

    @Test
    void theWorldGreysWithTheBlightedHeartsAndTheTimeLeft() {
        // 20 health: 10 hearts. No Blight, no grey; fully blighted with time to spare, all grey
        assertEquals(0.0F, Blight.greyStrength(-1, 20.0F, 0), 1.0E-6F);
        assertEquals(1.0F, Blight.greyStrength(9, 20.0F, 2400), 1.0E-6F);
        // As grey as the hearts are blighted: half, a tenth
        assertEquals(0.5F, Blight.greyStrength(4, 20.0F, 2400), 1.0E-6F);
        assertEquals(0.1F, Blight.greyStrength(0, 20.0F, 2400), 1.0E-6F);
        // Fully blighted, wearing off in its last half minute: the grey fades with it
        assertEquals(0.5F, Blight.greyStrength(9, 20.0F, 300), 1.0E-6F);
        // Endless: as grey as its hearts
        assertEquals(1.0F, Blight.greyStrength(9, 20.0F, -1), 1.0E-6F);
    }

    @Test
    void theShakeBeginsBeforeItsFullAndPeaksWhenItIs() {
        // 20 health: 10 hearts. A third blighted: still; half: a tremor; all of them: the worst of it
        assertEquals(0.0F, Blight.shakeStrength(-1, 20.0F), 1.0E-6F);
        assertEquals(0.0F, Blight.shakeStrength(2, 20.0F), 1.0E-6F);
        float half = Blight.shakeStrength(4, 20.0F);
        float most = Blight.shakeStrength(7, 20.0F);
        assertTrue(0.0F < half && half < most && most < 1.0F, half + ", " + most);
        assertEquals(1.0F, Blight.shakeStrength(9, 20.0F), 1.0E-6F);
        // More than all of them is no more than all
        assertEquals(1.0F, Blight.shakeStrength(30, 20.0F), 1.0E-6F);
    }

    @Test
    void fullyBlightedItTakesAHeartEveryTenTicks() {
        // From the tick it's full: then, and every FULL_DAMAGE_TICKS after
        assertEquals(10, VersusSettings.Blight.FULL_DAMAGE_TICKS);
        assertEquals(2.0F, VersusSettings.Blight.FULL_DAMAGE);
        assertTrue(Blight.damagesNow(0));
        assertFalse(Blight.damagesNow(1));
        assertFalse(Blight.damagesNow(9));
        assertTrue(Blight.damagesNow(10));
        assertTrue(Blight.damagesNow(40));
        assertFalse(Blight.damagesNow(45));
    }

    @Test
    void aWitherIsBlightOfTheSameStrengthUpToALimit() {
        // Wither I: a heart (amplifier 0); Wither III: three
        assertEquals(0, Blight.fromWitherAmplifier(0));
        assertEquals(2, Blight.fromWitherAmplifier(2));
        // A Wither of hundreds of levels (a command's, say) is no more than the limit in one dose
        assertEquals(VersusSettings.Blight.WITHER_MAX_HEARTS - 1, Blight.fromWitherAmplifier(250));
    }

    @Test
    void healingAndAbsorptionPushTheBlightBack() {
        // Healing I heals two hearts, II four; a splash's edge less
        assertEquals(2, Blight.healingPurge(0, 1.0));
        assertEquals(4, Blight.healingPurge(1, 1.0));
        assertEquals(1, Blight.healingPurge(0, 0.5));
        assertEquals(0, Blight.healingPurge(0, 0.0));
        // Absorption: a heart for each it gives (Absorption I is two hearts)
        assertEquals(2, Blight.absorptionPurge(4.0F));
        assertEquals(1, Blight.absorptionPurge(1.0F));
        assertEquals(0, Blight.absorptionPurge(0.0F));
        assertEquals(0, Blight.absorptionPurge(-3.0F));
        // What's left: four blighted hearts less two are two
        assertEquals(1, Blight.purgedAmplifier(3, 2));
        assertEquals(Blight.blightedHearts(3) - 2, Blight.blightedHearts(Blight.purgedAmplifier(3, 2)));
        // As many as there are leaves none (an amplifier below nothing)
        assertTrue(Blight.purgedAmplifier(1, 2) < 0);
        assertTrue(Blight.purgedAmplifier(1, 5) < 0);
        assertEquals(2, Blight.purgedAmplifier(2, 0));
    }
}
