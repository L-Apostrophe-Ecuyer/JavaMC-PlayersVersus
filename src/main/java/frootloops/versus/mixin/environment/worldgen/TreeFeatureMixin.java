package frootloops.versus.mixin.environment.worldgen;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.blocks.trees.TreeBlocks;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Every tree the tree feature grows (in new chunks, from saplings, from bone meal) is made of tree blocks (TreeBlocks):
 * its trunk, and the logs its crown grows round it (a poplar's), go down through setters that put each log's tree block
 * in its place, and once the tree and its decorations are in, the ground under its trunk turns to rooted dirt, its
 * roots grow (a pale oak's) and the faces of its tree blocks are set.
 */
@Mixin(TreeFeature.class)
public abstract class TreeFeatureMixin {

    @WrapOperation(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/TreeFeature;doPlace(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Ljava/util/function/BiConsumer;Ljava/util/function/BiConsumer;Lnet/minecraft/world/level/levelgen/feature/foliageplacers/FoliagePlacer$FoliageSetter;)Z"))
    private boolean playersVersus$growTreeBlocks(TreeFeature self, WorldGenLevel level, RandomSource random, BlockPos origin,
                                                 BiConsumer<BlockPos, BlockState> rootSetter, BiConsumer<BlockPos, BlockState> trunkSetter,
                                                 FoliagePlacer.FoliageSetter foliageSetter, Operation<Boolean> original,
                                                 @Share("treeBlocks") LocalRef<Set<BlockPos>> treeBlocks) {
        if (!VersusSettings.Trees.DO_TREE_BLOCKS) return original.call(self, level, random, origin, rootSetter, trunkSetter, foliageSetter);
        Set<BlockPos> grown = new HashSet<>();
        treeBlocks.set(grown);
        return original.call(self, level, random, origin, rootSetter, TreeBlocks.growing(trunkSetter, grown), TreeBlocks.growing(foliageSetter, grown));
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void playersVersus$setTreeBlockFaces(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin,
                                                 CallbackInfoReturnable<Boolean> cir, @Share("treeBlocks") LocalRef<Set<BlockPos>> treeBlocks) {
        if (treeBlocks.get() != null) TreeBlocks.finish(level, random, treeBlocks.get(), Boolean.TRUE.equals(cir.getReturnValue()));
    }
}
