package frootloops.versus.mod.items;

import frootloops.versus.mod.Combat;
import frootloops.versus.mod.environment.CustomBlocks;
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
    public static final CopperToolMaterial COPPER_TOOL_MATERIAL = new CopperToolMaterial();
    public static final RegistryEntry<ArmorMaterial> COPPER_ARMOR_MATERIAL = registerCustomArmorMaterial("copper", Util.make(new EnumMap(ArmorItem.Type.class), map -> {
        map.put(ArmorItem.Type.BOOTS, 1);
        map.put(ArmorItem.Type.LEGGINGS, 3);
        map.put(ArmorItem.Type.CHESTPLATE, 5);
        map.put(ArmorItem.Type.HELMET, 2);
        map.put(ArmorItem.Type.BODY, 7);
    }), -4, SoundEvents.ITEM_ARMOR_EQUIP_TURTLE, 0.0f, 0.0f, () -> Ingredient.ofItems(net.minecraft.item.Items.COPPER_INGOT));

    public static final Item COPPER_HELMET = new ArmorItem(COPPER_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Item.Settings().maxDamage(ArmorItem.Type.HELMET.getMaxDamage(10)));
    public static final Item COPPER_CHESTPLATE = new ArmorItem(COPPER_ARMOR_MATERIAL, ArmorItem.Type.CHESTPLATE, new Item.Settings().maxDamage(ArmorItem.Type.CHESTPLATE.getMaxDamage(10)));
    public static final Item COPPER_LEGGINGS = new ArmorItem(COPPER_ARMOR_MATERIAL, ArmorItem.Type.LEGGINGS, new Item.Settings().maxDamage(ArmorItem.Type.LEGGINGS.getMaxDamage(10)));
    public static final Item COPPER_BOOTS = new ArmorItem(COPPER_ARMOR_MATERIAL, ArmorItem.Type.BOOTS, new Item.Settings().maxDamage(ArmorItem.Type.BOOTS.getMaxDamage(10)));
    public static HoeItem COPPER_HOE = new HoeItem(COPPER_TOOL_MATERIAL, new Item.Settings().attributeModifiers(HoeItem.createAttributeModifiers(COPPER_TOOL_MATERIAL, Combat.getHoeDamageModifier(), Combat.getHoeSpeedModifier())));
    public static AxeItem COPPER_AXE = new AxeItem(COPPER_TOOL_MATERIAL, new Item.Settings().attributeModifiers(HoeItem.createAttributeModifiers(COPPER_TOOL_MATERIAL, Combat.getAxeDamageModifier(), Combat.getAxeSpeedModifier())));
    public static PickaxeItem COPPER_PICKAXE = new PickaxeItem(COPPER_TOOL_MATERIAL, new Item.Settings().attributeModifiers(HoeItem.createAttributeModifiers(COPPER_TOOL_MATERIAL, Combat.getPickaxeDamageModifier(), Combat.getPickaxeSpeedModifier())));
    public static SwordItem COPPER_SWORD = new SwordItem(COPPER_TOOL_MATERIAL, new Item.Settings().attributeModifiers(HoeItem.createAttributeModifiers(COPPER_TOOL_MATERIAL, Combat.getSwordDamageModifier(), Combat.getSwordSpeedModifier())));
    public static ShovelItem COPPER_SHOVEL = new ShovelItem(COPPER_TOOL_MATERIAL, new Item.Settings().attributeModifiers(HoeItem.createAttributeModifiers(COPPER_TOOL_MATERIAL, Combat.getShovelDamageModifier(), Combat.getShovelSpeedModifier())));
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
        setStackSizes(64, 64, 8, 8, 64, 16, 64);
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
        if (group != null) ItemGroupEvents.modifyEntriesEvent(group).register(entries -> entries.add(item));
        return Registry.register(Registries.ITEM, new Identifier(MOD_ID, name), item);
    }

    public static Item registerCustomItem(String name, Item item, RegistryKey<ItemGroup> group1, RegistryKey<ItemGroup> group2) {
        if (group1 != null) ItemGroupEvents.modifyEntriesEvent(group1).register(entries -> entries.add(item));
        if (group2 != null) ItemGroupEvents.modifyEntriesEvent(group2).register(entries -> entries.add(item));
        return Registry.register(Registries.ITEM, new Identifier(MOD_ID, name), item);
    }

    public static RegistryEntry<ArmorMaterial> registerCustomArmorMaterial(String name, EnumMap<ArmorItem.Type, Integer> defense, int enchantability, RegistryEntry<SoundEvent> equipSound, float toughness, float knockbackResistance, Supplier<Ingredient> repairIngredient) {
        Identifier id = new Identifier(MOD_ID, name);
        List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(id));
        EnumMap<ArmorItem.Type, Integer> enumMap = new EnumMap<ArmorItem.Type, Integer>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            enumMap.put(type, defense.get(type));
        }
        return Registry.registerReference(Registries.ARMOR_MATERIAL, id, new ArmorMaterial(enumMap, enchantability, equipSound, repairIngredient, layers, toughness, knockbackResistance));
    }

    private static void setStackSizes(final int maxFoods, final int maxMeals, final int maxBottled, final int maxStews, final int maxThrowables, final int maxPlaceableEntities, final int maxPlaceableBlocks) {
        for (Item item : Registries.ITEM) {

            if (item.getComponents().contains(DataComponentTypes.FOOD)) {
                if (item.getTranslationKey().contains("cooked")) setDefaultMaxStackSize(item, maxMeals);
                else if (item instanceof StewItem || item instanceof SuspiciousStewItem)
                    setDefaultMaxStackSize(item, maxStews);
                else setDefaultMaxStackSize(item, maxFoods);
            } else if (item instanceof BoatItem || item instanceof MinecartItem || item instanceof ArmorStandItem || item instanceof EndCrystalItem)
                setDefaultMaxStackSize(item, maxPlaceableEntities);

            else if (item instanceof BlockItem) setDefaultMaxStackSize(item, maxPlaceableBlocks);
        }

        // Other foods:
        setDefaultMaxStackSize(net.minecraft.item.Items.CAKE, maxMeals);
        setDefaultMaxStackSize(net.minecraft.item.Items.CAKE, maxMeals);
        setDefaultMaxStackSize(net.minecraft.item.Items.BREAD, maxMeals);
        setDefaultMaxStackSize(net.minecraft.item.Items.PUMPKIN_PIE, maxMeals);
        setDefaultMaxStackSize(net.minecraft.item.Items.SALMON, maxMeals);
        setDefaultMaxStackSize(net.minecraft.item.Items.COOKED_SALMON, maxMeals);
        setDefaultMaxStackSize(net.minecraft.item.Items.COD, maxMeals);
        setDefaultMaxStackSize(net.minecraft.item.Items.COOKED_COD, maxMeals);
        setDefaultMaxStackSize(net.minecraft.item.Items.TROPICAL_FISH, Math.max(maxFoods, maxMeals));
        setDefaultMaxStackSize(net.minecraft.item.Items.ROTTEN_FLESH, Math.max(maxFoods, maxMeals));

        // Bottles:
        setDefaultMaxStackSize(net.minecraft.item.Items.POTION, maxBottled);
        setDefaultMaxStackSize(net.minecraft.item.Items.HONEY_BOTTLE, maxBottled);

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
        // setDefaultMaxStackSize(net.minecraft.item.FutureItems.HEART_OF_THE_SEA, 1);
        // setDefaultMaxStackSize(net.minecraft.item.FutureItems.NETHER_STAR, 1);
        setDefaultMaxStackSize(net.minecraft.item.Items.SADDLE, maxThrowables);
    }

    private static void setDefaultMaxStackSize(Item item, int maxCount) {
        DEFAULT_MAX_STACK_SIZE.put(item, maxCount);
    }

    public static int getDefaultMaxStackSize(Item item) {
        return DEFAULT_MAX_STACK_SIZE.getOrDefault(item, -1);
    }
}