package frootloops.versus.mixin.mobs;

import frootloops.versus.mod.environment.blocks.clays.CustomMudBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LandPathNodeMaker.class)
public class LandPathfindingMixin {

    @Inject(
            method = "getCommonNodeType",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void onGetCommonNodeType(BlockView world, BlockPos pos, CallbackInfoReturnable<PathNodeType> cir) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof CustomMudBlock) cir.setReturnValue(PathNodeType.DAMAGE_OTHER);
    }
}
