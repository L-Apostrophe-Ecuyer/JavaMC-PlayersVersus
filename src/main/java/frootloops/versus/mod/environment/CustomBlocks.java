package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.blocks.BrownMudBlock;
import frootloops.versus.mod.environment.blocks.SmolderingTorchBlock;
import frootloops.versus.mod.environment.blocks.SmolderingWallTorchBlock;
import net.minecraft.block.*;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;


public class CustomBlocks {

    public static final Block SMOLDERING_TORCH = new SmolderingTorchBlock(ParticleTypes.SMALL_FLAME, AbstractBlock.Settings.create().noCollision().breakInstantly().luminance((state) -> 12).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY));
    public static final Block SMOLDERING_WALL_TORCH = new SmolderingWallTorchBlock(ParticleTypes.SMALL_FLAME, AbstractBlock.Settings.create().noCollision().breakInstantly().luminance((state) -> 12).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY));
    public static final Block EXTINGUISHED_TORCH = new TorchBlock(ParticleTypes.SMOKE, AbstractBlock.Settings.create().noCollision().breakInstantly().luminance((state) -> 6).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY));
    public static final Block EXTINGUISHED_WALL_TORCH = new WallTorchBlock(ParticleTypes.SMOKE, AbstractBlock.Settings.create().noCollision().breakInstantly().luminance((state) -> 6).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY));

    public static final Block GRANITE_BRICKS = new Block(AbstractBlock.Settings.copy(Blocks.GRANITE));
    public static final Block GRANITE_BRICK_SLAB = new SlabBlock(AbstractBlock.Settings.copy(GRANITE_BRICKS));
    public static final Block GRANITE_BRICK_STAIRS = new StairsBlock(GRANITE_BRICKS.getDefaultState(), AbstractBlock.Settings.copy(GRANITE_BRICKS));

    public static final Block GRANITE_TILES = new Block(AbstractBlock.Settings.copy(Blocks.GRANITE));
    public static final Block GRANITE_TILES_SLAB = new SlabBlock(AbstractBlock.Settings.copy(GRANITE_TILES));
    public static final Block GRANITE_TILES_STAIRS = new StairsBlock(GRANITE_TILES.getDefaultState(), AbstractBlock.Settings.copy(GRANITE_TILES));

    public static final Block POLISHED_STONE = new Block(AbstractBlock.Settings.create().mapColor(MapColor.GRAY).instrument(NoteBlockInstrument.BASEDRUM).requiresTool().strength(1.5f, 6.0f));
    public static final Block POLISHED_STONE_SLAB = new SlabBlock(AbstractBlock.Settings.copy(POLISHED_STONE));
    public static final Block POLISHED_STONE_STAIRS = new StairsBlock(POLISHED_STONE.getDefaultState(), AbstractBlock.Settings.copy(POLISHED_STONE));

    //public static final Block BROWN_MUD = new MudBlock(AbstractBlock.Settings.copy(Blocks.MUD).mapColor(MapColor.BROWN).slipperiness(0.92f).velocityMultiplier(0.94f));
    public static final Block BROWN_MUD = new BrownMudBlock(AbstractBlock.Settings.copy(Blocks.MUD).mapColor(MapColor.BROWN).slipperiness(0.92f).velocityMultiplier(0.94f).dynamicBounds().solidBlock(Blocks::never));
    public static final Block BROWN_MUD_BRICKS = new Block(AbstractBlock.Settings.copy(Blocks.MUD_BRICKS).strength(1.1f, 2.5f));
    public static final Block BROWN_MUD_BRICK_SLAB = new SlabBlock(AbstractBlock.Settings.copy(BROWN_MUD_BRICKS));
    public static final Block BROWN_MUD_BRICK_STAIRS = new StairsBlock(BROWN_MUD_BRICKS.getDefaultState(), AbstractBlock.Settings.copy(BROWN_MUD_BRICKS));
    public static final Block BROWN_MUD_TILES = new Block(AbstractBlock.Settings.copy(BROWN_MUD_BRICKS));
    public static final Block BROWN_MUD_TILES_SLAB = new SlabBlock(AbstractBlock.Settings.copy(BROWN_MUD_TILES));
    public static final Block BROWN_MUD_TILES_STAIRS = new StairsBlock(BROWN_MUD_TILES.getDefaultState(), AbstractBlock.Settings.copy(BROWN_MUD_TILES));
    public static final Block PACKED_MUD_TILES = new Block(AbstractBlock.Settings.copy(Blocks.MUD_BRICKS));
    public static final Block PACKED_MUD_TILES_SLAB = new SlabBlock(AbstractBlock.Settings.copy(PACKED_MUD_TILES));
    public static final Block PACKED_MUD_TILES_STAIRS = new StairsBlock(PACKED_MUD_TILES.getDefaultState(), AbstractBlock.Settings.copy(PACKED_MUD_TILES));


    private static void registerBlock(String name, Block block) {
        Registry.register(Registries.BLOCK,  Identifier.of(VersusMod.MOD_ID, name), block);
    }


    public static void onInitialize() {
        registerBlock("smoldering_torch", SMOLDERING_TORCH);
        registerBlock("smoldering_wall_torch", SMOLDERING_WALL_TORCH);
        registerBlock("extinguished_torch", EXTINGUISHED_TORCH);
        registerBlock("extinguished_wall_torch", EXTINGUISHED_WALL_TORCH);
        registerBlock("granite_bricks", GRANITE_BRICKS);
        registerBlock("granite_brick_slab", GRANITE_BRICK_SLAB);
        registerBlock("granite_brick_stairs", GRANITE_BRICK_STAIRS);
        registerBlock("granite_tiles", GRANITE_TILES);
        registerBlock("granite_tile_slab", GRANITE_TILES_SLAB);
        registerBlock("granite_tile_stairs", GRANITE_TILES_STAIRS);
        registerBlock("polished_stone", POLISHED_STONE);
        registerBlock("polished_stone_slab", POLISHED_STONE_SLAB);
        registerBlock("polished_stone_stairs", POLISHED_STONE_STAIRS);
        registerBlock("brown_mud", BROWN_MUD);
        registerBlock("brown_mud_bricks", BROWN_MUD_BRICKS);
        registerBlock("brown_mud_brick_slab", BROWN_MUD_BRICK_SLAB);
        registerBlock("brown_mud_brick_stairs", BROWN_MUD_BRICK_STAIRS);
        registerBlock("brown_mud_tiles", BROWN_MUD_TILES);
        registerBlock("brown_mud_tile_slab", BROWN_MUD_TILES_SLAB);
        registerBlock("brown_mud_tile_stairs", BROWN_MUD_TILES_STAIRS);
        registerBlock("packed_mud_tiles", PACKED_MUD_TILES);
        registerBlock("packed_mud_tile_slab", PACKED_MUD_TILES_SLAB);
        registerBlock("packed_mud_tile_stairs", PACKED_MUD_TILES_STAIRS);
    }

}
