package frootloops.versus.mod.hostile_mobs.overworld;

import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;

import java.util.List;

public class ZombieSoundListener {
    public static void OnGameEvent(ServerWorld serverWorld, GameEvent event, Vec3d emitterPos, GameEvent.Emitter emitter) {

        // Optimization for walking:
        if(event == GameEvent.STEP && serverWorld.getTime() % 5 != 0) return;

        // If the sound comes from an entity, skip if the entity is sneaking or on wool:
        boolean heardProjectileLanding = (event == GameEvent.PROJECTILE_LAND);
        boolean heardPlayerSprinting = false;

        if(emitter.sourceEntity() != null) {

            // For performance's sake:
            if (!heardProjectileLanding
                    && !(emitter.sourceEntity() instanceof PlayerEntity)
                    && !(emitter.sourceEntity() instanceof VillagerEntity)) return;

            // For consistency with wool occlusion and sneaking mechanics:
            if (event == GameEvent.STEP) {
                if (emitter.sourceEntity().bypassesSteppingEffects()) return; // Sneaking
                if (emitter.affectedState() != null && emitter.affectedState().isIn(BlockTags.DAMPENS_VIBRATIONS)) return; // Walking on wool
                heardPlayerSprinting = emitter.sourceEntity().isSprinting();
            }
        }
        else if(!heardProjectileLanding) return;


        // How much zombies should be attracted to the sound:
        boolean isHighPriority = (event == GameEvent.EAT || event == GameEvent.DRINK || event == GameEvent.ENTITY_DAMAGE);
        boolean isPriority = !isHighPriority && (heardPlayerSprinting || heardProjectileLanding || event.getId().startsWith("block"));
        double range = isHighPriority ? 64d : isPriority? 32d : 20d;
        double speedMultiplier = isHighPriority ? 1.2d : isPriority ? 1.0d : 0.8d;

        // Create a bounding box surrounding the event's position:
        double x = emitterPos.x, y = emitterPos.y, z = emitterPos.z;
        Box boundingBox = new Box(x - range, y - 12d, z - range, x + range, y + 12d, z + range);

        // For every zombie inside the bounds, make them walk towards the sound:
        List<ZombieEntity> zombiesNearby = serverWorld.getEntitiesByClass(ZombieEntity.class, boundingBox, EntityPredicates.VALID_LIVING_ENTITY);
        for (ZombieEntity zombie : zombiesNearby) {
            if(zombie instanceof ZombifiedPiglinEntity) continue;
            if(zombie.getTarget() == null) {
                zombie.getNavigation().startMovingTo(x, y, z, speedMultiplier);
                zombie.ambientSoundChance += isHighPriority ? 1000 : isPriority ? 500 : 250;
            }
        }
    }

    private static boolean IsPriority(final GameEvent event){
        String id = event.getId();
        return (id.equals("eat") || id.startsWith("block"));
    }
}
