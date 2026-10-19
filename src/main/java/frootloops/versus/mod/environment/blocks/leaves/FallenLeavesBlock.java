package frootloops.versus.mod.environment.blocks.leaves;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Green leaf litter: what the green-leaved trees shed (LeafLitter), tinted with the biome's foliage colour. Natural
 * piles brown into vanilla leaf litter over about a day; piles a player places are persistent and never do.
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
}
