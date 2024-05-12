package frootloops.versus.mod.enchantments.tools;

import net.minecraft.enchantment.DamageEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;

import java.util.Optional;


public class CleavingEnchantment extends DamageEnchantment {


    public CleavingEnchantment() {
        super(
                Enchantment.properties(ItemTags.AXES, 2, 3,
                        Enchantment.leveledCost(15, 11),
                        Enchantment.leveledCost(26, 20), 6,
                        EquipmentSlot.MAINHAND), Optional.empty()
        );
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return stack.getItem() instanceof AxeItem;
    }

    @Override
    public float getAttackDamage(int level, EntityType type) {
        return  1F + (float)(level)/2F;
    }
}