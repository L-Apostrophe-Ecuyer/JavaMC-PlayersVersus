package frootloops.versus.mod.items;

import frootloops.versus.mixin.players.accessors.ItemAccessor;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.items.equipment.copper.BronzeArmorMaterial;
import frootloops.versus.mod.items.equipment.copper.BronzeToolMaterial;
import frootloops.versus.mod.items.equipment.slime.SlimeArmorMaterial;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import static frootloops.versus.VersusMod.MOD_ID;

public abstract class Items {

    public static final BronzeToolMaterial BRONZE_TOOL_MATERIAL = new BronzeToolMaterial();
    public static final BronzeArmorMaterial BRONZE_ARMOR_MATERIAL = new BronzeArmorMaterial();
    public static final SlimeArmorMaterial SLIME_ARMOR_MATERIAL = new SlimeArmorMaterial();

    public static final Item BRONZE_INGOT = new Item(new Item.Settings());
    public static final Item RAW_BRONZE_ALLOY = new Item(new Item.Settings());
    public static final Item BRONZE_HELMET = new ArmorItem(BRONZE_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Settings());
    public static final Item BRONZE_CHESTPLATE = new ArmorItem(BRONZE_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Settings());
    public static final Item BRONZE_LEGGINGS = new ArmorItem(BRONZE_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Settings());
    public static final Item BRONZE_BOOTS = new ArmorItem(BRONZE_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Settings());
    public static ToolItem BRONZE_HOE = new HoeItem(BRONZE_TOOL_MATERIAL, 4, Combat.getHoeSpeedModifier(), new Item.Settings());
    public static ToolItem BRONZE_AXE = new AxeItem(BRONZE_TOOL_MATERIAL, 8, Combat.getAxeSpeedModifier(), new Item.Settings());
    public static ToolItem BRONZE_SWORD = new SwordItem(BRONZE_TOOL_MATERIAL, 5, Combat.getSwordSpeedModifier(), new Item.Settings());
    public static ToolItem BRONZE_SHOVEL = new ShovelItem(BRONZE_TOOL_MATERIAL, 4, Combat.getShovelSpeedModifier(), new Item.Settings());
    public static ToolItem BRONZE_PICKAXE = new PickaxeItem(BRONZE_TOOL_MATERIAL, 5, Combat.getPickaxeSpeedModifier(), new Item.Settings());

    public static void onInitialize() {
        int maxSnacks = 64, maxMeals = 64, maxStews = 8, maxBottled = 8, maxThrowables = 64, maxPlaceableEntities = 16;
        setStackSizes(maxSnacks, maxMeals, maxBottled, maxStews, maxThrowables, maxPlaceableEntities);

        registerCustomItem("bronze_ingot", BRONZE_INGOT);
        registerCustomItem("raw_bronze_ingot", RAW_BRONZE_ALLOY);

        registerCustomItem("bronze_chestplate", BRONZE_CHESTPLATE);
        registerCustomItem("bronze_leggings", BRONZE_LEGGINGS);
        registerCustomItem("bronze_helmet", BRONZE_HELMET);
        registerCustomItem("bronze_boots", BRONZE_BOOTS);
        registerCustomItem("bronze_hoe", BRONZE_HOE);
        registerCustomItem("bronze_axe", BRONZE_AXE);
        registerCustomItem("bronze_sword", BRONZE_SWORD);
        registerCustomItem("bronze_shovel", BRONZE_SHOVEL);
        registerCustomItem("bronze_pickaxe", BRONZE_PICKAXE);
    }

    private static Item registerCustomItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(MOD_ID, name), item);
    }

    private static void setStackSizes(final int maxSnacks, final int maxMeals, final int maxBottled, final int maxStews, final int maxThrowables, final int maxPlaceableEntities){
        for (Item item : Registries.ITEM) {

            if(item.getFoodComponent() != null) {
                if(item.getFoodComponent().isMeat()) ((ItemAccessor) item).setMaxCount(maxMeals);
                else if(item instanceof StewItem || item instanceof SuspiciousStewItem) ((ItemAccessor) item).setMaxCount(maxStews);
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
