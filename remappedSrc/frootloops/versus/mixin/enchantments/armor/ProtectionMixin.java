package frootloops.versus.mixin.enchantments.armor;

import Rarity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.Item;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ProtectionEnchantment.class)
public class ProtectionMixin extends Enchantment {
    @Shadow public final ProtectionEnchantment.Type protectionType;

    protected ProtectionMixin(Rarity rarity, TagKey<Item> applicableItems, EquipmentSlot[] slotTypes, ProtectionEnchantment.Type protectionType) {
        super(rarity, applicableItems, slotTypes);
        this.protectionType = protectionType;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public boolean isAvailableForEnchantedBookOffer() {
        return this.isAvailableForRandomSelection() && this.protectionType != ProtectionEnchantment.Type.ALL;
    }

    @Override
    public int getMinPower(int level) {
        return this.protectionType.getBasePower() + (((level * level)/4) * this.protectionType.getPowerPerLevel());
    }

    @Override
    public int getMaxPower(int level) {
        if(level == getMaxLevel()) return 60;
        return this.getMinPower(level + 1);
    }

    @Override
    public boolean isTreasure() {
        return this.protectionType == ProtectionEnchantment.Type.ALL;
    }

    @Override
    public boolean isAvailableForRandomSelection() {
        return (this != Enchantments.PROJECTILE_PROTECTION); // Effectively disabled, it was too situational
    }

    @Override
    public boolean canAccept(Enchantment other) {
        return super.canAccept(other); // Protection enchantments can be combined
    }

    @Override
    public Rarity getRarity() {
        if(this.protectionType == ProtectionEnchantment.Type.ALL) return Rarity.VERY_RARE;
        return Rarity.RARE;
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
            return level * 2;
        }
        if (this.protectionType == ProtectionEnchantment.Type.EXPLOSION && source.isIn(DamageTypeTags.IS_EXPLOSION)) {
            return level * 2;
        }
        if (this.protectionType == ProtectionEnchantment.Type.PROJECTILE && source.isIn(DamageTypeTags.IS_PROJECTILE)) {
            return level * 2 + 1;
        }
        return 0;
    }
}