package frootloops.versus.mixin.environment.archeology;

import net.minecraft.block.BlockState;
import net.minecraft.block.BrushableBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.BrushableBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BrushItem;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(BrushableBlockEntity.class)
public abstract class SuspiciousBlocksMixin extends BlockEntity {

    public SuspiciousBlocksMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "finishBrushing", at = @At("RETURN"), cancellable = false)
    private void finishBrushing(PlayerEntity player, CallbackInfo info) {
        if(player.getWorld() instanceof ServerWorld serverWorld && !(player.getActiveItem().getItem() instanceof BrushItem)) {
            if(!(this.getWorld().getBlockState(this.getPos()).getBlock() instanceof BrushableBlock)) {
                serverWorld.breakBlock(this.getPos(), true, player);
            }
        }
    }

    @ModifyConstant(method = "brush", constant = @Constant(longValue = 10L))
    private long immediateFeedback(long tickDelayAfterUpdate) {return 1L;}

    @ModifyConstant(method = "brush", constant = @Constant(intValue = 40))
    private int immediateFeedbackTwo(int tickDelayToUpdateAfterBrushing) {return 1;}

    @ModifyConstant(method = "scheduledTick", constant = @Constant(longValue = 4L))
    private long immediateFeedbackThree(long tickDelayAfterUpdate) {
        return 1L;
    }
}
