package frootloops.versus.mod.environment;

import net.minecraft.item.*;
import net.minecraft.util.math.Direction;

public abstract class CustomBlockItems {
    public static final VerticallyAttachableBlockItem SMOLDERING_TORCH_ITEM = new VerticallyAttachableBlockItem(CustomBlocks.SMOLDERING_TORCH, CustomBlocks.SMOLDERING_WALL_TORCH, new Item.Settings(), Direction.DOWN);
    public static final VerticallyAttachableBlockItem EXTINGUISHED_TORCH_ITEM = new VerticallyAttachableBlockItem(CustomBlocks.EXTINGUISHED_TORCH, CustomBlocks.EXTINGUISHED_WALL_TORCH, new Item.Settings(), Direction.DOWN);

    public static final BlockItem CORRUPTED_WART_ITEM = new AliasedBlockItem(CustomBlocks.CORRUPTED_WART_PLANT, new Item.Settings());
    public static final BlockItem WITHERED_WART_ITEM = new AliasedBlockItem(CustomBlocks.WITHERED_WART_PLANT, new Item.Settings());

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

    public static final BlockItem WHEAT_GRASS_ITEM = new BlockItem(CustomBlocks.WHEAT_GRASS, new Item.Settings());
    public static final BlockItem WILD_WHEAT_ITEM = new BlockItem(CustomBlocks.WILD_WHEAT, new Item.Settings());
    public static final BlockItem WHITE_CLOVERS_ITEM = new BlockItem(CustomBlocks.WHITE_CLOVERS, new Item.Settings());
    public static final BlockItem CLOVERS_ITEM = new BlockItem(CustomBlocks.CLOVERS, new Item.Settings());

}