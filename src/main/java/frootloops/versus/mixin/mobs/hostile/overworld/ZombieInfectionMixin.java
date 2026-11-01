package frootloops.versus.mixin.mobs.hostile.overworld;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.mobs.passive.ZombieChickenEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.function.Predicate;

/** Baby zombies ride zombie chickens: a new one, or one of those about, never a living chicken. */
@Mixin(Zombie.class)
public abstract class ZombieInfectionMixin {

    /** The chicken a baby zombie spawns riding is a zombie chicken. */
    @WrapOperation(method = "finalizeSpawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;"))
    private Entity playersVersus$zombieChickenJockey(EntityType<?> type, Level level, EntitySpawnReason reason, Operation<Entity> original) {
        return original.call(type == EntityTypes.CHICKEN ? ModEntities.ZOMBIE_CHICKEN : type, level, reason);
    }

    /** And the chickens about it that it might ride instead are only the zombie ones. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @WrapOperation(method = "finalizeSpawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/ServerLevelAccessor;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private List playersVersus$onlyZombieChickens(ServerLevelAccessor level, Class type, AABB box, Predicate filter, Operation<List> original) {
        List found = original.call(level, type, box, filter);
        if (type != Chicken.class) return found;
        return ((List<?>) found).stream().filter(entity -> entity instanceof ZombieChickenEntity).toList();
    }
}
