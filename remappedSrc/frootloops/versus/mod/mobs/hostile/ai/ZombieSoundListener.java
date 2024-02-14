package frootloops.versus.mod.mobs.hostile.ai;

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
        if(event == GameEvent.STEP && serverWorld.getTime() % 10 != 0) return;

        // If the sound comes from an entity, skip if the entity is sneaking or on wool:
        boolean heardProjectileLanding = (event == GameEvent.PROJECTILE_LAND);
        boolean heardPlayerSprinting = false;
        PlayerEntity player = null;
        if(emitter.sourceEntity() != null) {

            // For performance's sake:
            if (!heardProjectileLanding
                    && !(emitter.sourceEntity() instanceof PlayerEntity)
                    && !(emitter.sourceEntity() instanceof VillagerEntity)) return;

            // If it's a player:
            if(emitter.sourceEntity() instanceof PlayerEntity) {
                player = (PlayerEntity) emitter.sourceEntity();
                if(player.isCreative()) return;
            }

            // For consistency with wool occlusion and sneaking mechanics:
            if (event == GameEvent.STEP || event == GameEvent.HIT_GROUND) {
                if (emitter.sourceEntity().bypassesSteppingEffects()) return; // Sneaking
                if (emitter.affectedState() != null && emitter.affectedState().isIn(BlockTags.DAMPENS_VIBRATIONS)) return; // Walking on wool
                heardPlayerSprinting = emitter.sourceEntity().isSprinting();
            }
        }
        else if(!heardProjectileLanding) return;


        // How much zombies should be attracted to the sound:
        boolean isHighPriority = (heardProjectileLanding || event == GameEvent.DRINK || event == GameEvent.EAT);
        boolean isPriority = !isHighPriority && (heardPlayerSprinting || event == GameEvent.ENTITY_DAMAGE || event == GameEvent.BLOCK_DESTROY);
        double range = isHighPriority ? 32d : isPriority? 24d : 12d;
        double speedMultiplier = isHighPriority ? 1.3d : isPriority ? 1.2d : 0.9d;
        if(!isPriority && !isHighPriority && serverWorld.getTime() % 2 != 0) return; // Optimization: chance for zombies to ignore certain sounds/events

        // Create a bounding box surrounding the event's position:
        double x = emitterPos.x, y = emitterPos.y, z = emitterPos.z;
        Box boundingBox = new Box(x - range, y - 12d, z - range, x + range, y + 12d, z + range);

        // For every zombie inside the bounds, make them walk towards the sound:
        List<ZombieEntity> zombiesNearby = serverWorld.getEntitiesByClass(ZombieEntity.class, boundingBox, EntityPredicates.VALID_LIVING_ENTITY);
        for (ZombieEntity zombie : zombiesNearby) {
            if(zombie instanceof ZombifiedPiglinEntity) continue;
            if(zombie.getTarget() == null) {
                if((isHighPriority || isPriority) && player != null) {
                    double dx = x - zombie.getX();
                    double dz = y - zombie.getY();
                    if ((dx * dx + dz * dz) < 256d) zombie.setTarget(player);
                    else {
                        zombie.getNavigation().startMovingTo(x, y, z, speedMultiplier);
                        zombie.ambientSoundChance += 1000;
                    }
                }
                else {
                    zombie.getNavigation().startMovingTo(x, y, z, speedMultiplier);
                    zombie.ambientSoundChance += isPriority ? 400 : 200;
                }
            }
        }
    }
}
