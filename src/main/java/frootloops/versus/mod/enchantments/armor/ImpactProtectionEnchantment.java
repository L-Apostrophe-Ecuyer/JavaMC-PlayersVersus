package frootloops.versus.mod.enchantments.armor;

import net.minecraft.enchantment.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.math.MathHelper;


public class ImpactProtectionEnchantment extends ProtectionEnchantment {
    public ImpactProtectionEnchantment() {
        super(Rarity.RARE, Type.PROJECTILE);
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
    public int getMinPower(int level) {
        return 6 + (8 * level);
    }

    @Override
    public int getProtectionAmount(int level, DamageSource source) {
        if(source.isOf(DamageTypes.FALL) || source.isIn(DamageTypeTags.IS_EXPLOSION))
            return level;
        if(source.isOf(DamageTypes.CRAMMING) || source.isOf(DamageTypes.IN_WALL) )
            return level * 2;
        if(source.isOf(DamageTypes.FLY_INTO_WALL) || source.isOf(DamageTypes.FALLING_ANVIL))
            return level * 2;
        if(source.isOf(DamageTypes.MOB_ATTACK) || source.isOf(DamageTypes.PLAYER_ATTACK)) {
            if( source.getAttacker() instanceof LivingEntity attacker) {
                if(attacker instanceof RavagerEntity) return level + level/2;

                ItemStack mainHandStack = attacker.getMainHandStack();
                if(!mainHandStack.isEmpty() && mainHandStack.getItem().isDamageable()) {
                    Item weapon = mainHandStack.getItem();
                    if(weapon instanceof AxeItem || weapon instanceof ShovelItem) return level;
                    if(weapon instanceof ToolItem || weapon instanceof TridentItem) return level/2;
                }
                else return level + level/2;
            }
        }
        return 0;
    }
}