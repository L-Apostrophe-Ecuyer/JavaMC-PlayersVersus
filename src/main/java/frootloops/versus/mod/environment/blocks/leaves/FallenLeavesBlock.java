package frootloops.versus.mod.environment.blocks.leaves;

import frootloops.versus.VersusSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.SegmentableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Green leaf litter: what the green-leaved trees shed (LeafLitter), tinted with the biome's foliage colour. Natural
 * piles fade over about a day, half browning into vanilla leaf litter and half blowing away; piles a player places are
 * persistent and stay green. Broken, it drops itself, by hand too.
 */
public class FallenLeavesBlock extends LeafLitterBlock {

    public static final BooleanProperty PERSISTENT = BlockStateProperties.PERSISTENT;

    public FallenLeavesBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(PERSISTENT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PERSISTENT);
    }

    /** A pile a player places, or adds to, is theirs. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(PERSISTENT, true);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(PERSISTENT);
    }

    /** Natural fallen leaves fade by chance: brown into vanilla leaf litter, keeping their segments and facing, or blow away. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PERSISTENT) || random.nextDouble() >= VersusSettings.Leaves.FALLEN_LEAVES_FADING_CHANCE) return;
        if (random.nextDouble() >= VersusSettings.Leaves.FALLEN_LEAVES_BROWNING_SHARE) {
            level.removeBlock(pos, false);
            return;
        }
        level.setBlock(pos, Blocks.LEAF_LITTER.defaultBlockState()
                .setValue(FACING, state.getValue(FACING))
                .setValue(SegmentableBlock.AMOUNT, state.getValue(SegmentableBlock.AMOUNT)), Block.UPDATE_ALL);
    }
}
