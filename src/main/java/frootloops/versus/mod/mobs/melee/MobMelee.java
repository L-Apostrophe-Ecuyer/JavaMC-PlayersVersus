package frootloops.versus.mod.mobs.melee;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.Combat;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
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
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;

/**
 * Mob melee as a swing a player can read and dodge: a wind-up, then a strike that hits if the target is still in reach
 * and in sight, and misses otherwise, which costs recovery.
 *
 * <p>Now and then a humanoid (a zombie, skeleton, piglin or illager) swings heavy instead, if its target is no higher
 * up: it crouches, growling, then leaps at where the target stood as it crouched, and strikes on the way down. The strike
 * hits only if the target is still about there, and then as a crit: half again the damage, more knockback, and the
 * crit's particles and sound. A hit in the air knocks the mob out of its leap; after its strike it stays down a moment,
 * longer after a miss.
 *
 * <p>Mobs on {@code MeleeAttackGoal} time their regular swings in the goal (MeleeAttackGoalMixin); mobs on the brain's
 * {@code MeleeAttack} behaviour start theirs there (MeleeAttackMixin) and count them down here every tick, and every
 * leap runs here. Clients get each wind-up, crouch, take-off, hit, miss and cut-short swing to animate
 * ({@link MobMeleePayload}).
 */
public final class MobMelee {

    public enum Kind { REGULAR, HEAVY }

    /** Ticks from the start of a regular swing to its strike, and for arthropods (spiders). */
    public static final int REGULAR_WIND_UP = 8, ARTHROPOD_WIND_UP = 4;
    /** How often a humanoid that can leap at its target swings heavy. */
    public static final float HEAVY_CHANCE = 0.2F;
    /** Recovery added after a regular swing misses. */
    public static final int MISS_EXTRA_RECOVERY = 8;
    /** A leap's crouch, and the most ticks it stays up before it strikes anyway. */
    public static final int CROUCH_TICKS = 10, LEAP_TIMEOUT = 30;
    /** Ticks the mob stays down after a leap's strike: after a hit, and after a miss. */
    public static final int LANDED_HIT_TICKS = 6, LANDED_MISS_TICKS = 14;
    /** About how long a leap stays up on level ground, which brain mobs' cooldowns count before it takes off. */
    static final int TYPICAL_LEAP_TICKS = 12;
    /** How far the target can have moved from where it stood and still be hit by a leap, in blocks. */
    static final double SPOT_REACH = 1.0;
    /** The fastest a leap carries the mob forward, in blocks per tick. */
    static final double MAX_LEAP_SPEED = 0.6;

