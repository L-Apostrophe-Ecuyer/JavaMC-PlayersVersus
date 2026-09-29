package frootloops.versus.mixin.mobs.hostile;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.Combat;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.AbstractIllager;
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

@Mixin(MeleeAttackGoal.class)
public abstract class MeleeAttackGoalMixin extends Goal {
    protected MeleeAttackGoalMixin(double speed, PathfinderMob mob) {
        this.speedModifier = speed;
        this.mob = mob;
    }

    private final boolean DEBUG = false;

    private int numTicksEndlag = -1;
    private final int TICKS_SWING_QUICK = 20;
    private final int TICKS_SWING_TOOLS = 30;
    private final int TICKS_SWING_HEAVY = 40;

    private int maxCooldown = 0;

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

    private int getCooldownAmount(){
        if(maxCooldown > 0) return maxCooldown;
        if(numTicksEndlag == -1) numTicksEndlag = mob.getType().is(EntityTypeTags.ARTHROPOD) ? 4 : 8;
        if(!this.mob.getMainHandItem().isEmpty()) {
            if(this.mob.getMainHandItem().is(ItemTags.AXES) || this.mob.getMainHandItem().is(Items.TRIDENT))
                maxCooldown = TICKS_SWING_HEAVY + numTicksEndlag;
        }
        else maxCooldown = TICKS_SWING_QUICK + numTicksEndlag;
        return maxCooldown;
    }

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    public void shouldContinue(CallbackInfoReturnable cir) {
        if(this.ticksUntilNextAttack > 0) {
            mob.setAggressive(true);
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    public void start(CallbackInfo info) {
        this.mob.getNavigation().moveTo(this.path, this.speedModifier * 1.075);
        this.mob.setAggressive(true);
        this.ticksUntilNextPathRecalculation = 0;
        info.cancel();
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void tick(CallbackInfo info) {
        LivingEntity target = this.mob.getTarget();
        if (target != null) {

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

            // Attacking:
            } else if (this.ticksUntilNextAttack >= 0) {
                if (this.mob.getPose() == Pose.CROUCHING) {
                    this.mob.setPose(Pose.STANDING);
                    this.mob.releaseUsingItem();
                    info.cancel();
                }
                if (this.ticksUntilNextAttack > this.getCooldownAmount() - 4 && this.mob.hurtTime < 8) {
                    this.ticksUntilNextAttack = Math.max(this.ticksUntilNextAttack - 1, 0);
                    this.mob.setAggressive(true);
                    info.cancel();
                }
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"), cancellable = false)
    public void mobsNeedToBeAimingToLandHit(CallbackInfo info) {
        // If the mob started attacking or blocking, it can't properly adjust its aim mid-swing anymore:
        if(numTicksEndlag == -1) numTicksEndlag = mob.getType().is(EntityTypeTags.ARTHROPOD) ? 1 : 8;
        if (this.ticksUntilNextAttack < 0 || this.ticksUntilNextAttack > numTicksEndlag) {
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
        if(this.mob.swinging) return false;
        if(this.mob.invulnerableTime > 4) return false;

        LivingEntity opponent = mob.getLastAttacker();
        if(opponent == null) opponent = mob.level().getNearestPlayer(mob, 8d);
        if(opponent != null) {

            // If the enemy already attacked, and mob wasn't hurt, exit (attack of opportunity);
            if(opponent.swinging && this.mob.invulnerableTime < 6) return false;

            // If enemy isn't in the "danger zone" for an incoming attack, and mob isn't hurt, exit to attack;
            double d = this.mob.position().distanceToSqr(opponent.position());
            if((d > 16.0d || d < 4.0d) && this.mob.invulnerableTime != 0) return false;

            // If opponent is about to crit or sprint attack, sometimes try blocking:
            if((!opponent.onGround() || opponent.isSprinting()) && this.ticksUntilNextAttack % 3 == 0) return Combat.isLookingTowards(mob,opponent.position());

            // if enemy is walking slowly, easy target, exit to attack;
            if(opponent.getDeltaMovement().x == 0.0d || opponent.getDeltaMovement().z == 0.0d) return false;
            return true;
        }
        return false;
    }

    @Overwrite
    public void checkAndPerformAttack(LivingEntity target) {
        if(numTicksEndlag == -1) numTicksEndlag = mob.getType().is(EntityTypeTags.ARTHROPOD) ? 1 : 8;
        int cooldownAmount = this.getCooldownAmount();
        boolean canTrySwinging = this.ticksUntilNextAttack <= 0;
        boolean willTryLandingAnAttack = this.mob.isAggressive() && (this.ticksUntilNextAttack == (cooldownAmount - numTicksEndlag) || this.ticksUntilNextAttack == (cooldownAmount - numTicksEndlag) - 1);

        // Attack interruption, if the player swung right after the mob did:
        if(this.mob.hurtTime > 14 && cooldownAmount > numTicksEndlag) {
            ticksUntilNextAttack = numTicksEndlag - 2;
            mob.setAggressive(false);
            mob.attackAnim = 0f;
            if(DEBUG) VersusMod.MOD_LOGGER.warn("Couldn't attack: interrupted.");
        }

        // Otherwise, see if we can attack (cooldown is reduced in tick()):
        else if (canTrySwinging || willTryLandingAnAttack) {
            boolean isInCloseQuarters = (target.getEyePosition().distanceToSqr(mob.getEyePosition()) < 1.5d) || (target.position().distanceToSqr(mob.position()) < 1.5d);
            if(isInCloseQuarters || Combat.isLookingTowards(this.mob, target.getEyePosition(), true)) {

                if(DEBUG && canTrySwinging) VersusMod.MOD_LOGGER.warn("-------------------- SWING ATTEMPT");
                else if(DEBUG) VersusMod.MOD_LOGGER.warn("-------------------- ATTACK ATTEMPT");

                boolean canAttack = false;
                if(isInCloseQuarters) {
                    if(DEBUG && canTrySwinging) VersusMod.MOD_LOGGER.warn("Can swing, by means of being near the player");
                    else if(DEBUG) VersusMod.MOD_LOGGER.warn("Can land attack, by means of being near the player");
                    canAttack = true;
                }
                else {
                    if(Combat.getMobAttackBox(mob, false).intersects(Combat.getEntityHitbox(target))) {
                        if(DEBUG && canTrySwinging) VersusMod.MOD_LOGGER.warn("Can swing, by means of intersecting with the player");
                        else if(DEBUG) VersusMod.MOD_LOGGER.warn("Can land attack, by means of intersecting with the player");
                        canAttack = true;
                    }
                    else if(canTrySwinging && target.getVehicle() == null && mob.onGround() && Combat.getMobAttackBox(mob, true).intersects(Combat.getEntityHitbox(target))) {
                        if(DEBUG) VersusMod.MOD_LOGGER.warn("Jump attack!");
                        double jumpBlockMultiplier = mob.level().getBlockState(mob.blockPosition()).getBlock().getJumpFactor();
                        double jumpVelocity = 0.5 * jumpBlockMultiplier + mob.getJumpBoostPower();
                        mob.getDeltaMovement().scale(1.6);
                        mob.push(0.0, jumpVelocity, 0.0);
                        canAttack = true;
                    }
                }

                if(canAttack) {
                    if(DEBUG) VersusMod.MOD_LOGGER.warn("Can attack...");

                    // Start swinging:
                    if (canTrySwinging) {
                        if(DEBUG) VersusMod.MOD_LOGGER.warn("Started swinging!");
                        this.mob.swing(InteractionHand.MAIN_HAND);
                        this.ticksUntilNextAttack = cooldownAmount;

                    // After 6 ticks, see if the swing landed:
                    } else if (willTryLandingAnAttack) {
                        if(this.mob.hasLineOfSight(target)) {
                            if(DEBUG) VersusMod.MOD_LOGGER.warn("Landing attack!");
                            if(this.mob.doHurtTarget(getServerLevel(this.mob), target)) {
                                this.mob.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 0.6F, 1.4F);
                            }
                            this.ticksUntilNextAttack -= 2;
                        }
                        else {
                            if(DEBUG) VersusMod.MOD_LOGGER.warn("Missed: couldn't see target.");
                            this.mob.playSound(SoundEvents.PLAYER_ATTACK_NODAMAGE, 1.2F, 0.9F);
                            this.ticksUntilNextAttack -= 1;
                        }
                    }
                }
                else if (willTryLandingAnAttack) {
                    if(DEBUG) VersusMod.MOD_LOGGER.warn("Couldn't attack.");
                    this.mob.playSound(SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.8F, 0.8F);
                }
            }
            else if (willTryLandingAnAttack) {
                if(DEBUG) VersusMod.MOD_LOGGER.warn("Couldn't attack: neither in close quarters, nor looking towards target");
                this.mob.playSound(SoundEvents.PLAYER_ATTACK_NODAMAGE, 0.8F, 0.8F);
            }
        }
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
