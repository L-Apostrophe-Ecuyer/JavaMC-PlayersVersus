package frootloops.versus.mixin.mobs.hostile;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.mobs.melee.MobMelee;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mob melee on MeleeAttackGoal as a telegraphed swing ({@link MobMelee}): a swing starts when the target is in reach,
 * winds up (holding still at first), then strikes, hitting if the target is still in reach and in sight and missing
 * otherwise. Now and then a humanoid swings heavy instead and leaps at the target, which MobMelee runs while the goal
 * waits. {@code ticksUntilNextAttack} counts down the whole cycle, wind-up and recovery; mobs with a shield block
 * between swings.
 */
@Mixin(MeleeAttackGoal.class)
public abstract class MeleeAttackGoalMixin extends Goal {
    protected MeleeAttackGoalMixin(double speed, PathfinderMob mob) {
        this.speedModifier = speed;
        this.mob = mob;
    }

    private final boolean DEBUG = false;

    /** Recovery after a strike, by what the mob holds: nothing, a tool or weapon, or an axe or trident. */
    private final int TICKS_SWING_QUICK = 20;
    private final int TICKS_SWING_TOOLS = 30;
    private final int TICKS_SWING_HEAVY = 40;
    /** How long a regular swing holds the mob still at the start of its wind-up. */
    private final int TICKS_FREEZE = 4;

    /** The regular swing in progress: ticks until it strikes (0 when none), and its whole wind-up. */
    private int windUpLeft;
    private int windUpTicks;

    @Shadow
    private final double speedModifier;

    @Shadow
    private int ticksUntilNextPathRecalculation;

    @Shadow
    private Path path;

    @Shadow
    protected final PathfinderMob mob;

    @Shadow
    private int ticksUntilNextAttack;

    private int getRecovery() {
        ItemStack held = this.mob.getMainHandItem();
        if (held.isEmpty()) return TICKS_SWING_QUICK;
        if (held.is(ItemTags.AXES) || held.is(Items.TRIDENT)) return TICKS_SWING_HEAVY;
        return TICKS_SWING_TOOLS;
    }

    /** A regular swing's whole cycle, wind-up and recovery: vanilla's attack interval. */
    private int getCooldownAmount() {
        return MobMelee.windUpTicks(this.mob) + this.getRecovery();
    }

