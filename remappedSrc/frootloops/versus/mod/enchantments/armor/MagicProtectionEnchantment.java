package frootloops.versus.mod.enchantments.armor;

import net.minecraft.enchantment.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.ItemTags;

import static frootloops.versus.mod.enchantments.Enchants.ALL_ARMOR;


public class MagicProtectionEnchantment extends ProtectionEnchantment {
    public MagicProtectionEnchantment() {
        super(
                Enchantment.properties(ItemTags.ARMOR_ENCHANTABLE, 3, 1,
                        Enchantment.leveledCost(16, 10),
                        Enchantment.leveledCost(50, 10), 10,
                        ALL_ARMOR),
                Type.ALL
        );
    }

    @Override
    public boolean isTreasure() {
        return false;
    }

    @Override
    public boolean isAvailableForRandomSelection() {
        return true;
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
                    if(attackDamage > 0f) return (int)(attackDamage * ((float)level/2f));
                }
            }
        }
        return 0;
    }
}