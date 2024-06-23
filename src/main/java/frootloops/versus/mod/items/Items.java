package frootloops.versus.mod.items;

import frootloops.versus.mod.Combat;
import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.items.brewing.CustomStatusEffects;
import frootloops.versus.mod.items.equipment.copper.CopperToolMaterial;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.*;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Direction;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static frootloops.versus.VersusMod.MOD_ID;

public abstract class Items {

    private static Map<Item, Integer> DEFAULT_MAX_STACK_SIZE = new HashMap<>();

    public static void onInitialize() {
        setStackSizes(16, 8, 4, 4, 64, 16, 64);
        CustomItems.registerAllCustomItems();
        CustomStatusEffects.registerCustomStatusEffects();
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
}