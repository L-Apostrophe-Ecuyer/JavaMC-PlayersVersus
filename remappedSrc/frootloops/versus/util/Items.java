package frootloops.versus.util;

import frootloops.versus.mixin.players.accessors.ItemAccessor;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import static frootloops.versus.Main.MOD_ID;

public abstract class Items {

    public static void init() {
        int maxSnacks = 64, maxMeals = 64, maxStews = 8, maxBottled = 8, maxThrowables = 64, maxPlaceableEntities = 16;
        setStackSizes(maxSnacks, maxMeals, maxBottled, maxStews, maxThrowables, maxPlaceableEntities);
    }

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(MOD_ID, name), item);
    }

    private static void setStackSizes(final int maxSnacks, final int maxMeals, final int maxBottled, final int maxStews, final int maxThrowables, final int maxPlaceableEntities){
        for (Item item : Registries.ITEM) {

            if(item.getFoodComponent() != null) {
                if(item.getFoodComponent().isMeat()) ((ItemAccessor) item).setMaxCount(maxMeals);
                else if(item instanceof StewItem) ((ItemAccessor) item).setMaxCount(maxStews);
                else ((ItemAccessor) item).setMaxCount(maxSnacks);
            }

            else if(item instanceof BoatItem || item instanceof MinecartItem || item instanceof ArmorStandItem || item instanceof EndCrystalItem)
                ((ItemAccessor) item).setMaxCount(maxPlaceableEntities);
        }

        // Other foods:
        ((ItemAccessor) net.minecraft.item.Items.CAKE).setMaxCount(maxMeals);
        ((ItemAccessor) net.minecraft.item.Items.BREAD).setMaxCount(maxMeals);
        ((ItemAccessor) net.minecraft.item.Items.PUMPKIN_PIE).setMaxCount(maxMeals);
        ((ItemAccessor) net.minecraft.item.Items.SALMON).setMaxCount(maxMeals);
        ((ItemAccessor) net.minecraft.item.Items.COOKED_SALMON).setMaxCount(maxMeals);
        ((ItemAccessor) net.minecraft.item.Items.COD).setMaxCount(maxMeals);
        ((ItemAccessor) net.minecraft.item.Items.COOKED_COD).setMaxCount(maxMeals);
        ((ItemAccessor) net.minecraft.item.Items.PUFFERFISH).setMaxCount(maxMeals);
        ((ItemAccessor) net.minecraft.item.Items.TROPICAL_FISH).setMaxCount(maxMeals);
        ((ItemAccessor) net.minecraft.item.Items.ROTTEN_FLESH).setMaxCount(64);

        // Bottles:
        ((ItemAccessor) net.minecraft.item.Items.POTION).setMaxCount(maxBottled);
        ((ItemAccessor) net.minecraft.item.Items.HONEY_BOTTLE).setMaxCount(maxBottled);

        // Throwables:
        ((ItemAccessor) net.minecraft.item.Items.EGG).setMaxCount(maxThrowables);
        ((ItemAccessor) net.minecraft.item.Items.SNOWBALL).setMaxCount(maxThrowables);
        ((ItemAccessor) net.minecraft.item.Items.ENDER_PEARL).setMaxCount(maxThrowables);

        // Empty buckets
        ((ItemAccessor) net.minecraft.item.Items.BUCKET).setMaxCount(64);
        ((ItemAccessor) net.minecraft.item.Items.POWDER_SNOW_BUCKET).setMaxCount(16);

        // Rarities
        ((ItemAccessor) net.minecraft.item.Items.HEART_OF_THE_SEA).setMaxCount(1);
        ((ItemAccessor) net.minecraft.item.Items.NETHER_STAR).setMaxCount(1);
    }
}
