package frootloops.versus.mod.mobs.melee;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.Combat;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;

/**
 * Mob melee as a swing a player can read and dodge: a wind-up, then a strike that hits if the target is still in reach
 * and in sight, and misses otherwise. Most swings are regular; some are heavy, with a long wind-up, half again the
 * damage, more knockback and a longer recovery, and a hit doesn't cut them short. A miss costs recovery too.
 *
 * <p>Mobs on {@code MeleeAttackGoal} time their swings in the goal (MeleeAttackGoalMixin); mobs on the brain's
 * {@code MeleeAttack} behaviour start them there (MeleeAttackMixin) and count them down here every tick. Clients get
 * each wind-up, hit, miss and cut-short swing to animate ({@link MobMeleePayload}).
 */
public final class MobMelee {

    public enum Kind { REGULAR, HEAVY }

    /** Ticks from the start of a swing to its strike: regular, regular for arthropods (spiders), heavy. */
    public static final int REGULAR_WIND_UP = 8, ARTHROPOD_WIND_UP = 4, HEAVY_WIND_UP = 20;
    /** How often a swing is heavy. */
    public static final float HEAVY_CHANCE = 0.2F;
    /** Recovery added after a heavy swing, and after a miss. */
    public static final int HEAVY_EXTRA_RECOVERY = 10, MISS_EXTRA_RECOVERY = 8;

