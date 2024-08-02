package frootloops.versus.mod.mobs;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.mobs.hostile.overworld.DeeperCreeperEntity;
import frootloops.versus.mod.mobs.hostile.overworld.FrostedZombieEntity;
import frootloops.versus.mod.mobs.hostile.overworld.WitheredZombieEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.biome.BiomeKeys;

public class MobSpawning {

    public static void addCustomSpawns() {


        // Deep caves:
        // BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, VanillaEntities.WITHER_SKELETON, 50, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.excludeByKey(BiomeKeys.DEEP_DARK), SpawnGroup.MONSTER, ModEntities.DEEPER_CREEPER, 260, 1, 2);
        SpawnRestriction.register(ModEntities.DEEPER_CREEPER, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MobSpawning::canSpawnDeeperCreeper);

        BiomeModifications.addSpawn(BiomeSelectors.excludeByKey(BiomeKeys.DEEP_DARK), SpawnGroup.MONSTER, ModEntities.WITHERED_ZOMBIE, 180, 4, 4);
        SpawnRestriction.register(ModEntities.WITHERED_ZOMBIE, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MobSpawning::canSpawnWitheredZombie);

        // SpawnRestriction.register(VanillaEntities.CREEPER, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MobSpawning::canSpawnCreeper);
        // SpawnRestriction.register(VanillaEntities.WITHER_SKELETON, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MobSpawning::canSpawnWitherSkelly);

        // Surface:
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.VILLAGE_SNOWY_HAS_STRUCTURE), SpawnGroup.MONSTER, ModEntities.FROSTED_ZOMBIE, 140, 4, 4);
        SpawnRestriction.register(ModEntities.FROSTED_ZOMBIE, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MobSpawning::canSpawnFrostedZombie);

        // Desert:
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.DESERT), SpawnGroup.CREATURE, EntityType.CAVE_SPIDER, 60, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.DESERT), SpawnGroup.CREATURE, EntityType.HUSK, 120, 4, 4);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.DESERT), SpawnGroup.CREATURE, EntityType.CAMEL, 40, 3, 4);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.DESERT), SpawnGroup.CREATURE, EntityType.CAT, 40, 1, 3);

        // Air:
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, EntityType.PHANTOM, 160, 1, 2);

    }


    public static boolean canSpawnDeeperCreeper(EntityType<DeeperCreeperEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos blockPos, Random random) {
        if(world.getDifficulty() == Difficulty.PEACEFUL || !HostileEntity.canMobSpawn(type, world, spawnReason, blockPos, random)) return false;
        if(SpawnReason.isTrialSpawner(spawnReason)) return true;
        return blockPos.getY() < -16 && world.getBlockState(blockPos.down()).isIn(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
    }

    public static boolean canSpawnWitheredZombie(EntityType<WitheredZombieEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos blockPos, Random random) {
        if(world.getDifficulty() == Difficulty.PEACEFUL || !HostileEntity.canMobSpawn(type, world, spawnReason, blockPos, random)) return false;
        if(SpawnReason.isTrialSpawner(spawnReason)) return true;
        return blockPos.getY() < 0 && world.getBlockState(blockPos.down()).isIn(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
    }

    public static boolean canSpawnFrostedZombie(EntityType<FrostedZombieEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos blockPos, Random random) {
        if(world.getDifficulty() == Difficulty.PEACEFUL || !HostileEntity.canMobSpawn(type, world, spawnReason, blockPos, random)) return false;
        if(spawnReason != SpawnReason.NATURAL ) return true;
        while (world.getBlockState(blockPos = blockPos.up()).isOf(Blocks.POWDER_SNOW)) {}
        return HostileEntity.canSpawnInDark(type, world, spawnReason, blockPos, random) && (world.isSkyVisible(blockPos.down()));
    }

    public static boolean canSpawnCreeper(EntityType<CreeperEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos blockPos, Random random) {
        if(world.getDifficulty() == Difficulty.PEACEFUL || !HostileEntity.canMobSpawn(type, world, spawnReason, blockPos, random)) return false;
        if(SpawnReason.isTrialSpawner(spawnReason)) return true;
        int y = blockPos.getY();
        if (y > 96 || y < 24) return false;
        if (y > 60 && world.getLightLevel(blockPos) > 1) return false;
        return world.getBlockState(blockPos.down()).isIn(BlockTags.OVERWORLD_CARVER_REPLACEABLES);
    }

    public static boolean canSpawnWitherSkelly(EntityType<WitherSkeletonEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos blockPos, Random random) {
        if(world.getDifficulty() == Difficulty.PEACEFUL || !HostileEntity.canMobSpawn(type, world, spawnReason, blockPos, random)) return false;
        if(SpawnReason.isTrialSpawner(spawnReason)) return true;
        if(world.getDimension().hasSkyLight()) return blockPos.getY() < 0 && world.getBlockState(blockPos.down()).isIn(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
        else return true;
    }
}
