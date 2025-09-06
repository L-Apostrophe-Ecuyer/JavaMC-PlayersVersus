package frootloops.versus.mixin.environment.archeology;

import frootloops.versus.VersusSettings;
import net.minecraft.block.BlockState;
import net.minecraft.block.BrushableBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.BrushableBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BrushItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
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
    private void finishBrushing(ServerWorld world, LivingEntity brusher, ItemStack itemStack, CallbackInfo info) {
        if(itemStack.getUseAction() != UseAction.BRUSH || !itemStack.isOf(Items.BRUSH)) {
            if(!(world.getBlockState(this.getPos()).getBlock() instanceof BrushableBlock)) {
                world.breakBlock(this.getPos(), true, brusher);
            }
        }
    }

    @ModifyConstant(method = "brush", constant = @Constant(longValue = 10L))
    private long fasterBrushing(long tickDelayUntilNextBrushStage) {return (long) VersusSettings.BRUSHING_TICKS_PER_STAGE;}
}
