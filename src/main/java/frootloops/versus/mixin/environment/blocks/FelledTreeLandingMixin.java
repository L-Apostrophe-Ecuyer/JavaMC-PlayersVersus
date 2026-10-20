package frootloops.versus.mixin.environment.blocks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import frootloops.versus.mod.environment.blocks.trees.TreeFelling;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * How a felled tree's falling blocks land (TreeFelling): a trunk log as a block of the fallen log unless there's a log
 * under it or a creature in the way, any other log as its item, a leaf bursting as if it decayed there. A leaf that
 * can't land at all bursts too, rather than dropping as a leaf block.
 */
@Mixin(FallingBlockEntity.class)
public abstract class FelledTreeLandingMixin {

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean playersVersus$landFelled(Level level, BlockPos pos, BlockState state, Operation<Boolean> original) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        if (level instanceof ServerLevel server && TreeFelling.isFelled(self) && !TreeFelling.land(server, self, pos)) return true;
        return original.call(level, pos, state);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity playersVersus$dropFelled(FallingBlockEntity self, ServerLevel level, ItemLike item, Operation<ItemEntity> original) {
        if (self.entityTags().contains(TreeFelling.LEAF)) {
            TreeFelling.breakOnLanding(level, self, self.blockPosition());
            return null;
        }
        return original.call(self, level, item);
    }
}
