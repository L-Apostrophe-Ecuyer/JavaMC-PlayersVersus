package frootloops.versus.mod.environment.blocks.trees;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.blocks.leaves.LeafReach;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * How a tree falls (step 2 of docs/superpowers/specs/2026-10-19-trees-design.md), worked out from the blocks alone, so
 * the server and a client asking the same blocks agree: what a cut leaves with nothing holding it up, which way each
 * group of it goes, which leaves go with it, and how much longer the cut takes. Nothing here changes the world; what
 * brings a tree down builds on it.
 */
public final class Felling {

    /** One group of tree blocks a cut leaves with nothing holding it up: its blocks, the leaves going with it, and which way it falls. */
    public record Fall(Set<BlockPos> blocks, Set<BlockPos> crown, Direction direction) {}

    /** Tree blocks connected to each other, and whether that's all of them (not when there were too many, or unloaded chunks). */
    private record Group(Set<BlockPos> blocks, boolean complete) {}

    /** What a leaf going with a tree weighs, against one of its tree blocks, for which way the tree leans. */
    private static final double LEAF_WEIGHT = 0.25;
    /** How far a tree's leaves reach from it through leaves: vanilla's six steps. */
    private static final int CROWN_REACH = 6;

    private Felling() {
    }

    /** What falls were the block at this spot cut (whether or not it's gone yet), by a cutter standing here or nobody. */
    public static List<Fall> afterCut(LevelReader level, BlockPos cut, @Nullable Vec3 cutter, RandomSource random) {
        Set<BlockPos> gone = Set.of(cut.immutable());
        List<Set<BlockPos>> groups = falling(level, gone);
        if (groups.isEmpty()) return List.of();
        Set<BlockPos> leaving = new HashSet<>(gone);
        groups.forEach(leaving::addAll);
        List<Fall> falls = new ArrayList<>();
        for (Set<BlockPos> group : groups) {
            Set<BlockPos> crown = crown(level, group, leaving);
            falls.add(new Fall(group, crown, direction(group, crown, cut, cutter, random)));
        }
        return falls;
    }

    /** How many times as long as a log's cutting the tree block at this spot takes: 1 + CUT_TIME_SCALE x sqrt(what would fall). */
    public static double cutTimeFactor(LevelReader level, BlockPos pos) {
        int falling = 0;
        for (Set<BlockPos> group : falling(level, Set.of(pos.immutable()))) falling += group.size();
        return 1.0 + VersusSettings.Trees.CUT_TIME_SCALE * Math.sqrt(falling);
    }

    /**
     * The groups of tree blocks next to these spots (by faces, edges or corners) that nothing would hold up were the
     * blocks there gone. A group too big to follow, or reaching chunks that aren't loaded, counts as standing.
     */
    private static List<Set<BlockPos>> falling(LevelReader level, Set<BlockPos> gone) {
        List<Set<BlockPos>> falling = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>(gone);
        for (BlockPos removed : gone) {
            for (BlockPos next : BlockPos.betweenClosed(removed.offset(-1, -1, -1), removed.offset(1, 1, 1))) {
                if (seen.contains(next) || !level.hasChunkAt(next) || !(level.getBlockState(next).getBlock() instanceof TreeBlock)) continue;
                Group group = group(level, next.immutable(), gone);
                seen.addAll(group.blocks());
                if (group.complete() && !standing(level, group.blocks(), gone)) falling.add(group.blocks());
            }
        }
        return falling;
    }

