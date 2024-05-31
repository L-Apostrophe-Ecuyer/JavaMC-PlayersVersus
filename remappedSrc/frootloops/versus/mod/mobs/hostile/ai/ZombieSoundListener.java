package frootloops.versus.mod.mobs.hostile.ai;

import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;

import java.util.List;

public class ZombieSoundListener {

    private static long lastUpdateTime = -1;
    private final static GameEvent STEP_EVENT =  GameEvent.STEP.value();
    private final static GameEvent PROJECTILE_EVENT =  GameEvent.PROJECTILE_LAND.value();
    private final static GameEvent EAT_EVENT =  GameEvent.EAT.value();
    private final static GameEvent DRINK_EVENT =  GameEvent.DRINK.value();
    private final static GameEvent DAMAGE_EVENT =  GameEvent.ENTITY_DAMAGE.value();
    private final static GameEvent BREAK_EVENT =  GameEvent.BLOCK_DESTROY.value();

    public static boolean OnGameEvent(ServerWorld serverWorld, GameEvent event, Vec3d emitterPos, GameEvent.Emitter emitter) {

        // Optimizations:
        long currentTime = serverWorld.getTime();
        if(currentTime - lastUpdateTime < 3) return false;
        if(event == STEP_EVENT && currentTime % 10 != 0) return false;

        // If the sound comes from an entity, skip if the entity is sneaking or on wool:
        boolean heardProjectileLanding = (event == PROJECTILE_EVENT);
        boolean heardPlayerSprinting = false;
        PlayerEntity player = null;
        if(emitter.sourceEntity() != null) {
            if(!heardProjectileLanding) {

                // Check if the target is valid:
                if (!(emitter.sourceEntity() instanceof PlayerEntity) && !(emitter.sourceEntity() instanceof VillagerEntity))
                    return false;
                if (emitter.sourceEntity() instanceof PlayerEntity) {
                    player = (PlayerEntity) emitter.sourceEntity();
                    if (player.isCreative()) return false;
                }

                // For consistency with wool occlusion and sneaking mechanics:
                if (event == STEP_EVENT) {
                    if (emitter.sourceEntity().bypassesSteppingEffects()) return false; // Sneaking
                    if (emitter.affectedState() != null && emitter.affectedState().isIn(BlockTags.DAMPENS_VIBRATIONS))
                        return false; // Walking on wool
                    heardPlayerSprinting = emitter.sourceEntity().isSprinting();
                }
            }
        }
        else if(!heardProjectileLanding) return false;

        // How much zombies should be attracted to the sound:
        boolean isHighPriority = (heardProjectileLanding || event == DRINK_EVENT || event == EAT_EVENT);
        boolean isPriority = !isHighPriority && (heardPlayerSprinting || event == DAMAGE_EVENT || event == BREAK_EVENT);
        double range = isHighPriority ? 40d : isPriority? 20d : 12d;
        double speedMultiplier = isHighPriority ? 1.4d : isPriority ? 1.3d : 1.0d;
        if(!isPriority && !isHighPriority && currentTime % 2 != 0) return false; // Optimization: chance for zombies to ignore certain sounds/events

        // Create a bounding box surrounding the event's position:
        double x = emitterPos.x, y = emitterPos.y, z = emitterPos.z;
        Box boundingBox = new Box(x - range, y - 12d, z - range, x + range, y + 10d, z + range);

        // For every zombie inside the bounds, make them walk towards the sound:
        List<ZombieEntity> zombiesNearby = serverWorld.getEntitiesByClass(ZombieEntity.class, boundingBox, EntityPredicates.VALID_LIVING_ENTITY);
        boolean shouldMoveZombie;
        for (ZombieEntity zombie : zombiesNearby) {
            if(zombie instanceof ZombifiedPiglinEntity) continue;
            if(zombie.getTarget() == null && zombie.getNavigation().isIdle()) {

                lastUpdateTime = currentTime;
                shouldMoveZombie = false;
                if((isHighPriority || isPriority) && player != null) {
                    double dx = x - zombie.getX();
                    double dz = y - zombie.getY();
                    if ((dx * dx + dz * dz) < 144d) {
                        zombie.setTarget(player);
                    }
                    else {
                        shouldMoveZombie = true;
                        zombie.ambientSoundChance += 1000;
                    }
                }
                else {
                    shouldMoveZombie = true;
                    zombie.ambientSoundChance += isPriority ? 400 : 200;
                }

                if(shouldMoveZombie) {
                    Path path = zombie.getNavigation().findPathTo(BlockPos.ofFloored(x, y, z), 1);
                    if (path != null) zombie.getNavigation().startMovingAlong(path, speedMultiplier);
                }
            }
        }
        return true;
    }
}
