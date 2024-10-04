package frootloops.versus.mixin.items;

import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.items.brewing.ConcentrateItem;
import frootloops.versus.mod.items.equipment.RecoveryCompassItem;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.FoodComponents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;


@Mixin(Items.class)
public class VanillaItemsMixin {

    @Shadow public static final Item RECOVERY_COMPASS = Items.register("recovery_compass", new RecoveryCompassItem(new Item.Settings()));

    @Shadow public static final Item GLISTERING_MELON_SLICE = Items.register("glistering_melon_slice", new Item(new Item.Settings().food(new FoodComponent.Builder().nutrition(3).saturationModifier(0.8f).build())));

    @Shadow public static final Item BLAZE_POWDER = Items.register("blaze_powder", new ConcentrateItem(StatusEffects.STRENGTH, CustomBlocks.STRENGTH_BILE));

    @Shadow public static final Item MAGMA_CREAM = Items.register("magma_cream", new ConcentrateItem(StatusEffects.FIRE_RESISTANCE, CustomBlocks.FIRE_BILE));

    @Shadow public static final Item FERMENTED_SPIDER_EYE = Items.register("fermented_spider_eye", new ConcentrateItem(StatusEffects.HUNGER, 2, 120, CustomBlocks.CORRUPTED_BILE));

}
