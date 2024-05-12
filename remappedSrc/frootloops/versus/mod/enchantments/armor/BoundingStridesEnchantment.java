package frootloops.versus.mod.enchantments.armor;

import net.minecraft.enchantment.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.registry.tag.ItemTags;

public class BoundingStridesEnchantment extends Enchantment {
    public BoundingStridesEnchantment() {
        super(
                Enchantment.properties(ItemTags.LEG_ARMOR_ENCHANTABLE, 2, 2,
                        Enchantment.leveledCost(10, 20),
                        Enchantment.leveledCost(32, 26), 8,
                        EquipmentSlot.LEGS)
        );
    }

    public boolean isTreasure() {
        return true;
    }

    @Override
    public boolean isAvailableForEnchantedBookOffer() {
        return false;
    }

}
