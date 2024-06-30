package frootloops.versus.mod.items;

import frootloops.versus.mod.items.brewing.ConcentrateItem;
import frootloops.versus.mod.items.brewing.CustomBrewingItems;
import frootloops.versus.mod.items.brewing.CustomPotions;
import frootloops.versus.mod.items.brewing.CustomStatusEffects;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;

import java.util.HashMap;
import java.util.Map;

public abstract class ItemsAndStacks {

    private static Map<Item, Integer> DEFAULT_MAX_STACK_SIZE = new HashMap<>();

    private static Map<Item, Item> TRANSFORM_VANILLA_ITEMS_TO_MODDED = new HashMap<>();

    public static int MAX_POTION_STACK_SIZE = 8;


    public static void onInitialize() {
        // Order is important here:
        setUpTransformVanillaItemsToModded();
        CustomStatusEffects.registerCustomStatusEffects();
        CustomPotions.registerCustomPotions();
        RegisteringCustomItems.registerAllCustomItems();

        setStackSizes(16, 8, MAX_POTION_STACK_SIZE, 8, 64, 16, 64);
    }

    private static void setUpTransformVanillaItemsToModded() {
        //TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.GLISTERING_MELON_SLICE, CustomBrewingItems.CONCENTRATE_OF_HEALTH);
        //TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.MAGMA_CREAM, CustomBrewingItems.CONCENTRATE_OF_FIRE);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.DIORITE, Items.CALCITE);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.CHARCOAL, Items.COAL);
    }

    private static void setStackSizes(final int maxFoods, final int maxMeals, final int maxBottled, final int maxStews, final int maxThrowables, final int maxPlaceableEntities, final int maxPlaceableBlocks) {
        for (Item item : Registries.ITEM) {
            if (item.getComponents().contains(DataComponentTypes.FOOD)) {
                if (item instanceof BlockItem) setDefaultMaxStackSize(item, maxPlaceableBlocks);
                else if (item instanceof ConcentrateItem) setDefaultMaxStackSize(item, 64);
                else if(item.getComponents().get(DataComponentTypes.FOOD).eatSeconds() ==  0.8f) setDefaultMaxStackSize(item, maxFoods);
                else if (item.getTranslationKey().contains("cooked_") || item.getTranslationKey().contains("raw_")) setDefaultMaxStackSize(item, maxMeals);
                else if (item.getTranslationKey().contains("stew")) setDefaultMaxStackSize(item, maxStews);
                else if (item.getTranslationKey().contains("soup")) setDefaultMaxStackSize(item, maxStews);
                else setDefaultMaxStackSize(item, maxFoods);

            }
            else if (item instanceof BoatItem || item instanceof MinecartItem || item instanceof ArmorStandItem || item instanceof EndCrystalItem)
                setDefaultMaxStackSize(item, maxPlaceableEntities);

            else if (item instanceof BlockItem)
                setDefaultMaxStackSize(item, maxPlaceableBlocks);
        }

        // Other foods:
        setDefaultMaxStackSize(Items.CAKE, maxMeals);
        setDefaultMaxStackSize(Items.BREAD, maxFoods);
        setDefaultMaxStackSize(Items.PUMPKIN_PIE, maxMeals);
        setDefaultMaxStackSize(Items.SALMON, maxMeals);
        setDefaultMaxStackSize(Items.COOKED_SALMON, maxMeals);
        setDefaultMaxStackSize(Items.COD, maxMeals);
        setDefaultMaxStackSize(Items.COOKED_COD, maxMeals);
        setDefaultMaxStackSize(Items.TROPICAL_FISH, Math.max(maxFoods, maxMeals));
        setDefaultMaxStackSize(Items.ROTTEN_FLESH, Math.max(maxFoods, maxMeals));
        setDefaultMaxStackSize(Items.MELON_SLICE, Math.max(maxFoods, maxPlaceableBlocks));

        // Bottles:
        setDefaultMaxStackSize(Items.POTION, maxBottled);
        setDefaultMaxStackSize(Items.HONEY_BOTTLE, Math.max(maxFoods, maxBottled));

        // Throwables:
        setDefaultMaxStackSize(Items.EGG, maxThrowables);
        setDefaultMaxStackSize(Items.SNOWBALL, maxThrowables);
        setDefaultMaxStackSize(Items.ENDER_PEARL, maxThrowables);
        setDefaultMaxStackSize(Items.FIRE_CHARGE, maxThrowables);
        setDefaultMaxStackSize(Items.PUFFERFISH, maxThrowables);

        // Empty buckets
        setDefaultMaxStackSize(Items.BUCKET, maxPlaceableBlocks);
        setDefaultMaxStackSize(Items.POWDER_SNOW_BUCKET, maxPlaceableEntities);

        // Rarities
        setDefaultMaxStackSize(Items.SADDLE, maxThrowables);
        setDefaultMaxStackSize(Items.RECOVERY_COMPASS, 1);
    }

    private static void setDefaultMaxStackSize(Item item, int maxCount) {
        DEFAULT_MAX_STACK_SIZE.put(item, maxCount);
    }

    public static int getDefaultMaxStackSize(Item item) {
        return DEFAULT_MAX_STACK_SIZE.getOrDefault(item, -1);
    }

    public static boolean hasReplacementItem(Item item) {
        return TRANSFORM_VANILLA_ITEMS_TO_MODDED.containsKey(item);
    }

    public static Item getReplacementItem(Item item) {
        return TRANSFORM_VANILLA_ITEMS_TO_MODDED.getOrDefault(item, null);
    }
}