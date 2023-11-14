package frootloops.versus.mixin.enchantments;

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
        return 6 + level * 12;
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        if (this.target == EnchantmentTarget.WEAPON && (stack.getItem() instanceof AxeItem || stack.getItem() instanceof ShovelItem || stack.getItem() instanceof HoeItem || stack.getItem() instanceof TridentItem)){
            return true;
        }
        return this.target.isAcceptableItem(stack.getItem());
    }
}