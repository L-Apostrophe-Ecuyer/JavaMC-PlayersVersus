package frootloops.versus.mixin.enchantments;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.ProtectionEnchantment;
import net.minecraft.entity.EquipmentSlot;
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
    public boolean isAvailableForEnchantedBookOffer() {
        return false;
    }

    @Override
    public boolean isTreasure() {
        return this.protectionType == ProtectionEnchantment.Type.ALL;
    }

    @Override
    public Rarity getRarity() {
        if(this.protectionType == ProtectionEnchantment.Type.ALL) return Rarity.VERY_RARE;
        return Rarity.RARE;
    }
}