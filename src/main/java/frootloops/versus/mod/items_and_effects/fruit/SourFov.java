package frootloops.versus.mod.items_and_effects.fruit;

import frootloops.versus.VersusSettings;

/**
 * How far Sour widens the view, from what is left of it: at once (over {@code SOUR_RISE_TICKS}) to {@code SOUR_PEAK}
 * times the field of view, held, then eased back to nothing over the last {@code SOUR_FALL_TICKS}. Scaled by the FOV
 * Effects slider (0 leaves the view alone), and never past {@code SOUR_MAX_FOV}. Pure, so the client's camera
 * (CameraSourMixin) and the tests share it.
 */
public final class SourFov {

    private SourFov() {
    }

    /** The multiplier with this much of Sour left, in ticks (fractional, between ticks), out of a Sour this long. */
    public static float multiplier(float remaining, int duration) {
        if (remaining <= 0) return 1.0F;
        float peak = VersusSettings.Lemons.SOUR_PEAK;
        float rise = Math.max(1, VersusSettings.Lemons.SOUR_RISE_TICKS), fall = Math.max(1, VersusSettings.Lemons.SOUR_FALL_TICKS);
        // A Sour longer than a lemon's (a command's) has risen already: no rise to wait for
        float elapsed = remaining > duration ? rise : duration - remaining;
        float up = elapsed >= rise ? 1.0F : easeOut(elapsed / rise);
        float down = remaining >= fall ? 1.0F : smooth(remaining / fall);
        return 1.0F + (peak - 1.0F) * Math.min(up, down);
    }

    /** The field of view with Sour applied to it, the FOV Effects slider scaling how much. */
    public static float apply(float fov, float remaining, int duration, float effectScale) {
        float widened = fov * (1.0F + (multiplier(remaining, duration) - 1.0F) * Math.clamp(effectScale, 0.0F, 1.0F));
        return Math.max(fov, Math.min(widened, VersusSettings.Lemons.SOUR_MAX_FOV));
    }

    private static float easeOut(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t);
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }
}
