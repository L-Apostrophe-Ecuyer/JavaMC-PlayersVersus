package frootloops.versus.mod.items_and_effects.fruit;

import frootloops.versus.VersusSettings;

/**
 * How far Sour widens the view, from what is left of it: at once (over {@code SOUR_RISE_TICKS}) to {@code SOUR_PEAK}
 * times the field of view, held, then eased back to nothing over the last {@code SOUR_FALL_TICKS}. Scaled by the FOV
 * Effects slider (0 leaves the view alone), and never past {@code SOUR_MAX_FOV}. A Sour renewed while it lasts (another
 * lemon eaten while sour) holds the view at its peak rather than drop it to normal and widen it again: the camera
 * (CameraSourMixin) keeps a {@link Tracker} to tell. Pure otherwise, so the camera and the tests share it.
 */
public final class SourFov {

    private SourFov() {
    }

    /** The multiplier with this much of Sour left, in ticks (fractional, between ticks), out of a Sour this long. */
    public static float multiplier(float remaining, int duration) {
        return multiplier(remaining, duration, false);
    }

    /** As {@link #multiplier(float, int)}, for a Sour that has {@code risen} already (renewed while it lasted) or not. */
    public static float multiplier(float remaining, int duration, boolean risen) {
        if (remaining <= 0) return 1.0F;
        float peak = VersusSettings.Lemons.SOUR_PEAK;
        float rise = Math.max(1, VersusSettings.Lemons.SOUR_RISE_TICKS), fall = Math.max(1, VersusSettings.Lemons.SOUR_FALL_TICKS);
        // A renewed Sour, or one longer than a lemon's (a command's), has risen already: no rise to wait for
        float elapsed = risen || remaining > duration ? rise : duration - remaining;
        float up = elapsed >= rise ? 1.0F : easeOut(elapsed / rise);
        float down = remaining >= fall ? 1.0F : smooth(remaining / fall);
        return 1.0F + (peak - 1.0F) * Math.min(up, down);
    }

    /** The field of view with Sour applied to it, the FOV Effects slider scaling how much. */
    public static float apply(float fov, float remaining, int duration, float effectScale) {
        return widen(fov, multiplier(remaining, duration), effectScale);
    }

    /** The field of view widened by this multiplier, the FOV Effects slider scaling how much, never past the cap. */
    public static float widen(float fov, float multiplier, float effectScale) {
        float widened = fov * (1.0F + (multiplier - 1.0F) * Math.clamp(effectScale, 0.0F, 1.0F));
        return Math.max(fov, Math.min(widened, VersusSettings.Lemons.SOUR_MAX_FOV));
    }

    /**
     * What the camera remembers of Sour from frame to frame: whether the Sour it sees was renewed while it lasted (its time
     * left went up: another lemon eaten while sour). Once Sour is over, the next one widens the view from nothing.
     */
    public static final class Tracker {

        private float last;
        private boolean renewed;

        /** The multiplier this frame, with this much of Sour left (0 or less: none). */
        public float multiplier(float remaining, int duration) {
            if (remaining <= 0) {
                this.last = 0.0F;
                this.renewed = false;
                return 1.0F;
            }
            if (this.last > 0 && remaining > this.last) this.renewed = true;
            this.last = remaining;
            return SourFov.multiplier(remaining, duration, this.renewed);
        }
    }

    private static float easeOut(float t) {
        return 1.0F - (1.0F - t) * (1.0F - t);
    }

    private static float smooth(float t) {
        return t * t * (3.0F - 2.0F * t);
    }
}
