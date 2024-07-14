
package frootloops.versus.mod.environment.blocks;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.block.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;

public class WheatGrass extends ShortPlantBlock {
    public WheatGrass(Settings settings) {
        super(settings);
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        BlockState blockToTurnInto = (state.getBlock() == CustomBlocks.WHEAT_GRASS) ? CustomBlocks.WILD_WHEAT.getDefaultState() : Blocks.WHEAT.getDefaultState().with(CropBlock.AGE, 7);
        world.setBlockState(pos, blockToTurnInto, NOTIFY_LISTENERS);
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isIn(BlockTags.DIRT) || floor.isOf(Blocks.FARMLAND) || floor.isIn(BlockTags.DEAD_BUSH_MAY_PLACE_ON);
    }
}
