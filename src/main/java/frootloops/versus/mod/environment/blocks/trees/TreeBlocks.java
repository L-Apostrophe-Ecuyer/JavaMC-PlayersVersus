package frootloops.versus.mod.environment.blocks.trees;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.CustomBlocks;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

/**
 * Trees grow tree blocks (TreeFeatureMixin, FallenTreeFeatureMixin): every log a growing tree puts down, trunk or crown,
 * becomes its wood's tree block, turned as the log was, the dirt it puts under its trunk becomes rooted dirt, a pale oak
 * grows its roots (TreeRoots), and once the tree and its decorations are in place each tree block's faces are set from
 * what is next to it. A fallen tree's stump and log are tree blocks too. A log with no tree block of its own, such as
 * another mod's, stays as it is.
 */
public final class TreeBlocks {

    /** The tree blocks a fallen tree being placed on this thread has put down so far, or null when none is. */
    private static final ThreadLocal<Set<BlockPos>> FALLEN = new ThreadLocal<>();

    private TreeBlocks() {
    }

    /** The tree block standing for this vanilla log, or null when it has none. */
    public static TreeBlock forLog(Block log) {
        return CustomBlocks.TREE_BLOCKS.get(log);
    }

    /** What a growing tree puts its trunk down through: its wood's tree block for each log, noting where in grown. */
    public static BiConsumer<BlockPos, BlockState> growing(BiConsumer<BlockPos, BlockState> trunkSetter, Set<BlockPos> grown) {
        return (pos, state) -> trunkSetter.accept(pos, grow(pos, state, grown));
    }

    /** What a growing tree puts its crown down through, as its trunk: a poplar's crown grows logs round its trunk. */
    public static FoliagePlacer.FoliageSetter growing(FoliagePlacer.FoliageSetter foliageSetter, Set<BlockPos> grown) {
        return new FoliagePlacer.FoliageSetter() {
            @Override
            public void set(BlockPos pos, BlockState state) {
                foliageSetter.set(pos, grow(pos, state, grown));
            }

            @Override
            public boolean isSet(BlockPos pos) {
                return foliageSetter.isSet(pos);
            }
        };
    }

    /** The tree block for this log, turned as it is, noting where in grown; the dirt under the trunk as rooted dirt; anything else as it is. */
    private static BlockState grow(BlockPos pos, BlockState state, Set<BlockPos> grown) {
        if (state.is(Blocks.DIRT) && VersusSettings.Trees.ROOTED_DIRT_UNDER_TREES) return Blocks.ROOTED_DIRT.defaultBlockState();
        TreeBlock tree = forLog(state.getBlock());
        if (tree == null || !state.hasProperty(RotatedPillarBlock.AXIS)) return state;
        grown.add(pos.immutable());
        return tree.defaultBlockState().setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
    }

    /** A tree grew these tree blocks (when it grew at all): its roots grow, then every tree block's faces are set. */
    public static void finish(WorldGenLevel level, RandomSource random, Set<BlockPos> grown, boolean placed) {
        if (placed) TreeRoots.grow(level, random, grown);
        setFaces(level, grown);
    }

    /** A fallen tree starts being placed. */
    public static void startFallen() {
        if (VersusSettings.Trees.DO_TREE_BLOCKS) FALLEN.set(new HashSet<>());
    }

    /** What a fallen tree puts down at this spot instead of this: its wood's tree block for its log. */
    public static BlockState fallen(BlockPos pos, BlockState state) {
        Set<BlockPos> grown = FALLEN.get();
        return grown == null ? state : grow(pos, state, grown);
    }

    /** A fallen tree is in place, its decorations too: its tree blocks' faces are set. */
    public static void finishFallen(WorldGenLevel level) {
        Set<BlockPos> grown = FALLEN.get();
        FALLEN.remove();
        if (grown != null) setFaces(level, grown);
    }

    /** Sets the faces of the tree blocks a tree grew, now that it's all in place; one replaced since (a creaking heart) is left be. */
    public static void setFaces(WorldGenLevel level, Set<BlockPos> grown) {
        for (BlockPos pos : grown) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof TreeBlock) level.setBlock(pos, TreeBlock.withFaces(level, pos, state), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }
}
