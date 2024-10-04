package frootloops.versus.mod.items.brewing;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.block.Blocks;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;

import static net.minecraft.item.Item.BASE_ATTACK_DAMAGE_MODIFIER_ID;

public abstract class CustomBrewingItems {

    public static final Item FOUR_LEAF_CLOVER = new Item(new Item.Settings().attributeModifiers(AttributeModifiersComponent.builder().add(EntityAttributes.LUCK, new EntityAttributeModifier(BASE_ATTACK_DAMAGE_MODIFIER_ID, 1.0, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.OFFHAND).build()));
    //public static final Item THREE_LEAF_CLOVER = new Item(new Item.Settings());

    public static PotionItem BOTTLE_OF_ENDER;
    public static SplashPotionItem SPLASH_BOTTLE_OF_ENDER;
    public static LingeringPotionItem LINGERING_BOTTLE_OF_ENDER;

    public static final Item LIVING_FLAME = new Item(new Item.Settings());
    public static final Item GLISTERING_BEETROOT = new Item(new Item.Settings().food(new FoodComponent.Builder().nutrition(4).saturationModifier(0.8f).build()));
    public static final Item GLISTERING_MELON_SLICE = new Item(new Item.Settings().food(new FoodComponent.Builder().nutrition(3).saturationModifier(0.8f).build()));

    public static final Item CONCENTRATE_OF_DEATH = new ConcentrateItem(StatusEffects.WITHER, 2, 80, CustomBlocks.DEATHLY_BILE);
    public static final Item CORRUPTED_WART_POWDER = new ConcentrateItem(StatusEffects.HUNGER, 4, 120, CustomBlocks.CORRUPTED_BILE); //-> Will be replacing Fermented Spider Eye

    public static final Item CONCENTRATE_OF_HARM = new ConcentrateItem(StatusEffects.INSTANT_DAMAGE, CustomBlocks.HARMFUL_BILE);
    public static final Item CONCENTRATE_OF_HEALTH = new ConcentrateItem(StatusEffects.INSTANT_HEALTH, CustomBlocks.HEALTHY_BILE);

    public static final Item CONCENTRATE_OF_REGENERATION = new ConcentrateItem(StatusEffects.REGENERATION, CustomBlocks.REGENERATION_BILE);
    public static final Item CONCENTRATE_OF_DECAY = new ConcentrateItem(StatusEffects.WITHER, 0, 160, CustomBlocks.WITHERING_BILE);

    public static final Item CONCENTRATE_OF_MINING_SPEED = new ConcentrateItem(StatusEffects.HASTE, CustomBlocks.MINING_SPEED_BILE);            // New potion!
    public static final Item CONCENTRATE_OF_MINING_FATIGUE = new ConcentrateItem(StatusEffects.MINING_FATIGUE, CustomBlocks.MINING_FATIGUE_BILE); // New potion!

    public static final Item CONCENTRATE_OF_TOUGHNESS = new ConcentrateItem(StatusEffects.RESISTANCE, CustomBlocks.TOUGHNESS_BILE);
    public static final Item CONCENTRATE_OF_VULNERABILITY = new ConcentrateItem(CustomStatusEffects.VULNERABILITY, CustomBlocks.VULNERABILITY_BILE); // New potion!

    public static final Item CONCENTRATE_OF_VISION = new ConcentrateItem(StatusEffects.NIGHT_VISION, CustomBlocks.VISION_BILE);
    public static final Item CONCENTRATE_OF_DARKNESS = new ConcentrateItem(StatusEffects.DARKNESS, CustomBlocks.DARKNESS_BILE);         // New potion!

    public static final Item CONCENTRATE_OF_LEAPING = new ConcentrateItem(StatusEffects.JUMP_BOOST, CustomBlocks.LEAPING_BILE);
    public static final Item CONCENTRATE_OF_SLOW_FALL = new ConcentrateItem(StatusEffects.SLOW_FALLING, CustomBlocks.SLOW_FALL_BILE);

    public static final Item CONCENTRATE_OF_SPEED = new ConcentrateItem(StatusEffects.SPEED, CustomBlocks.SPEED_BILE);
    public static final Item CONCENTRATE_OF_SLOWNESS = new ConcentrateItem(StatusEffects.SLOWNESS, CustomBlocks.SLOWNESS_BILE);

    public static final Item CONCENTRATE_OF_BREATH = new ConcentrateItem(StatusEffects.WATER_BREATHING, CustomBlocks.BREATH_BILE);
    public static final Item CONCENTRATE_OF_BUOYANCY = new ConcentrateItem(CustomStatusEffects.BUOYANCY, CustomBlocks.BUOYANCY_BILE);       // New potion & effect!

    public static final Item CONCENTRATE_OF_LARGENESS = new ConcentrateItem(CustomStatusEffects.LARGENESS, CustomBlocks.LARGENESS_BILE);     // New potion & effect!
    public static final Item CONCENTRATE_OF_SMALLNESS = new ConcentrateItem(CustomStatusEffects.SMALLNESS, CustomBlocks.SMALLNESS_BILE);     // New potion & effect!

    public static final Item CONCENTRATE_OF_INVISIBILITY = new ConcentrateItem(StatusEffects.INVISIBILITY, CustomBlocks.INVISIBILITY_BILE);
    public static final Item CONCENTRATE_OF_GLOWING = new ConcentrateItem(StatusEffects.GLOWING, CustomBlocks.GLOWING_BILE);

    public static final Item CONCENTRATE_OF_WEAKNESS = new ConcentrateItem(StatusEffects.WEAKNESS, CustomBlocks.WEAKNESS_BILE);
    public static final Item CONCENTRATE_OF_STRENGTH = new ConcentrateItem(StatusEffects.STRENGTH, CustomBlocks.STRENGTH_BILE); //-> Will be replacing Blaze Powder

    public static final Item CONCENTRATE_OF_WIND = new ConcentrateItem(StatusEffects.WIND_CHARGED, CustomBlocks.WIND_BILE);
    public static final Item CONCENTRATE_OF_FIRE = new ConcentrateItem(StatusEffects.FIRE_RESISTANCE, CustomBlocks.FIRE_BILE); //-> Will be replacing Magma Cream

    public static final Item CONCENTRATE_OF_OOZE = new ConcentrateItem(StatusEffects.OOZING, CustomBlocks.OOZE_BILE);
    public static final Item CONCENTRATE_OF_INFESTATION = new ConcentrateItem(StatusEffects.INFESTED, CustomBlocks.INFESTATION_BILE);

    public static final Item CONCENTRATE_OF_POISON = new ConcentrateItem(StatusEffects.POISON, 0, 200, CustomBlocks.POISON_BILE);
    public static final Item CONCENTRATE_OF_WEAVING = new ConcentrateItem(StatusEffects.WEAVING, CustomBlocks.WEAVING_BILE);

    public static final Item CONCENTRATE_OF_LUCK = new ConcentrateItem(StatusEffects.LUCK, CustomBlocks.LUCK_BILE);
    public static final Item CONCENTRATE_OF_UNLUCK = new ConcentrateItem(StatusEffects.UNLUCK, CustomBlocks.UNLUCK_BILE);

}