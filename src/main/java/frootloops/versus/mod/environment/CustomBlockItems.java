package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.blocks.LadderBlockItem;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import static frootloops.versus.mod.items.RegisteringCustomItems.getItemSettings;

public abstract class CustomBlockItems {

    private static Item.Settings getBlockSettings(String name) {
        return new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(VersusMod.MOD_ID, name))).useBlockPrefixedTranslationKey();
    }

    public static final LadderBlockItem LADDER = new LadderBlockItem((LadderBlock)Blocks.LADDER, getBlockSettings("ladder"));

    public static final VerticallyAttachableBlockItem SMOLDERING_TORCH = new VerticallyAttachableBlockItem(CustomBlocks.SMOLDERING_TORCH, CustomBlocks.SMOLDERING_WALL_TORCH,  Direction.DOWN, getBlockSettings("smoldering_torch"));
    public static final VerticallyAttachableBlockItem EXTINGUISHED_TORCH = new VerticallyAttachableBlockItem(CustomBlocks.EXTINGUISHED_TORCH, CustomBlocks.EXTINGUISHED_WALL_TORCH, Direction.DOWN, getBlockSettings("extinguished_torch"));

    public static final BlockItem CORRUPTED_WART = new BlockItem(CustomBlocks.CORRUPTED_WART_PLANT, getBlockSettings("corrupted_wart"));
    public static final BlockItem WITHERED_WART = new BlockItem(CustomBlocks.WITHERED_WART_PLANT, getBlockSettings("withered_wart"));


    public static final BlockItem CUT_LAPIS = new BlockItem(CustomBlocks.CUT_LAPIS, getBlockSettings("cut_lapis"));
    public static final BlockItem CUT_LAPIS_SLAB = new BlockItem(CustomBlocks.CUT_LAPIS_SLAB, getBlockSettings("cut_lapis_slab"));
    public static final BlockItem CUT_LAPIS_STAIRS = new BlockItem(CustomBlocks.CUT_LAPIS_STAIRS, getBlockSettings("cut_lapis_stairs"));

    public static final BlockItem POLISHED_STONE = new BlockItem(CustomBlocks.POLISHED_STONE, getBlockSettings("polished_stone"));
    public static final BlockItem POLISHED_STONE_SLAB = new BlockItem(CustomBlocks.POLISHED_STONE_SLAB, getBlockSettings("polished_stone_slab"));
    public static final BlockItem POLISHED_STONE_STAIRS = new BlockItem(CustomBlocks.POLISHED_STONE_STAIRS, getBlockSettings("polished_stone_stairs"));

    public static final BlockItem DRIPSTONE_SLAB = new BlockItem(CustomBlocks.DRIPSTONE_SLAB, getBlockSettings("dripstone_slab"));
    public static final BlockItem DRIPSTONE_STAIRS = new BlockItem(CustomBlocks.DRIPSTONE_STAIRS, getBlockSettings("dripstone_stairs"));
    public static final BlockItem DRIPSTONE_WALL = new BlockItem(CustomBlocks.DRIPSTONE_WALL, getBlockSettings("dripstone_wall"));
    public static final BlockItem POLISHED_DRIPSTONE = new BlockItem(CustomBlocks.POLISHED_DRIPSTONE, getBlockSettings("polished_dripstone"));
    public static final BlockItem POLISHED_DRIPSTONE_SLAB = new BlockItem(CustomBlocks.POLISHED_DRIPSTONE_SLAB, getBlockSettings("polished_dripstone_slab"));
    public static final BlockItem POLISHED_DRIPSTONE_STAIRS = new BlockItem(CustomBlocks.POLISHED_DRIPSTONE_STAIRS, getBlockSettings("polished_dripstone_stairs"));
    public static final BlockItem DRIPSTONE_PILLAR = new BlockItem(CustomBlocks.DRIPSTONE_PILLAR, getBlockSettings("dripstone_pillar"));
    public static final BlockItem DRIPSTONE_BRICKS = new BlockItem(CustomBlocks.DRIPSTONE_BRICKS, getBlockSettings("dripstone_bricks"));
    public static final BlockItem DRIPSTONE_BRICK_SLAB = new BlockItem(CustomBlocks.DRIPSTONE_BRICK_SLAB, getBlockSettings("dripstone_brick_slab"));
    public static final BlockItem DRIPSTONE_BRICK_STAIRS = new BlockItem(CustomBlocks.DRIPSTONE_BRICK_STAIRS, getBlockSettings("dripstone_brick_stairs"));
    public static final BlockItem DRIPSTONE_BRICK_WALL = new BlockItem(CustomBlocks.DRIPSTONE_BRICK_WALL, getBlockSettings("dripstone_brick_wall"));


    public static final BlockItem GRAY_CLAY = new BlockItem(CustomBlocks.GRAY_CLAY, getBlockSettings("gray_clay"));
    public static final BlockItem GRAY_MUD = new BlockItem(CustomBlocks.BROWN_MUD, getBlockSettings("gray_mud"));

    public static final Item BROWN_CLAY_BALL =  new Item(getItemSettings("brown_clay_ball"));
    public static final BlockItem BROWN_CLAY = new BlockItem(CustomBlocks.BROWN_CLAY, getBlockSettings("brown_clay"));
    public static final BlockItem BROWN_MUD = new BlockItem(CustomBlocks.BROWN_MUD, getBlockSettings("brown_mud"));
    public static final BlockItem BROWN_CLAY_BRICKS = new BlockItem(CustomBlocks.BROWN_CLAY_BRICKS, getBlockSettings("brown_clay_bricks"));
    public static final BlockItem BROWN_CLAY_BRICK_SLAB = new BlockItem(CustomBlocks.BROWN_CLAY_BRICK_SLAB, getBlockSettings("brown_clay_brick_slab"));
    public static final BlockItem BROWN_CLAY_BRICK_STAIRS = new BlockItem(CustomBlocks.BROWN_CLAY_BRICK_STAIRS, getBlockSettings("brown_clay_brick_stairs"));
    public static final BlockItem BROWN_CLAY_BRICK_WALL = new BlockItem(CustomBlocks.BROWN_CLAY_BRICK_WALL, getBlockSettings("brown_clay_brick_wall"));
    public static final BlockItem BROWN_MUD_BRICKS = new BlockItem(CustomBlocks.BROWN_MUD_BRICKS, getBlockSettings("brown_mud_bricks"));
    public static final BlockItem BROWN_MUD_BRICK_SLAB = new BlockItem(CustomBlocks.BROWN_MUD_BRICK_SLAB, getBlockSettings("brown_mud_brick_slab"));
    public static final BlockItem BROWN_MUD_BRICK_STAIRS = new BlockItem(CustomBlocks.BROWN_MUD_BRICK_STAIRS, getBlockSettings("brown_mud_brick_stairs"));
    public static final BlockItem BROWN_MUD_BRICK_WALL = new BlockItem(CustomBlocks.BROWN_MUD_BRICK_WALL, getBlockSettings("brown_mud_brick_wall"));

    public static final BlockItem TERRACOTTA_BRICKS = new BlockItem(CustomBlocks.TERRACOTTA_BRICKS, getBlockSettings("terracotta_bricks"));
    public static final BlockItem TERRACOTTA_BRICK_SLAB = new BlockItem(CustomBlocks.TERRACOTTA_BRICK_SLAB, getBlockSettings("terracotta_brick_slab"));
    public static final BlockItem TERRACOTTA_BRICK_STAIRS = new BlockItem(CustomBlocks.TERRACOTTA_BRICK_STAIRS, getBlockSettings("terracotta_brick_stairs"));
    public static final BlockItem TERRACOTTA_BRICK_WALL = new BlockItem(CustomBlocks.TERRACOTTA_BRICK_WALL, getBlockSettings("terracotta_brick_wall"));
    public static final BlockItem TERRACOTTA_TILES = new BlockItem(CustomBlocks.TERRACOTTA_TILES, getBlockSettings("terracotta_tiles"));
    public static final BlockItem TERRACOTTA_TILE_SLAB = new BlockItem(CustomBlocks.TERRACOTTA_TILE_SLAB, getBlockSettings("terracotta_tile_slab"));
    public static final BlockItem TERRACOTTA_TILE_STAIRS = new BlockItem(CustomBlocks.TERRACOTTA_TILE_STAIRS, getBlockSettings("terracotta_tile_stairs"));
    public static final BlockItem CHISELED_TERRACOTTA = new BlockItem(CustomBlocks.CHISELED_TERRACOTTA, getBlockSettings("chiseled_terracotta"));

    public static final BlockItem WHEAT_GRASS = new BlockItem(CustomBlocks.WHEAT_GRASS, getBlockSettings("wheat_grass"));
    public static final BlockItem WILD_WHEAT = new BlockItem(CustomBlocks.WILD_WHEAT, getBlockSettings("wild_wheat"));
    public static final BlockItem CLOVERS = new BlockItem(CustomBlocks.CLOVERS, getBlockSettings("clovers"));

    public static final BlockItem CREEPER_SPORE_BLOSSOM = new BlockItem(CustomBlocks.CREEPER_SPORE_BLOSSOM, getBlockSettings("creeper_spore_blossom"));

}