package frootloops.versus.mixin.players;

import net.minecraft.entity.DamageUtil;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageUtil.class)
public class DamageUtilMixin {

    @Inject(method = "getInflictedDamage", at = @At("HEAD"), cancellable = true)
    private static void protectionEnchantsNerfed(float damageDealt, float protection, CallbackInfoReturnable<Float> cir) {
        // Previous: damageDealt * (1 - Clamp(proection, 0, 20)/25) = 0% to 75%, with one piece of protection IV shielding 16% of damage (full set: 64%)
        // New: 0% to 83%, with one piece of protection IV shielding 11% of damage (full set: 44%)
        // Encourages players to have different sets for different situations, and actually use the other protection enchants.
        cir.setReturnValue(damageDealt * (1f - MathHelper.clamp(protection, 0f, 30f)/36f));
        cir.cancel();
    }
}
