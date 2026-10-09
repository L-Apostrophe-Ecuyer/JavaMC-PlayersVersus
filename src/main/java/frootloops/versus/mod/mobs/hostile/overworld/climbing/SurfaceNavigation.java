package frootloops.versus.mod.mobs.hostile.overworld.climbing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.phys.Vec3;

/**
 * A climber's navigation: paths in three dimensions like a flyer's, along surfaces only ({@link SurfaceNodeEvaluator}),
 * searched twice as far since they wind over walls and ceilings, and followed node by node: a shortcut in a straight
 * line could leave the surfaces.
 */
public class SurfaceNavigation extends FlyingPathNavigation {
    private static final int SEARCH_MULTIPLIER = 2;

    public SurfaceNavigation(Mob mob, Level level) {
        super(mob, level);
    }

    @Override
    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new SurfaceNodeEvaluator();
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes * SEARCH_MULTIPLIER);
    }

    @Override
    protected boolean canMoveDirectly(Vec3 from, Vec3 to) {
        return false;
    }

    /** Somewhere to stand: on top of a block, as for walkers, so wandering ends on the ground. */
    @Override
    public boolean isStableDestination(BlockPos pos) {
        BlockPos below = pos.below();
        return !this.level.getBlockState(below).getCollisionShape(this.level, below).isEmpty();
    }

    @Override
    public boolean canNavigateGround() {
        return true;
    }
}