    /** The tree blocks, and creaking hearts, connected to this one by faces, edges or corners, leaving out those gone. */
    private static Group group(LevelReader level, BlockPos start, Set<BlockPos> gone) {
        Set<BlockPos> blocks = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        blocks.add(start);
        queue.add(start);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (BlockPos next : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if (blocks.contains(next) || gone.contains(next)) continue;
                if (!level.hasChunkAt(next)) return new Group(blocks, false);
                if (!isPart(level.getBlockState(next))) continue;
                if (blocks.size() >= VersusSettings.Trees.MOST_TREE_BLOCKS) return new Group(blocks, false);
                BlockPos found = next.immutable();
                blocks.add(found);
                queue.add(found);
            }
        }
        return new Group(blocks, true);
    }

    /** A tree block, or a creaking heart in a trunk of them. */
    static boolean isPart(BlockState state) {
        return state.getBlock() instanceof TreeBlock || state.is(Blocks.CREAKING_HEART);
    }

    /** Whether something outside the group holds it up: one of its blocks resting on a sturdy top face below it, never leaves. */
    private static boolean standing(LevelReader level, Set<BlockPos> group, Set<BlockPos> gone) {
        for (BlockPos pos : group) {
            BlockPos below = pos.below();
            if (group.contains(below) || gone.contains(below)) continue;
            BlockState state = level.getBlockState(below);
            if (!state.is(BlockTags.LEAVES) && state.isFaceSturdy(level, below, Direction.UP)) return true;
        }
        return false;
    }

    /**
     * Which way a group falls: toward the side its weight sits more than LEAN blocks off the cut; else straight away from
     * whoever cut it (the nearest of north, south, east and west); else, with nobody to fall away from or a cutter right
     * above or below, toward its heavier side, or a random one when it's balanced.
     */
    private static Direction direction(Set<BlockPos> group, Set<BlockPos> crown, BlockPos cut, @Nullable Vec3 cutter, RandomSource random) {
        double weight = 0.0, x = 0.0, z = 0.0;
        for (BlockPos pos : group) {
            weight += 1.0;
            x += pos.getX() - cut.getX();
            z += pos.getZ() - cut.getZ();
        }
        for (BlockPos pos : crown) {
            weight += LEAF_WEIGHT;
            x += LEAF_WEIGHT * (pos.getX() - cut.getX());
            z += LEAF_WEIGHT * (pos.getZ() - cut.getZ());
        }
        x /= weight;
        z /= weight;
        if (Math.sqrt(x * x + z * z) > VersusSettings.Trees.LEAN) return nearest(x, z);
        if (cutter != null) {
            double awayX = cut.getX() + 0.5 - cutter.x, awayZ = cut.getZ() + 0.5 - cutter.z;
            if (Math.abs(awayX) >= 0.5 || Math.abs(awayZ) >= 0.5) return nearest(awayX, awayZ);
        }
        if (x != 0.0 || z != 0.0) return nearest(x, z);
        return Direction.from2DDataValue(random.nextInt(4));
    }

    /** The one of north, south, east and west nearest this horizontal heading. */
    private static Direction nearest(double x, double z) {
        if (Math.abs(x) >= Math.abs(z)) return x > 0.0 ? Direction.EAST : Direction.WEST;
        return z > 0.0 ? Direction.SOUTH : Direction.NORTH;
    }

    /**
     * The leaves that go with a falling group: the natural leaves its blocks keep alive, out to six steps through leaves
     * of their own kind, each nearer to the group than to any log or tree block left standing that keeps it alive. A leaf
     * as near another tree, or nearer, stays with that tree, so a tree felled out of another's crown takes only its own.
     */
    private static Set<BlockPos> crown(LevelReader level, Set<BlockPos> group, Set<BlockPos> leaving) {
        Map<BlockPos, Integer> fromGroup = reach(level, group);
        if (fromGroup.isEmpty()) return Set.of();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos leaf : fromGroup.keySet()) {
            minX = Math.min(minX, leaf.getX()); minY = Math.min(minY, leaf.getY()); minZ = Math.min(minZ, leaf.getZ());
            maxX = Math.max(maxX, leaf.getX()); maxY = Math.max(maxY, leaf.getY()); maxZ = Math.max(maxZ, leaf.getZ());
        }
        // The logs left standing that could reach those leaves: any within a crown's reach of them
        List<BlockPos> standing = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(minX - CROWN_REACH, minY - CROWN_REACH, minZ - CROWN_REACH,
                maxX + CROWN_REACH, maxY + CROWN_REACH, maxZ + CROWN_REACH)) {
            if (leaving.contains(pos) || !level.hasChunkAt(pos)) continue;
            if (level.getBlockState(pos).is(BlockTags.LOGS)) standing.add(pos.immutable());
        }
        Map<BlockPos, Integer> fromStanding = reach(level, standing);
        Set<BlockPos> crown = new HashSet<>();
        fromGroup.forEach((leaf, distance) -> {
            Integer other = fromStanding.get(leaf);
            if (other == null || distance < other) crown.add(leaf);
        });
        return crown;
    }

    /** How far the natural leaves these logs keep alive are from them, as vanilla counts: through leaves of their own kind, up to six steps. */
    private static Map<BlockPos, Integer> reach(LevelReader level, Collection<BlockPos> logs) {
        Map<BlockPos, Integer> distance = new HashMap<>();
        List<BlockPos> ring = new ArrayList<>();
        for (BlockPos log : logs) {
            BlockState logState = level.getBlockState(log);
            for (Direction side : Direction.values()) {
                BlockPos next = log.relative(side);
                if (distance.containsKey(next) || !level.hasChunkAt(next)) continue;
                BlockState leaf = level.getBlockState(next);
                if (isNatural(leaf) && LeafReach.isOwnLog(leaf.getBlock(), logState)) {
                    distance.put(next, 1);
                    ring.add(next);
                }
            }
        }
        for (int step = 2; step <= CROWN_REACH && !ring.isEmpty(); step++) {
            List<BlockPos> next = new ArrayList<>();
            for (BlockPos pos : ring) {
                Block kind = level.getBlockState(pos).getBlock();
                for (Direction side : Direction.values()) {
                    BlockPos beside = pos.relative(side);
                    if (distance.containsKey(beside) || !level.hasChunkAt(beside)) continue;
                    BlockState leaf = level.getBlockState(beside);
                    if (isNatural(leaf) && LeafReach.isOwnKind(kind, leaf)) {
                        distance.put(beside, step);
                        next.add(beside);
                    }
                }
            }
            ring = next;
        }
        return distance;
    }

    /** A tree's own leaf, not one a player placed. */
    private static boolean isNatural(BlockState state) {
        return state.getBlock() instanceof LeavesBlock && !state.getValue(LeavesBlock.PERSISTENT);
    }
}
