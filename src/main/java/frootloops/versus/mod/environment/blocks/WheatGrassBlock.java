
package frootloops.versus.mod.environment.blocks;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

public class WheatGrassBlock extends TallGrassBlock {

    public static final BooleanProperty IS_MUDDY = BooleanProperty.create("is_muddy");

    public WheatGrassBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(IS_MUDDY, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(IS_MUDDY);
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        if((state.getBlock() == CustomBlocks.WILD_WHEAT)) {
            if(random.nextFloat() < 0.33f) world.setBlock(pos, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7), UPDATE_CLIENTS);
        }
        else {
            world.setBlock(pos, CustomBlocks.WILD_WHEAT.withPropertiesOf(state), UPDATE_CLIENTS);
        }
    }

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return floor.is(BlockTags.DIRT) || floor.is(Blocks.FARMLAND) || floor.is(BlockTags.SUPPORTS_DRY_VEGETATION);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        if(world.getBlockState(pos.below()).is(CustomBlocks.BROWN_MUD)) {
            world.setBlock(pos, state.setValue(IS_MUDDY, true), Block.UPDATE_CLIENTS);
        }
    }
}
