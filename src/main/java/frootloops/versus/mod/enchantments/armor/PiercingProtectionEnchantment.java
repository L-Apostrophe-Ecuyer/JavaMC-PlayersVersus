package frootloops.versus.mod.enchantments.armor;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.*;
import net.minecraft.registry.tag.DamageTypeTags;


public class PiercingProtectionEnchantment extends Enchantment {
    public PiercingProtectionEnchantment() {
        super(Rarity.UNCOMMON, EnchantmentTarget.ARMOR, new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET});
    }

    @Override
    public boolean isTreasure() {
        return true;
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
        if(source.isOf(DamageTypes.STALAGMITE) || source.isIn(DamageTypeTags.DAMAGES_HELMET))
            return level * 2;
        if(source.isOf(DamageTypes.MOB_PROJECTILE) || source.isOf(DamageTypes.ARROW) || source.isOf(DamageTypes.TRIDENT))
            return level * 3;
        if(source.isOf(DamageTypes.MOB_ATTACK) || source.isOf(DamageTypes.PLAYER_ATTACK)) {
            if( source.getAttacker() instanceof LivingEntity attacker) {
                ItemStack mainHandStack = attacker.getMainHandStack();
                if(!mainHandStack.isEmpty() && mainHandStack.getItem().isDamageable()) {
                    Item weapon = mainHandStack.getItem();
                    if(weapon instanceof SwordItem || weapon instanceof PickaxeItem) return (level * 2) - (level >> 1);
                    if(weapon instanceof ToolItem || weapon instanceof TridentItem) return level;
                }
            }
        }
        if(source.isOf(DamageTypes.CACTUS) || source.isOf(DamageTypes.SWEET_BERRY_BUSH) || source.isOf(DamageTypes.STING))
            return level;
        return 0;
    }
}