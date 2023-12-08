package frootloops.versus.mixin.enchantments.tools;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.KnockbackEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(KnockbackEnchantment.class)
public class KnockbackMixin extends Enchantment {
    protected KnockbackMixin(Rarity weight, EnchantmentTarget type, EquipmentSlot[] slotTypes) {
        super(Rarity.UNCOMMON, type, slotTypes);
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return (stack.getItem() instanceof AxeItem ||
                stack.getItem() instanceof ShovelItem ||
                stack.getItem() instanceof HoeItem);
    }
}