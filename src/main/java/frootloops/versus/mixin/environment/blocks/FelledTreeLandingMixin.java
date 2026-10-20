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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * How a felled tree's falling blocks move and land (TreeFelling): while its tree turns over they move with it, not by
 * their own fall; landing, a trunk block lands as its wood's log lying the way it fell unless there's a log under it or
 * a creature in the way, a hive lands whole and lets its bees out, and anything that can't land whole breaks: a log
 * into its wood's log item, a leaf bursting as if it decayed there, a hive letting its bees out.
 */
@Mixin(FallingBlockEntity.class)
public abstract class FelledTreeLandingMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void playersVersus$turnWithItsTree(CallbackInfo info) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        if (self.level() instanceof ServerLevel && TreeFelling.isSwinging(self)) info.cancel();
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean playersVersus$landFelled(Level level, BlockPos pos, BlockState state, Operation<Boolean> original) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        if (!(level instanceof ServerLevel server) || !TreeFelling.isFelled(self)) return original.call(level, pos, state);
        BlockState landing = TreeFelling.landing(server, self, pos, state);
        if (landing == null) return true;
        boolean landed = original.call(level, pos, landing);
        if (landed) TreeFelling.landed(server, self, pos);
        return landed;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity playersVersus$dropFelled(FallingBlockEntity self, ServerLevel level, ItemLike item, Operation<ItemEntity> original) {
        if (!TreeFelling.isFelled(self)) return original.call(self, level, item);
        TreeFelling.breakOnLanding(level, self, self.blockPosition());
        return null;
    }
}
