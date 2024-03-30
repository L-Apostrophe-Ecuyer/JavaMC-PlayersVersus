package frootloops.versus.mod.enchantments.armor;

import net.minecraft.enchantment.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.DamageTypeTags;


public class MagicProtectionEnchantment extends ProtectionEnchantment {
    public MagicProtectionEnchantment() {
        super(Rarity.VERY_RARE, Type.PROJECTILE);
    }

    @Override
    public boolean isTreasure() {
        return true;
    }

    @Override
    public boolean isAvailableForRandomSelection() {
        return true;
    }

    @Override
    public int getMinPower(int level) {
        return 12 + (8 * (level - 1));
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
                    return (level * 2)/3 + amplifier;
                }
                if (attacker.getMainHandStack().hasEnchantments()) {
                    float attackDamage = EnchantmentHelper.getAttackDamage(attacker.getMainHandStack(), EntityType.PLAYER);
                    if(attackDamage > 0f) return (int)(attackDamage * level/2f);
                }
            }
        }
        return 0;
    }
}