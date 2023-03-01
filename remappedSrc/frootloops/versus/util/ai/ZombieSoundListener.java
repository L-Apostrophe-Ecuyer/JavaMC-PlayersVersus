package frootloops.versus.util.ai;

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

        // If the sound comes from an entity, skip if the entity is sneaking or on wool:
        if(emitter.sourceEntity() != null) {

            // For performance's sake:
            if (event.getId() != "projectile_land"
                    && !(emitter.sourceEntity() instanceof PlayerEntity)
                    && !(emitter.sourceEntity() instanceof VillagerEntity)) return;

            // For consistency with wool occlusion and sneaking mechanics:
            if (event.getId().equals("step")) {
                if (emitter.sourceEntity().bypassesSteppingEffects()) return; // Sneaking
                if (emitter.affectedState() != null && emitter.affectedState().isIn(BlockTags.DAMPENS_VIBRATIONS)) return; // Walking on wool
            }
        }

        // How much zombies should pay attention to the sound:
        boolean isPriority = IsPriority(event);
        double range = isPriority? 20d : 8d;
        double speedMultiplier = isPriority ? 0.75d : 0.5d;

        // Create a bounding box surrounding the event's position:
        double x = emitterPos.x, y = emitterPos.y, z = emitterPos.z;
        Box boundingBox = new Box(x - range, y - range, z - range, x + range, y + range, z + range);

        // For every zombie inside the bounds, make them walk towards the sound:
        List<ZombieEntity> zombiesNearby = serverWorld.getEntitiesByClass(ZombieEntity.class, boundingBox, EntityPredicates.VALID_LIVING_ENTITY);
        for (ZombieEntity zombie : zombiesNearby) {
            if(zombie instanceof ZombifiedPiglinEntity) continue;
            if(zombie.getNavigation().isIdle()) zombie.getNavigation().startMovingTo(x, y, z, speedMultiplier);
        }
    }

    private static boolean IsPriority(final GameEvent event){
        String id = event.getId();
        return (id.equals("eat") || id.startsWith("entity_d") || id.startsWith("block_"));
    }
}
