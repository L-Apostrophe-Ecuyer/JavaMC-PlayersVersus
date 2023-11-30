package frootloops.versus.mod.hostile_mobs.ai;

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
        long l = this.creeper.world.getTime();
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

        double squaredDistance = this.creeper.squaredDistanceTo(targetEntity);
        if (squaredDistance > 4.0 && Combat.isLookingTowards(targetEntity, this.creeper.getPos())) {
            // Freeze! Target player is looking! (Andy's coming!)
            if (targetEntity.canSee(this.creeper)) {
                if(!this.wasCoverBlown) {
                    this.creeper.getNavigation().stop();
                    return; // Won't explode, either
                }
            }
            // If the player can't see the creeper, start stalking again:
            else this.wasCoverBlown = false;
        }

        // Walk towards player target:
        if (squaredDistance > 4.0 || this.creeper.getNavigation().isIdle()) {
            this.creeper.getNavigation().startMovingTo(targetEntity, this.wasCoverBlown ? this.speed + 0.04 : this.speed - 0.02);
            this.creeper.getLookControl().lookAt(targetEntity, 30.0f, 30.0f);
        }

        // Explode when within 2 blocks:
        if(squaredDistance < 9.0 && this.creeper.getVisibilityCache().canSee(targetEntity)) this.creeper.setFuseSpeed(1);
        else this.creeper.setFuseSpeed(-1);
    }
}
