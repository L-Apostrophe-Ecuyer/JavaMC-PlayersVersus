package frootloops.versus.mod.mobs.hostile.end;

import net.minecraft.util.Mth;

/**
 * How a watchling moves (client WatchlingModel; the numbers and curves are here, pure, to be tested). Haunting, it
 * stands with its long arms hanging and its tall head cocked to one side, swaying a little, as it watches. Fighting, it
 * opens its maw (its head lifts off a jaw that stays, as an angry enderman's does, showing fangs) and reaches forward;
 * it strikes with both arms, the main one sweeping out fast and both coming back slower. Walking, its long limbs swing
 * less than a humanoid's. Model units for lengths, radians for angles.
 */
public final class WatchlingAnimation {
    /** How tall the head and its jaw are, and how far the head lifts off the jaw when fighting. */
    public static final float HEAD_HEIGHT = 12.0F, JAW_HEIGHT = 6.0F, JAW_LIFT = 5.0F;
    /** Ticks for the jaw to open all the way (it shuts as fast), and for the head to cock all the way (and straighten). */
    public static final int JAW_TICKS = 4, HEAD_TILT_TICKS = 30;
    /** How far the head cocks while it haunts, and how much it sways about that. */
    public static final float HEAD_TILT = 0.35F, HEAD_TILT_SWAY = 0.06F;
    /** Walking: this much of a humanoid's arm and leg swing, and the legs never further than LEG_LIMIT. */
    public static final float WALK_SWING = 0.4F, LEG_LIMIT = 0.4F;
    /** Fighting: how far forward the arms reach while it isn't winding up a swing. */
    public static final float FIGHT_REACH = 0.3F;
    /** A strike: how far forward the main arm sweeps, how much of that the other arm follows, how far the head lunges. */
    public static final float STRIKE_ARMS = 1.4F, OFF_ARM = 0.7F, STRIKE_HEAD = 0.15F;

    private WatchlingAnimation() {
    }

    /** A step from {@code value} towards {@code target}, never past it. */
    public static float ease(float value, float target, float step) {
        return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
    }

    /** Smoothstep: 0 to 1, gentle at both ends. */
    public static float smooth(float amount) {
        float t = Math.clamp(amount, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    /**
     * How far out a strike's arms are, 0 to 1, at this point of the swing (0 to 1, vanilla's swing progress): out fast,
     * back slower, nothing before or after.
     */
    public static float strike(float swing) {
        if (swing <= 0.0F || swing >= 1.0F) return 0.0F;
        return Mth.sin(Mth.sqrt(swing) * Mth.PI);
    }
}
