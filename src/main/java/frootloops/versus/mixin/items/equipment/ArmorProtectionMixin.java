package frootloops.versus.mixin.items.equipment;

import net.minecraft.entity.DamageUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(DamageUtil.class)
public class ArmorProtectionMixin {

    @ModifyConstant(method = "getDamageLeft", constant = @Constant(floatValue = 4.0f))
    private static float increaseToReduceEffectOfToughness(float toughnessDenominator) {
        return 12.0f;
    }

    @ModifyConstant(method = "getDamageLeft", constant = @Constant(floatValue = 2.0f))
    private static float increaseToIncreasePassiveToughness(float toughnessDenominator) {
        return 3.0f;
    }

    @ModifyConstant(method = "getDamageLeft", constant = @Constant(floatValue = 0.2f))
    private static float increaseToArmorPointMinimum(float protectionMaxValue) {
        return 0.2f;
    }

    @ModifyConstant(method = "getDamageLeft", constant = @Constant(floatValue = 25.0f))
    private static float increaseToReduceEffectOfArmorOverall(float protectionMaxValue) {
        return 30.0f;
    }  // Unchanged
}
