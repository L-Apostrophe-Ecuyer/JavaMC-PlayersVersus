package frootloops.versus.mod.items;

import frootloops.versus.mixin.items.ItemAccessor;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.items.equipment.copper.CopperArmorMaterial;
import frootloops.versus.mod.items.equipment.copper.CopperToolMaterial;
import frootloops.versus.mod.items.equipment.slime.SlimeArmorMaterial;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import static frootloops.versus.VersusMod.MOD_ID;

public abstract class Items {

    public static final CopperToolMaterial COPPER_TOOL_MATERIAL = new CopperToolMaterial();
    public static final CopperArmorMaterial COPPER_ARMOR_MATERIAL = new CopperArmorMaterial();
    public static final SlimeArmorMaterial SLIME_ARMOR_MATERIAL = new SlimeArmorMaterial();

    public static final Item COPPER_HELMET = new ArmorItem(COPPER_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Settings());
    public static final Item COPPER_CHESTPLATE = new ArmorItem(COPPER_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Settings());
    public static final Item COPPER_LEGGINGS = new ArmorItem(COPPER_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Settings());
    public static final Item COPPER_BOOTS = new ArmorItem(COPPER_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Settings());
    public static ToolItem COPPER_HOE = new HoeItem(COPPER_TOOL_MATERIAL, (int)Combat.getHoeDamageModifier(), Combat.getHoeSpeedModifier(), new Item.Settings());
    public static ToolItem COPPER_AXE = new AxeItem(COPPER_TOOL_MATERIAL, (int)Combat.getAxeDamageModifier(), Combat.getAxeSpeedModifier(), new Item.Settings());
    public static ToolItem COPPER_SWORD = new SwordItem(COPPER_TOOL_MATERIAL, (int)Combat.getSwordDamageModifier(), Combat.getSwordSpeedModifier(), new Item.Settings());
    public static ToolItem COPPER_SHOVEL = new ShovelItem(COPPER_TOOL_MATERIAL, (int)Combat.getShovelDamageModifier(), Combat.getShovelSpeedModifier(), new Item.Settings());
    public static ToolItem COPPER_PICKAXE = new PickaxeItem(COPPER_TOOL_MATERIAL, (int)Combat.getPickaxeDamageModifier(), Combat.getPickaxeSpeedModifier(), new Item.Settings());

