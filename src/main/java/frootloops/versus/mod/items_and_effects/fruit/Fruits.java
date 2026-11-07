package frootloops.versus.mod.items_and_effects.fruit;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.CustomBlockItems;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

import static frootloops.versus.mod.items_and_effects.RegisteringCustomItems.getItemSettings;

/** The fruit picked from fruiting leaves. Made once the effects exist (a lemon makes one Sour). */
public final class Fruits {

    /** As filling as sweet berries; eating one makes one Sour. Picked from lemon leaves, 2 to 6 at a time. */
    public static Item LEMON;

    private Fruits() {
    }

    public static void create() {
        LEMON = new Item(CustomBlockItems.compostable(getItemSettings("lemon"))
                .food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).build(),
                        Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(
                                new MobEffectInstance(CustomStatusEffects.SOUR, VersusSettings.Lemons.SOUR_TICKS, 0))).build()));
    }
}
