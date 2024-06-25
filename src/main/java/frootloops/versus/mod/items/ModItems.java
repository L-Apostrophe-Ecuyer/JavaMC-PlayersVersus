package frootloops.versus.mod.items;

import frootloops.versus.mod.items.brewing.CustomBrewingItems;
import frootloops.versus.mod.items.brewing.CustomStatusEffects;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;

import java.util.HashMap;
import java.util.Map;

public abstract class ModItems {

    private static Map<Item, Integer> DEFAULT_MAX_STACK_SIZE = new HashMap<>();

    private static Map<Item, Item> TRANSFORM_VANILLA_ITEMS_TO_MODDED = new HashMap<>();


    public static void onInitialize() {
        setStackSizes(16, 8, 8, 8, 64, 16, 64);
        setUpTransformVanillaItemsToModded();
        CustomItems.registerAllCustomItems();
        CustomStatusEffects.registerCustomStatusEffects();
    }

    private static void setUpTransformVanillaItemsToModded() {
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.BLAZE_POWDER, CustomBrewingItems.CONCENTRATE_OF_STRENGTH);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.GLOWSTONE_DUST, CustomBrewingItems.CONCENTRATE_OF_GLOWING);
        TRANSFORM_VANILLA_ITEMS_TO_MODDED.put(Items.DIORITE, Items.CALCITE);
    }

    private static void setStackSizes(final int maxFoods, final int maxMeals, final int maxBottled, final int maxStews, final int maxThrowables, final int maxPlaceableEntities, final int maxPlaceableBlocks) {
        for (Item item : Registries.ITEM) {
            if (item.getComponents().contains(DataComponentTypes.FOOD)) {
                if (item instanceof BlockItem) setDefaultMaxStackSize(item, maxPlaceableBlocks);
                else if(item.getComponents().get(DataComponentTypes.FOOD).eatSeconds() ==  0.8f) setDefaultMaxStackSize(item, maxFoods);
                else if (item.getTranslationKey().contains("cooked_") || item.getTranslationKey().contains("raw_")) setDefaultMaxStackSize(item, maxMeals);
                else if (item.getTranslationKey().contains("stew")) setDefaultMaxStackSize(item, maxStews);
                else if (item.getTranslationKey().contains("soup")) setDefaultMaxStackSize(item, maxStews);
                else setDefaultMaxStackSize(item, maxFoods);

            } else if (item instanceof BoatItem || item instanceof MinecartItem || item instanceof ArmorStandItem || item instanceof EndCrystalItem)
                setDefaultMaxStackSize(item, maxPlaceableEntities);

            else if (item instanceof BlockItem) setDefaultMaxStackSize(item, maxPlaceableBlocks);
        }

        // Other foods:
        setDefaultMaxStackSize(net.minecraft.item.Items.CAKE, maxMeals);
        setDefaultMaxStackSize(net.minecraft.item.Items.BREAD, maxFoods);
        setDefaultMaxStackSize(net.minecraft.item.Items.PUMPKIN_PIE, maxMeals);
        setDefaultMaxStackSize(net.minecraft.item.Items.SALMON, maxMeals);
        setDefaultMaxStackSize(net.minecraft.item.Items.COOKED_SALMON, maxMeals);
        setDefaultMaxStackSize(net.minecraft.item.Items.COD, maxMeals);
        setDefaultMaxStackSize(net.minecraft.item.Items.COOKED_COD, maxMeals);
        setDefaultMaxStackSize(net.minecraft.item.Items.TROPICAL_FISH, Math.max(maxFoods, maxMeals));
        setDefaultMaxStackSize(net.minecraft.item.Items.ROTTEN_FLESH, Math.max(maxFoods, maxMeals));

        // Bottles:
        setDefaultMaxStackSize(net.minecraft.item.Items.POTION, maxBottled);
        setDefaultMaxStackSize(net.minecraft.item.Items.HONEY_BOTTLE, Math.max(maxFoods, maxBottled));

        // Throwables:
        setDefaultMaxStackSize(net.minecraft.item.Items.EGG, maxThrowables);
        setDefaultMaxStackSize(net.minecraft.item.Items.SNOWBALL, maxThrowables);
        setDefaultMaxStackSize(net.minecraft.item.Items.ENDER_PEARL, maxThrowables);
        setDefaultMaxStackSize(net.minecraft.item.Items.FIRE_CHARGE, maxThrowables);
        setDefaultMaxStackSize(net.minecraft.item.Items.PUFFERFISH, maxThrowables);

        // Empty buckets
        setDefaultMaxStackSize(net.minecraft.item.Items.BUCKET, maxPlaceableBlocks);
        setDefaultMaxStackSize(net.minecraft.item.Items.POWDER_SNOW_BUCKET, maxPlaceableEntities);

        // Rarities
        setDefaultMaxStackSize(net.minecraft.item.Items.SADDLE, maxThrowables);
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