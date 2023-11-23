package frootloops.versus.mixin.enchantments.tools;

import net.minecraft.enchantment.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LuckEnchantment.class)
public class LuckMixin extends Enchantment {
    protected LuckMixin(Rarity weight, EnchantmentTarget type, EquipmentSlot[] slotTypes) {
        super(weight, type, slotTypes);
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
        if (this.target == EnchantmentTarget.FISHING_ROD){
            return (stack.getItem() instanceof FishingRodItem);
        }
        else if (this.target == EnchantmentTarget.WEAPON){
            return (stack.getItem() instanceof AxeItem || stack.getItem() instanceof HoeItem || stack.getItem() instanceof TridentItem);
        }
        else {
            return (stack.getItem() instanceof ShovelItem || stack.getItem() instanceof HoeItem || stack.getItem() instanceof PickaxeItem);
        }
    }
}