    private static final Identifier HEAVY_DAMAGE_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "heavy_swing_damage");
    private static final Identifier HEAVY_KNOCKBACK_ID = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "heavy_swing_knockback");
    private static final AttributeModifier HEAVY_DAMAGE = new AttributeModifier(HEAVY_DAMAGE_ID, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final AttributeModifier HEAVY_KNOCKBACK = new AttributeModifier(HEAVY_KNOCKBACK_ID, 0.6, AttributeModifier.Operation.ADD_VALUE);

    private MobMelee() {
    }

    /** A new swing's kind: now and then heavy, for a humanoid that can leap at its target. */
    public static Kind pickKind(Mob mob, LivingEntity target) {
        if (!leaps(mob) || !canLeapAt(mob, target)) return Kind.REGULAR;
        return mob.getRandom().nextFloat() < HEAVY_CHANCE ? Kind.HEAVY : Kind.REGULAR;
    }

    /** The humanoid bipeds, which swing heavy by leaping: zombies, skeletons, piglins and illagers. */
    public static boolean leaps(Mob mob) {
        return mob instanceof Zombie || mob instanceof AbstractSkeleton || mob instanceof AbstractPiglin || mob instanceof AbstractIllager;
    }

    /** A leap takes firm footing, and a target on the mob's Y level or below it. */
    static boolean canLeapAt(Mob mob, LivingEntity target) {
        return mob.onGround() && !mob.isPassenger() && !mob.isInWater() && !mob.isInLava() && target.getBlockY() <= mob.getBlockY();
    }

    /** Ticks from the start of a regular swing to its strike. */
    public static int windUpTicks(Mob mob) {
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

    /** Whether a hit since the mob's last tick cuts a regular swing short, or knocks it out of a leap. */
    public static boolean interrupted(Mob mob) {
        return mob.hurtTime > 0 && mob.hurtTime >= mob.hurtDuration - 1;
    }

    /** A regular swing begins: clients wind it up for {@code ticks}. */
    public static void windUp(Mob mob, int ticks) {
        send(mob, MobMeleePayload.WIND_UP, ticks);
    }

    /** A swing is cut short before its strike. */
    public static void cancel(Mob mob) {
        send(mob, MobMeleePayload.CANCEL, 0);
    }

    /**
     * The strike at the end of a swing: the mob swings, and hurts the target if the swing {@code lands} (a heavy one as
     * a crit) or whiffs. Returns whether it landed.
     */
    public static boolean strike(ServerLevel level, Mob mob, LivingEntity target, Kind kind, boolean lands) {
        mob.swingForAttack(InteractionHand.MAIN_HAND);
        if (lands) {
            send(mob, MobMeleePayload.HIT, 0);
            if (kind == Kind.HEAVY) {
                if (hurtAsACrit(level, mob, target)) crit(level, mob, target);
            } else if (mob.doHurtTarget(level, target)) {
                mob.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 0.6F, 1.4F);
            }
        } else {
            send(mob, MobMeleePayload.MISS, 0);
            mob.playSound(SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.2F, 0.9F);
        }
        return lands;
    }

    private static boolean hurtAsACrit(ServerLevel level, Mob mob, LivingEntity target) {
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

    /** The crit's particles on the target, as a player's crit shows them, and its sound. */
    private static void crit(ServerLevel level, Mob mob, LivingEntity target) {
        level.getChunkSource().sendToTrackingPlayersAndSelf(target, new ClientboundAnimatePacket(target, ClientboundAnimatePacket.CRITICAL_HIT));
        mob.playSound(SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.0F);
    }

    // Leaps (heavy swings), for goal and brain mobs alike.

    /** A heavy swing begins: the mob crouches, growling, facing where the target stands now, then leaps there. */
    public static void leap(Mob mob, LivingEntity target) {
        MeleeState state = MeleeState.of(mob);
        state.phase = MeleeState.Phase.CROUCH;
        state.ticks = CROUCH_TICKS;
        state.target = target;
        state.spot = target.position();
        state.aimY = target.getEyeY();
        state.standoff = (mob.getBbWidth() + target.getBbWidth()) / 2.0;
        holdStill(mob);
        face(mob, state);
        mob.playAmbientSound();
        send(mob, MobMeleePayload.HEAVY_WIND_UP, CROUCH_TICKS);
    }

    /** Whether the mob is in a leap, from its crouch until it's back on its feet: it holds still and doesn't swing. */
    public static boolean isLeaping(Mob mob) {
        MeleeState state = MeleeState.peek(mob);
        return state != null && state.phase.leaping();
    }

    // Brain mobs (the MeleeAttack behaviour).

    /** Whether a brain mob's melee winds up here rather than hitting at once: hostile ones, so not axolotls. */
    public static boolean windsUp(Mob mob) {
        return mob instanceof Enemy;
    }

    /** A brain mob starts a swing at the target, in place of MeleeAttack's instant hit. */
    public static void startSwing(Mob mob, LivingEntity target) {
        MeleeState state = MeleeState.of(mob);
        if (state.phase != MeleeState.Phase.NONE) return;
        if (pickKind(mob, target) == Kind.HEAVY) {
            leap(mob, target);
            return;
        }
        state.phase = MeleeState.Phase.WIND_UP;
        state.ticks = windUpTicks(mob);
        state.target = target;
        windUp(mob, state.ticks);
    }

    /** The ticks a brain mob's swing still takes, which MeleeAttack's cooldown waits out on top of its own. */
    public static int swingTicksLeft(Mob mob) {
        MeleeState state = MeleeState.peek(mob);
        if (state == null) return 0;
        return switch (state.phase) {
            case NONE -> 0;
            case WIND_UP, LANDED -> state.ticks;
            case CROUCH -> state.ticks + TYPICAL_LEAP_TICKS + LANDED_MISS_TICKS;
            case LEAP -> Math.max(state.flight - state.ticks, 0) + LANDED_MISS_TICKS;
        };
    }

    /** Every server tick of a mob, after its AI: counts a brain mob's swing down to its strike, and runs a leap. */
    public static void tick(Mob mob) {
        MeleeState state = MeleeState.peek(mob);
        if (state == null) return;
        switch (state.phase) {
            case NONE -> {
            }
            case WIND_UP -> windingUp(mob, state);
            case CROUCH -> crouching(mob, state);
            case LEAP -> leaping(mob, state);
            case LANDED -> {
                holdStill(mob);
                if (--state.ticks <= 0) state.end();
            }
        }
    }

    private static void windingUp(Mob mob, MeleeState state) {
        LivingEntity target = state.target;
        if (target == null || !target.isAlive() || interrupted(mob)) {
            state.end();
            cancel(mob);
            return;
        }
        if (--state.ticks > 0) return;
        state.end();
        boolean lands = (inCloseQuarters(mob, target) || mob.isWithinMeleeAttackRange(target)) && mob.hasLineOfSight(target);
        strike((ServerLevel) mob.level(), mob, target, Kind.REGULAR, lands);
    }

    private static void crouching(Mob mob, MeleeState state) {
        holdStill(mob);
        face(mob, state);
        if (--state.ticks > 0) return;
        // Knocked off its feet meanwhile: nothing to leap from.
        if (!mob.onGround()) {
            state.end();
            cancel(mob);
            return;
        }
        takeOff(mob, state);
    }

    /** Jumps as the mob would, at a speed forward that brings it down just short of where the target stood. */
    private static void takeOff(Mob mob, MeleeState state) {
        double dx = state.spot.x - mob.getX(), dz = state.spot.z - mob.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        double rise = jumpPower(mob);
        state.flight = flightTicks(rise, mob.getY() - state.spot.y);
        double speed = Math.min(Math.max(distance - state.standoff, 0.0) / state.flight, MAX_LEAP_SPEED);
        state.leapX = distance > 1.0E-4 ? dx / distance * speed : 0.0;
        state.leapZ = distance > 1.0E-4 ? dz / distance * speed : 0.0;
        mob.setDeltaMovement(state.leapX, rise, state.leapZ);
        state.phase = MeleeState.Phase.LEAP;
        state.ticks = 0;
        send(mob, MobMeleePayload.LEAP, state.flight);
    }

    private static void leaping(Mob mob, MeleeState state) {
        if (interrupted(mob)) {
            state.land(LANDED_MISS_TICKS);
            cancel(mob);
            return;
        }
        state.ticks++;
        boolean landed = state.ticks > 1 && (mob.onGround() || mob.isInWater() || mob.isInLava());
        boolean comingDown = mob.getDeltaMovement().y < 0.0 && state.ticks >= state.flight - 2;
        if (landed || comingDown || state.ticks >= LEAP_TIMEOUT) {
            strikeWhereItStood(mob, state);
            return;
        }
        // Straight at the spot, whatever the mob would do: no steering in the air.
        holdStill(mob);
        face(mob, state);
        mob.setDeltaMovement(state.leapX, mob.getDeltaMovement().y, state.leapZ);
    }

    /** The leap's strike, aimed where the target stood: it hits only if the target is still about there, and in reach. */
    private static void strikeWhereItStood(Mob mob, MeleeState state) {
        LivingEntity target = state.target;
        if (target == null) {
            state.land(LANDED_MISS_TICKS);
            return;
        }
        double dx = target.getX() - state.spot.x, dz = target.getZ() - state.spot.z;
        boolean lands = target.isAlive() && dx * dx + dz * dz <= SPOT_REACH * SPOT_REACH && mob.hasLineOfSight(target)
                && (inCloseQuarters(mob, target) || Combat.getMobAttackBox(mob, true).intersects(Combat.getEntityHitbox(target)));
        state.land(lands ? LANDED_HIT_TICKS : LANDED_MISS_TICKS);
        strike((ServerLevel) mob.level(), mob, target, Kind.HEAVY, lands);
    }

    /** Keeps a mob from walking or jumping on its own: no path, no input. */
    private static void holdStill(Mob mob) {
        mob.getNavigation().stop();
        mob.xxa = 0.0F;
        mob.zza = 0.0F;
        mob.setJumping(false);
    }

    /** Turns the mob, body and head, to where the target stood, and looks there. */
    private static void face(Mob mob, MeleeState state) {
        double dx = state.spot.x - mob.getX(), dz = state.spot.z - mob.getZ();
        if (dx * dx + dz * dz > 1.0E-4) {
            float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
            mob.setYRot(yaw);
            mob.setYBodyRot(yaw);
            mob.setYHeadRot(yaw);
        }
        mob.getLookControl().setLookAt(state.spot.x, state.aimY, state.spot.z, 30.0F, 30.0F);
    }

    /** How fast the mob jumps upward, as it would jump itself. */
    private static double jumpPower(Mob mob) {
        double factor = mob.level().getBlockState(mob.blockPosition()).getBlock().getJumpFactor();
        return mob.getAttributeValue(Attributes.JUMP_STRENGTH) * factor + mob.getJumpBoostPower();
    }

    /** Ticks a jump at upward speed {@code rise} stays up, coming down {@code drop} blocks below where it began. */
    static int flightTicks(double rise, double drop) {
        double height = 0.0, speed = rise;
        for (int tick = 1; tick < LEAP_TIMEOUT; tick++) {
            height += speed;
            if (speed < 0.0 && height <= -drop) return tick;
            speed = (speed - LivingEntity.DEFAULT_BASE_GRAVITY) * 0.98;
        }
        return LEAP_TIMEOUT;
    }

    private static void send(Mob mob, byte action, int ticks) {
        if (!(mob.level() instanceof ServerLevel)) return;
        MobMeleePayload payload = new MobMeleePayload(mob.getId(), action, ticks);
        for (ServerPlayer player : PlayerLookup.tracking(mob)) {
            if (ServerPlayNetworking.canSend(player, MobMeleePayload.TYPE.id())) ServerPlayNetworking.send(player, payload);
        }
    }
}
