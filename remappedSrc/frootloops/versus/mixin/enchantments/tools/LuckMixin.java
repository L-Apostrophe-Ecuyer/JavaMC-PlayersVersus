package frootloops.versus.mixin.enchantments.tools;

import Rarity;
import net.minecraft.enchantment.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LuckEnchantment.class)
public class LuckMixin extends Enchantment {

    protected LuckMixin(Rarity rarity, TagKey<Item> applicableItems, EquipmentSlot[] slotTypes) {
        super(rarity, applicableItems, slotTypes);
    }

    @Override
    public boolean isAvailableForEnchantedBookOffer() {
        return false;
    }

    @Override
    public boolean isTreasure() {
        return false;
    }

    @Override
    public Rarity getRarity() {
        return Rarity.RARE;
    }

    @Override
    public int getMinPower(int level) {
        return 6 + level * 16;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        TagKey<Item> applicableItems = this.getApplicableItems();
        if (applicableItems == ItemTags.SWORD_ENCHANTABLE){
            return (stack.getItem() instanceof AxeItem || stack.getItem() instanceof HoeItem || stack.getItem() instanceof TridentItem);
        }
        else {
            return super.isAcceptableItem(stack);
        }
    }
}