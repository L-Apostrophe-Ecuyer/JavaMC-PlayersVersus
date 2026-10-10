package frootloops.versus.mod.mobs.hostile.overworld;

import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

public class CreeperIgniteThroughBlocksGoal extends Goal {
    private final Creeper creeper;
    @Nullable
    private LivingEntity target;

    public CreeperIgniteThroughBlocksGoal(Creeper creeper) {
        this.creeper = creeper;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity creeperTarget = this.creeper.getTarget();

        //Always continue when already exploding, but never when no target
        if (this.creeper.getSwellDir() > 0)  return true;
        if (this.creeper.getTarget() == null) return false;

        //If method thinks creeper can start exploding, check if the player can see it
        if (!CreeperIgniteThroughBlocksGoal.canSee(creeperTarget, this.creeper)) {
            return false;
        }

        //If method thinks creeper can't start exploding, check if the creeper can try breaching
        if (this.creeper.tickCount > 60 && !this.creeper.isPathFinding() && this.creeper.distanceToSqr(creeperTarget) < 49) {
            return true;
        }

        return this.creeper.distanceToSqr(creeperTarget) < 9.0;
    }

    @Override
    public void start() {
        this.creeper.getNavigation().stop();
        this.target = this.creeper.getTarget();
    }

    @Override
    public void stop() {
        this.target = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.target == null) {
            this.creeper.setSwellDir(-1);
            return;
        }
        if (this.creeper.distanceToSqr(this.target) > 49.0) {
            this.creeper.setSwellDir(-1);
            return;
        }
        if(this.creeper.tickCount > 60 && !this.creeper.isPathFinding()) {
            this.creeper.setSwellDir(1);
            return;
        }
        if (!this.creeper.getSensing().hasLineOfSight(this.target)) {
            this.creeper.setSwellDir(-1);
            return;
        }
        this.creeper.setSwellDir(1);
    }

    public static boolean canSee(LivingEntity seer, Creeper peeper) {
        Vec3 peeperPos = peeper.position();
        Vec3 vecLook = seer.getLookAngle();
        Vec3 subtractedReverse = peeperPos.vectorTo(seer.position()).normalize();
        subtractedReverse = new Vec3(subtractedReverse.x, 0, subtractedReverse.z);
        double dot = subtractedReverse.dot(vecLook);
        return dot < 0.0;
    }
}
