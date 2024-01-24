package frootloops.versus.mixin.enchantments.tools;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.PowerEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PowerEnchantment.class)
public class PowerMixin extends Enchantment {

    protected PowerMixin(Rarity rarity, TagKey<Item> applicableItems, EquipmentSlot[] slotTypes) {
        super(rarity, applicableItems, slotTypes);
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