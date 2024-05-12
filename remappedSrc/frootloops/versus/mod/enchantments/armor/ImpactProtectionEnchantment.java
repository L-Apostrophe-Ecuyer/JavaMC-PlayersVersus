package frootloops.versus.mod.enchantments.armor;

import net.minecraft.enchantment.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.ItemTags;

import static frootloops.versus.mod.enchantments.Enchants.ALL_ARMOR;


public class ImpactProtectionEnchantment extends ProtectionEnchantment {
    public ImpactProtectionEnchantment() {
        super(
                Enchantment.properties(ItemTags.ARMOR_ENCHANTABLE, 3, 2,
                        Enchantment.leveledCost(12, 18),
                        Enchantment.leveledCost(40, 16), 6,
                        ALL_ARMOR),
                Type.PROJECTILE
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
        if(source.isOf(DamageTypes.FALL) || source.isIn(DamageTypeTags.IS_EXPLOSION))
            return level;
        if(source.isOf(DamageTypes.CRAMMING) || source.isOf(DamageTypes.IN_WALL) )
            return level * 2;
        if(source.isOf(DamageTypes.FLY_INTO_WALL) || source.isOf(DamageTypes.FALLING_ANVIL))
            return level * 3;
        if(source.isOf(DamageTypes.MOB_ATTACK) || source.isOf(DamageTypes.PLAYER_ATTACK)) {
            if( source.getAttacker() instanceof LivingEntity attacker) {
                if(attacker instanceof RavagerEntity || attacker instanceof WardenEntity || attacker instanceof IronGolemEntity) return level + 1;

                ItemStack mainHandStack = attacker.getMainHandStack();
                if(!mainHandStack.isEmpty() && mainHandStack.isDamageable()) {
                    Item weapon = mainHandStack.getItem();
                    if(weapon instanceof MaceItem) return level * 3;
                    if(weapon instanceof AxeItem || weapon instanceof ShovelItem) return level + 1;
                    return level/2;
                }
                else return level + level/2;
            }
        }
        return 0;
    }
}