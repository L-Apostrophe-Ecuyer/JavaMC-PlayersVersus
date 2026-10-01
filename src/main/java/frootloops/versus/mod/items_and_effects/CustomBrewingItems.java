package frootloops.versus.mod.items_and_effects;

import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.items_and_effects.brewing.ConcentrateItem;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import static frootloops.versus.mod.items_and_effects.RegisteringCustomItems.getItemSettings;
import static net.minecraft.world.item.Item.BASE_ATTACK_DAMAGE_ID;

public abstract class CustomBrewingItems {

    public static Item.Properties getBileSettings(String name) {
        return RegisteringCustomItems.getItemSettings(name).rarity(Rarity.UNCOMMON).component(DataComponents.CONSUMABLE, ConcentrateItem.CONCENTRATE_COMPONENT).useItemDescriptionPrefix();
    }

    public static final Item FOUR_LEAF_CLOVER = new Item(getItemSettings("four_leaf_clover").attributes(ItemAttributeModifiers.builder().add(Attributes.LUCK, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.OFFHAND).build()));

    public static final Item LIVING_FLAME = new Item(getItemSettings("living_flame").rarity(Rarity.UNCOMMON));
    public static final Item GLISTERING_BEETROOT = new Item(getItemSettings("glistering_beetroot").food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.8f).build()));

    public static final ConcentrateItem CONCENTRATE_OF_DEATH = new ConcentrateItem(getBileSettings("concentrate_of_death"),MobEffects.WITHER, 2, 80, CustomBlocks.DEATHLY_BILE);
    public static final ConcentrateItem CORRUPTED_WART_POWDER = new ConcentrateItem(getBileSettings("corrupted_wart_powder"),MobEffects.HUNGER, 4, 120, CustomBlocks.CORRUPTED_BILE); //-> Will be replacing Fermented Spider Eye

    public static final ConcentrateItem CONCENTRATE_OF_HARM = new ConcentrateItem(getBileSettings("concentrate_of_harm"),MobEffects.INSTANT_DAMAGE, CustomBlocks.HARMFUL_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_HEALTH = new ConcentrateItem(getBileSettings("concentrate_of_health"),MobEffects.INSTANT_HEALTH, CustomBlocks.HEALTHY_BILE);

    public static final ConcentrateItem CONCENTRATE_OF_REGENERATION = new ConcentrateItem(getBileSettings("concentrate_of_regeneration"),MobEffects.REGENERATION, CustomBlocks.REGENERATION_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_DECAY = new ConcentrateItem(getBileSettings("concentrate_of_decay"),MobEffects.WITHER, 0, 160, CustomBlocks.WITHERING_BILE);

    public static final ConcentrateItem CONCENTRATE_OF_MINING_SPEED = new ConcentrateItem(getBileSettings("concentrate_of_mining_speed"),MobEffects.HASTE, CustomBlocks.MINING_SPEED_BILE);            // New potion!
    public static final ConcentrateItem CONCENTRATE_OF_MINING_FATIGUE = new ConcentrateItem(getBileSettings("concentrate_of_mining_fatigue"),MobEffects.MINING_FATIGUE, CustomBlocks.MINING_FATIGUE_BILE); // New potion!

    public static final ConcentrateItem CONCENTRATE_OF_TOUGHNESS = new ConcentrateItem(getBileSettings("concentrate_of_toughness"),MobEffects.RESISTANCE, CustomBlocks.TOUGHNESS_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_VULNERABILITY = new ConcentrateItem(getBileSettings("concentrate_of_vulnerability"), CustomStatusEffects.VULNERABILITY, CustomBlocks.VULNERABILITY_BILE); // New potion!

    public static final ConcentrateItem CONCENTRATE_OF_VISION = new ConcentrateItem(getBileSettings("concentrate_of_vision"),MobEffects.NIGHT_VISION, CustomBlocks.VISION_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_DARKNESS = new ConcentrateItem(getBileSettings("concentrate_of_darkness"),MobEffects.DARKNESS, CustomBlocks.DARKNESS_BILE);         // New potion!

    public static final ConcentrateItem CONCENTRATE_OF_LEAPING = new ConcentrateItem(getBileSettings("concentrate_of_leaping"),MobEffects.JUMP_BOOST, CustomBlocks.LEAPING_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_SLOW_FALL = new ConcentrateItem(getBileSettings("concentrate_of_slow_fall"),MobEffects.SLOW_FALLING, CustomBlocks.SLOW_FALL_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_LEVITATION = new ConcentrateItem(getBileSettings("concentrate_of_levitation"),MobEffects.SLOW_FALLING, CustomBlocks.LEVITATION_BILE);

    public static final ConcentrateItem CONCENTRATE_OF_SPEED = new ConcentrateItem(getBileSettings("concentrate_of_speed"),MobEffects.SPEED, CustomBlocks.SPEED_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_SLOWNESS = new ConcentrateItem(getBileSettings("concentrate_of_slowness"),MobEffects.SLOWNESS, CustomBlocks.SLOWNESS_BILE);

    public static final ConcentrateItem CONCENTRATE_OF_BREATH = new ConcentrateItem(getBileSettings("concentrate_of_breath"),MobEffects.WATER_BREATHING, CustomBlocks.BREATH_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_BUOYANCY = new ConcentrateItem(getBileSettings("concentrate_of_buoyancy"),CustomStatusEffects.BUOYANCY, CustomBlocks.BUOYANCY_BILE);       // New potion & effect!

    public static final ConcentrateItem CONCENTRATE_OF_LARGENESS = new ConcentrateItem(getBileSettings("concentrate_of_largeness"),CustomStatusEffects.LARGENESS, CustomBlocks.LARGENESS_BILE);     // New potion & effect!
    public static final ConcentrateItem CONCENTRATE_OF_SMALLNESS = new ConcentrateItem(getBileSettings("concentrate_of_smallness"),CustomStatusEffects.SMALLNESS, CustomBlocks.SMALLNESS_BILE);     // New potion & effect!

    public static final ConcentrateItem CONCENTRATE_OF_INVISIBILITY = new ConcentrateItem(getBileSettings("concentrate_of_invisibility"),MobEffects.INVISIBILITY, CustomBlocks.INVISIBILITY_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_GLOWING = new ConcentrateItem(getBileSettings("concentrate_of_glowing"),MobEffects.GLOWING, CustomBlocks.GLOWING_BILE);

    public static final ConcentrateItem CONCENTRATE_OF_WEAKNESS = new ConcentrateItem(getBileSettings("concentrate_of_weakness"),MobEffects.WEAKNESS, CustomBlocks.WEAKNESS_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_STRENGTH = new ConcentrateItem(getBileSettings("concentrate_of_strength"),MobEffects.STRENGTH, CustomBlocks.STRENGTH_BILE); //-> Will be replacing Blaze Powder

    public static final ConcentrateItem CONCENTRATE_OF_WIND = new ConcentrateItem(getBileSettings("concentrate_of_wind"),MobEffects.WIND_CHARGED, CustomBlocks.WIND_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_FIRE = new ConcentrateItem(getBileSettings("concentrate_of_fire"),MobEffects.FIRE_RESISTANCE, CustomBlocks.FIRE_BILE); //-> Will be replacing Magma Cream

    public static final ConcentrateItem CONCENTRATE_OF_OOZE = new ConcentrateItem(getBileSettings("concentrate_of_ooze"),MobEffects.OOZING, CustomBlocks.OOZE_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_INFESTATION = new ConcentrateItem(getBileSettings("concentrate_of_infestation"),MobEffects.INFESTED, CustomBlocks.INFESTATION_BILE);

    public static final ConcentrateItem CONCENTRATE_OF_POISON = new ConcentrateItem(getBileSettings("concentrate_of_poison"),MobEffects.POISON, 0, 200, CustomBlocks.POISON_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_WEAVING = new ConcentrateItem(getBileSettings("concentrate_of_weaving"),MobEffects.WEAVING, CustomBlocks.WEAVING_BILE);

    public static final ConcentrateItem CONCENTRATE_OF_LUCK = new ConcentrateItem(getBileSettings("concentrate_of_luck"),MobEffects.LUCK, CustomBlocks.LUCK_BILE);
    public static final ConcentrateItem CONCENTRATE_OF_UNLUCK = new ConcentrateItem(getBileSettings("concentrate_of_unluck"),MobEffects.UNLUCK, CustomBlocks.UNLUCK_BILE);

}