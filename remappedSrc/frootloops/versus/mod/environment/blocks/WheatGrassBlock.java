
package frootloops.versus.mod.environment.blocks;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.block.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;

public class WheatGrassBlock extends ShortPlantBlock {
    public WheatGrassBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        if((state.getBlock() == CustomBlocks.WILD_WHEAT)) {
            if(random.nextFloat() < 0.33f) world.setBlockState(pos, Blocks.WHEAT.getDefaultState().with(CropBlock.AGE, 7), NOTIFY_LISTENERS);
        }
        else {
            world.setBlockState(pos, CustomBlocks.WILD_WHEAT.getDefaultState(), NOTIFY_LISTENERS);
        }
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isIn(BlockTags.DIRT) || floor.isOf(Blocks.FARMLAND) || floor.isIn(BlockTags.DEAD_BUSH_MAY_PLACE_ON);
    }
}
