package frootloops.versus.mixin.enchantments.tools;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.enchantment.PowerEnchantment;
import net.minecraft.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PowerEnchantment.class)
public class PowerMixin extends Enchantment {
    protected PowerMixin(Rarity weight, EnchantmentTarget type, EquipmentSlot[] slotTypes) {
        super(weight, type, slotTypes);
    }

    @Override
    public int getMinPower(int level) {
        return 1 + (level) * (8 + level);
    }

    @Override
    public Rarity getRarity() {
        return Rarity.VERY_RARE;
    }


    @Override
    public boolean isAvailableForEnchantedBookOffer() {
        return false;
    }
}