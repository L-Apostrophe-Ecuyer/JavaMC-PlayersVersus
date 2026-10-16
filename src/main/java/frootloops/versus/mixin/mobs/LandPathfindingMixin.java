package frootloops.versus.mixin.mobs;

import frootloops.versus.mod.environment.blocks.clays.CustomMudBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WalkNodeEvaluator.class)
public class LandPathfindingMixin {

    @Inject(
            method = "getPathTypeFromState",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void onGetCommonNodeType(BlockGetter world, BlockPos pos, CallbackInfoReturnable<PathType> cir) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof CustomMudBlock) cir.setReturnValue(PathType.DAMAGING);
    }
}
