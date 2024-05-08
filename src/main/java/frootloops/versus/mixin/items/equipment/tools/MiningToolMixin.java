package frootloops.versus.mixin.items.equipment.tools;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.*;
import net.minecraft.sound.BlockSoundGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MiningToolItem.class)
public abstract class MiningToolMixin extends ToolItem {

    public MiningToolMixin(ToolMaterial material, Settings settings, float miningSpeed) {
        super(material, settings);
        this.miningSpeed = miningSpeed;
    }

    @Shadow protected final float miningSpeed;

    @Inject(method = "getMiningSpeedMultiplier", at = @At("RETURN"), cancellable = true)
    private void getMiningSpeedMultiplier(ItemStack stack, BlockState state, CallbackInfoReturnable<Float> cir) {
        if(cir.getReturnValue() == 1.0f) {

            if(stack.getItem() instanceof PickaxeItem && state.isOf(Blocks.GRAVEL) || state.getSoundGroup() == BlockSoundGroup.STONE) {
                cir.setReturnValue(this.miningSpeed);
            }

            else if(stack.getItem() instanceof ShovelItem && state.isOf(Blocks.PACKED_MUD) || state.getSoundGroup() == BlockSoundGroup.MUD_BRICKS) {
                cir.setReturnValue(this.miningSpeed);
            }
        }
    }

    @Inject(method = "isSuitableFor", at = @At("RETURN"), cancellable = true)
    private void isSuitableFor(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if(!cir.getReturnValue()) {
            if((ToolItem)this instanceof PickaxeItem && (state.isOf(Blocks.GRAVEL) || state.isOf(Blocks.SUSPICIOUS_GRAVEL) || state.getSoundGroup() == BlockSoundGroup.STONE)) {
                cir.setReturnValue(true);
            }

            else if((ToolItem)this instanceof ShovelItem && (state.isOf(Blocks.PACKED_MUD) || state.getSoundGroup() == BlockSoundGroup.MUD_BRICKS)) {
                cir.setReturnValue(true);
            }
        }
    }

}
