package frootloops.versus.mixin.environment.blocks;

import frootloops.versus.VersusMod;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.TallGrassBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TallGrassBlock.class)
public abstract class ShortPlantMixin extends VegetationBlock implements BonemealableBlock {

    protected ShortPlantMixin(Properties settings) {
        super(settings);
    }

    @Override
    protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return floor.is(BlockTags.DIRT) || floor.is(Blocks.FARMLAND) || floor.is(BlockTags.SUPPORTS_DRY_VEGETATION);
    }

    @Override
    public boolean isBonemealSuccess(Level world, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return !world.getBlockState(pos.below()).is(BlockTags.SUPPORTS_DRY_VEGETATION);
    }
}
