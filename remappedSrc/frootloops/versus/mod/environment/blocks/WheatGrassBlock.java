
package frootloops.versus.mod.environment.blocks;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.block.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class WheatGrassBlock extends ShortPlantBlock {

    public static final BooleanProperty IS_MUDDY = BooleanProperty.of("is_muddy");

    public WheatGrassBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(IS_MUDDY, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(IS_MUDDY);
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        if((state.getBlock() == CustomBlocks.WILD_WHEAT)) {
            if(random.nextFloat() < 0.33f) world.setBlockState(pos, Blocks.WHEAT.getDefaultState().with(CropBlock.AGE, 7), NOTIFY_LISTENERS);
        }
        else {
            world.setBlockState(pos, CustomBlocks.WILD_WHEAT.getStateWithProperties(state), NOTIFY_LISTENERS);
        }
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isIn(BlockTags.DIRT) || floor.isOf(Blocks.FARMLAND) || floor.isIn(BlockTags.DEAD_BUSH_MAY_PLACE_ON);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if(world.getBlockState(pos.down()).isOf(CustomBlocks.BROWN_MUD)) {
            world.setBlockState(pos, state.with(IS_MUDDY, true), Block.NOTIFY_LISTENERS);
        }
    }
}
