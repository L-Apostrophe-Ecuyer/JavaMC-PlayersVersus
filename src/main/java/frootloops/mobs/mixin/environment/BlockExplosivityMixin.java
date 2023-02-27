package frootloops.mobs.mixin.environment;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public class BlockExplosivityMixin {

    @Inject(method = "getBlastResistance()F", at = @At("RETURN"), cancellable = true)
    private void lowerBlastResistance(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(cir.getReturnValue() * 0.5f);
    }

}
