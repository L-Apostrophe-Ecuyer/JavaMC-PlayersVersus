package frootloops.versus.mixin.environment;

import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CropBlock.class)
public abstract class CropMixin extends PlantBlock {

    protected CropMixin(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.getBlock() instanceof FarmlandBlock;
    }
}
