package frootloops.versus.mixin.enchantments.tools;

import Rarity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.SweepingEnchantment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SweepingEnchantment.class)
public class SweepingMixin extends Enchantment {
    protected SweepingMixin(Rarity rarity, TagKey<Item> applicableItems, EquipmentSlot[] slotTypes) {
        super(rarity, applicableItems, slotTypes);
    }

    @Override
    public int getMinPower(int level) {
        return 5 + level * 9;
    }

    @Override
    public Rarity getRarity() {
        return Rarity.RARE;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return ((stack.getItem() instanceof SwordItem) || (stack.getItem() instanceof HoeItem));
    }
}