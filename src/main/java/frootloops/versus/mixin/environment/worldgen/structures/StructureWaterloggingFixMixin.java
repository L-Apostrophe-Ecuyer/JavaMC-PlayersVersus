package frootloops.versus.mixin.environment.worldgen.structures;

import net.minecraft.block.BlockState;
import net.minecraft.block.FluidFillable;
import net.minecraft.fluid.FluidState;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StructureTemplate.class)
public abstract class StructureWaterloggingFixMixin {

    @Redirect(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/FluidFillable;tryFillWithFluid(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/FluidState;)Z"))
    public boolean dontFloodMyStructures(FluidFillable fluidFillableBlock, WorldAccess world, BlockPos blockPos, BlockState blockState, FluidState fluidState) {
        if(world.getBlockState(blockPos.add(0, -1, 0)).isAir()) return false; // No waterlogging, if the block below me is air!
        for(int x = -1; x < 2; x++) {
            for(int z = -1; z < 2; z++) {
                BlockState neighborSideways = world.getBlockState(blockPos.add(x, 0, z));
                if(neighborSideways.isAir()) return false; // No waterlogging, if my neighbors are air blocks!
                if(!neighborSideways.getFluidState().isStill()) break; // Waterlogging if a neighbor is waterlogged.
                if(neighborSideways.getBlock() instanceof FluidFillable) return false; // No waterlogging, if my neighbors are waterloggable, but dry!
            }
        }
        return fluidFillableBlock.tryFillWithFluid(world, blockPos, blockState, fluidState);
    }
}
