package frootloops.versus.mod.mobs.hostile.ai;

import frootloops.versus.mod.Combat;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;

import java.util.EnumSet;

public class CreepingAndExplodingGoal extends Goal {

    protected final CreeperEntity creeper;
    protected final double speed;
    private long lastUpdateTime;
    private boolean wasCoverBlown;

    public CreepingAndExplodingGoal(CreeperEntity creeper, double speed) {
        this.creeper = creeper;
        this.speed = speed;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        long l = this.creeper.method_48926().getTime();
        if (l - this.lastUpdateTime < 20L) {
            return false;
        }
        this.lastUpdateTime = l;
        this.wasCoverBlown = false;
        LivingEntity target = this.creeper.getTarget();
        if (target == null || !target.isAlive()) return false;
        return true;
    }

    @Override
    public boolean shouldContinue() {
        LivingEntity livingEntity = this.creeper.getTarget();
        if (livingEntity == null || !livingEntity.isAlive()) return false;
        if (!this.creeper.isInWalkTargetRange(livingEntity.getBlockPos())) return false;
        return !(livingEntity instanceof PlayerEntity) || !livingEntity.isSpectator() && !((PlayerEntity)livingEntity).isCreative();
    }

    @Override
    public void start() {
        this.creeper.setAttacking(true);
    }

    @Override
    public void stop() {
        LivingEntity livingEntity = this.creeper.getTarget();
        if (!EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR.test(livingEntity)) {
            this.creeper.setTarget(null);
        }
        this.creeper.setAttacking(false);
        this.creeper.getNavigation().stop();
    }

    @Override
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity targetEntity = this.creeper.getTarget();
        if (targetEntity == null) return;

        // If the creeper was attacked, drop all pretenses and rush them:
        if (this.creeper.getAttacker() != null) this.wasCoverBlown = true;
        if (this.creeper.getFuseSpeed() > 0) this.wasCoverBlown = true;

        double squaredDistance = this.creeper.squaredDistanceTo(targetEntity);
        boolean isPlayerLooking = Combat.isLookingTowards(targetEntity, this.creeper.getPos());
        boolean canPlayerSeeCreeper = targetEntity.canSee(this.creeper);

        // When far enough away from target, only move when not looking (unless cover was blown):
        if (squaredDistance > 16.0) {
            // Freeze! Target player is looking! (Andy's coming!)
            if (isPlayerLooking && canPlayerSeeCreeper && !this.wasCoverBlown) {
                this.creeper.getNavigation().stop();
                return; // Won't explode, either
            }
            // If the player can't see the creeper, creep up on them:
            else {
                if(this.creeper.getNavigation().isIdle()) {
                    this.creeper.getNavigation().startMovingTo(targetEntity, this.wasCoverBlown ? this.speed * 1.2 : this.speed);
                    this.creeper.getLookControl().lookAt(targetEntity, 30.0f, 30.0f);
                }
            }
        }

        // If the player is looking, but the creeper is close enough, start charging:
        else {
            if (targetEntity.handSwinging) this.wasCoverBlown = true;
            if(this.wasCoverBlown && !canPlayerSeeCreeper) this.wasCoverBlown = false;
            if(this.wasCoverBlown || !(isPlayerLooking && canPlayerSeeCreeper) || this.creeper.getNavigation().isIdle()) {
                this.creeper.getNavigation().startMovingTo(targetEntity, this.speed);
                this.creeper.getLookControl().lookAt(targetEntity, 30.0f, 30.0f);
            }
        }

        // Explode when within 2.5 blocks:
        boolean shouldStartExploding = (squaredDistance < 6.25 && isPlayerLooking && this.creeper.getVisibilityCache().canSee(targetEntity));
        boolean shouldKeepExploding = (this.creeper.getFuseSpeed() > 0);
        if(shouldStartExploding || shouldKeepExploding) this.creeper.setFuseSpeed(1);
        else this.creeper.setFuseSpeed(-1);
    }
}
