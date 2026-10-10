package frootloops.versus.mod.items_and_effects;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.CustomBlockItems;
import frootloops.versus.mod.mobs.ModEntities;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import static frootloops.versus.VersusMod.MOD_ID;

public abstract class RegisteringCustomItems {

    public static void registerAllCustomItems() {

        registerCustomItem("recovery_compass", CustomEquipment.RECOVERY_COMPASS);

        registerCustomItem("withered_zombie_spawn_egg", ModEntities.WITHERED_ZOMBIE_SPAWN_EGG, CreativeModeTabs.SPAWN_EGGS);
        registerCustomItem("frosted_zombie_spawn_egg", ModEntities.FROSTED_ZOMBIE_SPAWN_EGG, CreativeModeTabs.SPAWN_EGGS);
        registerCustomItem("deeper_creeper_spawn_egg", ModEntities.DEEPER_CREEPER_SPAWN_EGG, CreativeModeTabs.SPAWN_EGGS);
        registerCustomItem("pale_creeper_spawn_egg", ModEntities.PALE_CREEPER_SPAWN_EGG, CreativeModeTabs.SPAWN_EGGS);
        registerCustomItem("pale_zombie_spawn_egg", ModEntities.PALE_ZOMBIE_SPAWN_EGG, CreativeModeTabs.SPAWN_EGGS);
        registerCustomItem("pale_spider_spawn_egg", ModEntities.PALE_SPIDER_SPAWN_EGG, CreativeModeTabs.SPAWN_EGGS);
        registerCustomItem("wildfire_spawn_egg", ModEntities.WILDFIRE_SPAWN_EGG, CreativeModeTabs.SPAWN_EGGS);

        registerCustomItem("copper_chestplate", CustomEquipment.COPPER_CHESTPLATE, CreativeModeTabs.COMBAT);
        registerCustomItem("copper_leggings", CustomEquipment.COPPER_LEGGINGS, CreativeModeTabs.COMBAT);
        registerCustomItem("copper_helmet", CustomEquipment.COPPER_HELMET, CreativeModeTabs.COMBAT);
        registerCustomItem("copper_boots", CustomEquipment.COPPER_BOOTS, CreativeModeTabs.COMBAT);
        registerCustomItem("copper_chestplate_waxed", CustomEquipment.COPPER_CHESTPLATE_WAXED, CreativeModeTabs.COMBAT);
        registerCustomItem("copper_leggings_waxed", CustomEquipment.COPPER_LEGGINGS_WAXED, CreativeModeTabs.COMBAT);
        registerCustomItem("copper_helmet_waxed", CustomEquipment.COPPER_HELMET_WAXED, CreativeModeTabs.COMBAT);
        registerCustomItem("copper_boots_waxed", CustomEquipment.COPPER_BOOTS_WAXED, CreativeModeTabs.COMBAT);
        registerCustomItem("copper_chestplate_exposed", CustomEquipment.COPPER_CHESTPLATE_EXPOSED, CreativeModeTabs.COMBAT);
        registerCustomItem("copper_leggings_exposed", CustomEquipment.COPPER_LEGGINGS_EXPOSED, CreativeModeTabs.COMBAT);
        registerCustomItem("copper_helmet_exposed", CustomEquipment.COPPER_HELMET_EXPOSED, CreativeModeTabs.COMBAT);
        registerCustomItem("copper_boots_exposed", CustomEquipment.COPPER_BOOTS_EXPOSED, CreativeModeTabs.COMBAT);
        registerCustomItem("copper_hoe", CustomEquipment.COPPER_HOE, CreativeModeTabs.TOOLS_AND_UTILITIES);
        registerCustomItem("copper_axe", CustomEquipment.COPPER_AXE, CreativeModeTabs.TOOLS_AND_UTILITIES, CreativeModeTabs.COMBAT);
        registerCustomItem("copper_sword", CustomEquipment.COPPER_SWORD, CreativeModeTabs.COMBAT);
        registerCustomItem("copper_shovel", CustomEquipment.COPPER_SHOVEL, CreativeModeTabs.TOOLS_AND_UTILITIES);
        registerCustomItem("copper_pickaxe", CustomEquipment.COPPER_PICKAXE, CreativeModeTabs.TOOLS_AND_UTILITIES);
        registerCustomItem("copper_hoe_waxed", CustomEquipment.COPPER_HOE_WAXED, CreativeModeTabs.TOOLS_AND_UTILITIES);
        registerCustomItem("copper_axe_waxed", CustomEquipment.COPPER_AXE_WAXED, CreativeModeTabs.TOOLS_AND_UTILITIES, CreativeModeTabs.COMBAT);
        registerCustomItem("copper_sword_waxed", CustomEquipment.COPPER_SWORD_WAXED, CreativeModeTabs.COMBAT);
        registerCustomItem("copper_shovel_waxed", CustomEquipment.COPPER_SHOVEL_WAXED, CreativeModeTabs.TOOLS_AND_UTILITIES);
        registerCustomItem("copper_pickaxe_waxed", CustomEquipment.COPPER_PICKAXE_WAXED, CreativeModeTabs.TOOLS_AND_UTILITIES);

        registerCustomItem("living_flame", CustomBrewingItems.LIVING_FLAME, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("glistering_beetroot", CustomBrewingItems.GLISTERING_BEETROOT, CreativeModeTabs.INGREDIENTS, CreativeModeTabs.FOOD_AND_DRINKS);

        registerCustomItem("four_leaf_clover", CustomBrewingItems.FOUR_LEAF_CLOVER, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("corrupted_wart", CustomBlockItems.CORRUPTED_WART, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("withered_wart", CustomBlockItems.WITHERED_WART, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("corrupted_wart_powder", CustomBrewingItems.CORRUPTED_WART_POWDER); //-> Replacing Fermented Spider Eyes

        registerCustomItem("concentrate_of_death", CustomBrewingItems.CONCENTRATE_OF_DEATH, CreativeModeTabs.INGREDIENTS);          // New potion & effect!
        registerCustomItem("concentrate_of_health", CustomBrewingItems.CONCENTRATE_OF_HEALTH, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_harm", CustomBrewingItems.CONCENTRATE_OF_HARM, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_regeneration", CustomBrewingItems.CONCENTRATE_OF_REGENERATION, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_decay", CustomBrewingItems.CONCENTRATE_OF_DECAY, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_mining_speed", CustomBrewingItems.CONCENTRATE_OF_MINING_SPEED, CreativeModeTabs.INGREDIENTS);     // New potion!
        registerCustomItem("concentrate_of_mining_fatigue", CustomBrewingItems.CONCENTRATE_OF_MINING_FATIGUE, CreativeModeTabs.INGREDIENTS); // New potion!
        registerCustomItem("concentrate_of_toughness", CustomBrewingItems.CONCENTRATE_OF_TOUGHNESS, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_vulnerability", CustomBrewingItems.CONCENTRATE_OF_VULNERABILITY, CreativeModeTabs.INGREDIENTS);   // New potion!
        registerCustomItem("concentrate_of_vision", CustomBrewingItems.CONCENTRATE_OF_VISION, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_darkness", CustomBrewingItems.CONCENTRATE_OF_DARKNESS, CreativeModeTabs.INGREDIENTS);      // New potion!
        registerCustomItem("concentrate_of_leaping", CustomBrewingItems.CONCENTRATE_OF_LEAPING, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_slow_fall", CustomBrewingItems.CONCENTRATE_OF_SLOW_FALL, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_levitation", CustomBrewingItems.CONCENTRATE_OF_LEVITATION, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_speed", CustomBrewingItems.CONCENTRATE_OF_SPEED, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_slowness", CustomBrewingItems.CONCENTRATE_OF_SLOWNESS, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_breath", CustomBrewingItems.CONCENTRATE_OF_BREATH, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_buoyancy", CustomBrewingItems.CONCENTRATE_OF_BUOYANCY, CreativeModeTabs.INGREDIENTS);      // New potion & effect!
        registerCustomItem("concentrate_of_largeness", CustomBrewingItems.CONCENTRATE_OF_LARGENESS, CreativeModeTabs.INGREDIENTS);    // New potion & effect!
        registerCustomItem("concentrate_of_smallness", CustomBrewingItems.CONCENTRATE_OF_SMALLNESS, CreativeModeTabs.INGREDIENTS);    // New potion & effect!
        registerCustomItem("concentrate_of_invisibility", CustomBrewingItems.CONCENTRATE_OF_INVISIBILITY, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_glowing", CustomBrewingItems.CONCENTRATE_OF_GLOWING, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_weakness", CustomBrewingItems.CONCENTRATE_OF_WEAKNESS, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_strength", CustomBrewingItems.CONCENTRATE_OF_STRENGTH); //-> Replacing Blaze Powder
        registerCustomItem("concentrate_of_fire", CustomBrewingItems.CONCENTRATE_OF_FIRE);// -> Replacing Magma Cream
        registerCustomItem("concentrate_of_wind", CustomBrewingItems.CONCENTRATE_OF_WIND, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_ooze", CustomBrewingItems.CONCENTRATE_OF_OOZE, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_poison", CustomBrewingItems.CONCENTRATE_OF_POISON, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_infestation", CustomBrewingItems.CONCENTRATE_OF_INFESTATION, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_weaving", CustomBrewingItems.CONCENTRATE_OF_WEAVING, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_luck", CustomBrewingItems.CONCENTRATE_OF_LUCK, CreativeModeTabs.INGREDIENTS);
        registerCustomItem("concentrate_of_unluck", CustomBrewingItems.CONCENTRATE_OF_UNLUCK, CreativeModeTabs.INGREDIENTS);

        registerCustomItem("ladder", CustomBlockItems.LADDER, CreativeModeTabs.FUNCTIONAL_BLOCKS);
        registerCustomItem("smoldering_torch", CustomBlockItems.SMOLDERING_TORCH, CreativeModeTabs.FUNCTIONAL_BLOCKS);
        registerCustomItem("extinguished_torch", CustomBlockItems.EXTINGUISHED_TORCH, CreativeModeTabs.FUNCTIONAL_BLOCKS);

        registerCustomItem("cut_lapis", CustomBlockItems.CUT_LAPIS, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("cut_lapis_slab", CustomBlockItems.CUT_LAPIS_SLAB, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("cut_lapis_stairs", CustomBlockItems.CUT_LAPIS_STAIRS, CreativeModeTabs.BUILDING_BLOCKS);

        registerCustomItem("polished_stone", CustomBlockItems.POLISHED_STONE, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("polished_stone_slab", CustomBlockItems.POLISHED_STONE_SLAB, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("polished_stone_stairs", CustomBlockItems.POLISHED_STONE_STAIRS, CreativeModeTabs.BUILDING_BLOCKS);

        registerCustomItem("dripstone_slab", CustomBlockItems.DRIPSTONE_SLAB, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("dripstone_stairs", CustomBlockItems.DRIPSTONE_STAIRS, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("dripstone_wall", CustomBlockItems.DRIPSTONE_WALL, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("polished_dripstone", CustomBlockItems.POLISHED_DRIPSTONE, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("polished_dripstone_slab", CustomBlockItems.POLISHED_DRIPSTONE_SLAB, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("polished_dripstone_stairs", CustomBlockItems.POLISHED_DRIPSTONE_STAIRS, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("dripstone_pillar", CustomBlockItems.DRIPSTONE_PILLAR, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("dripstone_bricks", CustomBlockItems.DRIPSTONE_BRICKS, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("dripstone_brick_slab", CustomBlockItems.DRIPSTONE_BRICK_SLAB, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("dripstone_brick_stairs", CustomBlockItems.DRIPSTONE_BRICK_STAIRS, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("dripstone_brick_wall", CustomBlockItems.DRIPSTONE_BRICK_WALL, CreativeModeTabs.BUILDING_BLOCKS);

        registerCustomItem("gray_clay", CustomBlockItems.GRAY_CLAY, CreativeModeTabs.NATURAL_BLOCKS);
        registerCustomItem("gray_mud", CustomBlockItems.GRAY_MUD, CreativeModeTabs.NATURAL_BLOCKS);

        registerCustomItem("brown_clay_ball", CustomBlockItems.BROWN_CLAY_BALL, CreativeModeTabs.NATURAL_BLOCKS);
        registerCustomItem("brown_clay", CustomBlockItems.BROWN_CLAY);
        registerCustomItem("brown_mud", CustomBlockItems.BROWN_MUD, CreativeModeTabs.NATURAL_BLOCKS);
        registerCustomItem("brown_mud_bricks", CustomBlockItems.BROWN_MUD_BRICKS, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("brown_mud_brick_slab", CustomBlockItems.BROWN_MUD_BRICK_SLAB, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("brown_mud_brick_stairs", CustomBlockItems.BROWN_MUD_BRICK_STAIRS, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("brown_mud_brick_wall", CustomBlockItems.BROWN_MUD_BRICK_WALL, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("brown_clay_bricks", CustomBlockItems.BROWN_CLAY_BRICKS);
        registerCustomItem("brown_clay_brick_slab", CustomBlockItems.BROWN_CLAY_BRICK_SLAB);
        registerCustomItem("brown_clay_brick_stairs", CustomBlockItems.BROWN_CLAY_BRICK_STAIRS);
        registerCustomItem("brown_clay_brick_wall", CustomBlockItems.BROWN_CLAY_BRICK_WALL);

        registerCustomItem("terracotta_bricks", CustomBlockItems.TERRACOTTA_BRICKS, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("terracotta_brick_slab", CustomBlockItems.TERRACOTTA_BRICK_SLAB, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("terracotta_brick_stairs", CustomBlockItems.TERRACOTTA_BRICK_STAIRS, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("terracotta_brick_wall", CustomBlockItems.TERRACOTTA_BRICK_WALL, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("terracotta_tiles", CustomBlockItems.TERRACOTTA_TILES, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("terracotta_tile_slab", CustomBlockItems.TERRACOTTA_TILE_SLAB, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("terracotta_tile_stairs", CustomBlockItems.TERRACOTTA_TILE_STAIRS, CreativeModeTabs.BUILDING_BLOCKS);
        registerCustomItem("chiseled_terracotta", CustomBlockItems.CHISELED_TERRACOTTA, CreativeModeTabs.BUILDING_BLOCKS);

        registerCustomItem("wheat_grass", CustomBlockItems.WHEAT_GRASS, CreativeModeTabs.NATURAL_BLOCKS);
        registerCustomItem("wild_wheat", CustomBlockItems.WILD_WHEAT, CreativeModeTabs.NATURAL_BLOCKS);
        registerCustomItem("clovers", CustomBlockItems.CLOVERS, CreativeModeTabs.NATURAL_BLOCKS);

        registerCustomItem("creeper_spore_blossom", CustomBlockItems.CREEPER_SPORE_BLOSSOM, CreativeModeTabs.FUNCTIONAL_BLOCKS);
    }

    public static Item.Properties getItemSettings(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, name)));
    }

    public static Item registerCustomItem(String name, Item item) {
        return registerCustomItem(name, item, null, null);
    }

    public static Item registerCustomItem(String name, Item item, ResourceKey<CreativeModeTab> group) {
        return registerCustomItem(name, item, group, null);
    }

    public static Item registerCustomItem(String name, Item item, ResourceKey<CreativeModeTab> group1, ResourceKey<CreativeModeTab> group2) {
        if(item == null) VersusMod.MOD_LOGGER.error("  > [ERROR] Couldn't register 'players-versus:" + name + "' because the item was null.");
        if (group1 != null) CreativeModeTabEvents.modifyOutputEvent(group1).register(output -> output.accept(item));
        if (group2 != null) CreativeModeTabEvents.modifyOutputEvent(group2).register(output -> output.accept(item));
        return Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, name), item);
    }
}