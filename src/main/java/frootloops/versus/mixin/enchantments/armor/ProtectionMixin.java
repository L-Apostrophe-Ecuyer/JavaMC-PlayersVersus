package frootloops.versus.mixin.enchantments.armor;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ProtectionEnchantment.class)
public class ProtectionMixin extends Enchantment {
    @Shadow public final ProtectionEnchantment.Type protectionType;

    protected ProtectionMixin(Rarity weight, EnchantmentTarget type, EquipmentSlot[] slotTypes, ProtectionEnchantment.Type protectionType) {
        super(weight, type, slotTypes);
        this.protectionType = protectionType;
    }

    @Override
    public int getMaxLevel() {
        return 4;
    }

    @Override
    public boolean isAvailableForEnchantedBookOffer() {
        return this.protectionType != ProtectionEnchantment.Type.ALL;
    }

    @Override
    public boolean isTreasure() {
        return true;
    }

    @Override
    public boolean isAvailableForRandomSelection() {
        return true;// this.protectionType != ProtectionEnchantment.Type.ALL;
    }

    @Override
    public boolean canAccept(Enchantment other) {
        if (other instanceof ProtectionEnchantment protectionEnchantment) {
            return (this.protectionType != protectionEnchantment.protectionType);
        } else {
            return super.canAccept(other);
        }
    }

    @Override
    public Rarity getRarity() {
        if(this.protectionType == ProtectionEnchantment.Type.ALL) return Rarity.VERY_RARE;
        return Rarity.RARE;
    }
}