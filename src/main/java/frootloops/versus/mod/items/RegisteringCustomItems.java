package frootloops.versus.mod.items;

import frootloops.versus.mod.environment.CustomBlockItems;
import frootloops.versus.mod.items.brewing.CustomBrewingItems;
import frootloops.versus.mod.items.brewing.CustomPotions;
import frootloops.versus.mod.items.equipment.CustomEquipment;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import static frootloops.versus.VersusMod.MOD_ID;

public abstract class RegisteringCustomItems {
    public static void registerAllCustomItems() {
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
        registerCustomItem("corrupted_wart", CustomBlockItems.CORRUPTED_WART_ITEM, ItemGroups.INGREDIENTS);
        registerCustomItem("withered_wart", CustomBlockItems.WITHERED_WART_ITEM, ItemGroups.INGREDIENTS);
        //registerCustomItem("corrupted_wart_powder", CustomBrewingItems.CORRUPTED_WART_POWDER, ItemGroups.INGREDIENTS);

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
        //registerCustomItem("concentrate_of_strength", CustomBrewingItems.CONCENTRATE_OF_STRENGTH, ItemGroups.INGREDIENTS);
        //registerCustomItem("concentrate_of_fire", CustomBrewingItems.CONCENTRATE_OF_FIRE, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_wind", CustomBrewingItems.CONCENTRATE_OF_WIND, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_ooze", CustomBrewingItems.CONCENTRATE_OF_OOZE, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_poison", CustomBrewingItems.CONCENTRATE_OF_POISON, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_infestation", CustomBrewingItems.CONCENTRATE_OF_INFESTATION, ItemGroups.INGREDIENTS);
        registerCustomItem("concentrate_of_weaving", CustomBrewingItems.CONCENTRATE_OF_WEAVING, ItemGroups.INGREDIENTS);

        registerCustomItem("smoldering_torch", CustomBlockItems.SMOLDERING_TORCH_ITEM, ItemGroups.FUNCTIONAL);
        registerCustomItem("extinguished_torch", CustomBlockItems.EXTINGUISHED_TORCH_ITEM, ItemGroups.FUNCTIONAL);
        registerCustomItem("polished_stone", CustomBlockItems.POLISHED_STONE_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("polished_stone_slab", CustomBlockItems.POLISHED_STONE_SLAB_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("polished_stone_stairs", CustomBlockItems.POLISHED_STONE_STAIRS_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("granite_bricks", CustomBlockItems.GRANITE_BRICKS_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("granite_brick_slab", CustomBlockItems.GRANITE_BRICK_SLAB_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("granite_brick_stairs", CustomBlockItems.GRANITE_BRICK_STAIRS_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("granite_tiles", CustomBlockItems.GRANITE_TILES_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("granite_tile_slab", CustomBlockItems.GRANITE_TILES_SLAB_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("granite_tile_stairs", CustomBlockItems.GRANITE_TILES_STAIRS_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("brown_mud", CustomBlockItems.BROWN_MUD_ITEM, ItemGroups.NATURAL);
        registerCustomItem("brown_mud_bricks", CustomBlockItems.BROWN_MUD_BRICKS_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("brown_mud_brick_slab", CustomBlockItems.BROWN_MUD_BRICK_SLAB_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("brown_mud_brick_stairs", CustomBlockItems.BROWN_MUD_BRICK_STAIRS_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("brown_mud_tiles", CustomBlockItems.BROWN_MUD_TILES_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("brown_mud_tile_slab", CustomBlockItems.BROWN_MUD_TILES_SLAB_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("brown_mud_tile_stairs", CustomBlockItems.BROWN_MUD_TILES_STAIRS_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("packed_mud_tiles", CustomBlockItems.PACKED_MUD_TILES_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("packed_mud_tile_slab", CustomBlockItems.PACKED_MUD_TILES_SLAB_ITEM, ItemGroups.BUILDING_BLOCKS);
        registerCustomItem("packed_mud_tile_stairs", CustomBlockItems.PACKED_MUD_TILES_STAIRS_ITEM, ItemGroups.BUILDING_BLOCKS);
    }

    public static Item registerCustomItem(String name, Item item, RegistryKey<ItemGroup> group) {
        if (group != null) ItemGroupEvents.modifyEntriesEvent(group).register(entries -> entries.add(item));
        return Registry.register(Registries.ITEM, Identifier.of(MOD_ID, name), item);
    }

    public static Item registerCustomItem(String name, Item item, RegistryKey<ItemGroup> group1, RegistryKey<ItemGroup> group2) {
        if (group1 != null) ItemGroupEvents.modifyEntriesEvent(group1).register(entries -> entries.add(item));
        if (group2 != null) ItemGroupEvents.modifyEntriesEvent(group2).register(entries -> entries.add(item));
        return Registry.register(Registries.ITEM, Identifier.of(MOD_ID, name), item);
    }
}