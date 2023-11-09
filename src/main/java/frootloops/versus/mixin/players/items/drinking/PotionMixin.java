package frootloops.versus.mixin.players.items.drinking;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.potion.PotionUtil;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(PotionItem.class)
public class PotionMixin extends Item {
    public PotionMixin(Settings settings) {
        super(settings);
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        List<StatusEffectInstance> list = PotionUtil.getPotionEffects(stack);
        for (StatusEffectInstance statusEffectInstance : list)
            if (statusEffectInstance.getAmplifier() > 0) return 32;

        return 24;
    }
}

