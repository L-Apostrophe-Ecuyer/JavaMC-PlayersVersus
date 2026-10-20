package frootloops.versus.mod.environment.blocks.trees;

import frootloops.versus.VersusSettings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A creaking heart tree's roots (a pale oak's, as TreeFeatureMixin grows it): each column of its trunk goes on down into
 * the ground, ROOT_DEPTH_MIN to ROOT_DEPTH_MAX blocks as far as the ground lets it, and a root or two branches out
 * sideways near the top, some of them showing at the surface. The creaking heart its decorator set in the trunk moves
 * down into a root, HEART_IN_ROOTS of the time, into a knot of roots round it so it has pale oak logs on every side: it
 * keeps its creaking as it would in the trunk, and outlives the tree being felled, until someone digs it out. Roots
 * show bark where they meet the ground, as they would dug out; they are tree blocks like the trunk, so a pale oak is
 * cut down above them.
 */
public final class TreeRoots {

    /** How often a trunk column branches a root out sideways. */
    private static final float SIDE_ROOT_CHANCE = 0.6f;

    private TreeRoots() {
    }

    /** Whether trees of this wood grow roots. */
    private static boolean rooted(TreeBlock tree) {
        return tree.log() == Blocks.PALE_OAK_LOG;
    }

    /** Whether a root grows through this block: the ground, not air, water, or anything built. */
    private static boolean rootable(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.SUBSTRATE_OVERWORLD)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY);
    }

    /**
     * Grows the roots of the tree whose tree blocks were grown at these spots, if its wood grows them, moving its
     * creaking heart down into them by chance, and sets their faces.
     */
    public static void grow(WorldGenLevel level, RandomSource random, Set<BlockPos> grown) {
        if (!VersusSettings.Trees.DO_ROOTS) return;
        TreeBlock tree = null;
        int bottom = Integer.MAX_VALUE;
        for (BlockPos pos : grown) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof TreeBlock block) || state.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Y) continue;
            tree = block;
            bottom = Math.min(bottom, pos.getY());
        }
        if (tree == null || !rooted(tree)) return;
        BlockState root = tree.defaultBlockState();
        List<BlockPos> base = new ArrayList<>();
        for (BlockPos pos : grown) if (pos.getY() == bottom && level.getBlockState(pos).getBlock() instanceof TreeBlock) base.add(pos);
        Set<BlockPos> roots = new HashSet<>();
        // Each trunk column straight down, and the spots in it with a root above and below, where a heart could sit
        Map<BlockPos, Integer> depth = new HashMap<>();
        List<BlockPos> heartSpots = new ArrayList<>();
        for (BlockPos column : base) {
            int deep = VersusSettings.Trees.ROOT_DEPTH_MIN + random.nextInt(VersusSettings.Trees.ROOT_DEPTH_MAX - VersusSettings.Trees.ROOT_DEPTH_MIN + 1);
            int reached = 0;
            while (reached < deep && place(level, column.below(reached + 1), root, Direction.Axis.Y, roots)) reached++;
            depth.put(column, reached);
            for (int down = 2; down < reached; down++) heartSpots.add(column.below(down));
        }
        // A root out sideways from a column, away from the rest of the trunk, near the top
        for (BlockPos column : base) {
            if (random.nextFloat() >= SIDE_ROOT_CHANCE) continue;
            List<Direction> out = new ArrayList<>();
            for (Direction side : Direction.Plane.HORIZONTAL) if (!base.contains(column.relative(side))) out.add(side);
            if (out.isEmpty()) continue;
            Direction side = out.get(random.nextInt(out.size()));
            int down = 1 + random.nextInt(2);
            if (depth.get(column) < down) continue;
            int length = 1 + random.nextInt(2);
            BlockPos from = column.below(down);
            for (int step = 1; step <= length; step++) if (!place(level, from.relative(side, step), root, side.getAxis(), roots)) break;
        }
        BlockPos heart = heart(level, grown);
        if (heart != null && !heartSpots.isEmpty() && random.nextFloat() < VersusSettings.Trees.HEART_IN_ROOTS) {
            bury(level, heart, heartSpots.get(random.nextInt(heartSpots.size())), root, roots);
        }
        for (BlockPos pos : roots) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof TreeBlock) level.setBlock(pos, TreeBlock.withFaces(level, pos, state, false), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    /** A root at this spot, lying along this axis, if it can grow through what's there. */
    private static boolean place(WorldGenLevel level, BlockPos pos, BlockState root, Direction.Axis axis, Set<BlockPos> roots) {
        if (!rootable(level.getBlockState(pos))) return false;
        level.setBlock(pos, root.setValue(RotatedPillarBlock.AXIS, axis), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        roots.add(pos.immutable());
        return true;
    }

    /** The creaking heart the tree's decorator set in place of one of its tree blocks, if it set one. */
    private static @Nullable BlockPos heart(WorldGenLevel level, Set<BlockPos> grown) {
        for (BlockPos pos : grown) if (level.getBlockState(pos).is(Blocks.CREAKING_HEART)) return pos;
        return null;
    }

    /**
     * Moves the creaking heart from the trunk down to this spot in a root, standing up, a tree block back in its place;
     * roots grow round it on every side the trunk's columns don't, so it has pale oak logs all round.
     */
    private static void bury(WorldGenLevel level, BlockPos from, BlockPos to, BlockState root, Set<BlockPos> roots) {
        BlockState heart = level.getBlockState(from);
        level.setBlock(from, root.setValue(RotatedPillarBlock.AXIS, heart.getValue(CreakingHeartBlock.AXIS)), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos next = to.relative(side);
            if (!roots.contains(next)) place(level, next, root, side.getAxis(), roots);
        }
        roots.remove(to);
        level.setBlock(to, heart.setValue(CreakingHeartBlock.AXIS, Direction.Axis.Y), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }
}
