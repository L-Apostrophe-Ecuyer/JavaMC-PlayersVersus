package frootloops.versus.mixin.environment.blocks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A walking mob jumps when its feet are inside a block shorter than it can step (a slab, a snow layer), and asks the
 * block for its shape without saying who asks, which for leaves is a full block. Asked for the mob itself, leaves it
 * pushes through have no shape, so it walks through a hedge instead of hopping, and climbing it (LeafMovement).
 */
@Mixin(MoveControl.class)
public abstract class LeavesMoveControlMixin {

    @Shadow @Final protected Mob mob;

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getCollisionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape playersVersus$leavesAsTheMobFindsThem(BlockState state, BlockGetter level, BlockPos pos, Operation<VoxelShape> original) {
        if (state.getBlock() instanceof LeavesBlock) return state.getCollisionShape(level, pos, CollisionContext.of(this.mob));
        return original.call(state, level, pos);
    }
}
