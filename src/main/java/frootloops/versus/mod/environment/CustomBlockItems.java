package frootloops.versus.mod.environment;

import net.minecraft.item.*;
import net.minecraft.util.math.Direction;

public abstract class CustomBlockItems {

    public static final VerticallyAttachableBlockItem SMOLDERING_TORCH = new VerticallyAttachableBlockItem(CustomBlocks.SMOLDERING_TORCH, CustomBlocks.SMOLDERING_WALL_TORCH, new Item.Settings(), Direction.DOWN);
    public static final VerticallyAttachableBlockItem EXTINGUISHED_TORCH = new VerticallyAttachableBlockItem(CustomBlocks.EXTINGUISHED_TORCH, CustomBlocks.EXTINGUISHED_WALL_TORCH, new Item.Settings(), Direction.DOWN);

    public static final BlockItem CORRUPTED_WART = new AliasedBlockItem(CustomBlocks.CORRUPTED_WART_PLANT, new Item.Settings());
    public static final BlockItem WITHERED_WART = new AliasedBlockItem(CustomBlocks.WITHERED_WART_PLANT, new Item.Settings());

    public static final BlockItem POLISHED_STONE = new BlockItem(CustomBlocks.POLISHED_STONE, new Item.Settings());
    public static final BlockItem POLISHED_STONE_SLAB = new BlockItem(CustomBlocks.POLISHED_STONE_SLAB, new Item.Settings());
    public static final BlockItem POLISHED_STONE_STAIRS = new BlockItem(CustomBlocks.POLISHED_STONE_STAIRS, new Item.Settings());

    public static final BlockItem DRIPSTONE_SLAB = new BlockItem(CustomBlocks.DRIPSTONE_SLAB, new Item.Settings());
    public static final BlockItem DRIPSTONE_STAIRS = new BlockItem(CustomBlocks.DRIPSTONE_STAIRS, new Item.Settings());
    public static final BlockItem DRIPSTONE_WALL = new BlockItem(CustomBlocks.DRIPSTONE_WALL, new Item.Settings());
    public static final BlockItem POLISHED_DRIPSTONE = new BlockItem(CustomBlocks.POLISHED_DRIPSTONE, new Item.Settings());
    public static final BlockItem POLISHED_DRIPSTONE_SLAB = new BlockItem(CustomBlocks.POLISHED_DRIPSTONE_SLAB, new Item.Settings());
    public static final BlockItem POLISHED_DRIPSTONE_STAIRS = new BlockItem(CustomBlocks.POLISHED_DRIPSTONE_STAIRS, new Item.Settings());
    public static final BlockItem DRIPSTONE_PILLAR = new BlockItem(CustomBlocks.DRIPSTONE_PILLAR, new Item.Settings());
    public static final BlockItem DRIPSTONE_BRICKS = new BlockItem(CustomBlocks.DRIPSTONE_BRICKS, new Item.Settings());
    public static final BlockItem DRIPSTONE_BRICK_SLAB = new BlockItem(CustomBlocks.DRIPSTONE_BRICK_SLAB, new Item.Settings());
    public static final BlockItem DRIPSTONE_BRICK_STAIRS = new BlockItem(CustomBlocks.DRIPSTONE_BRICK_STAIRS, new Item.Settings());
    public static final BlockItem DRIPSTONE_BRICK_WALL = new BlockItem(CustomBlocks.DRIPSTONE_BRICK_WALL, new Item.Settings());


    public static final BlockItem BROWN_MUD = new BlockItem(CustomBlocks.BROWN_MUD, new Item.Settings());
    public static final BlockItem BROWN_MUD_BRICKS = new BlockItem(CustomBlocks.BROWN_MUD_BRICKS, new Item.Settings());
    public static final BlockItem BROWN_MUD_BRICK_SLAB = new BlockItem(CustomBlocks.BROWN_MUD_BRICK_SLAB, new Item.Settings());
    public static final BlockItem BROWN_MUD_BRICK_STAIRS = new BlockItem(CustomBlocks.BROWN_MUD_BRICK_STAIRS, new Item.Settings());
    public static final BlockItem BROWN_MUD_BRICK_WALL = new BlockItem(CustomBlocks.BROWN_MUD_BRICK_WALL, new Item.Settings());
    public static final BlockItem MUDSTONE = new BlockItem(CustomBlocks.MUDSTONE, new Item.Settings());
    public static final BlockItem MUDSTONE_BRICKS = new BlockItem(CustomBlocks.MUDSTONE_BRICKS, new Item.Settings());
    public static final BlockItem MUDSTONE_BRICK_SLAB = new BlockItem(CustomBlocks.MUDSTONE_BRICK_SLAB, new Item.Settings());
    public static final BlockItem MUDSTONE_BRICK_STAIRS = new BlockItem(CustomBlocks.MUDSTONE_BRICK_STAIRS, new Item.Settings());
    public static final BlockItem MUDSTONE_BRICK_WALL = new BlockItem(CustomBlocks.MUDSTONE_BRICK_WALL, new Item.Settings());

    public static final BlockItem TERRACOTTA_BRICKS = new BlockItem(CustomBlocks.TERRACOTTA_BRICKS, new Item.Settings());
    public static final BlockItem TERRACOTTA_BRICK_SLAB = new BlockItem(CustomBlocks.TERRACOTTA_BRICK_SLAB, new Item.Settings());
    public static final BlockItem TERRACOTTA_BRICK_STAIRS = new BlockItem(CustomBlocks.TERRACOTTA_BRICK_STAIRS, new Item.Settings());
    public static final BlockItem TERRACOTTA_BRICK_WALL = new BlockItem(CustomBlocks.TERRACOTTA_BRICK_WALL, new Item.Settings());
    public static final BlockItem TERRACOTTA_TILES = new BlockItem(CustomBlocks.TERRACOTTA_TILES, new Item.Settings());
    public static final BlockItem TERRACOTTA_TILE_SLAB = new BlockItem(CustomBlocks.TERRACOTTA_TILE_SLAB, new Item.Settings());
    public static final BlockItem TERRACOTTA_TILE_STAIRS = new BlockItem(CustomBlocks.TERRACOTTA_TILE_STAIRS, new Item.Settings());
    public static final BlockItem CHISELED_TERRACOTTA = new BlockItem(CustomBlocks.CHISELED_TERRACOTTA, new Item.Settings());

    public static final BlockItem CLAY_BLOCK = new BlockItem(CustomBlocks.CLAY, new Item.Settings());

    public static final BlockItem WHEAT_GRASS = new BlockItem(CustomBlocks.WHEAT_GRASS, new Item.Settings());
    public static final BlockItem WILD_WHEAT = new BlockItem(CustomBlocks.WILD_WHEAT, new Item.Settings());
    public static final BlockItem CLOVERS = new BlockItem(CustomBlocks.CLOVERS, new Item.Settings());


    public static final BlockItem INFESTED_DARK_OAK_WOOD = new BlockItem(CustomBlocks.INFESTED_DARK_OAK_WOOD, new Item.Settings());
    public static final BlockItem INFESTED_OAK_WOOD = new BlockItem(CustomBlocks.INFESTED_OAK_WOOD, new Item.Settings());

}