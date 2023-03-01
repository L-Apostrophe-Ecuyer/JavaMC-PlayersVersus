package frootloops.versus.mixin.enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.SweepingEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SweepingEnchantment.class)
public class SweepingMixin extends Enchantment {
    protected SweepingMixin(Rarity weight, EnchantmentTarget type, EquipmentSlot[] slotTypes) {
        super(weight, type, slotTypes);
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        if ((stack.getItem() instanceof SwordItem || stack.getItem() instanceof HoeItem)){
            return true;
        }
        return this.type.isAcceptableItem(stack.getItem());
    }
}