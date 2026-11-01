package frootloops.versus.mixin.mobs.hostile.overworld;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import frootloops.versus.mod.items_and_effects.conversion.Undeath;
import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.mobs.passive.ZombieChickenEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

/**
 * What zombies make of the living, as Conversion does (Undeath): a villager they turn (killed, or all white) turns into
 * the illager of its trade where paleness holds, a zombie villager elsewhere; and baby zombies ride zombie chickens.
 */
@Mixin(Zombie.class)
public abstract class ZombieInfectionMixin {

    @Inject(method = "convertVillagerToZombieVillager", at = @At("HEAD"), cancellable = true)
    private void playersVersus$paleVillagersTurnIllager(ServerLevel level, Villager villager, CallbackInfoReturnable<Boolean> info) {
        if (Undeath.turnIllager(level, villager, !((Zombie) (Object) this).isSilent())) info.setReturnValue(true);
    }

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
