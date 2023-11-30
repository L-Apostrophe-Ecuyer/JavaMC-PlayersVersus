package frootloops.versus.mod.hostile_mobs.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class CreeperIgniteThroughBlocksGoal extends Goal {
    private final CreeperEntity creeper;
    @Nullable
    private LivingEntity target;

    public CreeperIgniteThroughBlocksGoal(CreeperEntity creeper) {
        this.creeper = creeper;
        this.setControls(EnumSet.of(Goal.Control.MOVE));
    }

    @Override
    public boolean canStart() {
        LivingEntity creeperTarget = this.creeper.getTarget();

        //Always continue when already exploding, but never when no target
        if (this.creeper.getFuseSpeed() > 0)  return true;
        if (this.creeper.getTarget() == null) return false;

        //If method thinks creeper can start exploding, check if the player can see it
        if (!CreeperIgniteThroughBlocksGoal.canSee(creeperTarget, this.creeper)) {
            return false;
        }

        //If method thinks creeper can't start exploding, check if the creeper can try breaching
        if (this.creeper.age > 60 && !this.creeper.isNavigating() && this.creeper.squaredDistanceTo(creeperTarget) < 49) {
            return true;
        }

        return this.creeper.squaredDistanceTo(creeperTarget) < 9.0;
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
    public boolean shouldRunEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.target == null) {
            this.creeper.setFuseSpeed(-1);
            return;
        }
        if (this.creeper.squaredDistanceTo(this.target) > 49.0) {
            this.creeper.setFuseSpeed(-1);
            return;
        }
        if(this.creeper.age > 60 && !this.creeper.isNavigating()) {
            this.creeper.setFuseSpeed(1);
            return;
        }
        if (!this.creeper.getVisibilityCache().canSee(this.target)) {
            this.creeper.setFuseSpeed(-1);
            return;
        }
        this.creeper.setFuseSpeed(1);
    }

    public static boolean canSee(LivingEntity seer, CreeperEntity peeper) {
        Vec3d peeperPos = peeper.getPos();
        Vec3d vecLook = seer.getRotationVector();
        Vec3d subtractedReverse = peeperPos.relativize(seer.getPos()).normalize();
        subtractedReverse = new Vec3d(subtractedReverse.x, 0, subtractedReverse.z);
        double dot = subtractedReverse.dotProduct(vecLook);
        return dot < 0.0;
    }
}
