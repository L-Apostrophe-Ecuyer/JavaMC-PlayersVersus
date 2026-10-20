package frootloops.versus.mixin.environment.worldgen;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import frootloops.versus.mod.environment.blocks.trees.TreeBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.FallenTreeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A fallen tree (a stump and a log lying beside it) is made of tree blocks as a standing tree is (TreeBlocks): each log
 * it puts down is its wood's tree block, and once it and its decorations are in, their faces are set.
 */
@Mixin(FallenTreeFeature.class)
public abstract class FallenTreeFeatureMixin {

    @Inject(method = "place", at = @At("HEAD"))
    private void playersVersus$startFallenTree(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin, CallbackInfoReturnable<Boolean> cir) {
        TreeBlocks.startFallen();
    }

    @WrapOperation(method = "placeLogBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean playersVersus$growTreeBlocks(WorldGenLevel level, BlockPos pos, BlockState state, Operation<Boolean> original) {
        return original.call(level, pos, TreeBlocks.fallen(pos, state));
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void playersVersus$setTreeBlockFaces(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin, CallbackInfoReturnable<Boolean> cir) {
        TreeBlocks.finishFallen(level);
    }
}
