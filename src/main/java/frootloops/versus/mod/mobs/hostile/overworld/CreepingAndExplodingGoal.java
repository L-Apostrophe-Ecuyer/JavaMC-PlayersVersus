package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.mod.Combat;
import java.util.EnumSet;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;

public class CreepingAndExplodingGoal extends Goal {

    protected final Creeper creeper;
    protected final double speed;
    private long lastUpdateTime;
    private boolean wasCoverBlown;

    public CreepingAndExplodingGoal(Creeper creeper, double speed) {
        this.creeper = creeper;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        long l = this.creeper.level().getGameTime();
        if (l - this.lastUpdateTime < 40L) {
            return false;
        }
        this.lastUpdateTime = l;
        this.wasCoverBlown = false;
        LivingEntity target = this.creeper.getTarget();
        if (target == null || !target.isAlive()) return false;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity livingEntity = this.creeper.getTarget();
        if (livingEntity == null || !livingEntity.isAlive()) return false;
        if (!this.creeper.isWithinHome(livingEntity.blockPosition())) return false;
        return !(livingEntity instanceof Player) || !livingEntity.isSpectator() && !((Player)livingEntity).isCreative();
    }

    @Override
    public void start() {
        this.creeper.setAggressive(true);
    }

    @Override
    public void stop() {
        LivingEntity livingEntity = this.creeper.getTarget();
        if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(livingEntity)) {
            this.creeper.setTarget(null);
        }
        this.creeper.setAggressive(false);
        this.creeper.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity targetEntity = this.creeper.getTarget();
        if (targetEntity == null) return;

        // If the creeper was attacked, drop all pretenses and rush them:
        if (this.creeper.getLastHurtByMob() != null) this.wasCoverBlown = true;
        if (this.creeper.getSwellDir() > 0 || this.creeper.isInLiquid()) this.wasCoverBlown = true;

        double squaredDistance = this.creeper.distanceToSqr(targetEntity);
        boolean isPlayerLooking = Combat.isLookingTowards(targetEntity, this.creeper.position());
        boolean canPlayerSeeCreeper = targetEntity.hasLineOfSight(this.creeper);

        // When far enough away from target, only move when not looking (unless cover was blown):
        if (squaredDistance > 16.0) {
            // Freeze! Target player is looking! (Andy's coming!)
            if (isPlayerLooking && canPlayerSeeCreeper && !this.wasCoverBlown) {
                this.creeper.getNavigation().stop();
                return; // Won't explode, either
            }
            // If the player can't see the creeper, creep up on them:
            else {
                if(this.creeper.getNavigation().isDone()) {
                    this.creeper.getNavigation().moveTo(targetEntity, this.wasCoverBlown ? this.speed * 1.05 : this.speed * 0.9);
                    this.creeper.getLookControl().setLookAt(targetEntity, 30.0f, 30.0f);
                }
            }
        }

        // If the player is looking, but the creeper is close enough, start charging:
        else {
            if (targetEntity.isSwinging() && isPlayerLooking && canPlayerSeeCreeper) this.wasCoverBlown = true;
            if(this.wasCoverBlown && !canPlayerSeeCreeper) this.wasCoverBlown = false;
            if(this.wasCoverBlown || !(isPlayerLooking && canPlayerSeeCreeper) || this.creeper.getNavigation().isDone()) {
                this.creeper.getNavigation().moveTo(targetEntity, this.speed * (this.creeper.getSwellDir() > 0 ? 0.5 : 1.0));
                this.creeper.getLookControl().setLookAt(targetEntity, 30.0f, 30.0f);
            }
        }

        // Explode when within 2.5 blocks:
        boolean shouldStartExploding = (squaredDistance < 6.25 && isPlayerLooking && this.creeper.getSensing().hasLineOfSight(targetEntity));
        boolean shouldKeepExploding = (this.creeper.getSwellDir() > 0);
        if(shouldStartExploding || shouldKeepExploding) {
            this.creeper.setSwellDir(1);
        }
        else this.creeper.setSwellDir(-1);
    }
}
