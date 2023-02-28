package frootloops.versus.util.enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;


public class PhysicalProtectionEnchantment extends Enchantment {
    public PhysicalProtectionEnchantment() {
        super(Rarity.UNCOMMON, EnchantmentTarget.ARMOR, new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET});
    }

    @Override
    public int getMinPower(int level) {
        return 10 + 8 * (level - 1);
    }

    @Override
    public int getMaxPower(int level) {
        return super.getMinPower(level) + 8;
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }

    @Override
    public int getProtectionAmount(int level, DamageSource source) {
        if (!source.isProjectile() && !source.isExplosive()
                && !source.isMagic() && !source.isFromFalling()
                && !source.isFire() && !source.bypassesArmor())
            return level;
        return 0;
    }

    @Override
    public boolean canAccept(Enchantment other) {
        if(other instanceof PhysicalProtectionEnchantment || other instanceof MagicProtectionEnchantment)
            return false;
        if(other instanceof ProtectionEnchantment) {
            return ((ProtectionEnchantment) other).protectionType == ProtectionEnchantment.Type.FALL;
        }
        return true;
    }
}