package frootloops.versus.mod.environment.blocks.leaves;

import java.util.Map;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How far natural leaves live from their tree (LeavesMixin): within so many blocks of one of their own tree's logs,
 * stripped or not, counting through leaves of their own kind. Oak leaves also take the jungle log a jungle bush grows
 * them on, azalea's two leaves are one kind, and leaves whose tree has no logs of its own here take any log. Most trees'
 * leaves reach five blocks, a block short of vanilla.
 *
 * <p>Built the first time a leaf asks, not as a static part of the leaves block: that class loads while the game is still
 * making its blocks, before any leaves exist to be keys.
 */
public final class LeafReach {

    private static final int VANILLA_REACH = 6;
    private static final Map<Block, TagKey<Block>> OWN_LOGS = Map.of(
            Blocks.OAK_LEAVES, BlockTags.OAK_LOGS,
            Blocks.SPRUCE_LEAVES, BlockTags.SPRUCE_LOGS,
            Blocks.BIRCH_LEAVES, BlockTags.BIRCH_LOGS,
            Blocks.DARK_OAK_LEAVES, BlockTags.DARK_OAK_LOGS,
            Blocks.JUNGLE_LEAVES, BlockTags.JUNGLE_LOGS,
            Blocks.ACACIA_LEAVES, BlockTags.ACACIA_LOGS,
            Blocks.CHERRY_LEAVES, BlockTags.CHERRY_LOGS);
    private static final Map<Block, Integer> REACH = Map.of(
            Blocks.OAK_LEAVES, 5,
            Blocks.SPRUCE_LEAVES, 5,
            Blocks.BIRCH_LEAVES, 5,
            Blocks.DARK_OAK_LEAVES, 5,
            Blocks.PALE_OAK_LEAVES, 5,
            Blocks.ACACIA_LEAVES, 5,
            Blocks.CHERRY_LEAVES, 5);

    private LeafReach() {
    }

    /** Whether this block is a log that keeps these leaves alive. */
    public static boolean isOwnLog(Block leaves, BlockState log) {
        TagKey<Block> own = OWN_LOGS.get(leaves);
        if (own == null) return log.is(BlockTags.LOGS);
        return log.is(own) || leaves == Blocks.OAK_LEAVES && log.is(BlockTags.JUNGLE_LOGS);
    }

    /** Whether a log's reach passes from this neighbour on to these leaves: leaves of their own kind. */
    public static boolean isOwnKind(Block leaves, BlockState neighbour) {
        return neighbour.is(leaves) || isAzalea(leaves) && isAzalea(neighbour.getBlock());
    }

    /** How far from their logs these leaves live. */
    public static int reach(Block leaves) {
        return REACH.getOrDefault(leaves, VANILLA_REACH);
    }

    private static boolean isAzalea(Block leaves) {
        return leaves == Blocks.AZALEA_LEAVES || leaves == Blocks.FLOWERING_AZALEA_LEAVES;
    }
}
