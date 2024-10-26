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
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.EntityEffectParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;


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


    public static Block INFESTED_OAK_WOOD, INFESTED_DARK_OAK_WOOD;

    public static Block DEATHLY_BILE, CORRUPTED_BILE, HARMFUL_BILE, HEALTHY_BILE, REGENERATION_BILE, WITHERING_BILE, MINING_SPEED_BILE, MINING_FATIGUE_BILE, TOUGHNESS_BILE, VISION_BILE, DARKNESS_BILE, LEAPING_BILE, SLOW_FALL_BILE, SPEED_BILE, SLOWNESS_BILE, BREATH_BILE;
    public static Block INVISIBILITY_BILE, GLOWING_BILE, WEAKNESS_BILE, STRENGTH_BILE, WIND_BILE, FIRE_BILE, OOZE_BILE, INFESTATION_BILE, POISON_BILE, WEAVING_BILE, LUCK_BILE, UNLUCK_BILE;
    public static Block LARGENESS_BILE,SMALLNESS_BILE, VULNERABILITY_BILE, BUOYANCY_BILE;


    public static void onInitialize() {

        SMOLDERING_TORCH = registerBlock("smoldering_torch", new SmolderingTorchBlock(ParticleTypes.SMALL_FLAME, getSettings("smoldering_torch").noCollision().breakInstantly().luminance((state) -> 12).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY)));
        SMOLDERING_WALL_TORCH = registerBlock("smoldering_wall_torch", new SmolderingWallTorchBlock(ParticleTypes.SMALL_FLAME, getSettings("smoldering_wall_torch").noCollision().breakInstantly().luminance((state) -> 12).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY)));
        EXTINGUISHED_TORCH = registerBlock("extinguished_torch", new TorchBlock(ParticleTypes.SMOKE, getSettings("extinguished_torch").noCollision().breakInstantly().luminance((state) -> 6).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY)));
        EXTINGUISHED_WALL_TORCH = registerBlock("extinguished_wall_torch", new WallTorchBlock(ParticleTypes.SMOKE, getSettings("extinguished_wall_torch").noCollision().breakInstantly().luminance((state) -> 6).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY)));

        CORRUPTED_WART_PLANT = registerBlock("corrupted_wart", new NetherWartBlock(getSettings("corrupted_wart", Blocks.NETHER_WART)));
        WITHERED_WART_PLANT = registerBlock("withered_wart", new NetherWartBlock(getSettings("withered_wart", Blocks.NETHER_WART)));

        DRIPSTONE_SLAB = registerBlock("dripstone_slab", new SlabBlock(getSettings("dripstone_slab", Blocks.DRIPSTONE_BLOCK)));
        DRIPSTONE_STAIRS = registerBlock("dripstone_stairs", new StairsBlock(Blocks.DRIPSTONE_BLOCK.getDefaultState(), getSettings("dripstone_stairs", Blocks.DRIPSTONE_BLOCK)));
        DRIPSTONE_WALL = registerBlock("dripstone_wall", new WallBlock(getSettings("dripstone_wall", Blocks.DRIPSTONE_BLOCK)));
        POLISHED_DRIPSTONE = registerBlock("polished_dripstone", new Block(getSettings("polished_dripstone", Blocks.DRIPSTONE_BLOCK)));
        POLISHED_DRIPSTONE_SLAB = registerBlock("polished_dripstone_slab", new SlabBlock(getSettings("polished_dripstone_slab", POLISHED_DRIPSTONE)));
        POLISHED_DRIPSTONE_STAIRS = registerBlock("polished_dripstone_stairs", new StairsBlock(POLISHED_DRIPSTONE.getDefaultState(), getSettings("polished_dripstone_stairs", POLISHED_DRIPSTONE)));
        POLISHED_DRIPSTONE_WALL = registerBlock("polished_dripstone_wall", new WallBlock(getSettings("polished_dripstone_wall", POLISHED_DRIPSTONE)));
        DRIPSTONE_PILLAR = registerBlock("dripstone_pillar", new Block(getSettings("dripstone_pillar", POLISHED_DRIPSTONE)));
        DRIPSTONE_BRICKS = registerBlock("dripstone_bricks", new Block(getSettings("dripstone_bricks", POLISHED_DRIPSTONE)));
        DRIPSTONE_BRICK_SLAB = registerBlock("dripstone_brick_slab", new SlabBlock(getSettings("dripstone_brick_slab", DRIPSTONE_BRICKS)));
        DRIPSTONE_BRICK_STAIRS = registerBlock("dripstone_brick_stairs", new StairsBlock(DRIPSTONE_BRICKS.getDefaultState(), getSettings("dripstone_brick_stairs", DRIPSTONE_BRICKS)));
        DRIPSTONE_BRICK_WALL = registerBlock("dripstone_brick_wall", new WallBlock(getSettings("dripstone_brick_wall", DRIPSTONE_BRICKS)));

        POLISHED_STONE = registerBlock("polished_stone", new Block(getSettings("polished_stone", Blocks.STONE)));
        POLISHED_STONE_SLAB = registerBlock("polished_stone_slab", new SlabBlock(getSettings("polished_stone_slab", POLISHED_STONE)));
        POLISHED_STONE_STAIRS = registerBlock("polished_stone_stairs", new StairsBlock(POLISHED_STONE.getDefaultState(), getSettings("polished_stone_stairs", POLISHED_STONE)));

        TERRACOTTA_BRICKS = registerBlock("terracotta_bricks", new Block(getSettings("terracotta_bricks", Blocks.TERRACOTTA)));
        TERRACOTTA_BRICK_SLAB = registerBlock("terracotta_brick_slab", new SlabBlock(getSettings("terracotta_brick_slab", TERRACOTTA_BRICKS)));
        TERRACOTTA_BRICK_STAIRS = registerBlock("terracotta_brick_stairs", new StairsBlock(TERRACOTTA_BRICKS.getDefaultState(), getSettings("terracotta_brick_stairs", TERRACOTTA_BRICKS)));
        TERRACOTTA_BRICK_WALL = registerBlock("terracotta_brick_wall", new WallBlock(getSettings("terracotta_brick_wall", TERRACOTTA_BRICKS)));
        TERRACOTTA_TILES = registerBlock("terracotta_tiles", new Block(getSettings("terracotta_tiles", Blocks.TERRACOTTA)));
        TERRACOTTA_TILE_SLAB = registerBlock("terracotta_tile_slab", new SlabBlock(getSettings("terracotta_tile_slab", TERRACOTTA_TILES)));
        TERRACOTTA_TILE_STAIRS = registerBlock("terracotta_tile_stairs", new StairsBlock(TERRACOTTA_TILES.getDefaultState(), getSettings("terracotta_tile_stairs", TERRACOTTA_TILES)));
        TERRACOTTA_TILE_WALL = registerBlock("terracotta_tile_wall", new WallBlock(getSettings("terracotta_tile_wall", TERRACOTTA_TILES)));
        CHISELED_TERRACOTTA = registerBlock("chiseled_terracotta", new Block(getSettings("chiseled_terracotta", Blocks.TERRACOTTA)));
        CLAY = registerBlock("clay", new MoistBlock(getSettings("clay", Blocks.CLAY).strength(1.1f, 2.5f).slipperiness(0.92f).velocityMultiplier(0.94f).sounds(BlockSoundGroup.MUD), Blocks.TERRACOTTA));

        MUDSTONE = registerBlock("mudstone", new MoistBlock(getSettings("mudstone", Blocks.PACKED_MUD), Blocks.DRIPSTONE_BLOCK, Blocks.DRIPSTONE_BLOCK));
        MUDSTONE_BRICKS = registerBlock("mudstone_bricks", new MoistBlock(getSettings("mudstone_bricks", MUDSTONE), DRIPSTONE_BRICKS, DRIPSTONE_BRICKS));
        MUDSTONE_BRICK_SLAB = registerBlock("mudstone_brick_slab", new MoistSlabBlock(getSettings("mudstone_brick_slab", MUDSTONE), DRIPSTONE_BRICK_SLAB, DRIPSTONE_BRICK_STAIRS));
        MUDSTONE_BRICK_STAIRS = registerBlock("mudstone_brick_stairs", new MoistStairsBlock(getSettings("mudstone_brick_stairs", MUDSTONE), MUDSTONE.getDefaultState(), TERRACOTTA_BRICK_STAIRS, TERRACOTTA_BRICK_STAIRS));
        MUDSTONE_BRICK_WALL = registerBlock("mudstone_brick_wall", new MoistWallBlock(getSettings("mudstone_brick_wall", MUDSTONE), TERRACOTTA_BRICK_SLAB, TERRACOTTA_BRICK_SLAB));
        BROWN_MUD = registerBlock("brown_mud", new BrownMudBlock(getSettings("brown_mud", Blocks.DIRT).mapColor(MapColor.BROWN).dynamicBounds().allowsSpawning(Blocks::never).blockVision(Blocks::always).suffocates(Blocks::never).sounds(BlockSoundGroup.MUD)));
        BROWN_MUD_BRICKS = registerBlock("brown_mud_bricks", new MoistBlock(getSettings("brown_mud_bricks", BROWN_MUD).strength(1.1f, 2.5f).slipperiness(0.92f).velocityMultiplier(0.94f).sounds(BlockSoundGroup.MUD), MUDSTONE_BRICKS));
        BROWN_MUD_BRICK_SLAB = registerBlock("brown_mud_brick_slab", new MoistSlabBlock(getSettings("brown_mud_brick_slab", BROWN_MUD_BRICKS), MUDSTONE_BRICK_SLAB));
        BROWN_MUD_BRICK_STAIRS = registerBlock("brown_mud_brick_stairs", new MoistStairsBlock(getSettings("brown_mud_brick_stairs", BROWN_MUD_BRICKS), BROWN_MUD_BRICKS.getDefaultState(), MUDSTONE_BRICK_SLAB));
        BROWN_MUD_BRICK_WALL = registerBlock("brown_mud_brick_wall", new MoistWallBlock(getSettings("brown_mud_brick_wall", BROWN_MUD_BRICKS), MUDSTONE_BRICK_WALL));
        ((MoistBlock)MUDSTONE).wetterVersion = BROWN_MUD;
        ((MoistBlock)MUDSTONE_BRICKS).wetterVersion = BROWN_MUD_BRICKS;
        ((MoistSlabBlock)MUDSTONE_BRICK_SLAB).wetterVersion = BROWN_MUD_BRICK_SLAB;
        ((MoistStairsBlock)MUDSTONE_BRICK_STAIRS).wetterVersion = BROWN_MUD_BRICK_STAIRS;
        ((MoistWallBlock)BROWN_MUD_BRICK_WALL).wetterVersion = BROWN_MUD_BRICK_WALL;


        WHEAT_GRASS = registerBlock("wheat_grass", new WheatGrassBlock(getSettings("wheat_grass", Blocks.SHORT_GRASS)));
        WILD_WHEAT = registerBlock("wild_wheat", new WheatGrassBlock(getSettings("wild_wheat", Blocks.SHORT_GRASS)));
        CLOVERS = registerBlock("clovers", new CloverBlock(getSettings("clovers", Blocks.SHORT_GRASS)));

        INFESTED_OAK_WOOD = registerBlock("infested_oak_wood", new InfestedBlock(Blocks.OAK_WOOD, getSettings("infested_oak_wood", Blocks.OAK_WOOD)));
        INFESTED_DARK_OAK_WOOD = registerBlock("infested_dark_oak_wood", new InfestedBlock(Blocks.OAK_WOOD, getSettings("infested_dark_oak_wood", Blocks.OAK_WOOD)));

        DEATHLY_BILE = registerBileBlock("dealthy_bile", 0, StatusEffects.WITHER, 60, 2, 4, 0.4f);
        CORRUPTED_BILE = registerBileBlock("corrupted_bile", StatusEffects.HUNGER, 10, 1);
        HARMFUL_BILE = registerBileBlock("harmful_bile", StatusEffects.INSTANT_DAMAGE);
        HEALTHY_BILE = registerBileBlock("healthy_bile", StatusEffects.INSTANT_HEALTH, StatusEffects.REGENERATION, 10, 1);
        REGENERATION_BILE = registerBileBlock("regeneration_bile", StatusEffects.REGENERATION);
        WITHERING_BILE = registerBileBlock("withering_bile", StatusEffects.WITHER);
        MINING_SPEED_BILE = registerBileBlock("mining_speed_bile", StatusEffects.HASTE);            // New potion!
        MINING_FATIGUE_BILE = registerBileBlock("mining_fatigue_bile", StatusEffects.MINING_FATIGUE); // New potion!
        TOUGHNESS_BILE = registerBileBlock("toughness_bile", StatusEffects.RESISTANCE);
        VISION_BILE = registerBileBlock("vision_bile", StatusEffects.NIGHT_VISION);
        DARKNESS_BILE = registerBileBlock("darkness_bile", StatusEffects.DARKNESS);         // New potion!
        LEAPING_BILE = registerBileBlock("leaping_bile", StatusEffects.JUMP_BOOST);
        SLOW_FALL_BILE = registerBileBlock("slow_fall_bile", StatusEffects.SLOW_FALLING);
        SPEED_BILE = registerBileBlock("speed_bile", StatusEffects.SPEED);
        SLOWNESS_BILE = registerBileBlock("slowness_bile", StatusEffects.SLOWNESS);
        BREATH_BILE = registerBileBlock("breath_bile", StatusEffects.WATER_BREATHING);
        INVISIBILITY_BILE = registerBileBlock("invisibility_bile", StatusEffects.INVISIBILITY);
        GLOWING_BILE = registerBileBlock("glowing_bile", StatusEffects.GLOWING, StatusEffects.GLOWING, 0, 50, 5, 1.0F);
        WEAKNESS_BILE = registerBileBlock("weakness_bile", StatusEffects.WEAKNESS);
        STRENGTH_BILE = registerBileBlock("strength_bile", StatusEffects.STRENGTH); //-> Will be replacing Blaze Powder
        WIND_BILE = registerBileBlock("wind_bile", StatusEffects.WIND_CHARGED);
        FIRE_BILE = registerBileBlock("fire_bile", StatusEffects.FIRE_RESISTANCE); //-> Will be replacing Magma Cream
        OOZE_BILE = registerBileBlock("ooze_bile", StatusEffects.OOZING);
        INFESTATION_BILE = registerBileBlock("infestation_bile", StatusEffects.INFESTED);
        POISON_BILE = registerBileBlock("poison_bile", StatusEffects.POISON);
        WEAVING_BILE = registerBileBlock("weaving_bile", StatusEffects.WEAVING);
        LUCK_BILE = registerBileBlock("luck_bile", StatusEffects.LUCK);
        UNLUCK_BILE = registerBileBlock("unluck_bile", StatusEffects.UNLUCK);
        VULNERABILITY_BILE = registerBileBlock("vulnerability_bile", VulnerabilityStatusEffect.COLOR, CustomStatusEffects.VULNERABILITY); // New potion!
        BUOYANCY_BILE = registerBileBlock("buoyancy_bile", BuoyancyStatusEffect.COLOR, CustomStatusEffects.BUOYANCY);       // New potion & effect!
        LARGENESS_BILE = registerBileBlock("largeness_bile", LargenessStatusEffect.COLOR, CustomStatusEffects.LARGENESS);     // New potion & effect!
        SMALLNESS_BILE = registerBileBlock("smallness_bile", SmallnessStatusEffect.COLOR, CustomStatusEffects.SMALLNESS);     // New potion & effect!
    }


    private static AbstractBlock.Settings getSettings(String name) {
        return AbstractBlock.Settings.create().registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(VersusMod.MOD_ID, name)));
    }

    private static AbstractBlock.Settings getSettings(String name, Block blockToCopy) {
        return AbstractBlock.Settings.copy(blockToCopy).registryKey(RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(VersusMod.MOD_ID, name)));
    }
    private static Block registerBlock(String name, Block block) {
        Registry.register(Registries.BLOCK, Identifier.of(VersusMod.MOD_ID, name), block);
        return block;
    }

    private static Block registerBileBlock(String name, RegistryEntry<StatusEffect> statusEffect, int duration, int amplifier) {
        return registerBileBlock(name, statusEffect, statusEffect, duration, amplifier);
    }

    private static Block registerBileBlock(String name, RegistryEntry<StatusEffect> statusEffect) {
        return registerBileBlock(name, statusEffect.value().getColor(), statusEffect);
    }

    private static Block registerBileBlock(String name, int color, RegistryEntry<StatusEffect> statusEffect) {
        return registerBileBlock(name, ColorHelper.fullAlpha(color), statusEffect, 50, 0, 3, 0.8f);
    }

    private static Block registerBileBlock(String name, RegistryEntry<StatusEffect> statusEffect, RegistryEntry<StatusEffect> statusEffectToGrant, int duration, int amplifier) {
        return registerBileBlock(name, statusEffect, statusEffectToGrant, duration, amplifier, 3, 0.6f);
    }

    private static Block registerBileBlock(String name, RegistryEntry<StatusEffect> statusEffect, RegistryEntry<StatusEffect> statusEffectToGrant, int duration, int amplifier, int luminance, float ambientOcclusion) {
        return registerBileBlock(name, ColorHelper.fullAlpha(statusEffect.value().getColor()), statusEffectToGrant, duration, amplifier, luminance, ambientOcclusion);
    }

    private static Block registerBileBlock(String name, int color, RegistryEntry<StatusEffect> statusEffectToGrant, int duration, int amplifier, int luminance, float ambientOcclusion) {
        AbstractBlock.Settings settings = getSettings(name).sounds(BlockSoundGroup.SLIME).luminance(state -> luminance).noCollision().strength(0.2f, 0.4f).pistonBehavior(PistonBehavior.DESTROY).allowsSpawning((state, world, pos, entityType) -> false);
        return registerBlock(name, new PotionEffectBileBlock(settings, color, statusEffectToGrant, duration, amplifier, ambientOcclusion));
    }

}
