package frootloops.versus.mod.mobs;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes;
import frootloops.versus.mod.mobs.hostile.overworld.DeeperCreeperEntity;
import frootloops.versus.mod.mobs.hostile.overworld.FrostedZombieEntity;
import frootloops.versus.mod.mobs.hostile.overworld.WitheredZombieEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

public class MobSpawning {

    public static final int MOB_CAP_MONSTERS = 36;
    public static final int MOB_CAP_AMBIENT = 4;

    public static final TagKey<Block> UNDEAD_OVERWORLD_SPAWNABLE = blockTagOf("undead_overworld_spawnable_on");
    public static final TagKey<Block> CREEPER_SPAWNABLE = blockTagOf("creeper_spawnable_on");
    public static final TagKey<Block> DEEPER_CREEPER_SPAWNABLE = blockTagOf("stalker_spawnable_on");
    private static TagKey<Block> blockTagOf(String id) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, id));
    }


    public static void addCustomSpawns() {

        // Deep caves:
        // BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, VanillaEntities.WITHER_SKELETON, 50, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(CustomOverworldBiomes.DEEP_CAVES, CustomOverworldBiomes.REGULAR_CAVE), MobCategory.MONSTER, ModEntities.DEEPER_CREEPER, 100, 1, 1);
        SpawnPlacements.register(ModEntities.DEEPER_CREEPER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MobSpawning::canSpawnDeeperCreeper);

        //BiomeModifications.addSpawn(BiomeSelectors.excludeByKey(BiomeKeys.DEEP_DARK), SpawnGroup.MONSTER, ModEntities.WITHERED_ZOMBIE, 100, 4, 4);
        SpawnPlacements.register(ModEntities.WITHERED_ZOMBIE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MobSpawning::canSpawnWitheredZombie);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(CustomOverworldBiomes.DEEP_CAVES), MobCategory.MONSTER, EntityType.WITHER_SKELETON, 60, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(CustomOverworldBiomes.DEEP_CAVES), MobCategory.MONSTER, EntityType.ZOMBIFIED_PIGLIN, 3, 1, 4);

        // Surface:
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.FROZEN_PEAKS), MobCategory.MONSTER, EntityType.BREEZE, 100, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.SPAWNS_SNOW_FOXES), MobCategory.MONSTER, ModEntities.FROSTED_ZOMBIE, 140, 2, 4);
        SpawnPlacements.register(ModEntities.FROSTED_ZOMBIE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, MobSpawning::canSpawnFrostedZombie);

        // Desert:
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.DESERT), MobCategory.CREATURE, EntityType.CAVE_SPIDER, 60, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.DESERT), MobCategory.CREATURE, EntityType.HUSK, 120, 4, 4);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.DESERT), MobCategory.CREATURE, EntityType.CAMEL, 40, 3, 4);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.DESERT), MobCategory.CREATURE, EntityType.CAT, 40, 1, 3);

        // Air:
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, EntityType.PHANTOM, 80, 1, 2);

        // Nether:
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(Biomes.NETHER_WASTES), MobCategory.MONSTER, EntityType.BLAZE, 15, 1, 1);

    }

    public static boolean isMidnightDuringNewMoon(LevelAccessor world) {
        return isNewMoon(world) && isMidnight(world);
    }

    public static boolean isNewMoon(LevelAccessor world) {
        return world.getMoonPhase() == 7;
    }

    public static boolean isMidnight(LevelAccessor world) {
        long dayTime = world.dayTime() % 24000l;
        return !(dayTime < 18000l || dayTime > 20000l);
    }


    public static boolean canSpawnDeeperCreeper(EntityType<DeeperCreeperEntity> type, ServerLevelAccessor world, EntitySpawnReason spawnReason, BlockPos blockPos, RandomSource random) {
        if(world.getDifficulty() == Difficulty.PEACEFUL || !Monster.checkMobSpawnRules(type, world, spawnReason, blockPos, random)) return false;
        if(spawnReason != EntitySpawnReason.NATURAL) return true;
        if(world.getMaxLocalRawBrightness(blockPos) > 1 || !world.getBlockState(blockPos.below()).is(DEEPER_CREEPER_SPAWNABLE)) return false;
        return blockPos.getY() < 32;
    }

    public static boolean canSpawnWitheredZombie(EntityType<WitheredZombieEntity> type, ServerLevelAccessor world, EntitySpawnReason spawnReason, BlockPos blockPos, RandomSource random) {
        if(world.getDifficulty() == Difficulty.PEACEFUL || !Monster.checkMobSpawnRules(type, world, spawnReason, blockPos, random)) return false;
        if(spawnReason != EntitySpawnReason.NATURAL) return true;
        if(world.getMaxLocalRawBrightness(blockPos) > 0 || !world.getBlockState(blockPos.below()).is(UNDEAD_OVERWORLD_SPAWNABLE)) return false;
        if(world.getBiome(blockPos).unwrapKey().get() == CustomOverworldBiomes.DEEP_CAVES) return true;

        int y = blockPos.getY();
        if(y > 64) return false;
        if(y > 24) return isMidnightDuringNewMoon(world);
        return true;
    }

    public static boolean canSpawnFrostedZombie(EntityType<FrostedZombieEntity> type, ServerLevelAccessor world, EntitySpawnReason spawnReason, BlockPos blockPos, RandomSource random) {
        if(world.getDifficulty() == Difficulty.PEACEFUL || !Monster.checkMobSpawnRules(type, world, spawnReason, blockPos, random)) return false;
        if(spawnReason != EntitySpawnReason.NATURAL ) return true;
        while (world.getBlockState(blockPos = blockPos.above()).is(Blocks.POWDER_SNOW)) {}
        return Monster.checkMonsterSpawnRules(type, world, spawnReason, blockPos, random) && (world.canSeeSky(blockPos.below()) || (world.getBiome(blockPos).unwrapKey().get() == CustomOverworldBiomes.FROSTED_CAVE));
    }
}
