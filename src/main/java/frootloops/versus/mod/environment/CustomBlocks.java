package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.blocks.*;
import frootloops.versus.mod.environment.blocks.clays.*;
import frootloops.versus.mod.items.brewing.CustomStatusEffects;
import frootloops.versus.mod.items.brewing.effects.BuoyancyStatusEffect;
import frootloops.versus.mod.items.brewing.effects.LargenessStatusEffect;
import frootloops.versus.mod.items.brewing.effects.SmallnessStatusEffect;
import frootloops.versus.mod.items.brewing.effects.VulnerabilityStatusEffect;
import net.minecraft.block.*;
import net.minecraft.block.enums.NoteBlockInstrument;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;


public class CustomBlocks {

    public static Block SMOLDERING_TORCH;
    public static Block SMOLDERING_WALL_TORCH;
    public static Block EXTINGUISHED_TORCH;
    public static Block EXTINGUISHED_WALL_TORCH;

    public static Block CORRUPTED_WART_PLANT;
    public static Block WITHERED_WART_PLANT;

    public static Block DRIPSTONE_SLAB, DRIPSTONE_STAIRS, DRIPSTONE_WALL;
    public static Block POLISHED_DRIPSTONE, POLISHED_DRIPSTONE_SLAB, POLISHED_DRIPSTONE_STAIRS, POLISHED_DRIPSTONE_WALL, DRIPSTONE_PILLAR;
    public static Block DRIPSTONE_BRICKS, DRIPSTONE_BRICK_SLAB, DRIPSTONE_BRICK_STAIRS, DRIPSTONE_BRICK_WALL;

    public static Block POLISHED_STONE, POLISHED_STONE_SLAB, POLISHED_STONE_STAIRS;

    public static Block TERRACOTTA_BRICKS, TERRACOTTA_BRICK_SLAB, TERRACOTTA_BRICK_STAIRS, TERRACOTTA_BRICK_WALL;
    public static Block TERRACOTTA_TILES, TERRACOTTA_TILE_SLAB, TERRACOTTA_TILE_STAIRS, TERRACOTTA_TILE_WALL, CHISELED_TERRACOTTA;
    public static Block CLAY;
    public static Block MUDSTONE;
    public static Block MUDSTONE_BRICKS, MUDSTONE_BRICK_SLAB, MUDSTONE_BRICK_STAIRS, MUDSTONE_BRICK_WALL;

    public static Block BROWN_MUD, BROWN_MUD_BRICKS, BROWN_MUD_BRICK_SLAB, BROWN_MUD_BRICK_STAIRS, BROWN_MUD_BRICK_WALL;

    public static Block WHEAT_GRASS;
    public static Block WILD_WHEAT;
    public static Block CLOVERS;


    public static Block DEATHLY_BILE, CORRUPTED_BILE, HARMFUL_BILE, HEALTHY_BILE, REGENERATION_BILE, WITHERING_BILE, MINING_SPEED_BILE, MINING_FATIGUE_BILE, TOUGHNESS_BILE, VISION_BILE, DARKNESS_BILE, LEAPING_BILE, SLOW_FALL_BILE, SPEED_BILE, SLOWNESS_BILE, BREATH_BILE;
    public static Block INVISIBILITY_BILE, GLOWING_BILE, WEAKNESS_BILE, STRENGTH_BILE, WIND_BILE, FIRE_BILE, OOZE_BILE, INFESTATION_BILE, POISON_BILE, WEAVING_BILE, LUCK_BILE, UNLUCK_BILE;
    public static Block LARGENESS_BILE,SMALLNESS_BILE, VULNERABILITY_BILE, BUOYANCY_BILE;



    private static Block registerBlock(String name, Block block) {
        Registry.register(Registries.BLOCK,  Identifier.of(VersusMod.MOD_ID, name), block);
        return block;
    }


