package frootloops.versus.mixin.enchantments.tools;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.KnockbackEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(KnockbackEnchantment.class)
public class KnockbackMixin extends Enchantment {

    protected KnockbackMixin(Rarity rarity, TagKey<Item> applicableItems, EquipmentSlot[] slotTypes) {
        super(rarity, applicableItems, slotTypes);
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