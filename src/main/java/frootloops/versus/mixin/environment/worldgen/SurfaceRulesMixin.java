package frootloops.versus.mixin.environment.worldgen;

import com.google.common.collect.ImmutableList;
import frootloops.versus.VersusMod;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.noise.NoiseParametersKeys;
import net.minecraft.world.gen.surfacebuilder.MaterialRules;
import net.minecraft.world.gen.surfacebuilder.VanillaSurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VanillaSurfaceRules.class)
public class SurfaceRulesMixin {

    private static final MaterialRules.MaterialRule AIR = materialRuleOf(Blocks.AIR);
    private static final MaterialRules.MaterialRule BEDROCK = materialRuleOf(Blocks.BEDROCK);
    private static final MaterialRules.MaterialRule WHITE_TERRACOTTA = materialRuleOf(Blocks.WHITE_TERRACOTTA);
    private static final MaterialRules.MaterialRule ORANGE_TERRACOTTA = materialRuleOf(Blocks.ORANGE_TERRACOTTA);
    private static final MaterialRules.MaterialRule TERRACOTTA = materialRuleOf(Blocks.TERRACOTTA);
    private static final MaterialRules.MaterialRule RED_SAND = materialRuleOf(Blocks.RED_SAND);
    private static final MaterialRules.MaterialRule RED_SANDSTONE = materialRuleOf(Blocks.RED_SANDSTONE);
    private static final MaterialRules.MaterialRule STONE = materialRuleOf(Blocks.STONE);
    private static final MaterialRules.MaterialRule DEEPSLATE = materialRuleOf(Blocks.DEEPSLATE);
    private static final MaterialRules.MaterialRule DIRT = materialRuleOf(Blocks.DIRT);
    private static final MaterialRules.MaterialRule PODZOL = materialRuleOf(Blocks.PODZOL);
    private static final MaterialRules.MaterialRule COARSE_DIRT = materialRuleOf(Blocks.COARSE_DIRT);
    private static final MaterialRules.MaterialRule MYCELIUM = materialRuleOf(Blocks.MYCELIUM);
    private static final MaterialRules.MaterialRule GRASS_BLOCK = materialRuleOf(Blocks.GRASS_BLOCK);
    private static final MaterialRules.MaterialRule CALCITE = materialRuleOf(Blocks.CALCITE);
    private static final MaterialRules.MaterialRule GRAVEL = materialRuleOf(Blocks.GRAVEL);
    private static final MaterialRules.MaterialRule SAND = materialRuleOf(Blocks.SAND);
    private static final MaterialRules.MaterialRule SANDSTONE = materialRuleOf(Blocks.SANDSTONE);
    private static final MaterialRules.MaterialRule PACKED_ICE = materialRuleOf(Blocks.PACKED_ICE);
    private static final MaterialRules.MaterialRule SNOW_BLOCK = materialRuleOf(Blocks.SNOW_BLOCK);
    private static final MaterialRules.MaterialRule MUD = materialRuleOf(Blocks.MUD);
    private static final MaterialRules.MaterialRule POWDER_SNOW = materialRuleOf(Blocks.POWDER_SNOW);
    private static final MaterialRules.MaterialRule ICE = materialRuleOf(Blocks.ICE);
    private static final MaterialRules.MaterialRule WATER = materialRuleOf(Blocks.WATER);
    private static final MaterialRules.MaterialRule LAVA = materialRuleOf(Blocks.LAVA);

