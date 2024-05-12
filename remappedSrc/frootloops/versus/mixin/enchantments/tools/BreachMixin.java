package frootloops.versus.mixin.enchantments.tools;

import net.minecraft.enchantment.BreachEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BreachEnchantment.class)
public abstract class BreachMixin extends Enchantment {

    public BreachMixin(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        if (stack.getItem() instanceof PickaxeItem) return true;
        return super.isAcceptableItem(stack);
    }

    @Inject(method = "getFactor", at = @At("RETURN"), cancellable = true)
    private static void getFactor(float level, float f, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(cir.getReturnValue() * 0.8f);
    }
}