package frootloops.versus.mixin.mobs.hostile;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.Combat;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.item.*;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MeleeAttackGoal.class)
public abstract class MeleeAttackGoalMixin extends Goal {
    protected MeleeAttackGoalMixin(double speed, PathAwareEntity mob) {
        this.speed = speed;
        this.mob = mob;
    }

    private final boolean DEBUG = false;

    private final int TICKS_ENDLAG = 8;
    private final int TICKS_SWING_QUICK = TICKS_ENDLAG + 10;
    private final int TICKS_SWING_TOOLS = TICKS_ENDLAG + 12;
    private final int TICKS_SWING_HEAVY = TICKS_ENDLAG + 16;

    @Shadow
    private final double speed;

    @Shadow
    private int updateCountdownTicks;

    @Shadow
    private Path path;

    @Shadow
    protected final PathAwareEntity mob;

    @Shadow
    private int cooldown;

    private int getCooldownAmount(){
        if(this.mob instanceof WardenEntity || this.mob instanceof IronGolemEntity || this.mob instanceof HoglinEntity)
            return TICKS_SWING_HEAVY;
        else if(this.mob.getMainHandStack() != null) {
            Item weapon = this.mob.getMainHandStack().getItem();
            if(weapon instanceof AxeItem || weapon instanceof TridentItem) return TICKS_SWING_HEAVY;
            else if(weapon instanceof HoeItem) return TICKS_SWING_QUICK;
            else if(weapon instanceof ToolItem) return TICKS_SWING_TOOLS;
        }
        return TICKS_SWING_QUICK;
    }

