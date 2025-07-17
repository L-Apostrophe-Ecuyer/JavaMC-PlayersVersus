package frootloops.versus.mod.items.brewing;

import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.items.RegisteringCustomItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.util.Rarity;

import static frootloops.versus.mod.items.RegisteringCustomItems.getItemSettings;
import static net.minecraft.item.Item.BASE_ATTACK_DAMAGE_MODIFIER_ID;

public abstract class CustomBrewingItems {

    public static Item.Settings getBileSettings(String name) {
        return RegisteringCustomItems.getItemSettings(name).rarity(Rarity.UNCOMMON).component(DataComponentTypes.CONSUMABLE, ConcentrateItem.CONCENTRATE_COMPONENT).useItemPrefixedTranslationKey();
    }

    public static final Item FOUR_LEAF_CLOVER = new Item(getItemSettings("four_leaf_clover").attributeModifiers(AttributeModifiersComponent.builder().add(EntityAttributes.LUCK, new EntityAttributeModifier(BASE_ATTACK_DAMAGE_MODIFIER_ID, 1.0, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.OFFHAND).build()));
    //public static final Item THREE_LEAF_CLOVER = new Item(new Item.Settings());

    public static PotionItem BOTTLE_OF_ENDER;
    public static SplashPotionItem SPLASH_BOTTLE_OF_ENDER;
    public static LingeringPotionItem LINGERING_BOTTLE_OF_ENDER;

    public static final Item LIVING_FLAME = new Item(getItemSettings("living_flame"));
    public static final Item GLISTERING_BEETROOT = new Item(getItemSettings("glistering_beetroot").food(new FoodComponent.Builder().nutrition(4).saturationModifier(0.8f).build()));
    public static final Item GLISTERING_MELON_SLICE = new Item(getItemSettings("glistering_melon_slice").food(new FoodComponent.Builder().nutrition(3).saturationModifier(0.8f).build()));

    public static final ConcentrateItem CONCENTRATE_OF_DEATH = new ConcentrateItem(getBileSettings("concentrate_of_death"),StatusEffects.WITHER, 2, 80, CustomBlocks.DEATHLY_BILE);
    public static final ConcentrateItem CORRUPTED_WART_POWDER = new ConcentrateItem(getBileSettings("corrupted_wart_powder"),StatusEffects.HUNGER, 4, 120, CustomBlocks.CORRUPTED_BILE); //-> Will be replacing Fermented Spider Eye

    public static final ConcentrateItem CONCENTRATE_OF_HARM = new ConcentrateItem(getBileSettings("concentrate_of_harm"),StatusEffects.INSTANT_DAMAGE, CustomBlocks.HARMFUL_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_HEALTH = new ConcentrateItem(getBileSettings("concentrate_of_health"),StatusEffects.INSTANT_HEALTH, CustomBlocks.HEALTHY_BILE);

    public static final ConcentrateItem CONCENTRATE_OF_REGENERATION = new ConcentrateItem(getBileSettings("concentrate_of_regeneration"),StatusEffects.REGENERATION, CustomBlocks.REGENERATION_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_DECAY = new ConcentrateItem(getBileSettings("concentrate_of_decay"),StatusEffects.WITHER, 0, 160, CustomBlocks.WITHERING_BILE);

    public static final ConcentrateItem CONCENTRATE_OF_MINING_SPEED = new ConcentrateItem(getBileSettings("concentrate_of_mining_speed"),StatusEffects.HASTE, CustomBlocks.MINING_SPEED_BILE);            // New potion!
    public static final ConcentrateItem CONCENTRATE_OF_MINING_FATIGUE = new ConcentrateItem(getBileSettings("concentrate_of_mining_fatigue"),StatusEffects.MINING_FATIGUE, CustomBlocks.MINING_FATIGUE_BILE); // New potion!

    public static final ConcentrateItem CONCENTRATE_OF_TOUGHNESS = new ConcentrateItem(getBileSettings("concentrate_of_toughness"),StatusEffects.RESISTANCE, CustomBlocks.TOUGHNESS_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_VULNERABILITY = new ConcentrateItem(getBileSettings("concentrate_of_vulnerability"),CustomStatusEffects.VULNERABILITY, CustomBlocks.VULNERABILITY_BILE); // New potion!

    public static final ConcentrateItem CONCENTRATE_OF_VISION = new ConcentrateItem(getBileSettings("concentrate_of_vision"),StatusEffects.NIGHT_VISION, CustomBlocks.VISION_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_DARKNESS = new ConcentrateItem(getBileSettings("concentrate_of_darkness"),StatusEffects.DARKNESS, CustomBlocks.DARKNESS_BILE);         // New potion!

    public static final ConcentrateItem CONCENTRATE_OF_LEAPING = new ConcentrateItem(getBileSettings("concentrate_of_leaping"),StatusEffects.JUMP_BOOST, CustomBlocks.LEAPING_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_SLOW_FALL = new ConcentrateItem(getBileSettings("concentrate_of_slow_fall"),StatusEffects.SLOW_FALLING, CustomBlocks.SLOW_FALL_BILE);

    public static final ConcentrateItem CONCENTRATE_OF_SPEED = new ConcentrateItem(getBileSettings("concentrate_of_speed"),StatusEffects.SPEED, CustomBlocks.SPEED_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_SLOWNESS = new ConcentrateItem(getBileSettings("concentrate_of_slowness"),StatusEffects.SLOWNESS, CustomBlocks.SLOWNESS_BILE);

    public static final ConcentrateItem CONCENTRATE_OF_BREATH = new ConcentrateItem(getBileSettings("concentrate_of_breath"),StatusEffects.WATER_BREATHING, CustomBlocks.BREATH_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_BUOYANCY = new ConcentrateItem(getBileSettings("concentrate_of_buoyancy"),CustomStatusEffects.BUOYANCY, CustomBlocks.BUOYANCY_BILE);       // New potion & effect!

    public static final ConcentrateItem CONCENTRATE_OF_LARGENESS = new ConcentrateItem(getBileSettings("concentrate_of_largeness"),CustomStatusEffects.LARGENESS, CustomBlocks.LARGENESS_BILE);     // New potion & effect!
    public static final ConcentrateItem CONCENTRATE_OF_SMALLNESS = new ConcentrateItem(getBileSettings("concentrate_of_smallness"),CustomStatusEffects.SMALLNESS, CustomBlocks.SMALLNESS_BILE);     // New potion & effect!

    public static final ConcentrateItem CONCENTRATE_OF_INVISIBILITY = new ConcentrateItem(getBileSettings("concentrate_of_invisibility"),StatusEffects.INVISIBILITY, CustomBlocks.INVISIBILITY_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_GLOWING = new ConcentrateItem(getBileSettings("concentrate_of_glowing"),StatusEffects.GLOWING, CustomBlocks.GLOWING_BILE);

    public static final ConcentrateItem CONCENTRATE_OF_WEAKNESS = new ConcentrateItem(getBileSettings("concentrate_of_weakness"),StatusEffects.WEAKNESS, CustomBlocks.WEAKNESS_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_STRENGTH = new ConcentrateItem(getBileSettings("concentrate_of_strength"),StatusEffects.STRENGTH, CustomBlocks.STRENGTH_BILE); //-> Will be replacing Blaze Powder

    public static final ConcentrateItem CONCENTRATE_OF_WIND = new ConcentrateItem(getBileSettings("concentrate_of_wind"),StatusEffects.WIND_CHARGED, CustomBlocks.WIND_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_FIRE = new ConcentrateItem(getBileSettings("concentrate_of_fire"),StatusEffects.FIRE_RESISTANCE, CustomBlocks.FIRE_BILE); //-> Will be replacing Magma Cream

    public static final ConcentrateItem CONCENTRATE_OF_OOZE = new ConcentrateItem(getBileSettings("concentrate_of_ooze"),StatusEffects.OOZING, CustomBlocks.OOZE_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_INFESTATION = new ConcentrateItem(getBileSettings("concentrate_of_infestation"),StatusEffects.INFESTED, CustomBlocks.INFESTATION_BILE);

    public static final ConcentrateItem CONCENTRATE_OF_POISON = new ConcentrateItem(getBileSettings("concentrate_of_poison"),StatusEffects.POISON, 0, 200, CustomBlocks.POISON_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_WEAVING = new ConcentrateItem(getBileSettings("concentrate_of_weaving"),StatusEffects.WEAVING, CustomBlocks.WEAVING_BILE);

    public static final ConcentrateItem CONCENTRATE_OF_LUCK = new ConcentrateItem(getBileSettings("concentrate_of_luck"),StatusEffects.LUCK, CustomBlocks.LUCK_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_UNLUCK = new ConcentrateItem(getBileSettings("concentrate_of_unluck"),StatusEffects.UNLUCK, CustomBlocks.UNLUCK_BILE);

}