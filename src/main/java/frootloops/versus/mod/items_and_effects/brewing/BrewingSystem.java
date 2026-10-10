package frootloops.versus.mod.items_and_effects.brewing;


import frootloops.versus.mod.items_and_effects.CustomBrewingItems;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * The brewing recipe graph of Players Versus: which ingredient turns which potion into which (strong, long, inverted and
 * decayed variants, one concentrate per effect), and the vanilla container recipes.
 *
 * <p>Since 26.3 brewing is data (recipes of type {@code minecraft:brewing}) instead of a registry that mods add mixes
 * to, so this class no longer registers anything by itself: it describes the graph to a {@link Recipes} sink, which the
 * data step turns into recipes. The potions themselves are registered by {@link CustomPotions#registerCustomPotions}.
 */
public abstract class BrewingSystem {

    /** What {@link #describeRecipes} tells about the graph: the mixes and container recipes, in vanilla's terms. */
    public interface Recipes {
        void addContainer(Item container);
        void addMix(Holder<Potion> from, Item ingredient, Holder<Potion> to);
        void addContainerRecipe(Item from, Item ingredient, Item to);
    }

    private record RelatedPotions(Holder<Potion> strongPotion, Holder<Potion> longPotion,  Holder<Potion> invertedPotion) {}

    private static volatile Set<Item> ingredients;

    /** Whether an item is the ingredient of some mix or container recipe of the graph (for sorting and tooltips). */
    public static boolean isIngredient(Item item) {
        Set<Item> known = ingredients;
        if (known == null) {
            Set<Item> found = new HashSet<>();
            describeRecipes(new Recipes() {
                @Override
                public void addContainer(Item container) {
                }

                @Override
                public void addMix(Holder<Potion> from, Item ingredient, Holder<Potion> to) {
                    found.add(ingredient);
                }

                @Override
                public void addContainerRecipe(Item from, Item ingredient, Item to) {
                    found.add(ingredient);
                }
            });
            ingredients = known = Set.copyOf(found);
        }
        return known.contains(item);
    }

    public static void describeRecipes(Recipes builder) {

        // Generate recipes:
        HashMap<Holder<Potion>, RelatedPotions> brewablePotionTypes = new HashMap<>() {{

            put(Potions.HEALING, new RelatedPotions(Potions.STRONG_HEALING, Potions.REGENERATION, Potions.HARMING));
            put(Potions.HARMING, new RelatedPotions(Potions.STRONG_HARMING, CustomPotions.DECAY,  Potions.HEALING));

            put(Potions.REGENERATION, new RelatedPotions(Potions.STRONG_REGENERATION, Potions.LONG_REGENERATION, CustomPotions.DECAY));
            put(CustomPotions.DECAY,  new RelatedPotions(CustomPotions.DECAY_STRONG, CustomPotions.DECAY_LONG, Potions.REGENERATION));

            put(CustomPotions.HASTE, new RelatedPotions(CustomPotions.HASTE_STRONG, CustomPotions.HASTE_LONG, CustomPotions.MINING_FATIGUE));
            put(CustomPotions.MINING_FATIGUE, new RelatedPotions(CustomPotions.MINING_FATIGUE_STRONG, CustomPotions.MINING_FATIGUE_LONG, CustomPotions.HASTE));

            put(Potions.TURTLE_MASTER, new RelatedPotions(Potions.STRONG_TURTLE_MASTER, Potions.LONG_TURTLE_MASTER, CustomPotions.VULNERABILITY));
            put(CustomPotions.VULNERABILITY, new RelatedPotions(CustomPotions.MINING_FATIGUE_STRONG, CustomPotions.VULNERABILITY_LONG, Potions.TURTLE_MASTER));

            put(CustomPotions.DARKNESS, new RelatedPotions(CustomPotions.DARKNESS_STRONG, CustomPotions.DARKNESS_LONG, CustomPotions.GLOWING));
            put(Potions.NIGHT_VISION,   new RelatedPotions(CustomPotions.NIGHT_VISION_STRONG, Potions.LONG_NIGHT_VISION, CustomPotions.DARKNESS));

            put(Potions.LEAPING, new RelatedPotions(Potions.STRONG_LEAPING, Potions.LONG_LEAPING, Potions.SLOW_FALLING));
            put(Potions.SLOW_FALLING, new RelatedPotions(Potions.LONG_SLOW_FALLING, Potions.LONG_SLOW_FALLING, Potions.LEAPING));
            put(CustomPotions.LEVITATION, new RelatedPotions(CustomPotions.LEVITATION_STRONG, CustomPotions.LEVITATION_LONG, Potions.SLOW_FALLING));

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

            put(Potions.FIRE_RESISTANCE,  new RelatedPotions(CustomPotions.FIRE_RESISTANCE_STRONG, Potions.LONG_FIRE_RESISTANCE, Potions.FREEZING));
            put(Potions.FREEZING,  new RelatedPotions(null, Potions.LONG_FREEZING, Potions.FIRE_RESISTANCE));
            put(Potions.WIND_CHARGED,  new RelatedPotions(null, null, Potions.FIRE_RESISTANCE));

            put(Potions.OOZING,  new RelatedPotions(null, null, Potions.INFESTED));
            put(Potions.INFESTED,  new RelatedPotions(null, null, Potions.OOZING));

            put(Potions.POISON,  new RelatedPotions(Potions.STRONG_POISON, Potions.LONG_POISON, Potions.WEAVING));
            put(Potions.WEAVING,  new RelatedPotions(null,null, Potions.POISON));

            put(Potions.LUCK, new RelatedPotions(Potions.LUCK, Potions.LUCK,  CustomPotions.UNLUCK));
            put(CustomPotions.UNLUCK,  new RelatedPotions(CustomPotions.UNLUCK, CustomPotions.UNLUCK, Potions.LUCK));
        }};

        builder.addContainer(Items.POTION);
        builder.addContainer(Items.SPLASH_POTION);
        builder.addContainer(Items.LINGERING_POTION);

        builder.addMix(Potions.WATER, Items.GLOWSTONE_DUST, Potions.THICK);
        builder.addMix(Potions.WATER, Items.SUGAR, Potions.AWKWARD);

        builder.addContainerRecipe(Items.POTION, Items.GUNPOWDER, Items.SPLASH_POTION);
        builder.addContainerRecipe(Items.POTION, Items.REDSTONE, Items.LINGERING_POTION);
        builder.addContainerRecipe(Items.LINGERING_POTION, Items.GUNPOWDER, Items.SPLASH_POTION);
        builder.addContainerRecipe(Items.SPLASH_POTION, Items.REDSTONE, Items.LINGERING_POTION);

        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_DEATH, CustomPotions.HAUNTING);

        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_HEALTH, Potions.HEALING);
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
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_LEVITATION, CustomPotions.LEVITATION);

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
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_FREEZING, Potions.FREEZING);
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_WIND, Potions.WIND_CHARGED);

        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_OOZE, Potions.OOZING);
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_INFESTATION, Potions.INFESTED);

        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_POISON, Potions.POISON);
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_WEAVING, Potions.WEAVING);

        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_LUCK, Potions.LUCK);
        registerConcentrateRecipe(builder, brewablePotionTypes, CustomBrewingItems.CONCENTRATE_OF_UNLUCK, CustomPotions.UNLUCK);

        brewablePotionTypes.clear();
    }

    private static void  registerConcentrateRecipe(Recipes builder, HashMap<Holder<Potion>, RelatedPotions> brewablePotionTypes, Item ingredient, Holder<Potion> potion) {
        builder.addMix(Potions.WATER, ingredient, potion);
        builder.addMix(potion, CustomBrewingItems.CONCENTRATE_OF_DECAY, CustomPotions.DECAY);
        if(brewablePotionTypes.containsKey(potion)) {

            RelatedPotions current = brewablePotionTypes.get(potion);
            boolean hasLongPotion = (current.longPotion != null && current.longPotion != potion);
            boolean hasStrongPotion = (current.strongPotion != null && current.strongPotion != potion);

            if (hasLongPotion) {
                builder.addMix(potion, Items.SUGAR, current.longPotion);
                builder.addMix(Potions.THICK, ingredient, current.longPotion);
                builder.addMix(current.longPotion, CustomBrewingItems.CONCENTRATE_OF_DECAY, CustomPotions.DECAY_LONG);
            }
            if (hasStrongPotion) {
                builder.addMix(potion, Items.GLOWSTONE_DUST, current.strongPotion);
                builder.addMix(Potions.AWKWARD, ingredient, current.strongPotion);
                builder.addMix(current.strongPotion, CustomBrewingItems.CONCENTRATE_OF_DECAY, CustomPotions.DECAY_STRONG);
            }

            if (current.invertedPotion != null && current.invertedPotion != potion) {
                builder.addMix(potion, Items.FERMENTED_SPIDER_EYE, current.invertedPotion);
                if(brewablePotionTypes.containsKey(current.invertedPotion)) {
                    RelatedPotions inverted = brewablePotionTypes.get(current.invertedPotion);
                    if (hasLongPotion) {
                        if(inverted.longPotion != null) builder.addMix(current.longPotion, Items.FERMENTED_SPIDER_EYE, inverted.longPotion);
                        else builder.addMix(current.longPotion, Items.FERMENTED_SPIDER_EYE, current.invertedPotion);
                    }
                    if (hasStrongPotion) {
                        if(inverted.strongPotion != null) builder.addMix(current.strongPotion, Items.FERMENTED_SPIDER_EYE, inverted.strongPotion);
                        else builder.addMix(current.strongPotion, Items.FERMENTED_SPIDER_EYE, current.invertedPotion);
                    }
                }
            }
        }
    }

}