    public static void onInitialize() {

        SMOLDERING_TORCH = registerBlock("smoldering_torch", new SmolderingTorchBlock(ParticleTypes.SMALL_FLAME, AbstractBlock.Settings.create().noCollision().breakInstantly().luminance((state) -> 12).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY)));
        SMOLDERING_WALL_TORCH = registerBlock("smoldering_wall_torch", new SmolderingWallTorchBlock(ParticleTypes.SMALL_FLAME, AbstractBlock.Settings.create().noCollision().breakInstantly().luminance((state) -> 12).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY)));
        EXTINGUISHED_TORCH = registerBlock("extinguished_torch", new TorchBlock(ParticleTypes.SMOKE, AbstractBlock.Settings.create().noCollision().breakInstantly().luminance((state) -> 6).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY)));
        EXTINGUISHED_WALL_TORCH = registerBlock("extinguished_wall_torch", new WallTorchBlock(ParticleTypes.SMOKE, AbstractBlock.Settings.create().noCollision().breakInstantly().luminance((state) -> 6).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY)));

        CORRUPTED_WART_PLANT = registerBlock("corrupted_wart", new NetherWartBlock(AbstractBlock.Settings.copy(Blocks.NETHER_WART)));
        WITHERED_WART_PLANT = registerBlock("withered_wart", new NetherWartBlock(AbstractBlock.Settings.copy(Blocks.NETHER_WART)));

        DRIPSTONE_SLAB = registerBlock("dripstone_slab", new SlabBlock(AbstractBlock.Settings.create().mapColor(MapColor.TERRACOTTA_BROWN).instrument(NoteBlockInstrument.BASEDRUM).sounds(BlockSoundGroup.DRIPSTONE_BLOCK).requiresTool().strength(1.5F, 1.0F)));
        DRIPSTONE_STAIRS = registerBlock("dripstone_stairs", new StairsBlock(Blocks.DRIPSTONE_BLOCK.getDefaultState(), AbstractBlock.Settings.create().mapColor(MapColor.TERRACOTTA_BROWN).instrument(NoteBlockInstrument.BASEDRUM).sounds(BlockSoundGroup.DRIPSTONE_BLOCK).requiresTool().strength(1.5F, 1.0F)));
        DRIPSTONE_WALL = registerBlock("dripstone_wall", new WallBlock(AbstractBlock.Settings.create().mapColor(MapColor.TERRACOTTA_BROWN).instrument(NoteBlockInstrument.BASEDRUM).sounds(BlockSoundGroup.DRIPSTONE_BLOCK).requiresTool().strength(1.5F, 1.0F)));
        POLISHED_DRIPSTONE = registerBlock("polished_dripstone", new Block(AbstractBlock.Settings.create().mapColor(MapColor.TERRACOTTA_BROWN).instrument(NoteBlockInstrument.BASEDRUM).sounds(BlockSoundGroup.DRIPSTONE_BLOCK).requiresTool().strength(1.5F, 1.0F)));
        POLISHED_DRIPSTONE_SLAB = registerBlock("polished_dripstone_slab", new SlabBlock(AbstractBlock.Settings.copy(POLISHED_DRIPSTONE)));
        POLISHED_DRIPSTONE_STAIRS = registerBlock("polished_dripstone_stairs", new StairsBlock(POLISHED_DRIPSTONE.getDefaultState(), AbstractBlock.Settings.copy(POLISHED_DRIPSTONE)));
        POLISHED_DRIPSTONE_WALL = registerBlock("polished_dripstone_wall", new WallBlock(AbstractBlock.Settings.copy(POLISHED_DRIPSTONE)));
        DRIPSTONE_PILLAR = registerBlock("dripstone_pillar", new Block(AbstractBlock.Settings.copy(POLISHED_DRIPSTONE)));
        DRIPSTONE_BRICKS = registerBlock("dripstone_bricks", new Block(AbstractBlock.Settings.copy(POLISHED_DRIPSTONE)));
        DRIPSTONE_BRICK_SLAB = registerBlock("dripstone_brick_slab", new SlabBlock(AbstractBlock.Settings.copy(DRIPSTONE_BRICKS)));
        DRIPSTONE_BRICK_STAIRS = registerBlock("dripstone_brick_stairs", new StairsBlock(DRIPSTONE_BRICKS.getDefaultState(), AbstractBlock.Settings.copy(DRIPSTONE_BRICKS)));
        DRIPSTONE_BRICK_WALL = registerBlock("dripstone_brick_wall", new WallBlock(AbstractBlock.Settings.copy(DRIPSTONE_BRICKS)));

        POLISHED_STONE = registerBlock("polished_stone", new Block(AbstractBlock.Settings.create().copy(Blocks.STONE)));
        POLISHED_STONE_SLAB = registerBlock("polished_stone_slab", new SlabBlock(AbstractBlock.Settings.copy(POLISHED_STONE)));
        POLISHED_STONE_STAIRS = registerBlock("polished_stone_stairs", new StairsBlock(POLISHED_STONE.getDefaultState(), AbstractBlock.Settings.copy(POLISHED_STONE)));

        TERRACOTTA_BRICKS = registerBlock("terracotta_bricks", new Block(AbstractBlock.Settings.copy(Blocks.TERRACOTTA)));
        TERRACOTTA_BRICK_SLAB = registerBlock("terracotta_brick_slab", new SlabBlock(AbstractBlock.Settings.copy(TERRACOTTA_BRICKS)));
        TERRACOTTA_BRICK_STAIRS = registerBlock("terracotta_brick_stairs", new StairsBlock(TERRACOTTA_BRICKS.getDefaultState(), AbstractBlock.Settings.copy(TERRACOTTA_BRICKS)));
        TERRACOTTA_BRICK_WALL = registerBlock("terracotta_brick_wall", new WallBlock(AbstractBlock.Settings.copy(TERRACOTTA_BRICKS)));
        TERRACOTTA_TILES = registerBlock("terracotta_tiles", new Block(AbstractBlock.Settings.copy(Blocks.TERRACOTTA)));
        TERRACOTTA_TILE_SLAB = registerBlock("terracotta_tile_slab", new SlabBlock(AbstractBlock.Settings.copy(TERRACOTTA_TILES)));
        TERRACOTTA_TILE_STAIRS = registerBlock("terracotta_tile_stairs", new StairsBlock(TERRACOTTA_TILES.getDefaultState(), AbstractBlock.Settings.copy(TERRACOTTA_TILES)));
        TERRACOTTA_TILE_WALL = registerBlock("terracotta_tile_wall", new WallBlock(AbstractBlock.Settings.copy(TERRACOTTA_TILES)));
        CHISELED_TERRACOTTA = registerBlock("chiseled_terracotta", new Block(AbstractBlock.Settings.copy(Blocks.TERRACOTTA)));
        CLAY = registerBlock("clay", new MoistBlock(AbstractBlock.Settings.copy(Blocks.CLAY).strength(1.1f, 2.5f).slipperiness(0.92f).velocityMultiplier(0.94f).sounds(BlockSoundGroup.MUD), Blocks.TERRACOTTA));

        MUDSTONE = registerBlock("mudstone", new MoistBlock(AbstractBlock.Settings.copy(Blocks.PACKED_MUD), Blocks.DRIPSTONE_BLOCK, Blocks.DRIPSTONE_BLOCK));
        MUDSTONE_BRICKS = registerBlock("mudstone_bricks",new MoistBlock(AbstractBlock.Settings.copy(MUDSTONE), DRIPSTONE_BRICKS, DRIPSTONE_BRICKS));
        MUDSTONE_BRICK_SLAB = registerBlock("mudstone_brick_slab",new MoistSlabBlock(AbstractBlock.Settings.copy(MUDSTONE), DRIPSTONE_BRICK_SLAB, DRIPSTONE_BRICK_STAIRS));
        MUDSTONE_BRICK_STAIRS = registerBlock("mudstone_brick_stairs", new MoistStairsBlock(MUDSTONE.getDefaultState(), TERRACOTTA_BRICK_STAIRS, TERRACOTTA_BRICK_STAIRS));
        MUDSTONE_BRICK_WALL = registerBlock("mudstone_brick_wall",new MoistWallBlock(AbstractBlock.Settings.copy(MUDSTONE), TERRACOTTA_BRICK_SLAB, TERRACOTTA_BRICK_SLAB));
        BROWN_MUD = registerBlock("brown_mud", new BrownMudBlock(AbstractBlock.Settings.copy(Blocks.DIRT).mapColor(MapColor.BROWN).dynamicBounds().allowsSpawning(Blocks::never).blockVision(Blocks::always).suffocates(Blocks::never).sounds(BlockSoundGroup.MUD)));
        BROWN_MUD_BRICKS = registerBlock("brown_mud_bricks",new MoistBlock(AbstractBlock.Settings.copy(BROWN_MUD).strength(1.1f, 2.5f).slipperiness(0.92f).velocityMultiplier(0.94f).sounds(BlockSoundGroup.MUD), MUDSTONE_BRICKS));
        BROWN_MUD_BRICK_SLAB = registerBlock("brown_mud_brick_slab",new MoistSlabBlock(AbstractBlock.Settings.copy(BROWN_MUD_BRICKS), MUDSTONE_BRICK_SLAB));
        BROWN_MUD_BRICK_STAIRS = registerBlock("brown_mud_brick_stairs", new MoistStairsBlock(BROWN_MUD_BRICKS.getDefaultState(), MUDSTONE_BRICK_SLAB));
        BROWN_MUD_BRICK_WALL = registerBlock("brown_mud_brick_wall",new MoistWallBlock(AbstractBlock.Settings.copy(BROWN_MUD_BRICKS), MUDSTONE_BRICK_WALL));
        ((MoistBlock)MUDSTONE).wetterVersion = BROWN_MUD;
        ((MoistBlock)MUDSTONE_BRICKS).wetterVersion = BROWN_MUD_BRICKS;
        ((MoistSlabBlock)MUDSTONE_BRICK_SLAB).wetterVersion = BROWN_MUD_BRICK_SLAB;
        ((MoistStairsBlock)MUDSTONE_BRICK_STAIRS).wetterVersion = BROWN_MUD_BRICK_STAIRS;
        ((MoistWallBlock)BROWN_MUD_BRICK_WALL).wetterVersion = BROWN_MUD_BRICK_WALL;


        WHEAT_GRASS = registerBlock("wheat_grass", new WheatGrassBlock(AbstractBlock.Settings.copy(Blocks.SHORT_GRASS)));
        WILD_WHEAT = registerBlock("wild_wheat",new WheatGrassBlock(AbstractBlock.Settings.copy(Blocks.SHORT_GRASS)));
        CLOVERS = registerBlock("clovers",new CloverBlock(AbstractBlock.Settings.copy(Blocks.SHORT_GRASS)));

        DEATHLY_BILE = registerBlock("dealthy_bile", new PotionEffectBileBlock(0, StatusEffects.WITHER, 60, 2, 4, 0.4f));
        CORRUPTED_BILE = registerBlock("corrupted_bile", new PotionEffectBileBlock(StatusEffects.HUNGER, 10, 1));
        HARMFUL_BILE = registerBlock("harmful_bile", new PotionEffectBileBlock(StatusEffects.INSTANT_DAMAGE));
        HEALTHY_BILE = registerBlock("healthy_bile", new PotionEffectBileBlock(StatusEffects.INSTANT_HEALTH, StatusEffects.REGENERATION, 10, 1));
        REGENERATION_BILE = registerBlock("regeneration_bile", new PotionEffectBileBlock(StatusEffects.REGENERATION));
        WITHERING_BILE = registerBlock("withering_bile", new PotionEffectBileBlock(StatusEffects.WITHER));
        MINING_SPEED_BILE = registerBlock("mining_speed_bile", new PotionEffectBileBlock(StatusEffects.HASTE));            // New potion!
        MINING_FATIGUE_BILE = registerBlock("mining_fatigue_bile", new PotionEffectBileBlock(StatusEffects.MINING_FATIGUE)); // New potion!
        TOUGHNESS_BILE = registerBlock("toughness_bile", new PotionEffectBileBlock(StatusEffects.RESISTANCE));
        VISION_BILE = registerBlock("vision_bile", new PotionEffectBileBlock(StatusEffects.NIGHT_VISION));
        DARKNESS_BILE = registerBlock("darkness_bile", new PotionEffectBileBlock(StatusEffects.DARKNESS));         // New potion!
        LEAPING_BILE = registerBlock("leaping_bile", new PotionEffectBileBlock(StatusEffects.JUMP_BOOST));
        SLOW_FALL_BILE = registerBlock("slow_fall_bile", new PotionEffectBileBlock(StatusEffects.SLOW_FALLING));
        SPEED_BILE = registerBlock("speed_bile", new PotionEffectBileBlock(StatusEffects.SPEED));
        SLOWNESS_BILE = registerBlock("slowness_bile", new PotionEffectBileBlock(StatusEffects.SLOWNESS));
        BREATH_BILE = registerBlock("breath_bile", new PotionEffectBileBlock(StatusEffects.WATER_BREATHING));
        INVISIBILITY_BILE = registerBlock("invisibility_bile", new PotionEffectBileBlock(StatusEffects.INVISIBILITY));
        GLOWING_BILE = registerBlock("glowing_bile", new PotionEffectBileBlock(StatusEffects.GLOWING, StatusEffects.GLOWING, 0, 50, 5, 1.0F));
        WEAKNESS_BILE = registerBlock("weakness_bile", new PotionEffectBileBlock(StatusEffects.WEAKNESS));
        STRENGTH_BILE = registerBlock("strength_bile", new PotionEffectBileBlock(StatusEffects.STRENGTH)); //-> Will be replacing Blaze Powder
        WIND_BILE = registerBlock("wind_bile", new PotionEffectBileBlock(StatusEffects.WIND_CHARGED));
        FIRE_BILE = registerBlock("fire_bile", new PotionEffectBileBlock(StatusEffects.FIRE_RESISTANCE)); //-> Will be replacing Magma Cream
        OOZE_BILE = registerBlock("ooze_bile", new PotionEffectBileBlock(StatusEffects.OOZING));
        INFESTATION_BILE = registerBlock("infestation_bile", new PotionEffectBileBlock(StatusEffects.INFESTED));
        POISON_BILE = registerBlock("poison_bile", new PotionEffectBileBlock(StatusEffects.POISON));
        WEAVING_BILE = registerBlock("weaving_bile", new PotionEffectBileBlock(StatusEffects.WEAVING));
        LUCK_BILE = registerBlock("luck_bile", new PotionEffectBileBlock(StatusEffects.LUCK));
        UNLUCK_BILE = registerBlock("unluck_bile", new PotionEffectBileBlock(StatusEffects.UNLUCK));
        VULNERABILITY_BILE = registerBlock("vulnerability_bile", new PotionEffectBileBlock(VulnerabilityStatusEffect.COLOR, CustomStatusEffects.VULNERABILITY)); // New potion!
        BUOYANCY_BILE = registerBlock("buoyancy_bile", new PotionEffectBileBlock(BuoyancyStatusEffect.COLOR, CustomStatusEffects.BUOYANCY));       // New potion & effect!
        LARGENESS_BILE = registerBlock("largeness_bile", new PotionEffectBileBlock(LargenessStatusEffect.COLOR, CustomStatusEffects.LARGENESS));     // New potion & effect!
        SMALLNESS_BILE = registerBlock("smallness_bile", new PotionEffectBileBlock(SmallnessStatusEffect.COLOR, CustomStatusEffects.SMALLNESS));     // New potion & effect!
    }

}
