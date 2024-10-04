package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.blocks.*;
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
import net.minecraft.util.math.ColorHelper;


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

    public static final Block PACKED_MUD_TILES = registerBlock("packed_mud_tiles", new Block(AbstractBlock.Settings.copy(Blocks.MUD_BRICKS)));
    public static final Block PACKED_MUD_TILES_SLAB = registerBlock("packed_mud_tile_slab", new SlabBlock(AbstractBlock.Settings.copy(PACKED_MUD_TILES)));
    public static final Block PACKED_MUD_TILES_STAIRS = registerBlock("packed_mud_tile_stairs", new StairsBlock(PACKED_MUD_TILES.getDefaultState(), AbstractBlock.Settings.copy(PACKED_MUD_TILES)));
    public static final Block BROWN_MUD = registerBlock("brown_mud", new BrownMudBlock(AbstractBlock.Settings.copy(Blocks.DIRT).mapColor(MapColor.BROWN).dynamicBounds().allowsSpawning(Blocks::never).blockVision(Blocks::always).suffocates(Blocks::never).sounds(BlockSoundGroup.MUD)));
    public static final Block BROWN_MUD_BRICKS = registerBlock("brown_mud_bricks",new BrownMudBuildingBlock(AbstractBlock.Settings.copy(BROWN_MUD).strength(1.1f, 2.5f).slipperiness(0.92f).velocityMultiplier(0.94f).sounds(BlockSoundGroup.MUD), Blocks.MUD_BRICKS));
    public static final Block BROWN_MUD_BRICK_SLAB = registerBlock("brown_mud_brick_slab",new BrownMudSlabBlock(AbstractBlock.Settings.copy(BROWN_MUD_BRICKS), Blocks.MUD_BRICK_SLAB));
    public static final Block BROWN_MUD_BRICK_STAIRS = registerBlock("brown_mud_brick_stairs", new BrownMudStairsBlock(BROWN_MUD_BRICKS.getDefaultState(), AbstractBlock.Settings.copy(BROWN_MUD_BRICKS), Blocks.MUD_BRICK_STAIRS));
    public static final Block BROWN_MUD_TILES = registerBlock("brown_mud_tiles", new BrownMudBuildingBlock(AbstractBlock.Settings.copy(BROWN_MUD_BRICKS), PACKED_MUD_TILES));
    public static final Block BROWN_MUD_TILES_SLAB = registerBlock("brown_mud_tile_slab",new BrownMudSlabBlock(AbstractBlock.Settings.copy(BROWN_MUD_TILES), PACKED_MUD_TILES_SLAB));
    public static final Block BROWN_MUD_TILES_STAIRS =  registerBlock("brown_mud_tile_stairs", new BrownMudStairsBlock(BROWN_MUD_TILES.getDefaultState(), AbstractBlock.Settings.copy(BROWN_MUD_TILES), PACKED_MUD_TILES_STAIRS));

    public static final Block WHITE_CLOVERS = registerBlock("white_clovers",new CloverBlock(AbstractBlock.Settings.copy(Blocks.SHORT_GRASS)));
    public static final Block WHEAT_GRASS = registerBlock("wheat_grass", new WheatGrassBlock(AbstractBlock.Settings.copy(Blocks.SHORT_GRASS)));
    public static final Block WILD_WHEAT = registerBlock("wild_wheat",new WheatGrassBlock(AbstractBlock.Settings.copy(Blocks.SHORT_GRASS)));
    public static final Block CLOVERS = registerBlock("clovers",new CloverBlock(AbstractBlock.Settings.copy(Blocks.SHORT_GRASS)));


    public static final Block DEATHLY_BILE = registerBlock("dealthy_bile", new PotionEffectBileBlock(0, StatusEffects.WITHER, 60, 2, 4, 0.4f));
    public static final Block CORRUPTED_BILE = registerBlock("corrupted_bile", new PotionEffectBileBlock(StatusEffects.HUNGER, 10, 1));

    public static final Block HARMFUL_BILE = registerBlock("harmful_bile", new PotionEffectBileBlock(StatusEffects.INSTANT_DAMAGE));
    public static final Block HEALTHY_BILE = registerBlock("healthy_bile", new PotionEffectBileBlock(StatusEffects.INSTANT_HEALTH, StatusEffects.REGENERATION, 10, 1));

    public static final Block REGENERATION_BILE = registerBlock("regeneration_bile", new PotionEffectBileBlock(StatusEffects.REGENERATION));
    public static final Block WITHERING_BILE = registerBlock("withering_bile", new PotionEffectBileBlock(StatusEffects.WITHER));

    public static final Block MINING_SPEED_BILE = registerBlock("mining_speed_bile", new PotionEffectBileBlock(StatusEffects.HASTE));            // New potion!
    public static final Block MINING_FATIGUE_BILE = registerBlock("mining_fatigue_bile", new PotionEffectBileBlock(StatusEffects.MINING_FATIGUE)); // New potion!

    public static final Block TOUGHNESS_BILE = registerBlock("toughness_bile", new PotionEffectBileBlock(StatusEffects.RESISTANCE));
    public static Block VULNERABILITY_BILE;

    public static final Block VISION_BILE = registerBlock("vision_bile", new PotionEffectBileBlock(StatusEffects.NIGHT_VISION));
    public static final Block DARKNESS_BILE = registerBlock("darkness_bile", new PotionEffectBileBlock(StatusEffects.DARKNESS));         // New potion!

    public static final Block LEAPING_BILE = registerBlock("leaping_bile", new PotionEffectBileBlock(StatusEffects.JUMP_BOOST));
    public static final Block SLOW_FALL_BILE = registerBlock("slow_fall_bile", new PotionEffectBileBlock(StatusEffects.SLOW_FALLING));

    public static final Block SPEED_BILE = registerBlock("speed_bile", new PotionEffectBileBlock(StatusEffects.SPEED));
    public static final Block SLOWNESS_BILE = registerBlock("slowness_bile", new PotionEffectBileBlock(StatusEffects.SLOWNESS));

    public static final Block BREATH_BILE = registerBlock("breath_bile", new PotionEffectBileBlock(StatusEffects.WATER_BREATHING));
    public static Block BUOYANCY_BILE;

    public static Block LARGENESS_BILE;
    public static Block SMALLNESS_BILE;

    public static final Block INVISIBILITY_BILE = registerBlock("invisibility_bile", new PotionEffectBileBlock(StatusEffects.INVISIBILITY));
    public static final Block GLOWING_BILE = registerBlock("glowing_bile", new PotionEffectBileBlock(StatusEffects.GLOWING, StatusEffects.GLOWING, 0, 50, 5, 1.0F));

    public static final Block WEAKNESS_BILE = registerBlock("weakness_bile", new PotionEffectBileBlock(StatusEffects.WEAKNESS));
    public static final Block STRENGTH_BILE = registerBlock("strength_bile", new PotionEffectBileBlock(StatusEffects.STRENGTH)); //-> Will be replacing Blaze Powder

    public static final Block WIND_BILE = registerBlock("wind_bile", new PotionEffectBileBlock(StatusEffects.WIND_CHARGED));
    public static final Block FIRE_BILE = registerBlock("fire_bile", new PotionEffectBileBlock(StatusEffects.FIRE_RESISTANCE)); //-> Will be replacing Magma Cream

    public static final Block OOZE_BILE = registerBlock("ooze_bile", new PotionEffectBileBlock(StatusEffects.OOZING));
    public static final Block INFESTATION_BILE = registerBlock("infestation_bile", new PotionEffectBileBlock(StatusEffects.INFESTED));

    public static final Block POISON_BILE = registerBlock("poison_bile", new PotionEffectBileBlock(StatusEffects.POISON));
    public static final Block WEAVING_BILE = registerBlock("weaving_bile", new PotionEffectBileBlock(StatusEffects.WEAVING));

    public static final Block LUCK_BILE = registerBlock("luck_bile", new PotionEffectBileBlock(StatusEffects.LUCK));
    public static final Block UNLUCK_BILE = registerBlock("unluck_bile", new PotionEffectBileBlock(StatusEffects.UNLUCK));



    private static Block registerBlock(String name, Block block) {
        Registry.register(Registries.BLOCK,  Identifier.of(VersusMod.MOD_ID, name), block);
        return block;
    }


    public static void onInitialize() {

        // These need to be registered AFTER custom potion effects
        VULNERABILITY_BILE = registerBlock("vulnerability_bile", new PotionEffectBileBlock(VulnerabilityStatusEffect.COLOR, CustomStatusEffects.VULNERABILITY)); // New potion!
        BUOYANCY_BILE = registerBlock("buoyancy_bile", new PotionEffectBileBlock(BuoyancyStatusEffect.COLOR, CustomStatusEffects.BUOYANCY));       // New potion & effect!
        LARGENESS_BILE = registerBlock("largeness_bile", new PotionEffectBileBlock(LargenessStatusEffect.COLOR, CustomStatusEffects.LARGENESS));     // New potion & effect!
        SMALLNESS_BILE = registerBlock("smallness_bile", new PotionEffectBileBlock(SmallnessStatusEffect.COLOR, CustomStatusEffects.SMALLNESS));     // New potion & effect!

    }

}
