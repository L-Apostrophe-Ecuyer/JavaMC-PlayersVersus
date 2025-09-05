package frootloops.versus.mod.items;

import frootloops.versus.mod.environment.CustomBlockItems;
import frootloops.versus.mod.items.brewing.ConcentrateItem;
import frootloops.versus.mod.items.brewing.CustomBrewingItems;
import frootloops.versus.mod.items.equipment.CustomEquipment;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;

import java.util.HashMap;
import java.util.Map;

public abstract class VanillaItemsAndStacks {

    private static Map<Item, Integer> DEFAULT_MAX_STACK_SIZE = new HashMap<>();

    private static Map<Item, Item> TRANSFORM_VANILLA_ITEMS_TO_MODDED = new HashMap<>();
    private static Map<Item, Item> TRANSFORM_VANILLA_ITEMS_TO_VANILLA = new HashMap<>();

    private static final Map<Item,Integer> MAX_USE_TIME_MAP = new HashMap<>();

    public static int MAX_POTION_STACK_SIZE = 8;


    public static void onInitialize() {
        RegisteringCustomItems.registerAllCustomItems();
        setUpTransformVanillaItemsToModded();
        setUpTransformVanillaItemsToVanilla();
        setUpMaxUseTimeOverwrite();
        setStackSizes(64, 16, MAX_POTION_STACK_SIZE, 8, 64, 16, 64);
    }

    private static void setUpTransformVanillaItemsToModded() {
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.FERMENTED_SPIDER_EYE, CustomBrewingItems.CORRUPTED_WART_POWDER);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.GLISTERING_MELON_SLICE, CustomBrewingItems.GLISTERING_MELON_SLICE);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.BLAZE_POWDER, CustomBrewingItems.CONCENTRATE_OF_STRENGTH);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.MAGMA_CREAM, CustomBrewingItems.CONCENTRATE_OF_FIRE);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.RECOVERY_COMPASS, CustomEquipment.RECOVERY_COMPASS);

        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.LADDER, CustomBlockItems.LADDER);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.CLAY, CustomBlockItems.GRAY_CLAY);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.MUD, CustomBlockItems.GRAY_MUD);

        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.PACKED_MUD, CustomBlockItems.BROWN_CLAY);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.MUD_BRICKS, CustomBlockItems.BROWN_CLAY_BRICKS);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.MUD_BRICK_SLAB, CustomBlockItems.BROWN_CLAY_BRICK_SLAB);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.MUD_BRICK_STAIRS, CustomBlockItems.BROWN_CLAY_BRICK_STAIRS);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.MUD_BRICK_WALL, CustomBlockItems.BROWN_MUD_BRICK_WALL);
    }

    private static void setUpTransformVanillaItemsToVanilla() {
        //TRANSFORM_VANILLA_ITEMS_TO_VANILLA.put(Items.GRANITE, Items.DRIPSTONE_BLOCK);
        TRANSFORM_VANILLA_ITEMS_TO_VANILLA.put(Items.CHARCOAL, Items.COAL);
    }

    private static void setUpMaxUseTimeOverwrite() {
        MAX_USE_TIME_MAP.put(Items.BEETROOT, 12);
        MAX_USE_TIME_MAP.put(Items.BAKED_POTATO, 24);
        MAX_USE_TIME_MAP.put(Items.BREAD, 24);
        MAX_USE_TIME_MAP.put(Items.COOKIE, 12);
        MAX_USE_TIME_MAP.put(Items.DRIED_KELP, 12);
        MAX_USE_TIME_MAP.put(Items.PUMPKIN_PIE, 16);
        MAX_USE_TIME_MAP.put(Items.APPLE, 24);
        MAX_USE_TIME_MAP.put(Items.CARROT, 24);
        MAX_USE_TIME_MAP.put(Items.SUSPICIOUS_STEW, 16);
        MAX_USE_TIME_MAP.put(Items.BEETROOT_SOUP, 12);
        MAX_USE_TIME_MAP.put(Items.MUSHROOM_STEW, 16);
        MAX_USE_TIME_MAP.put(Items.RABBIT_STEW, 16);
        MAX_USE_TIME_MAP.put(Items.HONEY_BOTTLE, 16);
        MAX_USE_TIME_MAP.put(Items.MILK_BUCKET, 12);
    }

    private static void setStackSizes(final int maxFoods, final int maxMeals, final int maxBottled, final int maxStews, final int maxThrowables, final int maxPlaceableEntities, final int maxPlaceableBlocks) {
        for (Item item : Registries.ITEM) {
            if (item.getComponents().contains(DataComponentTypes.FOOD)) {
                if (item instanceof BlockItem) setNewMaxStackSize(item, maxPlaceableBlocks);
                else if (item instanceof ConcentrateItem) setNewMaxStackSize(item, 64);
                else if(item.getComponents().get(DataComponentTypes.CONSUMABLE).consumeSeconds() ==  0.8f) setNewMaxStackSize(item, maxFoods);
                else if (item.getTranslationKey().contains("cooked_") || item.getTranslationKey().contains("raw_")) setNewMaxStackSize(item, maxFoods);
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
        setNewMaxStackSize(Items.PUMPKIN_PIE, maxFoods);
        setNewMaxStackSize(Items.SALMON, maxMeals);
        setNewMaxStackSize(Items.COOKED_SALMON, maxFoods);
        setNewMaxStackSize(Items.COD, maxFoods);
        setNewMaxStackSize(Items.COOKED_COD, maxFoods);
        setNewMaxStackSize(Items.TROPICAL_FISH, Math.max(maxFoods, maxFoods));
        setNewMaxStackSize(Items.ROTTEN_FLESH, Math.max(maxFoods, maxFoods));
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

    public static boolean hasModdedReplacementItem(Item item) {
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