    @Inject(method = "createOverworldSurfaceRule", at = @At("HEAD"), cancellable = true)
    private static void createOverworldSurfaceRule(CallbackInfoReturnable<MaterialRules.MaterialRule> cir) {

        VersusMod.MOD_LOGGER.warn("------ \n\nGETTING SURFACE RULES!!\n\n------------------");

        MaterialRules.MaterialCondition isAbove97 = MaterialRules.aboveY(YOffset.fixed(97), 2);
        MaterialRules.MaterialCondition isAbove256 = MaterialRules.aboveY(YOffset.fixed(256), 0);
        MaterialRules.MaterialCondition isAboveSeaLevel = MaterialRules.aboveYWithStoneDepth(YOffset.fixed(63), -1);
        MaterialRules.MaterialCondition isAbove74 = MaterialRules.aboveYWithStoneDepth(YOffset.fixed(74), 1);
        MaterialRules.MaterialCondition isAbove63 = MaterialRules.aboveY(YOffset.fixed(63), 0);
        MaterialRules.MaterialCondition isAbove62 = MaterialRules.aboveY(YOffset.fixed(62), 0);
        MaterialRules.MaterialCondition isAbove60 = MaterialRules.aboveY(YOffset.fixed(60), 0);

        MaterialRules.MaterialCondition isBlockBelowDry = MaterialRules.water(-1, 0);
        MaterialRules.MaterialCondition isBlockDry = MaterialRules.water(0, 0);
        MaterialRules.MaterialCondition isBlockTotallyDry = MaterialRules.waterWithStoneDepth(-6, -1);
        MaterialRules.MaterialCondition isHole = MaterialRules.hole();
        MaterialRules.MaterialCondition conditionSteepSlope = MaterialRules.steepSlope();
        MaterialRules.MaterialCondition conditionSurfaceNoiseMin = MaterialRules.noiseThreshold(NoiseParametersKeys.SURFACE, -0.909, -0.5454);
        MaterialRules.MaterialCondition conditionSurfaceNoiseMid = MaterialRules.noiseThreshold(NoiseParametersKeys.SURFACE, -0.1818, 0.1818);
        MaterialRules.MaterialCondition conditionSurfaceNoiseMax = MaterialRules.noiseThreshold(NoiseParametersKeys.SURFACE, 0.5454, 0.909);

        MaterialRules.MaterialCondition isInFrozenOcean = MaterialRules.biome(BiomeKeys.FROZEN_OCEAN, BiomeKeys.DEEP_FROZEN_OCEAN);
        MaterialRules.MaterialCondition isInWarmOcean = MaterialRules.biome(BiomeKeys.WARM_OCEAN, BiomeKeys.BEACH, BiomeKeys.SNOWY_BEACH);
        MaterialRules.MaterialCondition isInDesert = MaterialRules.biome(BiomeKeys.DESERT);

        MaterialRules.MaterialRule grass = MaterialRules.sequence(MaterialRules.condition(isBlockDry, GRASS_BLOCK), DIRT);
        MaterialRules.MaterialRule sand = MaterialRules.sequence(MaterialRules.condition(MaterialRules.STONE_DEPTH_CEILING, SANDSTONE), SAND);
        MaterialRules.MaterialRule gravel = MaterialRules.sequence(MaterialRules.condition(MaterialRules.STONE_DEPTH_CEILING, STONE), GRAVEL);
        MaterialRules.MaterialRule powderSnowTop = MaterialRules.condition(MaterialRules.noiseThreshold(NoiseParametersKeys.POWDER_SNOW, 0.35, 0.6), MaterialRules.condition(isBlockDry, POWDER_SNOW));
        MaterialRules.MaterialRule powderSnowBelow = MaterialRules.condition(MaterialRules.noiseThreshold(NoiseParametersKeys.POWDER_SNOW, 0.45, 0.58), MaterialRules.condition(isBlockDry, POWDER_SNOW));


        // Custom: Steep slopes
        MaterialRules.MaterialRule steepStonySlopesAllBiomes = MaterialRules.condition(
                MaterialRules.not(MaterialRules.verticalGradient("stony_slopes", YOffset.fixed(88), YOffset.fixed(96))),
                MaterialRules.condition(conditionSteepSlope,
                        MaterialRules.sequence(
                                MaterialRules.condition(MaterialRules.biome(BiomeKeys.WARM_OCEAN, BiomeKeys.BEACH, BiomeKeys.SNOWY_BEACH, BiomeKeys.SAVANNA, BiomeKeys.BADLANDS), MaterialRules.condition(MaterialRules.noiseThreshold(NoiseParametersKeys.SURFACE, 0.35, 0.6), TERRACOTTA)),
                                STONE)));


        MaterialRules.MaterialRule stonySurface = MaterialRules.sequence(
                MaterialRules.condition(MaterialRules.biome(BiomeKeys.STONY_PEAKS), MaterialRules.sequence(MaterialRules.condition(MaterialRules.noiseThreshold(NoiseParametersKeys.CALCITE, -0.0125, 0.0125), CALCITE), STONE)),
                MaterialRules.condition(MaterialRules.biome(BiomeKeys.STONY_SHORE), MaterialRules.sequence(MaterialRules.condition(MaterialRules.noiseThreshold(NoiseParametersKeys.GRAVEL, -0.05, 0.05), gravel), STONE)),
                MaterialRules.condition(MaterialRules.biome(BiomeKeys.WINDSWEPT_HILLS), MaterialRules.condition(surfaceNoiseThresholdOf(1.0), STONE)),
                MaterialRules.condition(isInWarmOcean, sand), MaterialRules.condition(isInDesert, sand),
                MaterialRules.condition(MaterialRules.biome(BiomeKeys.DRIPSTONE_CAVES), STONE),
                steepStonySlopesAllBiomes);


        MaterialRules.MaterialRule surfaceRuleSubSoil = MaterialRules.sequence(
                MUD,
                MaterialRules.condition(MaterialRules.biome(BiomeKeys.FROZEN_PEAKS),
                        MaterialRules.sequence(MaterialRules.condition(conditionSteepSlope, PACKED_ICE),
                                MaterialRules.condition(MaterialRules.noiseThreshold(NoiseParametersKeys.PACKED_ICE, -0.5, 0.2), PACKED_ICE),
                                MaterialRules.condition(MaterialRules.noiseThreshold(NoiseParametersKeys.ICE, -0.0625, 0.025), ICE),
                                MaterialRules.condition(isBlockDry, SNOW_BLOCK))),

                MaterialRules.condition(MaterialRules.biome(BiomeKeys.SNOWY_SLOPES),
                        MaterialRules.sequence(MaterialRules.condition(conditionSteepSlope, STONE), powderSnowBelow, MaterialRules.condition(isBlockDry, SNOW_BLOCK))),

                MaterialRules.condition(MaterialRules.biome(BiomeKeys.JAGGED_PEAKS), STONE), MaterialRules.condition(MaterialRules.biome(BiomeKeys.GROVE),
                        MaterialRules.sequence(powderSnowBelow, DIRT)),
                stonySurface,
                MaterialRules.condition(MaterialRules.biome(BiomeKeys.WINDSWEPT_SAVANNA, BiomeKeys.SAVANNA_PLATEAU),
                        MaterialRules.condition(surfaceNoiseThresholdOf(1.75), STONE)),
                MaterialRules.condition(MaterialRules.biome(BiomeKeys.WINDSWEPT_GRAVELLY_HILLS), MaterialRules.sequence(
                        MaterialRules.condition(surfaceNoiseThresholdOf(2.0), gravel),
                        MaterialRules.condition(surfaceNoiseThresholdOf(1.0), STONE),
                        MaterialRules.condition(surfaceNoiseThresholdOf(-1.0), DIRT),
                        gravel)),
                MaterialRules.condition(MaterialRules.biome(BiomeKeys.MANGROVE_SWAMP), MUD),
                DIRT);


        MaterialRules.MaterialRule surfaceRuleTopsoil = MaterialRules.sequence(
                MUD,
                MaterialRules.condition(
                        MaterialRules.biome(BiomeKeys.FROZEN_PEAKS),
                        MaterialRules.sequence(
                                MaterialRules.condition(conditionSteepSlope, PACKED_ICE),
                                MaterialRules.condition(MaterialRules.noiseThreshold(NoiseParametersKeys.PACKED_ICE, 0.0, 0.2), PACKED_ICE),
                                MaterialRules.condition(MaterialRules.noiseThreshold(NoiseParametersKeys.ICE, 0.0, 0.025), ICE),
                                MaterialRules.condition(isBlockDry, SNOW_BLOCK))),

                MaterialRules.condition(
                        MaterialRules.biome(BiomeKeys.SNOWY_SLOPES),
                        MaterialRules.sequence(
                                MaterialRules.condition(conditionSteepSlope, STONE),
                                powderSnowTop,
                                MaterialRules.condition(isBlockDry, SNOW_BLOCK))),

                MaterialRules.condition(
                        MaterialRules.biome(BiomeKeys.JAGGED_PEAKS),
                        MaterialRules.sequence
                                (MaterialRules.condition(conditionSteepSlope, STONE),
                                MaterialRules.condition(isBlockDry, SNOW_BLOCK))),

                MaterialRules.condition(
                        MaterialRules.biome(BiomeKeys.GROVE),
                        MaterialRules.sequence(
                                powderSnowTop,
                                MaterialRules.condition(isBlockDry, SNOW_BLOCK))),

                stonySurface,
                MaterialRules.condition(MaterialRules.biome(BiomeKeys.WINDSWEPT_SAVANNA, BiomeKeys.SAVANNA_PLATEAU),
                        MaterialRules.sequence(
                                MaterialRules.condition(conditionSurfaceNoiseMax, STONE),
                                MaterialRules.condition(conditionSurfaceNoiseMin, COARSE_DIRT))),

                MaterialRules.condition(
                        MaterialRules.biome(BiomeKeys.WINDSWEPT_GRAVELLY_HILLS),
                        MaterialRules.sequence(
                            MaterialRules.condition(surfaceNoiseThresholdOf(1.8), gravel),
                            MaterialRules.condition(surfaceNoiseThresholdOf(1.0), STONE),
                            MaterialRules.condition(surfaceNoiseThresholdOf(-1.0), grass), gravel)),

                MaterialRules.condition(
                        MaterialRules.biome(BiomeKeys.OLD_GROWTH_PINE_TAIGA, BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA),
                        MaterialRules.sequence(MaterialRules.condition(surfaceNoiseThresholdOf(1.75), COARSE_DIRT),
                        MaterialRules.condition(surfaceNoiseThresholdOf(-0.95), PODZOL))),

                MaterialRules.condition(
                        MaterialRules.biome(BiomeKeys.ICE_SPIKES),
                        MaterialRules.condition(isBlockDry, SNOW_BLOCK)),

                MaterialRules.condition(
                        MaterialRules.biome(BiomeKeys.MANGROVE_SWAMP), MUD),

                MaterialRules.condition(MaterialRules.biome(BiomeKeys.MUSHROOM_FIELDS), MYCELIUM),

                grass);

        MaterialRules.MaterialRule surfaceRuleDeep =
                MaterialRules.sequence(
                        MaterialRules.condition(MaterialRules.STONE_DEPTH_FLOOR,
                                MaterialRules.sequence(
                                        MaterialRules.condition(MaterialRules.biome(BiomeKeys.WOODED_BADLANDS),
                                                MaterialRules.condition(
                                                        isAbove97,
                                                        MaterialRules.sequence(
                                                                MaterialRules.condition(conditionSurfaceNoiseMin, COARSE_DIRT),
                                                                MaterialRules.condition(conditionSurfaceNoiseMid, COARSE_DIRT),
                                                                MaterialRules.condition(conditionSurfaceNoiseMax, COARSE_DIRT),
                                                                grass))),
                                        MaterialRules.condition(
                                                MaterialRules.biome(BiomeKeys.SWAMP, BiomeKeys.JUNGLE),
                                                MaterialRules.condition(isAbove62,
                                                        MaterialRules.condition(MaterialRules.not(isAbove63), MaterialRules.condition(MaterialRules.noiseThreshold(NoiseParametersKeys.SURFACE_SWAMP, 0.0), WATER)))),
                                        MaterialRules.condition(
                                                MaterialRules.biome(BiomeKeys.MANGROVE_SWAMP),
                                                MaterialRules.condition(isAbove60,
                                                        MaterialRules.condition(MaterialRules.not(isAbove63), MaterialRules.condition(MaterialRules.noiseThreshold(NoiseParametersKeys.SURFACE_SWAMP, 0.0), WATER)))))),

                        MaterialRules.condition(MaterialRules.biome(BiomeKeys.BADLANDS, BiomeKeys.ERODED_BADLANDS, BiomeKeys.WOODED_BADLANDS),
                                MaterialRules.sequence(MaterialRules.condition(MaterialRules.STONE_DEPTH_FLOOR, MaterialRules.sequence(MaterialRules.condition(isAbove256, ORANGE_TERRACOTTA), MaterialRules.condition(isAbove74, MaterialRules.sequence(MaterialRules.condition(conditionSurfaceNoiseMin, TERRACOTTA), MaterialRules.condition(conditionSurfaceNoiseMid, TERRACOTTA), MaterialRules.condition(conditionSurfaceNoiseMax, TERRACOTTA), MaterialRules.terracottaBands())), MaterialRules.condition(isBlockBelowDry, MaterialRules.sequence(MaterialRules.condition(MaterialRules.STONE_DEPTH_CEILING, RED_SANDSTONE), RED_SAND)), MaterialRules.condition(MaterialRules.not(isHole), ORANGE_TERRACOTTA), MaterialRules.condition(isBlockTotallyDry, WHITE_TERRACOTTA), gravel)), MaterialRules.condition(isAboveSeaLevel, MaterialRules.sequence(MaterialRules.condition(isAbove63, MaterialRules.condition(MaterialRules.not(isAbove74), ORANGE_TERRACOTTA)), MaterialRules.terracottaBands())), MaterialRules.condition(MaterialRules.STONE_DEPTH_FLOOR_WITH_SURFACE_DEPTH, MaterialRules.condition(isBlockTotallyDry, WHITE_TERRACOTTA)))
                        ),

                        MaterialRules.condition(MaterialRules.STONE_DEPTH_FLOOR,
                                MaterialRules.condition(isBlockBelowDry,
                                        MaterialRules.sequence(
                                                MaterialRules.condition(isInFrozenOcean,
                                                        MaterialRules.condition(isHole,
                                                                MaterialRules.sequence(
                                                                        MaterialRules.condition(isBlockDry, AIR),
                                                                        MaterialRules.condition(MaterialRules.temperature(), ICE),
                                                                        WATER))),
                                                surfaceRuleTopsoil))),

                        // 
                        MaterialRules.condition(isBlockTotallyDry,
                                MaterialRules.sequence(
                                        MaterialRules.condition(MaterialRules.STONE_DEPTH_FLOOR, MaterialRules.condition(isInFrozenOcean, MaterialRules.condition(isHole, WATER))),
                                        MaterialRules.condition(MaterialRules.STONE_DEPTH_FLOOR_WITH_SURFACE_DEPTH, surfaceRuleSubSoil),
                                        MaterialRules.condition(isInWarmOcean, MaterialRules.condition(MaterialRules.STONE_DEPTH_FLOOR_WITH_SURFACE_DEPTH_RANGE_6, SANDSTONE)),
                                        MaterialRules.condition(isInDesert, MaterialRules.condition(MaterialRules.STONE_DEPTH_FLOOR_WITH_SURFACE_DEPTH_RANGE_30, SANDSTONE)))),

                        // Underwater surface:
                        MaterialRules.condition(MaterialRules.STONE_DEPTH_FLOOR,
                                MaterialRules.sequence(
                                        MaterialRules.condition(MaterialRules.biome(BiomeKeys.FROZEN_PEAKS, BiomeKeys.JAGGED_PEAKS), STONE),
                                        MaterialRules.condition(MaterialRules.biome(BiomeKeys.WARM_OCEAN, BiomeKeys.LUKEWARM_OCEAN, BiomeKeys.DEEP_LUKEWARM_OCEAN), sand),
                                        gravel)));


        ImmutableList.Builder builder = ImmutableList.builder();
        builder.add(MaterialRules.condition(MaterialRules.verticalGradient("bedrock_floor", YOffset.getBottom(), YOffset.aboveBottom(5)), BEDROCK));
        builder.add(MaterialRules.condition(MaterialRules.surface(), surfaceRuleDeep));
        builder.add(MaterialRules.condition(MaterialRules.verticalGradient("deepslate", YOffset.fixed(-8), YOffset.fixed(8)), DEEPSLATE));
        MaterialRules.MaterialRule defaultRule = MaterialRules.sequence((MaterialRules.MaterialRule[])builder.build().toArray(MaterialRules.MaterialRule[]::new));
        cir.setReturnValue(defaultRule);
    }

    private static MaterialRules.MaterialRule materialRuleOf(Block block) {
        return MaterialRules.block(block.getDefaultState());
    }

    private static MaterialRules.MaterialCondition surfaceNoiseThresholdOf(double min) {
        return MaterialRules.noiseThreshold(NoiseParametersKeys.SURFACE, min / 8.25, Double.MAX_VALUE);
    }
}
