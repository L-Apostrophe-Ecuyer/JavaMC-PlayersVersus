package frootloops.versus.mod.mobs.melee;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A mob's melee swing. On the server, a brain mob's regular swing (goal mobs time theirs in the goal) or any mob's leap
 * ({@link MobMelee}); on clients, what the latest {@link MobMeleePayload}s said, in the mob's tick count, and how to
 * draw it ({@link #look}).
 */
public final class MeleeState {
    public static final int NONE = Integer.MIN_VALUE;

    /** Ticks the drawn-back arm, or the raised ones, take to come through after the strike. */
    static final float RELEASE_TICKS = 3.0F;
    /** Ticks the angry brows stay after the strike. */
    static final float ANGRY_AFTER_TICKS = 8.0F;
    /** A miss's lunge: forward over the first ticks, held, then back up by the last. */
    static final float LUNGE_RISE_TICKS = 3.0F, LUNGE_HOLD_TICKS = 5.0F, LUNGE_TICKS = 14.0F;
    /** A leap: ticks to sink into the crouch, to rise out of it at take-off, and to raise the arms overhead. */
    static final float CROUCH_IN_TICKS = 3.0F, UNCROUCH_TICKS = 2.0F, RAISE_TICKS = 3.0F;
    /** Ticks past its end a wind-up, crouch or leap waits for what comes next before it's dropped (a lost packet). */
    static final float LOST_STRIKE_TICKS = 10.0F;

    /** Where a swing is, on the server: a brain mob's regular wind-up, or a leap's crouch, flight and landing. */
    enum Phase {
        NONE, WIND_UP, CROUCH, LEAP, LANDED;

        /** From the crouch until the mob is back on its feet. */
        boolean leaping() {
            return this == CROUCH || this == LEAP || this == LANDED;
        }
    }

    // Server: the swing in progress.
    Phase phase = Phase.NONE;
    /** Ticks left to a wind-up's strike, a crouch's take-off or the getting up after a leap; in a leap, ticks up. */
    int ticks;
    @Nullable
    LivingEntity target;
    /** A leap's aim: where the target stood as the mob crouched, its eye height there, and how far short to land. */
    Vec3 spot = Vec3.ZERO;
    double aimY;
    double standoff;
    /** A leap's expected ticks in the air, and its speed forward. */
    int flight = 1;
    double leapX, leapZ;

    // Clients: the latest swing.
    boolean heavy;
    int windUpStart = NONE;
    int windUpTicks = 1;
    int leapTick = NONE;
    int leapTicks = 1;
    int strikeTick = NONE;
    boolean missed;

    public static MeleeState of(Mob mob) {
        return ((MeleeHolder) mob).playersVersus$melee();
    }

    @Nullable
    public static MeleeState peek(Mob mob) {
        return ((MeleeHolder) mob).playersVersus$meleeIfAny();
    }

    /** Server: a leap's strike is over; the mob gets up over {@code ticks}. */
    void land(int ticks) {
        this.phase = Phase.LANDED;
        this.ticks = ticks;
        this.target = null;
    }

    /** Server: no swing any more. */
    void end() {
        this.phase = Phase.NONE;
        this.target = null;
    }

    /** Clients: a swing event for this mob, at its tick count {@code now}. */
    public void onEvent(byte action, int ticks, int now) {
        switch (action) {
            case MobMeleePayload.WIND_UP, MobMeleePayload.HEAVY_WIND_UP -> {
                this.heavy = action == MobMeleePayload.HEAVY_WIND_UP;
                this.windUpStart = now;
                this.windUpTicks = Math.max(ticks, 1);
                this.leapTick = NONE;
                this.strikeTick = NONE;
                this.missed = false;
            }
            case MobMeleePayload.LEAP -> {
                if (!this.heavy || this.windUpStart == NONE) {
                    this.heavy = true;
                    this.windUpStart = now - (int) CROUCH_IN_TICKS;
                }
                this.leapTick = now;
                this.leapTicks = Math.max(ticks, 1);
                this.strikeTick = NONE;
            }
            case MobMeleePayload.HIT, MobMeleePayload.MISS -> {
                if (this.windUpStart == NONE) this.windUpStart = now - this.windUpTicks;
                this.strikeTick = now;
                this.missed = action == MobMeleePayload.MISS;
            }
            default -> this.forget();
        }
    }

    /**
     * How a swing looks at the mob's tick count {@code now} (with the partial tick), each part 0 to 1: a regular swing's
     * arm drawn back; a leap's crouch, then its arms raised overhead in the air, brought down by the strike; a miss's
     * lunge after; and whether the mob shows its angry brows. A swing that has played out is forgotten.
     */
    public Look look(float now) {
        if (this.windUpStart == NONE) return Look.NONE;
        if (this.strikeTick != NONE) return this.afterStrike(now - this.strikeTick);
        if (!this.heavy) {
            float wound = (now - this.windUpStart) / this.windUpTicks;
            if (wound > 1.0F + LOST_STRIKE_TICKS / this.windUpTicks) return this.forget();
            return new Look(ease(wound), 0.0F, 0.0F, 0.0F, true);
        }
        float crouch = ease((now - this.windUpStart) / CROUCH_IN_TICKS);
        if (this.leapTick == NONE) {
            if (now - this.windUpStart > this.windUpTicks + LOST_STRIKE_TICKS) return this.forget();
            return new Look(0.0F, crouch, 0.0F, 0.0F, true);
        }
        float flying = now - this.leapTick;
        if (flying > this.leapTicks + LOST_STRIKE_TICKS) return this.forget();
        return new Look(0.0F, crouch * (1.0F - ease(flying / UNCROUCH_TICKS)), ease(flying / RAISE_TICKS), 0.0F, true);
    }

    private Look afterStrike(float since) {
        if (since > Math.max(LUNGE_TICKS, ANGRY_AFTER_TICKS)) return this.forget();
        float release = 1.0F - ease(since / RELEASE_TICKS);
        float lunge = this.missed ? lunge(since) : 0.0F;
        boolean angry = since < ANGRY_AFTER_TICKS;
        return this.heavy ? new Look(0.0F, 0.0F, release, lunge, angry) : new Look(release, 0.0F, 0.0F, lunge, angry);
    }

    private Look forget() {
        this.windUpStart = NONE;
        this.leapTick = NONE;
        this.strikeTick = NONE;
        return Look.NONE;
    }

    /** The miss's lunge {@code since} ticks after the strike, 0 to 1. */
    static float lunge(float since) {
        if (since <= 0.0F) return 0.0F;
        if (since < LUNGE_RISE_TICKS) return ease(since / LUNGE_RISE_TICKS);
        if (since < LUNGE_HOLD_TICKS) return 1.0F;
        return 1.0F - ease((since - LUNGE_HOLD_TICKS) / (LUNGE_TICKS - LUNGE_HOLD_TICKS));
    }

    /** Smoothstep over {@code t}, clamped to 0 to 1. */
    private static float ease(float t) {
        t = Math.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    public record Look(float drawBack, float crouch, float raise, float lunge, boolean angry) {
        public static final Look NONE = new Look(0.0F, 0.0F, 0.0F, 0.0F, false);
    }
}
