package frootloops.versus.mixin.mobs;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import frootloops.versus.mod.mobs.PaleKin;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The pale kin (PaleKin) in the natural spawner: the biome's entry it picked stays the zombie's, creeper's or spider's
 * (the spawner checks the biome still lists it), but below the pale line the kind it reads from that entry, for the
 * place's checks, for the spawn's cost and for the mob it makes, is the pale kin.
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerPaleKinMixin {

    private static final String TYPE = "Lnet/minecraft/world/level/biome/MobSpawnSettings$SpawnerData;type()Lnet/minecraft/world/entity/EntityType;";

    /** The place's checks: the pale kin's placement and spawn rules. */
    @WrapOperation(method = "isValidSpawnPostitionForType", at = @At(value = "INVOKE", target = TYPE), require = 1)
    private static EntityType<?> playersVersus$paleKinPlaced(MobSpawnSettings.SpawnerData data, Operation<EntityType<?>> original,
                                                             @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) BlockPos.MutableBlockPos pos) {
        return PaleKin.at(original.call(data), level, pos);
    }

    /** The spawn's cost (the extra test) and the mob made: the pale kin. */
    @WrapOperation(
            method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
            at = @At(value = "INVOKE", target = TYPE),
            require = 2
    )
    private static EntityType<?> playersVersus$paleKinSpawned(MobSpawnSettings.SpawnerData data, Operation<EntityType<?>> original,
                                                              @Local(argsOnly = true) ServerLevel level, @Local BlockPos.MutableBlockPos pos) {
        return PaleKin.at(original.call(data), level, pos);
    }
}
