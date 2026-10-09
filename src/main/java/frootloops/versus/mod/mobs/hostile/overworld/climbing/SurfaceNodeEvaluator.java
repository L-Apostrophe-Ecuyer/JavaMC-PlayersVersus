package frootloops.versus.mod.mobs.hostile.overworld.climbing;

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.Nullable;

/**
 * Path nodes for a climber: the flyer's nodes in every direction, but only where the mob's box rests on or clings to a
 * block, on any side or around an edge, so paths run along floors, up walls and across ceilings and never through open
 * air. Walls cost a little extra and ceilings a little more, so a climber keeps to the floor when that's as short.
 */
public class SurfaceNodeEvaluator extends FlyNodeEvaluator {
    static final float FLOOR_COST = 0.0F, WALL_COST = 0.5F, EDGE_COST = 0.5F, CEILING_COST = 1.0F, OFF_SURFACES = -1.0F;

    private final Long2FloatOpenHashMap surfaceCosts = new Long2FloatOpenHashMap();
    private final Long2BooleanOpenHashMap solids = new Long2BooleanOpenHashMap();
    private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

    @Override
    public void prepare(PathNavigationRegion region, Mob mob) {
        super.prepare(region, mob);
        this.surfaceCosts.clear();
        this.solids.clear();
    }

    @Override
    public void done() {
        this.surfaceCosts.clear();
        this.solids.clear();
        super.done();
    }

    @Override
    @Nullable
    protected Node findAcceptedNode(int x, int y, int z) {
        PathType type = this.getCachedPathType(x, y, z);
        float malus = this.mob.getPathfindingMalus(type);
        if (malus < 0.0F) return null;
        float surface = this.surfaceCost(x, y, z);
        if (surface < 0.0F) return null;
        Node node = this.getNode(x, y, z);
        node.type = type;
        node.costMalus = Math.max(node.costMalus, malus + surface);
        return node;
    }

    private float surfaceCost(int x, int y, int z) {
        long key = BlockPos.asLong(x, y, z);
        if (this.surfaceCosts.containsKey(key)) return this.surfaceCosts.get(key);
        float cost = this.findSurface(x, y, z);
        this.surfaceCosts.put(key, cost);
        return cost;
    }

    /** What the box at a node rests on or clings to: the floor, a wall, a ceiling, a block around an edge, or nothing. */
    private float findSurface(int x, int y, int z) {
        int w = this.entityWidth, h = this.entityHeight, d = this.entityDepth;
        for (int i = x; i < x + w; i++) {
            for (int k = z; k < z + d; k++) {
                if (this.solid(i, y - 1, k)) return FLOOR_COST;
            }
        }
        for (int j = y; j < y + h; j++) {
            for (int k = z; k < z + d; k++) {
                if (this.solid(x - 1, j, k) || this.solid(x + w, j, k)) return WALL_COST;
            }
            for (int i = x; i < x + w; i++) {
                if (this.solid(i, j, z - 1) || this.solid(i, j, z + d)) return WALL_COST;
            }
        }
        for (int i = x; i < x + w; i++) {
            for (int k = z; k < z + d; k++) {
                if (this.solid(i, y + h, k)) return CEILING_COST;
            }
        }
        // Around an edge: the rims beside the floor and the ceiling, and the vertical corners.
        for (int i = x - 1; i <= x + w; i++) {
            for (int k = z - 1; k <= z + d; k++) {
                boolean rim = i == x - 1 || i == x + w || k == z - 1 || k == z + d;
                if (rim && (this.solid(i, y - 1, k) || this.solid(i, y + h, k))) return EDGE_COST;
            }
        }
        for (int j = y; j < y + h; j++) {
            if (this.solid(x - 1, j, z - 1) || this.solid(x + w, j, z - 1) || this.solid(x - 1, j, z + d) || this.solid(x + w, j, z + d)) return EDGE_COST;
        }
        return OFF_SURFACES;
    }

    private boolean solid(int x, int y, int z) {
        long key = BlockPos.asLong(x, y, z);
        if (this.solids.containsKey(key)) return this.solids.get(key);
        this.probe.set(x, y, z);
        boolean solid = !this.currentContext.getBlockState(this.probe).getCollisionShape(this.currentContext.level(), this.probe).isEmpty();
        this.solids.put(key, solid);
        return solid;
    }
}