    private static final Identifier HEAVY_DAMAGE_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "heavy_swing_damage");
    private static final Identifier HEAVY_KNOCKBACK_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "heavy_swing_knockback");
    private static final AttributeModifier HEAVY_DAMAGE = new AttributeModifier(HEAVY_DAMAGE_ID, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final AttributeModifier HEAVY_KNOCKBACK = new AttributeModifier(HEAVY_KNOCKBACK_ID, 0.6, AttributeModifier.Operation.ADD_VALUE);

    private MobMelee() {
    }

    /** A new swing's kind: heavy now and then, but never for spiders and the like, or the warden. */
    public static Kind pickKind(Mob mob) {
        if (mob.is(EntityTypeTags.ARTHROPOD) || mob instanceof Warden) return Kind.REGULAR;
        return mob.getRandom().nextFloat() < HEAVY_CHANCE ? Kind.HEAVY : Kind.REGULAR;
    }

    public static int windUpTicks(Mob mob, Kind kind) {
        if (kind == Kind.HEAVY) return HEAVY_WIND_UP;
        return mob.is(EntityTypeTags.ARTHROPOD) ? ARTHROPOD_WIND_UP : REGULAR_WIND_UP;
    }

    /** Whether the mob is close enough that any swing reaches: eye to eye or foot to foot within about a block. */
    public static boolean inCloseQuarters(Mob mob, LivingEntity target) {
        return target.getEyePosition().distanceToSqr(mob.getEyePosition()) < 1.5 || target.position().distanceToSqr(mob.position()) < 1.5;
    }

    /** Whether a swing reaches the target: in close quarters, or facing it with it inside the attack box. */
    public static boolean reaches(Mob mob, LivingEntity target, boolean jumping) {
        return inCloseQuarters(mob, target) || Combat.isLookingTowards(mob, target.getEyePosition(), true)
                && Combat.getMobAttackBox(mob, jumping).intersects(Combat.getEntityHitbox(target));
    }

    /** Whether a hit since the mob's last tick cuts a regular swing short. */
    public static boolean interrupted(Mob mob) {
        return mob.hurtTime > 0 && mob.hurtTime >= mob.hurtDuration - 1;
    }

    /** A swing begins: clients wind it up for {@code ticks}. */
    public static void windUp(Mob mob, Kind kind, int ticks) {
        send(mob, kind == Kind.HEAVY ? MobMeleePayload.HEAVY_WIND_UP : MobMeleePayload.WIND_UP, ticks);
    }

    /** A swing is cut short before its strike. */
    public static void cancel(Mob mob) {
        send(mob, MobMeleePayload.CANCEL, 0);
    }

    /**
     * The strike at the end of a wind-up: the mob swings, and hurts the target if the swing {@code lands} (harder for a
     * heavy one) or whiffs. Returns whether it landed.
     */
    public static boolean strike(ServerLevel level, Mob mob, LivingEntity target, Kind kind, boolean lands) {
        mob.swingForAttack(InteractionHand.MAIN_HAND);
        if (lands) {
            send(mob, MobMeleePayload.HIT, 0);
            if (hurt(level, mob, target, kind)) mob.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 0.6F, 1.4F);
        } else {
            send(mob, MobMeleePayload.MISS, 0);
            mob.playSound(SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.2F, 0.9F);
        }
        return lands;
    }

    private static boolean hurt(ServerLevel level, Mob mob, LivingEntity target, Kind kind) {
        if (kind != Kind.HEAVY) return mob.doHurtTarget(level, target);
        AttributeInstance damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance knockback = mob.getAttribute(Attributes.ATTACK_KNOCKBACK);
        if (damage != null) damage.addOrUpdateTransientModifier(HEAVY_DAMAGE);
        if (knockback != null) knockback.addOrUpdateTransientModifier(HEAVY_KNOCKBACK);
        try {
            return mob.doHurtTarget(level, target);
        } finally {
            if (damage != null) damage.removeModifier(HEAVY_DAMAGE_ID);
            if (knockback != null) knockback.removeModifier(HEAVY_KNOCKBACK_ID);
        }
    }

    // Brain mobs (the MeleeAttack behaviour).

    /** Whether a brain mob's melee winds up here rather than hitting at once: hostile ones, so not axolotls. */
    public static boolean windsUp(Mob mob) {
        return mob instanceof Enemy;
    }

    /** A brain mob starts a swing at the target, in place of MeleeAttack's instant hit. */
    public static void startSwing(Mob mob, LivingEntity target) {
        MeleeState state = MeleeState.of(mob);
        if (state.windUpLeft > 0) return;
        state.kind = pickKind(mob);
        state.windUpLeft = windUpTicks(mob, state.kind);
        state.target = target;
        windUp(mob, state.kind, state.windUpLeft);
    }

    /** The ticks a brain mob's swing still takes, which MeleeAttack's cooldown waits out on top of its own. */
    public static int swingTicksLeft(Mob mob) {
        MeleeState state = MeleeState.peek(mob);
        if (state == null || state.windUpLeft <= 0) return 0;
        return state.windUpLeft + (state.kind == Kind.HEAVY ? HEAVY_EXTRA_RECOVERY : 0);
    }

    /** Every server tick of a mob: counts a brain mob's swing down to its strike. */
    public static void tick(Mob mob) {
        MeleeState state = MeleeState.peek(mob);
        if (state == null || state.windUpLeft <= 0) return;
        LivingEntity target = state.target;
        if (target == null || !target.isAlive() || state.kind == Kind.REGULAR && interrupted(mob)) {
            state.windUpLeft = 0;
            state.target = null;
            cancel(mob);
            return;
        }
        if (--state.windUpLeft > 0) return;
        state.target = null;
        boolean lands = (inCloseQuarters(mob, target) || mob.isWithinMeleeAttackRange(target)) && mob.hasLineOfSight(target);
        strike((ServerLevel) mob.level(), mob, target, state.kind, lands);
    }

    private static void send(Mob mob, byte action, int ticks) {
        if (!(mob.level() instanceof ServerLevel)) return;
        MobMeleePayload payload = new MobMeleePayload(mob.getId(), action, ticks);
        for (ServerPlayer player : PlayerLookup.tracking(mob)) {
            if (ServerPlayNetworking.canSend(player, MobMeleePayload.TYPE.id())) ServerPlayNetworking.send(player, payload);
        }
    }
}
