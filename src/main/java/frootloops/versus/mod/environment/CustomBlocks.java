package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.blocks.BrownMudBlock;
import frootloops.versus.mod.environment.blocks.SmolderingTorchBlock;
import frootloops.versus.mod.environment.blocks.SmolderingWallTorchBlock;
import frootloops.versus.mod.environment.blocks.WheatGrass;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;


public class CustomBlocks {

    public static final Block SMOLDERING_TORCH = registerBlock("smoldering_torch", new SmolderingTorchBlock(ParticleTypes.SMALL_FLAME, AbstractBlock.Settings.create().noCollision().breakInstantly().luminance((state) -> 12).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block SMOLDERING_WALL_TORCH = registerBlock("smoldering_wall_torch", new SmolderingWallTorchBlock(ParticleTypes.SMALL_FLAME, AbstractBlock.Settings.create().noCollision().breakInstantly().luminance((state) -> 12).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block EXTINGUISHED_TORCH = registerBlock("extinguished_torch", new TorchBlock(ParticleTypes.SMOKE, AbstractBlock.Settings.create().noCollision().breakInstantly().luminance((state) -> 6).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY)));
    public static final Block EXTINGUISHED_WALL_TORCH = registerBlock("extinguished_wall_torch", new WallTorchBlock(ParticleTypes.SMOKE, AbstractBlock.Settings.create().noCollision().breakInstantly().luminance((state) -> 6).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY)));

    public static final Block CORRUPTED_WART_PLANT = registerBlock("corrupted_wart", new NetherWartBlock(AbstractBlock.Settings.copy(Blocks.NETHER_WART)));
    public static final Block WITHERED_WART_PLANT = registerBlock("withered_wart", new NetherWartBlock(AbstractBlock.Settings.copy(Blocks.NETHER_WART)));

    public static final Block GRANITE_BRICKS = registerBlock("granite_bricks", new Block(AbstractBlock.Settings.copy(Blocks.GRANITE)));
    public static final Block GRANITE_BRICK_SLAB = registerBlock("granite_brick_slab", new SlabBlock(AbstractBlock.Settings.copy(GRANITE_BRICKS)));
    public static final Block GRANITE_BRICK_STAIRS = registerBlock("granite_brick_stairs", new StairsBlock(GRANITE_BRICKS.getDefaultState(), AbstractBlock.Settings.copy(GRANITE_BRICKS)));

    public static final Block GRANITE_TILES = registerBlock("granite_tiles", new Block(AbstractBlock.Settings.copy(Blocks.GRANITE)));
    public static final Block GRANITE_TILES_SLAB = registerBlock("granite_tile_slab", new SlabBlock(AbstractBlock.Settings.copy(GRANITE_TILES)));
    public static final Block GRANITE_TILES_STAIRS = registerBlock("granite_tile_stairs", new StairsBlock(GRANITE_TILES.getDefaultState(), AbstractBlock.Settings.copy(GRANITE_TILES)));

    public static final Block POLISHED_STONE = registerBlock("polished_stone", new Block(AbstractBlock.Settings.create().copy(Blocks.STONE)));
    public static final Block POLISHED_STONE_SLAB = registerBlock("polished_stone_slab", new SlabBlock(AbstractBlock.Settings.copy(POLISHED_STONE)));
    public static final Block POLISHED_STONE_STAIRS = registerBlock("polished_stone_stairs", new StairsBlock(POLISHED_STONE.getDefaultState(), AbstractBlock.Settings.copy(POLISHED_STONE)));

    public static final Block BROWN_MUD = registerBlock("brown_mud", new BrownMudBlock(AbstractBlock.Settings.copy(Blocks.MUD).mapColor(MapColor.BROWN).slipperiness(0.92f).velocityMultiplier(0.94f).dynamicBounds().solidBlock(Blocks::never)));
    public static final Block BROWN_MUD_BRICKS = registerBlock("brown_mud_bricks",new Block(AbstractBlock.Settings.copy(Blocks.MUD_BRICKS).strength(1.1f, 2.5f)));
    public static final Block BROWN_MUD_BRICK_SLAB = registerBlock("brown_mud_brick_slab",new SlabBlock(AbstractBlock.Settings.copy(BROWN_MUD_BRICKS)));
    public static final Block BROWN_MUD_BRICK_STAIRS = registerBlock("brown_mud_brick_stairs", new StairsBlock(BROWN_MUD_BRICKS.getDefaultState(), AbstractBlock.Settings.copy(BROWN_MUD_BRICKS)));
    public static final Block BROWN_MUD_TILES = registerBlock("brown_mud_tiles", new Block(AbstractBlock.Settings.copy(BROWN_MUD_BRICKS)));
    public static final Block BROWN_MUD_TILES_SLAB = registerBlock("brown_mud_tile_slab",new SlabBlock(AbstractBlock.Settings.copy(BROWN_MUD_TILES)));
    public static final Block BROWN_MUD_TILES_STAIRS =  registerBlock("brown_mud_tile_stairs", new StairsBlock(BROWN_MUD_TILES.getDefaultState(), AbstractBlock.Settings.copy(BROWN_MUD_TILES)));
    public static final Block PACKED_MUD_TILES = registerBlock("packed_mud_tiles", new Block(AbstractBlock.Settings.copy(Blocks.MUD_BRICKS)));
    public static final Block PACKED_MUD_TILES_SLAB = registerBlock("packed_mud_tile_slab", new SlabBlock(AbstractBlock.Settings.copy(PACKED_MUD_TILES)));
    public static final Block PACKED_MUD_TILES_STAIRS = registerBlock("packed_mud_tile_stairs", new StairsBlock(PACKED_MUD_TILES.getDefaultState(), AbstractBlock.Settings.copy(PACKED_MUD_TILES)));

    public static final Block WHITE_CLOVERS = registerBlock("white_clovers",new ShortPlantBlock(AbstractBlock.Settings.copy(Blocks.SHORT_GRASS)));
    public static final Block WHEAT_GRASS = registerBlock("wheat_grass", new WheatGrass(AbstractBlock.Settings.copy(Blocks.SHORT_GRASS)));
    public static final Block WILD_WHEAT = registerBlock("wild_wheat",new WheatGrass(AbstractBlock.Settings.copy(Blocks.SHORT_GRASS)));
    public static final Block CLOVERS = registerBlock("clovers",new ShortPlantBlock(AbstractBlock.Settings.copy(Blocks.SHORT_GRASS)));


    private static Block registerBlock(String name, Block block) {
        Registry.register(Registries.BLOCK,  Identifier.of(VersusMod.MOD_ID, name), block);
        return block;
    }


    public static void onInitialize() {

    }

}
