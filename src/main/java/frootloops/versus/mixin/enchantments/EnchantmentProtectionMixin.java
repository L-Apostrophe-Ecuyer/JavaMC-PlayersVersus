package frootloops.versus.mixin.enchantments;

import net.minecraft.entity.DamageUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(DamageUtil.class)
public class EnchantmentProtectionMixin {

    @ModifyConstant(method = "getInflictedDamage", constant = @Constant(floatValue = 20.0f))
    private static float higherCeiling(float protectionMaxValue) {
        return 30.0f;
    }

    @ModifyConstant(method = "getInflictedDamage", constant = @Constant(floatValue = 25.0f))
    private static float lessProtectionOverall(float protectionMaxValue) {
        return 35.0f;
    }
}
