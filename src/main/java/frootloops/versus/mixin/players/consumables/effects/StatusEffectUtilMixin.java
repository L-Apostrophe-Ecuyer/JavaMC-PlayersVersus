package frootloops.versus.mixin.players.consumables.effects;

import net.minecraft.entity.effect.StatusEffectUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatusEffectUtil.class)
public class StatusEffectUtilMixin {

    @Inject(method = "getHasteAmplifier", at = @At("RETURN"), cancellable = true)
    private static void betterHaste(CallbackInfoReturnable<Integer> cir) {
        if(cir.getReturnValue() > 0) cir.setReturnValue(cir.getReturnValue() + 1);
    }

}
