package frootloops.versus.mod.enchantments.armor;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.*;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.ItemTags;

import static frootloops.versus.mod.enchantments.Enchants.ALL_ARMOR;


public class PiercingProtectionEnchantment extends ProtectionEnchantment {
    public PiercingProtectionEnchantment() {
        super(
                Enchantment.properties(ItemTags.ARMOR_ENCHANTABLE, 3, 1,
                        Enchantment.leveledCost(8, 18),
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
        if(source.isOf(DamageTypes.STALAGMITE) || source.isIn(DamageTypeTags.DAMAGES_HELMET))
            return level * 2;
        else if(source.isOf(DamageTypes.MOB_PROJECTILE) || source.isOf(DamageTypes.ARROW) || source.isOf(DamageTypes.TRIDENT))
            return level * 2;
        else if(source.isOf(DamageTypes.MOB_ATTACK) || source.isOf(DamageTypes.PLAYER_ATTACK)) {
            if( source.getAttacker() instanceof LivingEntity attacker) {
                ItemStack mainHandStack = attacker.getMainHandStack();
                if(!mainHandStack.isEmpty() && mainHandStack.isDamageable()) {
                    Item weapon = mainHandStack.getItem();
                    if(weapon instanceof PickaxeItem || weapon instanceof TridentItem) return level * 2;
                    if(weapon instanceof SwordItem) return (level * 3)/2;
                    if(weapon instanceof ToolItem) return level - 1;
                    return 0;
                }
            }
        }
        else if(source.isOf(DamageTypes.CACTUS) || source.isOf(DamageTypes.SWEET_BERRY_BUSH) || source.isOf(DamageTypes.STING))
            return level;
        return 0;
    }
}