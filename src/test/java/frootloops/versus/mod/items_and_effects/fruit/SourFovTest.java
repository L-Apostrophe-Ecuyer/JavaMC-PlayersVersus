package frootloops.versus.mod.items_and_effects.fruit;

import frootloops.versus.VersusSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SourFovTest {

    private static final int DURATION = VersusSettings.Lemons.SOUR_TICKS;

    @Test
    void theViewWidensAtOnceToItsPeakAndHoldsIt() {
        assertEquals(1.0F, SourFov.multiplier(DURATION, DURATION), 1e-6, "Sour widens the view before it has begun");
        float afterRise = SourFov.multiplier(DURATION - VersusSettings.Lemons.SOUR_RISE_TICKS, DURATION);
        assertEquals(VersusSettings.Lemons.SOUR_PEAK, afterRise, 1e-6, "not at its peak once risen");
        assertEquals(VersusSettings.Lemons.SOUR_PEAK, SourFov.multiplier(VersusSettings.Lemons.SOUR_FALL_TICKS + 1, DURATION), 1e-6, "the peak isn't held");
    }

    @Test
    void itEasesBackSmoothlyToNothing() {
        float previous = Float.MAX_VALUE;
        for (float remaining = VersusSettings.Lemons.SOUR_FALL_TICKS; remaining >= 0; remaining -= 0.25F) {
            float now = SourFov.multiplier(remaining, DURATION);
            assertTrue(now <= previous + 1e-6, "the view widened again while easing back, at " + remaining);
            previous = now;
        }
        assertEquals(1.0F, SourFov.multiplier(0, DURATION), 1e-6, "Sour over and the view still wide");
    }

    @Test
    void theFovEffectsSliderScalesItAndTheCapHolds() {
        float peakTime = VersusSettings.Lemons.SOUR_FALL_TICKS + 1;
        assertEquals(70.0F, SourFov.apply(70.0F, peakTime, DURATION, 0.0F), 1e-4, "FOV Effects at 0% still widened the view");
        assertEquals(70.0F * VersusSettings.Lemons.SOUR_PEAK, SourFov.apply(70.0F, peakTime, DURATION, 1.0F), 1e-3, "not the full peak at 100%");
        assertEquals(70.0F * (1 + (VersusSettings.Lemons.SOUR_PEAK - 1) / 2), SourFov.apply(70.0F, peakTime, DURATION, 0.5F), 1e-3, "not half at 50%");
        assertEquals(VersusSettings.Lemons.SOUR_MAX_FOV, SourFov.apply(110.0F, peakTime, DURATION, 1.0F), 1e-4, "a wide view went past the cap");
        // A view already past the cap (a mod's, a high setting with speed) is never narrowed
        assertEquals(170.0F, SourFov.apply(170.0F, peakTime, DURATION, 1.0F), 1e-4, "Sour narrowed a view wider than its cap");
    }

    @Test
    void aLongerSourHoldsItsPeakLonger() {
        // A command's longer Sour: no rise to wait for, the peak until its last ticks
        assertEquals(VersusSettings.Lemons.SOUR_PEAK, SourFov.multiplier(200, 200 * 2), 1e-6);
        // The camera knows only a lemon's Sour: one longer than that is at its peak from the start
        assertEquals(VersusSettings.Lemons.SOUR_PEAK, SourFov.multiplier(600, DURATION), 1e-6, "a long Sour didn't widen the view");
    }
}
