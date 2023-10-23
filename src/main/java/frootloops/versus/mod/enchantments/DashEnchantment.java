package frootloops.versus.mod.enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.SwiftSneakEnchantment;
import net.minecraft.entity.EquipmentSlot;

public class DashEnchantment extends Enchantment {
    public DashEnchantment() {
        super(Rarity.RARE, EnchantmentTarget.ARMOR, new EquipmentSlot[]{EquipmentSlot.LEGS});
    }

    public boolean isTreasure() {
        return true;
    }

    @Override
    public int getMinPower(int level) {
        return 10 + 20 * (level - 1);
    }

    @Override
    public int getMaxPower(int level) {
        return super.getMinPower(level) + 50;
    }

    @Override
    public boolean isAvailableForEnchantedBookOffer() {
        return false;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public boolean canAccept(Enchantment other) {
        return !(other instanceof DashEnchantment || other instanceof SwiftSneakEnchantment);
    }
}
