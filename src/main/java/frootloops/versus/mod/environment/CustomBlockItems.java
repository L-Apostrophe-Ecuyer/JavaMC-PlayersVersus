package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

public abstract class CustomBlockItems {

    private static Item.Settings getDefaultSettings(String name) {
        return new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(VersusMod.MOD_ID, name))).useBlockPrefixedTranslationKey();
    }

    public static final VerticallyAttachableBlockItem SMOLDERING_TORCH = new VerticallyAttachableBlockItem(CustomBlocks.SMOLDERING_TORCH, CustomBlocks.SMOLDERING_WALL_TORCH,  Direction.DOWN, getDefaultSettings("smoldering_torch"));
    public static final VerticallyAttachableBlockItem EXTINGUISHED_TORCH = new VerticallyAttachableBlockItem(CustomBlocks.EXTINGUISHED_TORCH, CustomBlocks.EXTINGUISHED_WALL_TORCH, Direction.DOWN, getDefaultSettings("extinguished_torch"));

    public static final BlockItem CORRUPTED_WART = new BlockItem(CustomBlocks.CORRUPTED_WART_PLANT, getDefaultSettings("corrupted_wart"));
    public static final BlockItem WITHERED_WART = new BlockItem(CustomBlocks.WITHERED_WART_PLANT, getDefaultSettings("withered_wart"));

    public static final BlockItem POLISHED_STONE = new BlockItem(CustomBlocks.POLISHED_STONE, getDefaultSettings("polished_stone"));
    public static final BlockItem POLISHED_STONE_SLAB = new BlockItem(CustomBlocks.POLISHED_STONE_SLAB, getDefaultSettings("polished_stone_slab"));
    public static final BlockItem POLISHED_STONE_STAIRS = new BlockItem(CustomBlocks.POLISHED_STONE_STAIRS, getDefaultSettings("polished_stone_stairs"));

    public static final BlockItem DRIPSTONE_SLAB = new BlockItem(CustomBlocks.DRIPSTONE_SLAB, getDefaultSettings("dripstone_slab"));
    public static final BlockItem DRIPSTONE_STAIRS = new BlockItem(CustomBlocks.DRIPSTONE_STAIRS, getDefaultSettings("dripstone_stairs"));
    public static final BlockItem DRIPSTONE_WALL = new BlockItem(CustomBlocks.DRIPSTONE_WALL, getDefaultSettings("dripstone_wall"));
    public static final BlockItem POLISHED_DRIPSTONE = new BlockItem(CustomBlocks.POLISHED_DRIPSTONE, getDefaultSettings("polished_dripstone"));
    public static final BlockItem POLISHED_DRIPSTONE_SLAB = new BlockItem(CustomBlocks.POLISHED_DRIPSTONE_SLAB, getDefaultSettings("polished_dripstone_slab"));
    public static final BlockItem POLISHED_DRIPSTONE_STAIRS = new BlockItem(CustomBlocks.POLISHED_DRIPSTONE_STAIRS, getDefaultSettings("polished_dripstone_stairs"));
    public static final BlockItem DRIPSTONE_PILLAR = new BlockItem(CustomBlocks.DRIPSTONE_PILLAR, getDefaultSettings("dripstone_pillar"));
    public static final BlockItem DRIPSTONE_BRICKS = new BlockItem(CustomBlocks.DRIPSTONE_BRICKS, getDefaultSettings("dripstone_bricks"));
    public static final BlockItem DRIPSTONE_BRICK_SLAB = new BlockItem(CustomBlocks.DRIPSTONE_BRICK_SLAB, getDefaultSettings("dripstone_brick_slab"));
    public static final BlockItem DRIPSTONE_BRICK_STAIRS = new BlockItem(CustomBlocks.DRIPSTONE_BRICK_STAIRS, getDefaultSettings("dripstone_brick_stairs"));
    public static final BlockItem DRIPSTONE_BRICK_WALL = new BlockItem(CustomBlocks.DRIPSTONE_BRICK_WALL, getDefaultSettings("dripstone_brick_wall"));


    public static final BlockItem BROWN_MUD = new BlockItem(CustomBlocks.BROWN_MUD, getDefaultSettings("brown_mud"));
    public static final BlockItem BROWN_MUD_BRICKS = new BlockItem(CustomBlocks.BROWN_MUD_BRICKS, getDefaultSettings("brown_mud_bricks"));
    public static final BlockItem BROWN_MUD_BRICK_SLAB = new BlockItem(CustomBlocks.BROWN_MUD_BRICK_SLAB, getDefaultSettings("brown_mud_brick_slab"));
    public static final BlockItem BROWN_MUD_BRICK_STAIRS = new BlockItem(CustomBlocks.BROWN_MUD_BRICK_STAIRS, getDefaultSettings("brown_mud_brick_stairs"));
    public static final BlockItem BROWN_MUD_BRICK_WALL = new BlockItem(CustomBlocks.BROWN_MUD_BRICK_WALL, getDefaultSettings("brown_mud_brick_wall"));
    public static final BlockItem MUDSTONE = new BlockItem(CustomBlocks.MUDSTONE, getDefaultSettings("mudstone"));
    public static final BlockItem MUDSTONE_BRICKS = new BlockItem(CustomBlocks.MUDSTONE_BRICKS, getDefaultSettings("mudstone_bricks"));
    public static final BlockItem MUDSTONE_BRICK_SLAB = new BlockItem(CustomBlocks.MUDSTONE_BRICK_SLAB, getDefaultSettings("mudstone_brick_slab"));
    public static final BlockItem MUDSTONE_BRICK_STAIRS = new BlockItem(CustomBlocks.MUDSTONE_BRICK_STAIRS, getDefaultSettings("mudstone_brick_stairs"));
    public static final BlockItem MUDSTONE_BRICK_WALL = new BlockItem(CustomBlocks.MUDSTONE_BRICK_WALL, getDefaultSettings("mudstone_brick_wall"));

    public static final BlockItem TERRACOTTA_BRICKS = new BlockItem(CustomBlocks.TERRACOTTA_BRICKS, getDefaultSettings("terracotta_bricks"));
    public static final BlockItem TERRACOTTA_BRICK_SLAB = new BlockItem(CustomBlocks.TERRACOTTA_BRICK_SLAB, getDefaultSettings("terracotta_brick_slab"));
    public static final BlockItem TERRACOTTA_BRICK_STAIRS = new BlockItem(CustomBlocks.TERRACOTTA_BRICK_STAIRS, getDefaultSettings("terracotta_brick_stairs"));
    public static final BlockItem TERRACOTTA_BRICK_WALL = new BlockItem(CustomBlocks.TERRACOTTA_BRICK_WALL, getDefaultSettings("terracotta_brick_wall"));
    public static final BlockItem TERRACOTTA_TILES = new BlockItem(CustomBlocks.TERRACOTTA_TILES, getDefaultSettings("terracotta_tiles"));
    public static final BlockItem TERRACOTTA_TILE_SLAB = new BlockItem(CustomBlocks.TERRACOTTA_TILE_SLAB, getDefaultSettings("terracotta_tile_slab"));
    public static final BlockItem TERRACOTTA_TILE_STAIRS = new BlockItem(CustomBlocks.TERRACOTTA_TILE_STAIRS, getDefaultSettings("terracotta_tile_stairs"));
    public static final BlockItem CHISELED_TERRACOTTA = new BlockItem(CustomBlocks.CHISELED_TERRACOTTA, getDefaultSettings("chiseled_terracotta"));

    public static final BlockItem CLAY_BLOCK = new BlockItem(CustomBlocks.CLAY, getDefaultSettings("clay_block"));

    public static final BlockItem WHEAT_GRASS = new BlockItem(CustomBlocks.WHEAT_GRASS, getDefaultSettings("wheat_grass"));
    public static final BlockItem WILD_WHEAT = new BlockItem(CustomBlocks.WILD_WHEAT, getDefaultSettings("wild_wheat"));
    public static final BlockItem CLOVERS = new BlockItem(CustomBlocks.CLOVERS, getDefaultSettings("clovers"));


    public static final BlockItem INFESTED_DARK_OAK_WOOD = new BlockItem(CustomBlocks.INFESTED_DARK_OAK_WOOD, getDefaultSettings("infested_dark_oak_wood"));
    public static final BlockItem INFESTED_OAK_WOOD = new BlockItem(CustomBlocks.INFESTED_OAK_WOOD, getDefaultSettings("infested_oak_wood"));

}