    /** The last ticks of a cycle, when the mob aims freely again. */
    private int getEndlag() {
        return this.mob.is(EntityTypeTags.ARTHROPOD) ? 4 : 8;
    }

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    public void shouldContinue(CallbackInfoReturnable<Boolean> cir) {
        // Only with a target: without one, nothing counts the swing or its cooldown down.
        if ((this.ticksUntilNextAttack > 0 || this.windUpLeft > 0) && this.mob.getTarget() != null) {
            mob.setAggressive(true);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    public void start(CallbackInfo info) {
        this.mob.getNavigation().moveTo(this.path, this.speedModifier * 1.075);
        this.mob.setAggressive(true);
        this.ticksUntilNextPathRecalculation = 0;
        info.cancel();
    }

    @Inject(method = "stop", at = @At("HEAD"))
    public void stop(CallbackInfo info) {
        if (this.windUpLeft > 0) {
            this.windUpLeft = 0;
            MobMelee.cancel(this.mob);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void tick(CallbackInfo info) {
        // Leaping (a heavy swing): MobMelee has the mob until it's back on its feet, and the cycle waits for it.
        if (MobMelee.isLeaping(this.mob)) {
            this.mob.setAggressive(true);
            info.cancel();
            return;
        }
        LivingEntity target = this.mob.getTarget();
        if (target == null && this.windUpLeft > 0) {
            this.windUpLeft = 0;
            MobMelee.cancel(this.mob);
        }
        if (target != null) {

            // Swinging: a hit cuts the swing short; otherwise it holds still at first, then strikes.
            if (this.windUpLeft > 0) {
                if (MobMelee.interrupted(this.mob)) {
                    this.windUpLeft = 0;
                    this.ticksUntilNextAttack = this.getEndlag() - 2;
                    this.mob.setAggressive(false);
                    MobMelee.cancel(this.mob);
                    if (DEBUG) VersusMod.MOD_LOGGER.warn("Couldn't attack: interrupted.");
                } else if (--this.windUpLeft == 0) {
                    this.strike(target);
                } else if (this.windUpTicks - this.windUpLeft <= TICKS_FREEZE && this.mob.hurtTime < 8) {
                    this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
                    this.mob.setAggressive(true);
                    info.cancel();
                    return;
                }
            }

            // Shield Blocking:
            boolean shouldBlockWithShield = false;
            if(this.ticksUntilNextAttack < -32) this.ticksUntilNextAttack = 8; // Up to 32 ticks with the shield up. After, attacks/shields on cooldown for 8 ticks
            else shouldBlockWithShield = this.canBlockWithShield() && this.shouldPlayDefensively();

            if (shouldBlockWithShield) {
                this.ticksUntilNextAttack = Math.min(this.ticksUntilNextAttack - 1, -16); // Minimum 16 ticks with the shield up
                Vec3 velocity = this.mob.getDeltaMovement();
                this.mob.setPose(Pose.CROUCHING);
                this.mob.setDeltaMovement(0, velocity.y, 0);
                this.mob.setAggressive(mob instanceof AbstractIllager);
                this.mob.getOffhandItem().onUseTick(this.mob.level(), this.mob, 8);
                this.mob.startUsingItem(InteractionHand.OFF_HAND);
                info.cancel();

            } else if (this.ticksUntilNextAttack >= 0 && this.mob.getPose() == Pose.CROUCHING) {
                this.mob.setPose(Pose.STANDING);
                this.mob.releaseUsingItem();
                info.cancel();
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"), cancellable = false)
    public void mobsNeedToBeAimingToLandHit(CallbackInfo info) {
        // If the mob started attacking or blocking, it can't properly adjust its aim mid-swing anymore:
        if (this.windUpLeft > 0 || this.ticksUntilNextAttack < 0 || this.ticksUntilNextAttack > this.getEndlag()) {
            LookControl lookControl = this.mob.getLookControl();
            if (lookControl.isLookingAtTarget()) lookControl.setLookAt(lookControl.getWantedX(), lookControl.getWantedY(), lookControl.getWantedZ(),10f,10f);
        }
    }

    private boolean canBlockWithShield() {
        if(this.mob.getOffhandItem().isEmpty()) return false;
        return (this.mob.getOffhandItem().getItem().getUseAnimation(this.mob.getOffhandItem()) == ItemUseAnimation.BLOCK);
    }

    private boolean shouldPlayDefensively() {
        if(this.ticksUntilNextAttack > 6) return false;
        if(this.ticksUntilNextAttack < 0) return true;
        if(this.mob.isSwinging()) return false;
        if(this.mob.getInvulnerableTime() > 4) return false;

        LivingEntity opponent = mob.getLastAttacker();
        if(opponent == null) opponent = mob.level().getNearestPlayer(mob, 8d);
        if(opponent != null) {

            // If the enemy already attacked, and mob wasn't hurt, exit (attack of opportunity);
            if(opponent.isSwinging() && this.mob.getInvulnerableTime() < 6) return false;

            // If enemy isn't in the "danger zone" for an incoming attack, and mob isn't hurt, exit to attack;
            double d = this.mob.position().distanceToSqr(opponent.position());
            if((d > 16.0d || d < 4.0d) && this.mob.getInvulnerableTime() != 0) return false;

            // If opponent is about to crit or sprint attack, sometimes try blocking:
            if((!opponent.onGround() || opponent.isSprinting()) && this.ticksUntilNextAttack % 3 == 0) return Combat.isLookingTowards(mob,opponent.position());

            // if enemy is walking slowly, easy target, exit to attack;
            if(opponent.getDeltaMovement().x == 0.0d || opponent.getDeltaMovement().z == 0.0d) return false;
            return true;
        }
        return false;
    }

    /** Starts a swing once the cycle is over and the target is in reach, jumping at it if only a jump reaches. */
    @Overwrite
    public void checkAndPerformAttack(LivingEntity target) {
        if (this.windUpLeft > 0 || this.ticksUntilNextAttack > 0) return;
        boolean isInCloseQuarters = MobMelee.inCloseQuarters(this.mob, target);
        if (!isInCloseQuarters && !Combat.isLookingTowards(this.mob, target.getEyePosition(), true)) return;

        boolean jumping = false;
        if (!isInCloseQuarters && !Combat.getMobAttackBox(mob, false).intersects(Combat.getEntityHitbox(target))) {
            if (target.getVehicle() != null || !mob.onGround() || !Combat.getMobAttackBox(mob, true).intersects(Combat.getEntityHitbox(target))) return;
            if(DEBUG) VersusMod.MOD_LOGGER.warn("Jump attack!");
            double jumpBlockMultiplier = mob.level().getBlockState(mob.blockPosition()).getBlock().getJumpFactor();
            double jumpVelocity = 0.5 * jumpBlockMultiplier + mob.getJumpBoostPower();
            mob.getDeltaMovement().scale(1.6);
            mob.push(0.0, jumpVelocity, 0.0);
            jumping = true;
        }

        // Now and then a humanoid swings heavy, leaping at the target (not when already jumping at it); its recovery
        // counts down once it's back on its feet.
        if (!jumping && MobMelee.pickKind(this.mob, target) == MobMelee.Kind.HEAVY) {
            MobMelee.leap(this.mob, target);
            this.ticksUntilNextAttack = this.getRecovery();
            this.mob.setAggressive(true);
            if(DEBUG) VersusMod.MOD_LOGGER.warn("Leaping.");
            return;
        }
        this.windUpTicks = MobMelee.windUpTicks(this.mob);
        this.windUpLeft = this.windUpTicks;
        this.ticksUntilNextAttack = this.windUpTicks + this.getRecovery();
        this.mob.setAggressive(true);
        MobMelee.windUp(this.mob, this.windUpTicks);
        if(DEBUG) VersusMod.MOD_LOGGER.warn("Started a swing.");
    }

    /** The end of the wind-up: hits if the target is still in reach and in sight; a miss takes longer to recover. */
    private void strike(LivingEntity target) {
        boolean lands = this.mob.hasLineOfSight(target) && MobMelee.reaches(this.mob, target, false);
        if (!MobMelee.strike(getServerLevel(this.mob), this.mob, target, MobMelee.Kind.REGULAR, lands)) {
            this.ticksUntilNextAttack += MobMelee.MISS_EXTRA_RECOVERY;
        }
        if(DEBUG) VersusMod.MOD_LOGGER.warn(lands ? "Landed the swing." : "Missed.");
    }

    @Overwrite
    public void resetAttackCooldown() {
        this.ticksUntilNextAttack = this.getCooldownAmount();
    }

    @Overwrite
    public int getAttackInterval() {
        return this.getCooldownAmount();
    }
}
