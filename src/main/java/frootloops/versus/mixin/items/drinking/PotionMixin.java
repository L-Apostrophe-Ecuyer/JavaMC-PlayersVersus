package frootloops.versus.mixin.items.drinking;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
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
        PotionContentsComponent potionContents = stack.getComponents().get(DataComponentTypes.POTION_CONTENTS);
        if(potionContents == null) return 32;

        for (StatusEffectInstance statusEffectInstance : potionContents.getEffects())
            if (statusEffectInstance.getAmplifier() > 0) return 40;

        return 32;
    }
}

