package frootloops.versus.mixin.items;

import frootloops.versus.mod.items.brewing.ConcentrateItem;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;


@Mixin(Items.class)
public class VanillaItemsMixin {

    //@Shadow public static final Item RECOVERY_COMPASS = Items.register("recovery_compass", new Item(new Item.Settings()));

    @Shadow public static final Item BLAZE_POWDER = Items.register("blaze_powder", new ConcentrateItem(StatusEffects.STRENGTH));

    @Shadow public static final Item MAGMA_CREAM = Items.register("magma_cream", new ConcentrateItem(StatusEffects.FIRE_RESISTANCE));

    @Shadow public static final Item GLISTERING_MELON_SLICE = Items.register("glistering_melon_slice", new ConcentrateItem(StatusEffects.INSTANT_HEALTH));

    @Shadow public static final Item FERMENTED_SPIDER_EYE = Items.register("fermented_spider_eye", new ConcentrateItem(StatusEffects.NAUSEA, 240, 2));

}
