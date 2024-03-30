package frootloops.versus.mixin.enchantments.armor;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Rarity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;

import static frootloops.versus.mod.enchantments.Enchants.MAX_PROTECTION_LEVELS_PER_ITEM;

@Mixin(ProtectionEnchantment.class)
public class ProtectionMixin extends Enchantment {

    @Shadow
    public final ProtectionEnchantment.Type protectionType;

    public ProtectionMixin(Properties properties, ProtectionEnchantment.Type protectionType) {
        super(properties);
        this.protectionType = protectionType;
    }

    @Override
    public boolean isAvailableForEnchantedBookOffer() {
        return this.isAvailableForRandomSelection() && this.protectionType != ProtectionEnchantment.Type.ALL;
    }

    @Override
    public boolean isTreasure() {
        return this.protectionType == ProtectionEnchantment.Type.ALL;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        if(stack.getItem() instanceof ArmorItem armorItem && armorItem.getSlotType() != EquipmentSlot.BODY) {
            if(!stack.hasEnchantments()) return true;
            int numProtectionEnchants = 0;
            Iterator<RegistryEntry<Enchantment>> iterator = stack.getEnchantments().getEnchantments().iterator();
            while(iterator.hasNext()) {
                if(iterator.next().value() instanceof ProtectionEnchantment) numProtectionEnchants++;
                if(numProtectionEnchants >= MAX_PROTECTION_LEVELS_PER_ITEM) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isAvailableForRandomSelection() {
        // Effectively disabled these two enchantments, as they were aimed at too narrow a problem.
        //  > Piercing Protection now reduces all piercing damage, including arrows
        //  > Impact Protection now reduces all impact damage, including falling
        return (this != Enchantments.PROJECTILE_PROTECTION && this != Enchantments.FEATHER_FALLING);
    }

    @Override
    public boolean canAccept(Enchantment other) {
        return super.canAccept(other); // Protection enchantments can be combined
    }

    @Override
    public int getProtectionAmount(int level, DamageSource source) {
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return 0;
        }
        if (this.protectionType == ProtectionEnchantment.Type.ALL) {
            return level + 1;
        }
        if (this.protectionType == ProtectionEnchantment.Type.FIRE && source.isIn(DamageTypeTags.IS_FIRE)) {
            return level * 2 + 1;
        }
        if (this.protectionType == ProtectionEnchantment.Type.FALL && source.isIn(DamageTypeTags.IS_FALL)) {
            return level * 3;
        }
        if (this.protectionType == ProtectionEnchantment.Type.EXPLOSION && source.isIn(DamageTypeTags.IS_EXPLOSION)) {
            return level * 2;
        }
        if (this.protectionType == ProtectionEnchantment.Type.PROJECTILE && source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            return level * 3 + 1;
        }
        return 0;
    }
}