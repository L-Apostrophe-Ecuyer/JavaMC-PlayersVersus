package frootloops.versus.mod.mobs.hostile;

import frootloops.versus.mod.ModEntities;
import frootloops.versus.mod.mobs.hostile.overworld.FrostedZombieEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.*;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.BiomeKeys;

public class MobSpawning {

    public static void addCustomSpawns() {

        // Deep caves:
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, EntityType.WITHER_SKELETON, 100, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.excludeByKey(BiomeKeys.DEEP_DARK), SpawnGroup.MONSTER, ModEntities.DEEPER_CREEPER, 100, 1, 1);
        SpawnRestriction.register(ModEntities.DEEPER_CREEPER, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canMobSpawn);

        // Surface:
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.VILLAGE_SNOWY_HAS_STRUCTURE), SpawnGroup.MONSTER, ModEntities.FROSTED_ZOMBIE, 140, 4, 4);
        SpawnRestriction.register(ModEntities.FROSTED_ZOMBIE, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, FrostedZombieEntity::canMobSpawn);
    }

}
