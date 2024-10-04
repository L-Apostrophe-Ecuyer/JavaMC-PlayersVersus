package frootloops.versus.mod.items;

import frootloops.versus.mod.items.brewing.ConcentrateItem;
import frootloops.versus.mod.items.brewing.CustomBrewingItems;
import frootloops.versus.mod.items.brewing.CustomPotions;
import frootloops.versus.mod.items.brewing.CustomStatusEffects;
import frootloops.versus.mod.items.equipment.CustomEquipment;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;

import java.util.HashMap;
import java.util.Map;

public abstract class ItemsAndStacks {

    private static Map<Item, Integer> DEFAULT_MAX_STACK_SIZE = new HashMap<>();

    private static Map<Item, Item> TRANSFORM_VANILLA_ITEMS_TO_MODDED = new HashMap<>();
    private static Map<Item, Item> TRANSFORM_VANILLA_ITEMS_TO_VANILLA = new HashMap<>();

    private static final Map<Item,Integer> MAX_USE_TIME_MAP = new HashMap<>();
    static {
        MAX_USE_TIME_MAP.put(Items.ROTTEN_FLESH, 50);
        MAX_USE_TIME_MAP.put(Items.SPIDER_EYE, 40);
        MAX_USE_TIME_MAP.put(Items.COOKED_BEEF, 36);
        MAX_USE_TIME_MAP.put(Items.COOKED_PORKCHOP, 36);
        MAX_USE_TIME_MAP.put(Items.COOKED_CHICKEN, 32);
        MAX_USE_TIME_MAP.put(Items.COOKED_MUTTON, 32);
        MAX_USE_TIME_MAP.put(Items.COOKED_RABBIT, 32);
        MAX_USE_TIME_MAP.put(Items.COOKED_SALMON, 28);
        MAX_USE_TIME_MAP.put(Items.COOKED_COD, 28);
        MAX_USE_TIME_MAP.put(Items.BEETROOT, 12);
        MAX_USE_TIME_MAP.put(Items.POTATO, 32);
        MAX_USE_TIME_MAP.put(Items.BAKED_POTATO, 24);
        MAX_USE_TIME_MAP.put(Items.POISONOUS_POTATO, 32);
        MAX_USE_TIME_MAP.put(Items.BREAD, 24);
        MAX_USE_TIME_MAP.put(Items.COOKIE, 12);
        MAX_USE_TIME_MAP.put(Items.DRIED_KELP, 12);
        MAX_USE_TIME_MAP.put(Items.PUMPKIN_PIE, 16);
        MAX_USE_TIME_MAP.put(Items.APPLE, 24);
        MAX_USE_TIME_MAP.put(Items.CARROT, 24);
        MAX_USE_TIME_MAP.put(Items.GOLDEN_CARROT, 28);
        MAX_USE_TIME_MAP.put(Items.GOLDEN_APPLE, 28);
        MAX_USE_TIME_MAP.put(Items.ENCHANTED_GOLDEN_APPLE, 28);
        MAX_USE_TIME_MAP.put(Items.SUSPICIOUS_STEW, 16);
        MAX_USE_TIME_MAP.put(Items.BEETROOT_SOUP, 12);
        MAX_USE_TIME_MAP.put(Items.MUSHROOM_STEW, 16);
        MAX_USE_TIME_MAP.put(Items.RABBIT_STEW, 16);
        MAX_USE_TIME_MAP.put(Items.HONEY_BOTTLE, 16);
        MAX_USE_TIME_MAP.put(Items.POTION, 36);
        MAX_USE_TIME_MAP.put(Items.MILK_BUCKET, 12);
    }

    public static int MAX_POTION_STACK_SIZE = 8;


    public static void onInitialize() {
        RegisteringCustomItems.registerAllCustomItems();
        setUpTransformVanillaItemsToModded();
        setUpTransformVanillaItemsToVanilla();
        setStackSizes(16, 8, MAX_POTION_STACK_SIZE, 8, 64, 16, 64);
    }

