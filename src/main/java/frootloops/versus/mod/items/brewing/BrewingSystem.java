package frootloops.versus.mod.items.brewing;


import frootloops.versus.VersusMod;
import frootloops.versus.mod.items.brewing.CustomBrewingItems;
import frootloops.versus.mod.items.brewing.CustomPotions;
import net.minecraft.item.*;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.HashMap;

public abstract class BrewingSystem {

    private record RelatedPotions(RegistryEntry<Potion> strongPotion, RegistryEntry<Potion> longPotion,  RegistryEntry<Potion> invertedPotion) {}
    public static void setBrewingRecipeRegistry(BrewingRecipeRegistry.Builder builder) {
        CustomPotions.registerCustomPotions();

        HashMap<RegistryEntry<Potion>, RelatedPotions> brewablePotionTypes = new HashMap<>() {{

            put(Potions.HEALING, new RelatedPotions(Potions.STRONG_HEALING, Potions.REGENERATION, Potions.HARMING));
            put(Potions.HARMING, new RelatedPotions(Potions.STRONG_HARMING, CustomPotions.DECAY,  Potions.HEALING));

            put(Potions.REGENERATION, new RelatedPotions(Potions.STRONG_REGENERATION, Potions.LONG_REGENERATION, CustomPotions.DECAY));
            put(CustomPotions.DECAY,  new RelatedPotions(CustomPotions.DECAY_STRONG, CustomPotions.DECAY_LONG, Potions.REGENERATION));

            put(CustomPotions.HASTE, new RelatedPotions(CustomPotions.HASTE_STRONG, CustomPotions.HASTE_LONG, CustomPotions.MINING_FATIGUE));
            put(CustomPotions.MINING_FATIGUE, new RelatedPotions(CustomPotions.MINING_FATIGUE_STRONG, CustomPotions.MINING_FATIGUE_LONG, CustomPotions.HASTE));

            put(Potions.TURTLE_MASTER, new RelatedPotions(Potions.STRONG_TURTLE_MASTER, Potions.LONG_TURTLE_MASTER, CustomPotions.VULNERABILITY));
            put(CustomPotions.VULNERABILITY, new RelatedPotions(CustomPotions.MINING_FATIGUE_STRONG, CustomPotions.VULNERABILITY_LONG, Potions.TURTLE_MASTER));

            put(CustomPotions.DARKNESS, new RelatedPotions(CustomPotions.DARKNESS_STRONG, CustomPotions.DARKNESS_LONG, CustomPotions.GLOWING));
            put(Potions.NIGHT_VISION,   new RelatedPotions(Potions.LONG_NIGHT_VISION, Potions.LONG_NIGHT_VISION, CustomPotions.DARKNESS));

            put(Potions.LEAPING, new RelatedPotions(Potions.STRONG_LEAPING, Potions.LONG_LEAPING, Potions.SLOW_FALLING));
            put(Potions.SLOW_FALLING, new RelatedPotions(Potions.LONG_SLOW_FALLING, Potions.LONG_SLOW_FALLING, Potions.LEAPING));

            put(Potions.SWIFTNESS, new RelatedPotions(Potions.STRONG_SWIFTNESS, Potions.LONG_SWIFTNESS, Potions.SLOWNESS));
            put(Potions.SLOWNESS,  new RelatedPotions(Potions.LONG_SLOWNESS, Potions.LONG_SLOWNESS, Potions.SWIFTNESS));

            put(Potions.WATER_BREATHING, new RelatedPotions(Potions.LONG_WATER_BREATHING, Potions.LONG_WATER_BREATHING, CustomPotions.BUOYANCY));
            put(CustomPotions.BUOYANCY,  new RelatedPotions(CustomPotions.BUOYANCY_STRONG, CustomPotions.BUOYANCY_LONG, Potions.WATER_BREATHING));

            put(CustomPotions.LARGENESS, new RelatedPotions(CustomPotions.LARGENESS_STRONG, CustomPotions.LARGENESS_LONG, CustomPotions.SMALLNESS));
            put(CustomPotions.SMALLNESS, new RelatedPotions(CustomPotions.SMALLNESS_STRONG, CustomPotions.SMALLNESS_LONG, CustomPotions.LARGENESS));

            put(Potions.INVISIBILITY,  new RelatedPotions(Potions.LONG_INVISIBILITY, Potions.LONG_INVISIBILITY, CustomPotions.GLOWING));
            put(CustomPotions.GLOWING, new RelatedPotions(CustomPotions.GLOWING_STRONG, CustomPotions.GLOWING_LONG, Potions.INVISIBILITY));

            put(Potions.STRENGTH, new RelatedPotions(Potions.STRONG_STRENGTH, Potions.LONG_STRENGTH, Potions.SLOWNESS));
            put(Potions.WEAKNESS,  new RelatedPotions(null, Potions.LONG_WEAKNESS, Potions.STRENGTH));

            put(Potions.FIRE_RESISTANCE,  new RelatedPotions(CustomPotions.FIRE_RESISTANCE_STRONG, Potions.LONG_FIRE_RESISTANCE, Potions.WIND_CHARGED));
            put(Potions.WIND_CHARGED,  new RelatedPotions(null, null, Potions.FIRE_RESISTANCE));

            put(Potions.OOZING,  new RelatedPotions(null, null, Potions.INFESTED));
            put(Potions.INFESTED,  new RelatedPotions(null, null, Potions.OOZING));

            put(Potions.POISON,  new RelatedPotions(Potions.STRONG_POISON, Potions.LONG_POISON, Potions.WEAVING));
            put(Potions.WEAVING,  new RelatedPotions(null,null, Potions.POISON));
        }};

        builder.registerPotionType(Items.POTION);
        builder.registerPotionType(Items.SPLASH_POTION);
        builder.registerPotionType(Items.LINGERING_POTION);

        builder.registerPotionRecipe(Potions.WATER, Items.GLOWSTONE_DUST, Potions.THICK);
        builder.registerPotionRecipe(Potions.WATER, Items.SUGAR, Potions.AWKWARD);

        builder.registerItemRecipe(Items.POTION, Items.GUNPOWDER, Items.SPLASH_POTION);
        builder.registerItemRecipe(Items.POTION, Items.REDSTONE, Items.LINGERING_POTION);
        builder.registerItemRecipe(Items.LINGERING_POTION, Items.GUNPOWDER, Items.SPLASH_POTION);
        builder.registerItemRecipe(Items.SPLASH_POTION, Items.REDSTONE, Items.LINGERING_POTION);
        VersusMod.MOD_LOGGER.warn("    -> Registered Recipes for Potions With No Effect");


        builder.registerItemRecipe(Items.POTION, CustomBrewingItems.CONCENTRATE_OF_DEATH, CustomBrewingItems.BOTTLE_OF_ENDER);
        builder.registerItemRecipe(CustomBrewingItems.BOTTLE_OF_ENDER, Items.GUNPOWDER, CustomBrewingItems.SPLASH_BOTTLE_OF_ENDER);
        builder.registerItemRecipe(CustomBrewingItems.BOTTLE_OF_ENDER, Items.REDSTONE, CustomBrewingItems.LINGERING_BOTTLE_OF_ENDER);
        builder.registerItemRecipe(CustomBrewingItems.LINGERING_BOTTLE_OF_ENDER, Items.GUNPOWDER, CustomBrewingItems.SPLASH_BOTTLE_OF_ENDER);
        builder.registerItemRecipe(CustomBrewingItems.SPLASH_BOTTLE_OF_ENDER, Items.REDSTONE, CustomBrewingItems.LINGERING_BOTTLE_OF_ENDER);

        registerConcentrateRecipe(builder, brewablePotionTypes, Items.GLISTERING_MELON_SLICE, Potions.HEALING);
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_HARM, Potions.HARMING);

        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_REGENERATION, Potions.REGENERATION);
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_DECAY, CustomPotions.DECAY);

        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_MINING_SPEED, CustomPotions.HASTE);
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_MINING_FATIGUE, CustomPotions.MINING_FATIGUE);

        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_TOUGHNESS, Potions.TURTLE_MASTER);
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_VULNERABILITY, CustomPotions.VULNERABILITY);

        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_VISION, Potions.NIGHT_VISION);
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_DARKNESS, CustomPotions.DARKNESS);

        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_LEAPING, Potions.LEAPING);
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_SLOW_FALL, Potions.SLOW_FALLING);

        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_SPEED, Potions.SWIFTNESS);
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_SLOWNESS, Potions.SLOWNESS);

        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_BREATH, Potions.WATER_BREATHING);
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_BUOYANCY, CustomPotions.BUOYANCY);

        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_LARGENESS, CustomPotions.LARGENESS);
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_SMALLNESS, CustomPotions.SMALLNESS);

        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_INVISIBILITY, Potions.INVISIBILITY);
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_GLOWING, CustomPotions.GLOWING);

        registerConcentrateRecipe(builder, brewablePotionTypes, Items.BLAZE_POWDER, Potions.STRENGTH);
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_WEAKNESS, Potions.WEAKNESS);

        registerConcentrateRecipe(builder, brewablePotionTypes, Items.MAGMA_CREAM, Potions.FIRE_RESISTANCE);
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_WIND, Potions.WIND_CHARGED);

        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_OOZE, Potions.OOZING);
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_INFESTATION, Potions.INFESTED);

        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_POISON, Potions.POISON);
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_WEAVING, Potions.WEAVING);

        brewablePotionTypes.clear();
    }

    private static void  registerConcentrateRecipe(BrewingRecipeRegistry.Builder builder, HashMap<RegistryEntry<Potion>, RelatedPotions> brewablePotionTypes, Item ingredient, RegistryEntry<Potion> potion) {
        builder.registerPotionRecipe(Potions.WATER, ingredient, potion);
        builder.registerPotionRecipe(potion, CustomBrewingItems.CONCENTRATE_OF_DECAY, CustomPotions.DECAY);
        if(brewablePotionTypes.containsKey(potion)) {

            RelatedPotions current = brewablePotionTypes.get(potion);
            boolean hasLongPotion = (current.longPotion != null && current.longPotion != potion);
            boolean hasStrongPotion = (current.strongPotion != null && current.strongPotion != potion);

            if (hasLongPotion) {
                builder.registerPotionRecipe(potion, Items.SUGAR, current.longPotion);
                builder.registerPotionRecipe(Potions.THICK, ingredient, current.longPotion);
                builder.registerPotionRecipe(potion, CustomBrewingItems.CONCENTRATE_OF_DECAY, CustomPotions.DECAY_LONG);
            }
            if (hasStrongPotion) {
                builder.registerPotionRecipe(potion, Items.GLOWSTONE_DUST, current.strongPotion);
                builder.registerPotionRecipe(Potions.AWKWARD, ingredient, current.strongPotion);
                builder.registerPotionRecipe(potion, CustomBrewingItems.CONCENTRATE_OF_DECAY, CustomPotions.DECAY_STRONG);
            }

            if (current.invertedPotion != null && current.invertedPotion != potion) {
                builder.registerPotionRecipe(potion, Items.FERMENTED_SPIDER_EYE, current.invertedPotion);

                if(Potions.WEAKNESS == potion) VersusMod.MOD_LOGGER.error("Registered inverted Potion");

                if(brewablePotionTypes.containsKey(current.invertedPotion)) {
                    RelatedPotions inverted = brewablePotionTypes.get(current.invertedPotion);
                    if (hasLongPotion) {
                        if(inverted.longPotion != null) builder.registerPotionRecipe(current.longPotion, Items.FERMENTED_SPIDER_EYE, inverted.longPotion);
                        else builder.registerPotionRecipe(current.longPotion, Items.FERMENTED_SPIDER_EYE, current.invertedPotion);
                    }
                    if (hasStrongPotion) {
                        if(inverted.strongPotion != null) builder.registerPotionRecipe(current.strongPotion, Items.FERMENTED_SPIDER_EYE, inverted.strongPotion);
                        else builder.registerPotionRecipe(current.strongPotion, Items.FERMENTED_SPIDER_EYE, current.invertedPotion);
                    }
                }
            }
        }
    }

}