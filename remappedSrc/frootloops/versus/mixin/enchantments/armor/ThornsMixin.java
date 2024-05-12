package frootloops.versus.mixin.enchantments.armor;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.ThornsEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ThornsEnchantment.class)
public class ThornsMixin extends Enchantment {

    public ThornsMixin(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        if (stack.getItem() instanceof ShieldItem || (stack.getItem() instanceof ArmorItem armorItem && armorItem.getSlotType() == EquipmentSlot.BODY)) return true;
        return false;
    }
}