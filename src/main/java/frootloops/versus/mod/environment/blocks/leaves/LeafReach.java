package frootloops.versus.mod.environment.blocks.leaves;

import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What keeps natural leaves alive (LeavesMixin): one of their own tree's logs, stripped or not, within vanilla's six
 * blocks, counting through leaves of their own kind. Oak leaves also take the jungle log a jungle bush grows them on,
 * azalea's two leaves are one kind, and leaves whose tree has no logs of its own here take any log.
 *
 * <p>Built the first time a leaf asks, not as a static part of the leaves block: that class loads while the game is still
 * making its blocks, before any leaves exist to be keys.
 */
public final class LeafReach {

    private static final Map<Block, TagKey<Block>> OWN_LOGS = Map.of(
            Blocks.OAK_LEAVES, logs("oak_logs"),
            Blocks.SPRUCE_LEAVES, logs("spruce_logs"),
            Blocks.BIRCH_LEAVES, logs("birch_logs"),
            Blocks.DARK_OAK_LEAVES, logs("dark_oak_logs"),
            Blocks.JUNGLE_LEAVES, BlockTags.JUNGLE_LOGS,
            Blocks.ACACIA_LEAVES, logs("acacia_logs"),
            Blocks.CHERRY_LEAVES, logs("cherry_logs"));

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

    /** A vanilla tag of one wood's logs, stripped or not, which 26.4 keeps as data but no longer names in BlockTags. */
    private static TagKey<Block> logs(String name) {
        return TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace(name));
    }

    private static boolean isAzalea(Block leaves) {
        return leaves == Blocks.AZALEA_LEAVES || leaves == Blocks.FLOWERING_AZALEA_LEAVES;
    }
}
