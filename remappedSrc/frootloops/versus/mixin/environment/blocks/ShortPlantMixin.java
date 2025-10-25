package frootloops.versus.mixin.environment.blocks;

import frootloops.versus.VersusMod;
import net.minecraft.block.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ShortPlantBlock.class)
public abstract class ShortPlantMixin extends PlantBlock implements Fertilizable {

    protected ShortPlantMixin(Settings settings) {
        super(settings);
    }

    @Override
    protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.isIn(BlockTags.DIRT) || floor.isOf(Blocks.FARMLAND) || floor.isIn(BlockTags.DRY_VEGETATION_MAY_PLACE_ON);
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return !world.getBlockState(pos.down()).isIn(BlockTags.DRY_VEGETATION_MAY_PLACE_ON);
    }
}
