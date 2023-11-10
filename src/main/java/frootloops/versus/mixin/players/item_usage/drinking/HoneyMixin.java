package frootloops.versus.mixin.players.item_usage.drinking;

import net.minecraft.item.HoneyBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(HoneyBottleItem.class)
public class HoneyMixin extends Item {
    public HoneyMixin(Settings settings) {
        super(settings);
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 16;
    }
}

