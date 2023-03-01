package frootloops.versus.util.enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.tag.DamageTypeTags;


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
        if(source.isIn(DamageTypeTags.IS_FALL) || source.isIn(DamageTypeTags.DAMAGES_HELMET))
            return (level + 1) >> 1;
        if(source.isIn(DamageTypeTags.IS_FALL) || source.isIn(DamageTypeTags.DAMAGES_HELMET))
            return (level + 1) >> 1;
        if(source.method_49708(DamageTypes.CRAMMING) || source.method_49708(DamageTypes.IN_WALL))
            return (level + 1) >> 1;
        if(source.method_49708(DamageTypes.MOB_ATTACK) || source.method_49708(DamageTypes.PLAYER_ATTACK))
            return level;
        if(source.method_49708(DamageTypes.CACTUS) || source.method_49708(DamageTypes.SWEET_BERRY_BUSH))
            return level + 1;
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