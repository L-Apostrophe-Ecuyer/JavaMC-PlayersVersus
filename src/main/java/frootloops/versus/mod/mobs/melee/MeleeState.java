package frootloops.versus.mod.mobs.melee;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

/**
 * A mob's melee swing. On the server, the swing a brain mob is winding up (goal mobs keep theirs in the goal); on
 * clients, what the latest {@link MobMeleePayload}s said, in the mob's tick count, and how to draw it ({@link #look}).
 */
public final class MeleeState {
    public static final int NONE = Integer.MIN_VALUE;

    /** Ticks the drawn-back arm takes to come through after the strike. */
    static final float RELEASE_TICKS = 3.0F;
    /** Ticks the angry brows stay after the strike. */
    static final float ANGRY_AFTER_TICKS = 8.0F;
    /** A miss's lunge: forward over the first ticks, held, then back up by the last. */
    static final float LUNGE_RISE_TICKS = 3.0F, LUNGE_HOLD_TICKS = 5.0F, LUNGE_TICKS = 14.0F;
    /** Ticks past its end a wind-up waits for its strike before it's dropped (a lost or late packet). */
    static final float LOST_STRIKE_TICKS = 10.0F;

    // Server, brain mobs: the swing in progress.
    MobMelee.Kind kind = MobMelee.Kind.REGULAR;
    int windUpLeft;
    @Nullable
    LivingEntity target;

    // Clients: the latest swing.
    boolean heavy;
    int windUpStart = NONE;
    int windUpTicks = 1;
    int strikeTick = NONE;
    boolean missed;

    public static MeleeState of(Mob mob) {
        return ((MeleeHolder) mob).playersVersus$melee();
    }

    @Nullable
    public static MeleeState peek(Mob mob) {
        return ((MeleeHolder) mob).playersVersus$meleeIfAny();
    }

    /** Clients: a swing event for this mob, at its tick count {@code now}. */
    public void onEvent(byte action, int ticks, int now) {
        switch (action) {
            case MobMeleePayload.WIND_UP, MobMeleePayload.HEAVY_WIND_UP -> {
                this.heavy = action == MobMeleePayload.HEAVY_WIND_UP;
                this.windUpStart = now;
                this.windUpTicks = Math.max(ticks, 1);
                this.strikeTick = NONE;
                this.missed = false;
            }
            case MobMeleePayload.HIT, MobMeleePayload.MISS -> {
                if (this.windUpStart == NONE) this.windUpStart = now - this.windUpTicks;
                this.strikeTick = now;
                this.missed = action == MobMeleePayload.MISS;
            }
            default -> {
                this.windUpStart = NONE;
                this.strikeTick = NONE;
            }
        }
    }

    /**
     * How a swing looks at the mob's tick count {@code now} (with the partial tick): how far it's wound up, 0 to 1
     * (the arm drawn back, or both arms raised for a heavy swing), how far a miss lunges, 0 to 1, and whether the mob
     * shows its angry brows. A swing that has played out is forgotten.
     */
    public Look look(float now) {
        if (this.windUpStart == NONE) return Look.NONE;
        if (this.strikeTick == NONE) {
            float wound = (now - this.windUpStart) / this.windUpTicks;
            if (wound > 1.0F + LOST_STRIKE_TICKS / this.windUpTicks) {
                this.windUpStart = NONE;
                return Look.NONE;
            }
            return new Look(ease(Mth.clamp(wound, 0.0F, 1.0F)), this.heavy, 0.0F, true);
        }
        float since = now - this.strikeTick;
        if (since > Math.max(LUNGE_TICKS, ANGRY_AFTER_TICKS)) {
            this.windUpStart = NONE;
            this.strikeTick = NONE;
            return Look.NONE;
        }
        float windUp = 1.0F - ease(Mth.clamp(since / RELEASE_TICKS, 0.0F, 1.0F));
        return new Look(windUp, this.heavy, this.missed ? lunge(since) : 0.0F, since < ANGRY_AFTER_TICKS);
    }

    /** The miss's lunge {@code since} ticks after the strike, 0 to 1. */
    static float lunge(float since) {
        if (since <= 0.0F) return 0.0F;
        if (since < LUNGE_RISE_TICKS) return ease(since / LUNGE_RISE_TICKS);
        if (since < LUNGE_HOLD_TICKS) return 1.0F;
        return 1.0F - ease(Mth.clamp((since - LUNGE_HOLD_TICKS) / (LUNGE_TICKS - LUNGE_HOLD_TICKS), 0.0F, 1.0F));
    }

    private static float ease(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    public record Look(float windUp, boolean heavy, float lunge, boolean angry) {
        public static final Look NONE = new Look(0.0F, false, 0.0F, false);
    }
}
