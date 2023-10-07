package frootloops.versus.mixin.melee_attacks;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.item.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
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

    @Shadow
    protected double getSquaredMaxAttackDistance(LivingEntity entity) {
        return this.mob.getWidth() * 2.0f * (this.mob.getWidth() * 2.0f) + entity.getWidth();
    }

    private int getCooldownAmount(){
        if(this.mob instanceof WardenEntity || this.mob instanceof IronGolemEntity || this.mob instanceof HoglinEntity)
            return 40;
        else if(this.mob.getMainHandStack() != null) {
            Item weapon = this.mob.getMainHandStack().getItem();
            if(weapon instanceof AxeItem || weapon instanceof TridentItem) return 40;
            else if(weapon instanceof HoeItem) return 24;
            else if(weapon instanceof ToolItem) return 32;
        }
        return 28;
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
        if(this.cooldown > this.getCooldownAmount() && this.mob.hurtTime < 8) {
            this.mob.setAttacking(true);
            this.cooldown = Math.max(this.cooldown - 1, 0);
            info.cancel();
        }
    }

    @Overwrite
    public void attack(LivingEntity target, double squaredDistance) {

        if(this.mob.hurtTime > 8) {
            cooldown = 0;
            mob.setAttacking(false);
            mob.handSwingProgress = 0f;
        }
        else {
            double d = this.getSquaredMaxAttackDistance(target);
            if (squaredDistance <= d + 2) {
                int cooldownAmount = this.getCooldownAmount();
                if (this.cooldown <= 0) {
                    this.mob.swingHand(Hand.MAIN_HAND);
                    this.cooldown = cooldownAmount;
                } else if (this.cooldown == (cooldownAmount - 7) || this.cooldown == (cooldownAmount - 8)) {

                    boolean isTargetInRange = (squaredDistance <= d);
                    if(!isTargetInRange) return;

                    boolean isTargetInBounds = (target.getBoundingBox().maxY + target.getY() > this.mob.getBoundingBox().minY + this.mob.getY());
                    if(!isTargetInBounds) return;

                    LookControl posMobLookingAt = this.mob.getLookControl();
                    double deltaX = Math.abs(posMobLookingAt.getLookX() - target.getX());
                    double deltaZ = Math.abs(posMobLookingAt.getLookZ() - target.getZ());
                    boolean isTargetInSight = deltaX + deltaZ < 2.0d;
                    if(!isTargetInSight) return;

                    this.mob.tryAttack(target);
                    this.cooldown -= 2;
                }
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
