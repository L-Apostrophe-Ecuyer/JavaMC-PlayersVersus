package frootloops.versus.util.enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShovelItem;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.SoundEvents;


public class MagicProtectionEnchantment extends Enchantment {
    public MagicProtectionEnchantment() {
        super(Rarity.RARE, EnchantmentTarget.ARMOR, new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET});
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
        if (source.isIn(DamageTypeTags.WITCH_RESISTANT_TO)) return (level * 3);
        if (source.isOf(DamageTypes.DRAGON_BREATH)) return (level * 3);
        if (source.isOf(DamageTypes.THORNS)) return (level * 3);
        if (source.isOf(DamageTypes.MOB_ATTACK) || source.isOf(DamageTypes.PLAYER_ATTACK)) {
            if(source.getAttacker() instanceof LivingEntity attacker) {
                if (attacker.hasStatusEffect(StatusEffects.STRENGTH)) return 4;
            }
        }
        return 0;
    }

    @Override
    public boolean canAccept(Enchantment other) {
        if(other instanceof PhysicalProtectionEnchantment || other instanceof MagicProtectionEnchantment)
            return false;
        if (other instanceof ProtectionEnchantment) {
            return ((ProtectionEnchantment) other).protectionType == ProtectionEnchantment.Type.FALL;
        }
        return true;
    }
}