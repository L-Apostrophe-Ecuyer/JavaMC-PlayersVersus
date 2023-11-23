package frootloops.versus.mixin.enchantments;

import net.minecraft.entity.DamageUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(DamageUtil.class)
public class EnchantmentProtectionMixin {

    @ModifyConstant(method = "getInflictedDamage", constant = @Constant(floatValue = 20.0f))
    private static float maxCeiling(float protectionMaxValue) {
        return 18.0f;
    }

    @ModifyConstant(method = "getInflictedDamage", constant = @Constant(floatValue = 25.0f))
    private static float demoninator(float protectionMaxValue) {
        return 24.0f;
    }
}