    @Inject(method = "shouldContinue", at = @At("HEAD"), cancellable = true)
    public void shouldContinue(CallbackInfoReturnable cir) {
        if(this.cooldown > 0) {
            mob.setAttacking(true);
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    public void start(CallbackInfo info) {
        this.mob.getNavigation().startMovingAlong(this.path, this.speed * 1.075);
        this.mob.setAttacking(true);
        this.updateCountdownTicks = 0;
        info.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void tick(CallbackInfo info) {
        LivingEntity target = this.mob.getTarget();
        if (target != null) {

            // Shield Blocking:
            boolean shouldBlockWithShield = false;
            if(this.cooldown < -32) this.cooldown = 8; // Up to 32 ticks with the shield up. After, attacks/shields on cooldown for 8 ticks
            else shouldBlockWithShield = this.canBlockWithShield() && this.shouldPlayDefensively();

            if (shouldBlockWithShield) {
                this.cooldown = Math.min(this.cooldown - 1, -16); // Minimum 16 ticks with the shield up
                Vec3d velocity = this.mob.getVelocity();
                this.mob.setPose(EntityPose.CROUCHING);
                this.mob.setVelocity(0, velocity.y, 0);
                this.mob.setAttacking(false);
                this.mob.getOffHandStack().usageTick(this.mob.getWorld(), this.mob, 8);
                this.mob.setCurrentHand(Hand.OFF_HAND);
                info.cancel();

            // Attacking:
            } else if (this.cooldown >= 0) {
                if (this.mob.getPose() == EntityPose.CROUCHING) {
                    this.mob.setPose(EntityPose.STANDING);
                    this.mob.stopUsingItem();
                    info.cancel();
                }
                if (this.cooldown > this.getCooldownAmount() - 4 && this.mob.hurtTime < 8) {
                    this.cooldown = Math.max(this.cooldown - 1, 0);
                    this.mob.setAttacking(true);
                    info.cancel();
                }
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"), cancellable = false)
    public void mobsNeedToBeAimingToLandHit(CallbackInfo info) {
        // If the mob started attacking or blocking, it can't properly adjust its aim mid-swing anymore:
        if (this.cooldown < 0 || this.cooldown > TICKS_ENDLAG) {
            LookControl lookControl = this.mob.getLookControl();
            if (lookControl.isLookingAtSpecificPosition()) lookControl.lookAt(lookControl.getLookX(), lookControl.getLookY(), lookControl.getLookZ(),15f,15f);
        }
    }

    private boolean canBlockWithShield() {
        if(this.mob.getOffHandStack().isEmpty()) return false;
        return (this.mob.getOffHandStack().getItem().getUseAction(this.mob.getOffHandStack()) == UseAction.BLOCK);
    }

    private boolean shouldPlayDefensively() {
        if(this.cooldown > 6) return false;
        if(this.cooldown < 0) return true;
        if(this.mob.handSwinging) return false;
        if(this.mob.timeUntilRegen > 4) return false;

        LivingEntity opponent = mob.getLastAttacker();
        if(opponent == null) opponent = mob.getWorld().getClosestPlayer(mob, 8d);
        if(opponent != null) {

            // If the enemy already attacked, and mob wasn't hurt, exit (attack of opportunity);
            if(opponent.handSwinging && this.mob.timeUntilRegen < 6) return false;

            // If enemy isn't in the "danger zone" for an incoming attack, and mob isn't hurt, exit to attack;
            double d = this.mob.getPos().squaredDistanceTo(opponent.getPos());
            if((d > 16.0d || d < 4.0d) && this.mob.timeUntilRegen != 0) return false;

            // If opponent is about to crit or sprint attack, sometimes try blocking:
            if((!opponent.isOnGround() || opponent.isSprinting()) && this.cooldown % 3 == 0) return Combat.isLookingTowards(mob,opponent.getPos());

            // if enemy is walking slowly, easy target, exit to attack;
            if(opponent.getVelocity().x == 0.0d || opponent.getVelocity().z == 0.0d) return false;
            return true;
        }
        return false;
    }

    @Overwrite
    public void attack(LivingEntity target) {
        int cooldownAmount = this.getCooldownAmount();
        boolean canTrySwinging = this.cooldown <= 0;
        boolean willTryLandingAnAttack = this.mob.isAttacking() && (this.cooldown == (cooldownAmount - TICKS_ENDLAG) || this.cooldown == (cooldownAmount - TICKS_ENDLAG) - 1);

        // Attack interruption, if the player swung right after the mob did:
        if(this.mob.hurtTime > 12 && cooldownAmount > TICKS_SWING_QUICK - 4) {
            cooldown = TICKS_ENDLAG;
            mob.setAttacking(false);
            mob.handSwingProgress = 0f;
        }

        // Otherwise, see if we can attack (cooldown is reduced in tick()):
        else if (canTrySwinging || willTryLandingAnAttack) {
            boolean isInCloseQuarters = (target.getEyePos().squaredDistanceTo(mob.getEyePos()) < 1.5d) || (target.getPos().squaredDistanceTo(mob.getEyePos()) < 1.5d);
            if(isInCloseQuarters || Combat.isLookingTowards(this.mob, target.getEyePos(), true)) {

                if(DEBUG && canTrySwinging) VersusMod.MOD_LOGGER.warn("-------------------- SWING ATTEMPT");
                else if(DEBUG) VersusMod.MOD_LOGGER.warn("-------------------- ATTACK ATTEMPT");

                boolean canAttack = false;
                if(isInCloseQuarters) {
                    if(DEBUG && canTrySwinging) VersusMod.MOD_LOGGER.warn("Can swing, by means of being near the player");
                    else if(DEBUG) VersusMod.MOD_LOGGER.warn("Can land attack, by means of being near the player");
                    canAttack = true;
                }
                else if(Combat.getMobAttackBox(mob, false).intersects(Combat.getEntityHitbox(target))) {
                    if(DEBUG && canTrySwinging) VersusMod.MOD_LOGGER.warn("Can swing, by means of intersecting with the player");
                    else if(DEBUG) VersusMod.MOD_LOGGER.warn("Can land attack, by means of intersecting with the player");
                    canAttack = true;
                }
                else if(canTrySwinging && target.getVehicle() == null && Combat.getMobAttackBox(mob, true).intersects(Combat.getEntityHitbox(target))) {
                    if(DEBUG) VersusMod.MOD_LOGGER.warn("Jump attack!");

                    double jumpBlockMultiplier = mob.getWorld().getBlockState(mob.getBlockPos()).getBlock().getJumpVelocityMultiplier();
                    double jumpVelocity = 0.5 * jumpBlockMultiplier + mob.getJumpBoostVelocityModifier();
                    mob.getVelocity().multiply(1.6);
                    mob.addVelocity(0.0, jumpVelocity, 0.0);
                    canAttack = true;
                }

                if(canAttack) {
                    if(DEBUG) VersusMod.MOD_LOGGER.warn("Can attack...");

                    // Start swinging:
                    if (canTrySwinging) {
                        if(DEBUG) VersusMod.MOD_LOGGER.warn("Started swinging!");
                        this.mob.swingHand(Hand.MAIN_HAND);
                        this.cooldown = cooldownAmount;

                    // After 6 ticks, see if the swing landed:
                    } else if (willTryLandingAnAttack) {
                        if(this.mob.canSee(target)) {
                            if(DEBUG) VersusMod.MOD_LOGGER.warn("Landing attack!");
                            this.mob.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 0.6F, 1.4F);
                            this.mob.tryAttack(target);
                            this.cooldown -= 2;
                        }
                        else {
                            if(DEBUG) VersusMod.MOD_LOGGER.warn("Missed: couldn't see target.");
                            this.mob.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, 1.2F, 0.9F);
                            this.cooldown -= 1;
                        }
                    }
                }
                else if (willTryLandingAnAttack) {
                    if(DEBUG) VersusMod.MOD_LOGGER.warn("Couldn't attack.");
                    this.mob.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, 0.8F, 0.8F);
                }
            }
            else if (willTryLandingAnAttack) {
                this.mob.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, 0.8F, 0.8F);
            }


        }
    }

    @Overwrite
    public void resetCooldown() {
        this.cooldown = this.getCooldownAmount();
    }

    @Overwrite
    public int getMaxCooldown() {
        return this.getCooldownAmount();
    }
}
