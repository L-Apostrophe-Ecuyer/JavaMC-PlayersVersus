package frootloops.versus.mod.items_and_effects.fruit;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.CustomBlockItems;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

import java.util.List;

import static frootloops.versus.mod.items_and_effects.RegisteringCustomItems.getItemSettings;

/** The mod's fruit. Made once the effects exist (a lemon makes one Sour). */
public final class Fruits {

    /**
     * A little food (1 hunger, no saturation, eaten in EAT_TICKS) and a quick Sour: eating one makes one Sour, slow and
     * hasty (VersusSettings.Lemons). Nothing grows them now (the lemon trees and bushes are gone).
     */
    public static Item LEMON;

    private Fruits() {
    }

    public static void create() {
        LEMON = new Item(CustomBlockItems.compostable(getItemSettings("lemon"))
                .food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.0F).build(),
                        Consumables.defaultFood().consumeSeconds(VersusSettings.Lemons.EAT_TICKS / 20.0F).onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                                new MobEffectInstance(CustomStatusEffects.SOUR, VersusSettings.Lemons.SOUR_TICKS, 0),
                                new MobEffectInstance(MobEffects.SLOWNESS, VersusSettings.Lemons.SLOWNESS_TICKS, 0),
                                new MobEffectInstance(MobEffects.HASTE, VersusSettings.Lemons.HASTE_TICKS, 0)))).build()));
    }
}
