package frootloops.versus.mixin.environment;

import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ScaffoldingBlock.class)
public abstract class ScaffoldingMixin extends PlantBlock {

    protected ScaffoldingMixin(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return floor.getBlock() instanceof FarmlandBlock;
    }
}
