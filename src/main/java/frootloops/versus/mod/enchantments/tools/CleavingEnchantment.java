package frootloops.versus.mod.enchantments.tools;

import net.minecraft.enchantment.DamageEnchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;

import java.util.Optional;


public class CleavingEnchantment extends DamageEnchantment {


    public CleavingEnchantment() {
        super(Rarity.UNCOMMON, 15, 11, 20, Optional.empty(), new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    @Override
    public int getMinPower(int level) {
        return 15 + (level - 1) * 11;
    }

    @Override
    public int getMaxPower(int level) {
        return super.getMinPower(level) + 50;
    }

    @Override
    public int getMaxLevel() {
        return 3;
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