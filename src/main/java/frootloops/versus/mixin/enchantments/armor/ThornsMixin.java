package frootloops.versus.mixin.enchantments.armor;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.ThornsEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ThornsEnchantment.class)
public class ThornsMixin extends Enchantment {

    public ThornsMixin(Properties properties) {
        super(properties);
    }

    @Inject(at = @At("HEAD"), method = "isAcceptableItem", cancellable = true)
    private void shieldAcceptable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItem() instanceof ShieldItem)
            cir.setReturnValue(true);
        if (stack.getItem() instanceof ArmorItem && !((ArmorItem)stack.getItem()).getSlotType().equals(EquipmentSlot.CHEST))
            cir.setReturnValue(false);
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        if (stack.getItem() instanceof ShieldItem) return true;
        return super.isAcceptableItem(stack);
    }
}