    public static final VerticallyAttachableBlockItem SMOLDERING_TORCH_ITEM = new VerticallyAttachableBlockItem(CustomBlocks.SMOLDERING_TORCH, CustomBlocks.SMOLDERING_WALL_TORCH, new Item.Settings(), Direction.DOWN);
    public static final VerticallyAttachableBlockItem EXTINGUISHED_TORCH_ITEM = new VerticallyAttachableBlockItem(CustomBlocks.EXTINGUISHED_TORCH, CustomBlocks.EXTINGUISHED_WALL_TORCH, new Item.Settings(), Direction.DOWN);
    public static final BlockItem GRANITE_BRICKS_ITEM = new BlockItem(CustomBlocks.GRANITE_BRICKS, new Item.Settings());
    public static final BlockItem GRANITE_BRICK_SLAB_ITEM = new BlockItem(CustomBlocks.GRANITE_BRICK_SLAB, new Item.Settings());
    public static final BlockItem GRANITE_BRICK_STAIRS_ITEM = new BlockItem(CustomBlocks.GRANITE_BRICK_STAIRS, new Item.Settings());
    public static final BlockItem GRANITE_TILES_ITEM = new BlockItem(CustomBlocks.GRANITE_TILES, new Item.Settings());
    public static final BlockItem GRANITE_TILES_SLAB_ITEM = new BlockItem(CustomBlocks.GRANITE_TILES_SLAB, new Item.Settings());
    public static final BlockItem GRANITE_TILES_STAIRS_ITEM = new BlockItem(CustomBlocks.GRANITE_TILES_STAIRS, new Item.Settings());
    public static final BlockItem POLISHED_STONE_ITEM = new BlockItem(CustomBlocks.POLISHED_STONE, new Item.Settings());
    public static final BlockItem POLISHED_STONE_SLAB_ITEM = new BlockItem(CustomBlocks.POLISHED_STONE_SLAB, new Item.Settings());
    public static final BlockItem POLISHED_STONE_STAIRS_ITEM = new BlockItem(CustomBlocks.POLISHED_STONE_STAIRS, new Item.Settings());
    public static final BlockItem BROWN_MUD_ITEM = new BlockItem(CustomBlocks.BROWN_MUD, new Item.Settings());
    public static final BlockItem BROWN_MUD_BRICKS_ITEM = new BlockItem(CustomBlocks.BROWN_MUD_BRICKS, new Item.Settings());
    public static final BlockItem BROWN_MUD_BRICK_SLAB_ITEM = new BlockItem(CustomBlocks.BROWN_MUD_BRICK_SLAB, new Item.Settings());
    public static final BlockItem BROWN_MUD_BRICK_STAIRS_ITEM = new BlockItem(CustomBlocks.BROWN_MUD_BRICK_STAIRS, new Item.Settings());
    public static final BlockItem BROWN_MUD_TILES_ITEM = new BlockItem(CustomBlocks.BROWN_MUD_TILES, new Item.Settings());
    public static final BlockItem BROWN_MUD_TILES_SLAB_ITEM = new BlockItem(CustomBlocks.BROWN_MUD_TILES_SLAB, new Item.Settings());
    public static final BlockItem BROWN_MUD_TILES_STAIRS_ITEM = new BlockItem(CustomBlocks.BROWN_MUD_TILES_STAIRS, new Item.Settings());
    public static final BlockItem PACKED_MUD_TILES_ITEM = new BlockItem(CustomBlocks.PACKED_MUD_TILES, new Item.Settings());
    public static final BlockItem PACKED_MUD_TILES_SLAB_ITEM = new BlockItem(CustomBlocks.PACKED_MUD_TILES_SLAB, new Item.Settings());
    public static final BlockItem PACKED_MUD_TILES_STAIRS_ITEM = new BlockItem(CustomBlocks.PACKED_MUD_TILES_STAIRS, new Item.Settings());