    private static void setUpTransformVanillaItemsToModded() {
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.FERMENTED_SPIDER_EYE, CustomBrewingItems.CORRUPTED_WART_POWDER);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.GLISTERING_MELON_SLICE, CustomBrewingItems.GLISTERING_MELON_SLICE);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.BLAZE_POWDER, CustomBrewingItems.CONCENTRATE_OF_STRENGTH);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.MAGMA_CREAM, CustomBrewingItems.CONCENTRATE_OF_FIRE);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.RECOVERY_COMPASS, CustomEquipment.RECOVERY_COMPASS);
    }

    private static void setUpTransformVanillaItemsToVanilla() {
        TRANSFORM_VANILLA_ITEMS_TO_VANILLA.put(Items.DIORITE, Items.CALCITE);
        TRANSFORM_VANILLA_ITEMS_TO_VANILLA.put(Items.CHARCOAL, Items.COAL);
    }

    private static void setStackSizes(final int maxFoods, final int maxMeals, final int maxBottled, final int maxStews, final int maxThrowables, final int maxPlaceableEntities, final int maxPlaceableBlocks) {
        for (Item item : Registries.ITEM) {
            if (item.getComponents().contains(DataComponentTypes.FOOD)) {
                if (item instanceof BlockItem) setNewMaxStackSize(item, maxPlaceableBlocks);
                else if (item instanceof ConcentrateItem) setNewMaxStackSize(item, 64);
                else if(item.getComponents().get(DataComponentTypes.CONSUMABLE).consumeSeconds() ==  0.8f) setNewMaxStackSize(item, maxFoods);
                else if (item.getTranslationKey().contains("cooked_") || item.getTranslationKey().contains("raw_")) setNewMaxStackSize(item, maxMeals);
                else if (item.getTranslationKey().contains("stew")) setNewMaxStackSize(item, maxStews);
                else if (item.getTranslationKey().contains("soup")) setNewMaxStackSize(item, maxStews);
                else setNewMaxStackSize(item, maxFoods);

            }
            else if (item instanceof BoatItem || item instanceof MinecartItem || item instanceof ArmorStandItem || item instanceof EndCrystalItem)
                setNewMaxStackSize(item, maxPlaceableEntities);

            else if (item instanceof BlockItem)
                setNewMaxStackSize(item, maxPlaceableBlocks);
        }

        // Other foods:
        setNewMaxStackSize(Items.CAKE, maxMeals);
        setNewMaxStackSize(Items.BREAD, maxFoods);
        setNewMaxStackSize(Items.PUMPKIN_PIE, maxMeals);
        setNewMaxStackSize(Items.SALMON, maxMeals);
        setNewMaxStackSize(Items.COOKED_SALMON, maxMeals);
        setNewMaxStackSize(Items.COD, maxMeals);
        setNewMaxStackSize(Items.COOKED_COD, maxMeals);
        setNewMaxStackSize(Items.TROPICAL_FISH, Math.max(maxFoods, maxMeals));
        setNewMaxStackSize(Items.ROTTEN_FLESH, Math.max(maxFoods, maxMeals));
        setNewMaxStackSize(Items.MELON_SLICE, Math.max(maxFoods, maxPlaceableBlocks));

        // Bottles:
        setNewMaxStackSize(Items.POTION, maxBottled);
        setNewMaxStackSize(Items.HONEY_BOTTLE, Math.max(maxFoods, maxBottled));

        // Throwables:
        setNewMaxStackSize(Items.EGG, maxThrowables);
        setNewMaxStackSize(Items.SNOWBALL, maxThrowables);
        setNewMaxStackSize(Items.ENDER_PEARL, maxThrowables);
        setNewMaxStackSize(Items.FIRE_CHARGE, maxThrowables);
        setNewMaxStackSize(Items.PUFFERFISH, maxThrowables);

        // Empty buckets
        setNewMaxStackSize(Items.BUCKET, maxPlaceableBlocks);
        setNewMaxStackSize(Items.POWDER_SNOW_BUCKET, maxPlaceableEntities);

        // Rarities
        setNewMaxStackSize(Items.SADDLE, maxThrowables);
        setNewMaxStackSize(Items.RECOVERY_COMPASS, 1);
    }

    private static void setNewMaxStackSize(Item item, int maxCount) {
        if(item.getMaxCount() != maxCount) DEFAULT_MAX_STACK_SIZE.put(item, maxCount);
    }

    public static int getOverhauledMaxStackSize(Item item) {
        return DEFAULT_MAX_STACK_SIZE.getOrDefault(item, -1);
    }

    public static int getOverhauledMaxUseTime(Item item) {
        return MAX_USE_TIME_MAP.getOrDefault(item, -1);
    }

    public static boolean hasReplacementItem(Item item) {
        return TRANSFORM_VANILLA_ITEMS_TO_MODDED.containsKey(item);
    }

    public static Item getReplacementItem(Item item) {
        return TRANSFORM_VANILLA_ITEMS_TO_MODDED.getOrDefault(item, item);
    }

    public static boolean hasVanillaReplacementItem(Item item) {
        return TRANSFORM_VANILLA_ITEMS_TO_VANILLA.containsKey(item);
    }

    public static Item getVanillaReplacementItem(Item item) {
        return TRANSFORM_VANILLA_ITEMS_TO_VANILLA.getOrDefault(item, item);
    }
}