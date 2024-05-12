package frootloops.versus.mod.enchantments.tools;

import frootloops.versus.mod.enchantments.Enchants;
import net.minecraft.enchantment.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.UseAction;


public class RiposteEnchantment extends Enchantment {

    public RiposteEnchantment() {
        super(
                Enchantment.properties(ItemTags.SWORD_ENCHANTABLE, 10, 3,
                        Enchantment.leveledCost(4, 8),
                        Enchantment.leveledCost(12, 20), 1,
                        EquipmentSlot.MAINHAND)
        );
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof SwordItem || stack.getItem() instanceof ShieldItem || stack.getUseAction() == UseAction.BLOCK;
    }

    @Override
    public boolean canAccept(Enchantment other) {
        return !(other instanceof ThornsEnchantment || other == Enchants.RIPOSTE || other == Enchantments.SWEEPING_EDGE);
    }
}