package frootloops.versus.mod.items;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.CustomBlockItems;
import frootloops.versus.mod.items.brewing.CustomBrewingItems;
import frootloops.versus.mod.items.equipment.CustomEquipment;
import frootloops.versus.mod.mobs.ModEntities;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import static frootloops.versus.VersusMod.MOD_ID;

public abstract class RegisteringCustomItems {
    public static void registerAllCustomItems() {

        registerCustomItem("recovery_compass", CustomEquipment.RECOVERY_COMPASS);

        registerCustomItem("withered_zombie_spawn_egg", ModEntities.WITHERED_ZOMBIE_SPAWN_EGG, ItemGroups.SPAWN_EGGS);
        registerCustomItem("frosted_zombie_spawn_egg", ModEntities.FROSTED_ZOMBIE_SPAWN_EGG, ItemGroups.SPAWN_EGGS);
        registerCustomItem("deeper_creeper_spawn_egg", ModEntities.DEEPER_CREEPER_SPAWN_EGG, ItemGroups.SPAWN_EGGS);

        registerCustomItem("copper_chestplate", CustomEquipment.COPPER_CHESTPLATE, ItemGroups.COMBAT);
        registerCustomItem("copper_leggings", CustomEquipment.COPPER_LEGGINGS, ItemGroups.COMBAT);
        registerCustomItem("copper_helmet", CustomEquipment.COPPER_HELMET, ItemGroups.COMBAT);
        registerCustomItem("copper_boots", CustomEquipment.COPPER_BOOTS, ItemGroups.COMBAT);
        registerCustomItem("copper_hoe", CustomEquipment.COPPER_HOE, ItemGroups.TOOLS);
        registerCustomItem("copper_axe", CustomEquipment.COPPER_AXE, ItemGroups.TOOLS, ItemGroups.COMBAT);
        registerCustomItem("copper_sword", CustomEquipment.COPPER_SWORD, ItemGroups.COMBAT);
        registerCustomItem("copper_shovel", CustomEquipment.COPPER_SHOVEL, ItemGroups.TOOLS);
        registerCustomItem("copper_pickaxe", CustomEquipment.COPPER_PICKAXE, ItemGroups.TOOLS);

        registerCustomItem("bottle_of_ender", CustomBrewingItems.BOTTLE_OF_ENDER, ItemGroups.FOOD_AND_DRINK);
        registerCustomItem("splash_bottle_of_ender", CustomBrewingItems.SPLASH_BOTTLE_OF_ENDER, ItemGroups.FOOD_AND_DRINK);
        registerCustomItem("lingering_bottle_of_ender", CustomBrewingItems.LINGERING_BOTTLE_OF_ENDER, ItemGroups.FOOD_AND_DRINK);

        registerCustomItem("living_flame", CustomBrewingItems.LIVING_FLAME, ItemGroups.INGREDIENTS);
        registerCustomItem("glistering_beetroot", CustomBrewingItems.GLISTERING_BEETROOT, ItemGroups.INGREDIENTS, ItemGroups.FOOD_AND_DRINK);
        registerCustomItem("glistering_melon_slice", CustomBrewingItems.GLISTERING_MELON_SLICE);

        registerCustomItem("four_leaf_clover", CustomBrewingItems.FOUR_LEAF_CLOVER, ItemGroups.INGREDIENTS);
        registerCustomItem("corrupted_wart", CustomBlockItems.CORRUPTED_WART, ItemGroups.INGREDIENTS);
        registerCustomItem("withered_wart", CustomBlockItems.WITHERED_WART, ItemGroups.INGREDIENTS);
        registerCustomItem("corrupted_wart_powder", CustomBrewingItems.CORRUPTED_WART_POWDER); //-> Replacing Fermented Spider Eyes

        registerCustomItem("concentrate_of_death", CustomBrewingItems.CONCENTRATE_OF_DEATH, ItemGroups.INGREDIENTS);          // New potion & effect!
        registerCustomItem("concentrate_of_health", CustomBrewingItems.CONCENTRATE_OF_HEALTH, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_harm", CustomBrewingItems.CONCENTRATE_OF_HARM, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_regeneration", CustomBrewingItems.CONCENTRATE_OF_REGENERATION, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_decay", CustomBrewingItems.CONCENTRATE_OF_DECAY, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_mining_speed", CustomBrewingItems.CONCENTRATE_OF_MINING_SPEED, ItemGroups.INGREDIENTS);     // New potion!
        registerCustomItem("concentrate_of_mining_fatigue", CustomBrewingItems.CONCENTRATE_OF_MINING_FATIGUE, ItemGroups.INGREDIENTS); // New potion!
        registerCustomItem("concentrate_of_toughness", CustomBrewingItems.CONCENTRATE_OF_TOUGHNESS, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_vulnerability", CustomBrewingItems.CONCENTRATE_OF_VULNERABILITY, ItemGroups.INGREDIENTS);   // New potion!
        registerCustomItem("concentrate_of_vision", CustomBrewingItems.CONCENTRATE_OF_VISION, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_darkness", CustomBrewingItems.CONCENTRATE_OF_DARKNESS, ItemGroups.INGREDIENTS);      // New potion!
        registerCustomItem("concentrate_of_leaping", CustomBrewingItems.CONCENTRATE_OF_LEAPING, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_slow_fall", CustomBrewingItems.CONCENTRATE_OF_SLOW_FALL, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_speed", CustomBrewingItems.CONCENTRATE_OF_SPEED, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_slowness", CustomBrewingItems.CONCENTRATE_OF_SLOWNESS, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_breath", CustomBrewingItems.CONCENTRATE_OF_BREATH, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_buoyancy", CustomBrewingItems.CONCENTRATE_OF_BUOYANCY, ItemGroups.INGREDIENTS);      // New potion & effect!
        registerCustomItem("concentrate_of_largeness", CustomBrewingItems.CONCENTRATE_OF_LARGENESS, ItemGroups.INGREDIENTS);    // New potion & effect!
        registerCustomItem("concentrate_of_smallness", CustomBrewingItems.CONCENTRATE_OF_SMALLNESS, ItemGroups.INGREDIENTS);    // New potion & effect!
        registerCustomItem("concentrate_of_invisibility", CustomBrewingItems.CONCENTRATE_OF_INVISIBILITY, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_glowing", CustomBrewingItems.CONCENTRATE_OF_GLOWING, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_weakness", CustomBrewingItems.CONCENTRATE_OF_WEAKNESS, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_strength", CustomBrewingItems.CONCENTRATE_OF_STRENGTH); //-> Replacing Blaze Powder
        registerCustomItem("concentrate_of_fire", CustomBrewingItems.CONCENTRATE_OF_FIRE);// -> Replacing Magma Cream
        registerCustomItem("concentrate_of_wind", CustomBrewingItems.CONCENTRATE_OF_WIND, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_ooze", CustomBrewingItems.CONCENTRATE_OF_OOZE, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_poison", CustomBrewingItems.CONCENTRATE_OF_POISON, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_infestation", CustomBrewingItems.CONCENTRATE_OF_INFESTATION, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_weaving", CustomBrewingItems.CONCENTRATE_OF_WEAVING, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_luck", CustomBrewingItems.CONCENTRATE_OF_LUCK, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_unluck", CustomBrewingItems.CONCENTRATE_OF_UNLUCK, ItemGroups.INGREDIENTS);

        registerCustomItem("smoldering_torch", CustomBlockItems.SMOLDERING_TORCH, ItemGroups.FUNCTIONAL);
        registerCustomItem("extinguished_torch", CustomBlockItems.EXTINGUISHED_TORCH, ItemGroups.FUNCTIONAL);

        registerCustomItem("polished_stone", CustomBlockItems.POLISHED_STONE, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("polished_stone_slab", CustomBlockItems.POLISHED_STONE_SLAB, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("polished_stone_stairs", CustomBlockItems.POLISHED_STONE_STAIRS, ItemGroups.BUILDING_BLOCKS);

        registerCustomItem("dripstone_slab", CustomBlockItems.DRIPSTONE_SLAB, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("dripstone_stairs", CustomBlockItems.DRIPSTONE_STAIRS, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("dripstone_wall", CustomBlockItems.DRIPSTONE_WALL, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("polished_dripstone", CustomBlockItems.POLISHED_DRIPSTONE, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("polished_dripstone_slab", CustomBlockItems.POLISHED_DRIPSTONE_SLAB, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("polished_dripstone_stairs", CustomBlockItems.POLISHED_DRIPSTONE_STAIRS, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("dripstone_pillar", CustomBlockItems.DRIPSTONE_PILLAR, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("dripstone_bricks", CustomBlockItems.DRIPSTONE_BRICKS, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("dripstone_brick_slab", CustomBlockItems.DRIPSTONE_BRICK_SLAB, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("dripstone_brick_stairs", CustomBlockItems.DRIPSTONE_BRICK_STAIRS, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("dripstone_brick_wall", CustomBlockItems.DRIPSTONE_BRICK_WALL, ItemGroups.BUILDING_BLOCKS);

        registerCustomItem("brown_mud", CustomBlockItems.BROWN_MUD, ItemGroups.NATURAL);
        registerCustomItem("brown_mud_bricks", CustomBlockItems.BROWN_MUD_BRICKS, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("brown_mud_brick_slab", CustomBlockItems.BROWN_MUD_BRICK_SLAB, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("brown_mud_brick_stairs", CustomBlockItems.BROWN_MUD_BRICK_STAIRS, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("brown_mud_brick_wall", CustomBlockItems.BROWN_MUD_BRICK_WALL, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("mudstone", CustomBlockItems.MUDSTONE);
        registerCustomItem("mudstone_bricks", CustomBlockItems.MUDSTONE_BRICKS);
        registerCustomItem("mudstone_brick_slab", CustomBlockItems.MUDSTONE_BRICK_SLAB);
        registerCustomItem("mudstone_brick_stairs", CustomBlockItems.MUDSTONE_BRICK_STAIRS);
        registerCustomItem("mudstone_brick_wall", CustomBlockItems.MUDSTONE_BRICK_WALL);

        registerCustomItem("terracotta_bricks", CustomBlockItems.TERRACOTTA_BRICKS, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("terracotta_brick_slab", CustomBlockItems.TERRACOTTA_BRICK_SLAB, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("terracotta_brick_stairs", CustomBlockItems.TERRACOTTA_BRICK_STAIRS, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("terracotta_brick_wall", CustomBlockItems.TERRACOTTA_BRICK_WALL, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("terracotta_tiles", CustomBlockItems.TERRACOTTA_TILES, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("terracotta_tile_slab", CustomBlockItems.TERRACOTTA_TILE_SLAB, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("terracotta_tile_stairs", CustomBlockItems.TERRACOTTA_TILE_STAIRS, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("chiseled_terracotta", CustomBlockItems.CHISELED_TERRACOTTA, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("clay_block", CustomBlockItems.CLAY_BLOCK);

        registerCustomItem("wheat_grass", CustomBlockItems.WHEAT_GRASS, ItemGroups.NATURAL);
        registerCustomItem("wild_wheat", CustomBlockItems.WILD_WHEAT, ItemGroups.NATURAL);
        registerCustomItem("clovers", CustomBlockItems.CLOVERS, ItemGroups.NATURAL);

        registerCustomItem("infested_dark_oak_wood", CustomBlockItems.INFESTED_DARK_OAK_WOOD);
        registerCustomItem("infested_oak_wood", CustomBlockItems.INFESTED_OAK_WOOD);
    }

    public static Item.Settings getSettings(String name) {
        return new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(VersusMod.MOD_ID, name)));
    }

    public static Item registerCustomItem(String name, Item item) {
        return registerCustomItem(name, item, null, null);
    }

    public static Item registerCustomItem(String name, Item item, RegistryKey<ItemGroup> group) {
        return registerCustomItem(name, item, group, null);
    }

    public static Item registerCustomItem(String name, Item item, RegistryKey<ItemGroup> group1, RegistryKey<ItemGroup> group2) {
        if(item == null) VersusMod.MOD_LOGGER.error("  > [ERROR] Couldn't register 'players-versus:" + name + "' because the item was null.");
        if (group1 != null) ItemGroupEvents.modifyEntriesEvent(group1).register(entries -> entries.add(item));
        if (group2 != null) ItemGroupEvents.modifyEntriesEvent(group2).register(entries -> entries.add(item));
        return Registry.register(Registries.ITEM, Identifier.of(MOD_ID, name), item);
    }
}