package frootloops.versus.mod.enchantments.armor;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.registry.tag.DamageTypeTags;


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
                if (attacker.hasStatusEffect(StatusEffects.STRENGTH)) {
                    int amplifier = attacker.getStatusEffect(StatusEffects.STRENGTH).getAmplifier();
                    return level/2 + amplifier;
                }
                if (attacker.getMainHandStack().hasEnchantments()) {
                    float attackDamage = EnchantmentHelper.getAttackDamage(attacker.getMainHandStack(), EntityGroup.DEFAULT);
                    if(attackDamage > 0f) return (int)(attackDamage * level/2f);
                }
            }
        }
        return 0;
    }

    @Override
    public void onUserDamaged(LivingEntity user, Entity attacker, int level) {
        if(attacker instanceof WardenEntity && user.isAlive()) {
            user.heal(2.0f);
            if(user.squaredDistanceTo(attacker) > 4.0d) user.heal(3.0f * (float)level);
        }
    }
}