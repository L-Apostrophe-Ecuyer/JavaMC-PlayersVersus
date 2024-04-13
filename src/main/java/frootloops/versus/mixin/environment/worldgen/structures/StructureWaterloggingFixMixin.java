package frootloops.versus.mixin.environment.worldgen.structures;

import net.minecraft.block.BlockState;
import net.minecraft.block.FluidFillable;
import net.minecraft.fluid.FluidState;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureTemplate.class)
public abstract class StructureWaterloggingFixMixin {

    /**
     * Improves the waterlogging of structure pieces, so that waterloggable blocks aren't flooded when in aquifers.
     * In vanilla, the use of chains or slabs in jigsaw structures would likely cause them to flood, but this fixes the issue (partially - half-slab floors can still flood under certain circumstances).
     */
    @Redirect(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/FluidFillable;tryFillWithFluid(Lnet/minecraft/world/WorldAccess;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/FluidState;)Z"))
    public boolean dontFloodMyStructures(FluidFillable fluidFillableBlock, WorldAccess world, BlockPos blockPos, BlockState blockState, FluidState fluidState) {
        if(world.getBlockState(blockPos.add(0, -1, 0)).isAir()) return false; // No waterlogging, if the block below me is air!
        for(int x = -1; x < 2; x++) {
            for(int z = -1; z < 2; z++) {
                BlockState neighborSideways = world.getBlockState(blockPos.add(x, 0, z));

                // No waterlogging, if my neighbors are air blocks!
                if(neighborSideways.isAir()) return false;

                // Waterlogging if a neighbor is waterlogged.
                if(neighborSideways.getFluidState().isStill()) return fluidFillableBlock.tryFillWithFluid(world, blockPos, blockState, fluidState);

                // No waterlogging, if my neighbors are waterloggable, but dry!
                if(neighborSideways.getBlock() instanceof FluidFillable) return false;
            }
        }
        return fluidFillableBlock.tryFillWithFluid(world, blockPos, blockState, fluidState);
    }


    /**
     * Helps ensure that jigsaw pieces can fit together.
     * In vanilla, they often think they're colliding when they in fact aren't)
      */
    @Inject(method = "createBox", at = @At("RETURN"), cancellable = true)
    private static void createBox(CallbackInfoReturnable<BlockBox> cir) {
        cir.setReturnValue(cir.getReturnValue());
    }
}
