package frootloops.versus.mod.mobs;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes;
import frootloops.versus.mod.mobs.hostile.overworld.DeeperCreeperEntity;
import frootloops.versus.mod.mobs.hostile.overworld.FrostedZombieEntity;
import frootloops.versus.mod.mobs.hostile.overworld.WitheredZombieEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.biome.BiomeKeys;

public class MobSpawning {

    public static final int MOB_CAP_MONSTERS = 36;
    public static final int MOB_CAP_AMBIENT = 4;

    public static final TagKey<Block> UNDEAD_OVERWORLD_SPAWNABLE = blockTagOf("undead_overworld_spawnable_on");
    public static final TagKey<Block> CREEPER_SPAWNABLE = blockTagOf("creeper_spawnable_on");
    public static final TagKey<Block> DEEPER_CREEPER_SPAWNABLE = blockTagOf("stalker_spawnable_on");
    private static TagKey<Block> blockTagOf(String id) {
        return TagKey.of(RegistryKeys.BLOCK, Identifier.of(VersusMod.MOD_ID, id));
    }


    public static void addCustomSpawns() {

        // Deep caves:
        // BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, VanillaEntities.WITHER_SKELETON, 50, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(CustomOverworldBiomes.DEEP_CAVES, CustomOverworldBiomes.REGULAR_CAVE), SpawnGroup.MONSTER, ModEntities.DEEPER_CREEPER, 100, 1, 1);
        SpawnRestriction.register(ModEntities.DEEPER_CREEPER, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MobSpawning::canSpawnDeeperCreeper);

        //BiomeModifications.addSpawn(BiomeSelectors.excludeByKey(BiomeKeys.DEEP_DARK), SpawnGroup.MONSTER, ModEntities.WITHERED_ZOMBIE, 100, 4, 4);
        SpawnRestriction.register(ModEntities.WITHERED_ZOMBIE, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MobSpawning::canSpawnWitheredZombie);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(CustomOverworldBiomes.DEEP_CAVES), SpawnGroup.MONSTER, EntityType.WITHER_SKELETON, 60, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(CustomOverworldBiomes.DEEP_CAVES), SpawnGroup.MONSTER, EntityType.ZOMBIFIED_PIGLIN, 3, 1, 4);

        // Surface:
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.FROZEN_PEAKS), SpawnGroup.MONSTER, EntityType.BREEZE, 100, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.SPAWNS_SNOW_FOXES), SpawnGroup.MONSTER, ModEntities.FROSTED_ZOMBIE, 140, 2, 4);
        SpawnRestriction.register(ModEntities.FROSTED_ZOMBIE, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MobSpawning::canSpawnFrostedZombie);

        // Desert:
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.DESERT), SpawnGroup.CREATURE, EntityType.CAVE_SPIDER, 60, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.DESERT), SpawnGroup.CREATURE, EntityType.HUSK, 120, 4, 4);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.DESERT), SpawnGroup.CREATURE, EntityType.CAMEL, 40, 3, 4);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.DESERT), SpawnGroup.CREATURE, EntityType.CAT, 40, 1, 3);

        // Air:
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, EntityType.PHANTOM, 80, 1, 2);

        // Nether:
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.NETHER_WASTES), SpawnGroup.MONSTER, EntityType.BLAZE, 15, 1, 1);

    }

    public static boolean isMidnightDuringNewMoon(WorldAccess world) {
        return isNewMoon(world) && isMidnight(world);
    }

    public static boolean isNewMoon(WorldAccess world) {
        return world.getMoonPhase() == 7;
    }

    public static boolean isMidnight(WorldAccess world) {
        long dayTime = world.getLunarTime() % 24000l;
        return !(dayTime < 18000l || dayTime > 20000l);
    }


    public static boolean canSpawnDeeperCreeper(EntityType<DeeperCreeperEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos blockPos, Random random) {
        if(world.getDifficulty() == Difficulty.PEACEFUL || !HostileEntity.canMobSpawn(type, world, spawnReason, blockPos, random)) return false;
        if(spawnReason != SpawnReason.NATURAL) return true;
        if(world.getLightLevel(blockPos) > 1 || !world.getBlockState(blockPos.down()).isIn(DEEPER_CREEPER_SPAWNABLE)) return false;
        return blockPos.getY() < 32;
    }

    public static boolean canSpawnWitheredZombie(EntityType<WitheredZombieEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos blockPos, Random random) {
        if(world.getDifficulty() == Difficulty.PEACEFUL || !HostileEntity.canMobSpawn(type, world, spawnReason, blockPos, random)) return false;
        if(spawnReason != SpawnReason.NATURAL) return true;
        if(world.getLightLevel(blockPos) > 0 || !world.getBlockState(blockPos.down()).isIn(UNDEAD_OVERWORLD_SPAWNABLE)) return false;
        if(world.getBiome(blockPos).getKey().get() == CustomOverworldBiomes.DEEP_CAVES) return true;

        int y = blockPos.getY();
        if(y > 64) return false;
        if(y > 24) return isMidnightDuringNewMoon(world);
        return true;
    }

    public static boolean canSpawnFrostedZombie(EntityType<FrostedZombieEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos blockPos, Random random) {
        if(world.getDifficulty() == Difficulty.PEACEFUL || !HostileEntity.canMobSpawn(type, world, spawnReason, blockPos, random)) return false;
        if(spawnReason != SpawnReason.NATURAL ) return true;
        while (world.getBlockState(blockPos = blockPos.up()).isOf(Blocks.POWDER_SNOW)) {}
        return HostileEntity.canSpawnInDark(type, world, spawnReason, blockPos, random) && (world.isSkyVisible(blockPos.down()) || (world.getBiome(blockPos).getKey().get() == CustomOverworldBiomes.FROSTED_CAVE));
    }
}