    public static void onInitialize() {
        int maxSnacks = 64, maxMeals = 64, maxStews = 8, maxBottled = 8, maxThrowables = 64, maxPlaceableEntities = 16;
        setStackSizes(maxSnacks, maxMeals, maxBottled, maxStews, maxThrowables, maxPlaceableEntities);

        registerCustomItem("copper_chestplate", COPPER_CHESTPLATE, ItemGroups.COMBAT);
        registerCustomItem("copper_leggings", COPPER_LEGGINGS, ItemGroups.COMBAT);
        registerCustomItem("copper_helmet", COPPER_HELMET, ItemGroups.COMBAT);
        registerCustomItem("copper_boots", COPPER_BOOTS, ItemGroups.COMBAT);
        registerCustomItem("copper_hoe", COPPER_HOE, ItemGroups.TOOLS);
        registerCustomItem("copper_axe", COPPER_AXE, ItemGroups.TOOLS, ItemGroups.COMBAT);
        registerCustomItem("copper_sword", COPPER_SWORD, ItemGroups.COMBAT);
        registerCustomItem("copper_shovel", COPPER_SHOVEL, ItemGroups.TOOLS);
        registerCustomItem("copper_pickaxe", COPPER_PICKAXE, ItemGroups.TOOLS);
        registerCustomItem("smoldering_torch", SMOLDERING_TORCH_ITEM, ItemGroups.FUNCTIONAL);
        registerCustomItem("extinguished_torch", EXTINGUISHED_TORCH_ITEM, ItemGroups.FUNCTIONAL);
        registerCustomItem("polished_stone", POLISHED_STONE_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("polished_stone_slab", POLISHED_STONE_SLAB_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("polished_stone_stairs", POLISHED_STONE_STAIRS_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("granite_bricks", GRANITE_BRICKS_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("granite_brick_slab", GRANITE_BRICK_SLAB_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("granite_brick_stairs", GRANITE_BRICK_STAIRS_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("granite_tiles", GRANITE_TILES_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("granite_tile_slab", GRANITE_TILES_SLAB_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("granite_tile_stairs", GRANITE_TILES_STAIRS_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("brown_mud", BROWN_MUD_ITEM, ItemGroups.NATURAL);
        registerCustomItem("brown_mud_bricks", BROWN_MUD_BRICKS_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("brown_mud_brick_slab", BROWN_MUD_BRICK_SLAB_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("brown_mud_brick_stairs", BROWN_MUD_BRICK_STAIRS_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("brown_mud_tiles", BROWN_MUD_TILES_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("brown_mud_tile_slab", BROWN_MUD_TILES_SLAB_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("brown_mud_tile_stairs", BROWN_MUD_TILES_STAIRS_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("packed_mud_tiles", PACKED_MUD_TILES_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("packed_mud_tile_slab", PACKED_MUD_TILES_SLAB_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("packed_mud_tile_stairs", PACKED_MUD_TILES_STAIRS_ITEM, ItemGroups.BUILDING_BLOCKS);
    }

    public static Item registerCustomItem(String name, Item item, RegistryKey<ItemGroup> group) {
        if(group != null) ItemGroupEvents.modifyEntriesEvent(group).register(entries -> entries.add(item));
        return Registry.register(Registries.ITEM, new Identifier(MOD_ID, name), item);
    }

    public static Item registerCustomItem(String name, Item item, RegistryKey<ItemGroup> group1, RegistryKey<ItemGroup> group2) {
        if(group1 != null) ItemGroupEvents.modifyEntriesEvent(group1).register(entries -> entries.add(item));
        if(group2 != null) ItemGroupEvents.modifyEntriesEvent(group2).register(entries -> entries.add(item));
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
        ((ItemAccessor) net.minecraft.item.Items.TROPICAL_FISH).setMaxCount(Math.max(maxSnacks,maxMeals));
        ((ItemAccessor) net.minecraft.item.Items.ROTTEN_FLESH).setMaxCount(Math.max(maxSnacks,maxMeals));

        // Bottles:
        ((ItemAccessor) net.minecraft.item.Items.POTION).setMaxCount(maxBottled);
        ((ItemAccessor) net.minecraft.item.Items.HONEY_BOTTLE).setMaxCount(maxBottled);

        // Throwables:
        ((ItemAccessor) net.minecraft.item.Items.EGG).setMaxCount(maxThrowables);
        ((ItemAccessor) net.minecraft.item.Items.SNOWBALL).setMaxCount(maxThrowables);
        ((ItemAccessor) net.minecraft.item.Items.ENDER_PEARL).setMaxCount(maxThrowables);
        ((ItemAccessor) net.minecraft.item.Items.FIRE_CHARGE).setMaxCount(maxThrowables);
        ((ItemAccessor) net.minecraft.item.Items.PUFFERFISH).setMaxCount(maxThrowables);

        // Empty buckets
        ((ItemAccessor) net.minecraft.item.Items.BUCKET).setMaxCount(64);
        ((ItemAccessor) net.minecraft.item.Items.POWDER_SNOW_BUCKET).setMaxCount(16);

        // Rarities
        // ((ItemAccessor) net.minecraft.item.FutureItems.HEART_OF_THE_SEA).setMaxCount(1);
        // ((ItemAccessor) net.minecraft.item.FutureItems.NETHER_STAR).setMaxCount(1);
        ((ItemAccessor) net.minecraft.item.Items.SADDLE).setMaxCount(64);
    }
}
