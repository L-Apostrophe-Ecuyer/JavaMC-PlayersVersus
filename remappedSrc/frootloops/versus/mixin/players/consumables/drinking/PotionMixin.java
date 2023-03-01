package frootloops.versus.mixin.players.consumables.drinking;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PotionItem.class)
public class PotionMixin extends Item {
    public PotionMixin(Settings settings) {
        super(settings);
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 24;
    }
}

