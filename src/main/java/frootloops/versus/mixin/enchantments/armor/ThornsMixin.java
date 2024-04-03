package frootloops.versus.mixin.enchantments.armor;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.ThornsEnchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ThornsEnchantment.class)
public class ThornsMixin extends Enchantment {

    public ThornsMixin(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        if (stack.getItem() instanceof ShieldItem) return true;
        return super.isAcceptableItem(stack);
    }
}