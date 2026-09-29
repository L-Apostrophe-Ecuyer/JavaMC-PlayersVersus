package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.mod.mobs.hostile.overworld.WitheredZombieEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ZombieSoundListener {

    private static long lastUpdateTime = -1;
    private final static GameEvent STEP_EVENT =  GameEvent.STEP.value();
    private final static GameEvent PROJECTILE_EVENT =  GameEvent.PROJECTILE_LAND.value();
    private final static GameEvent EAT_EVENT =  GameEvent.EAT.value();
    private final static GameEvent DRINK_EVENT =  GameEvent.DRINK.value();
    private final static GameEvent DAMAGE_EVENT =  GameEvent.ENTITY_DAMAGE.value();
    private final static GameEvent BREAK_EVENT =  GameEvent.BLOCK_DESTROY.value();

    public static boolean OnGameEvent(ServerLevel serverWorld, GameEvent event, Vec3 emitterPos, GameEvent.Context emitter) {

        // Optimizations:
        long currentTime = serverWorld.getGameTime();
        if(currentTime - lastUpdateTime < 3) return false;
        if(event == STEP_EVENT && currentTime % 10 != 0) return false;

        // If the sound comes from an entity, skip if the entity is sneaking or on wool:
        boolean heardProjectileLanding = (event == PROJECTILE_EVENT);
        boolean heardPlayerSprinting = false;
        Player player = null;
        if(emitter.sourceEntity() != null) {
            if(!heardProjectileLanding) {

                // Check if the target is valid:
                if (!(emitter.sourceEntity() instanceof Player) && !(emitter.sourceEntity() instanceof Villager))
                    return false;
                if (emitter.sourceEntity() instanceof Player) {
                    player = (Player) emitter.sourceEntity();
                    if (player.isCreative()) return false;
                }

                // For consistency with wool occlusion and sneaking mechanics:
                if (event == STEP_EVENT) {
                    if (emitter.sourceEntity().isSteppingCarefully()) return false; // Sneaking
                    if (emitter.affectedState() != null && emitter.affectedState().is(BlockTags.DAMPENS_VIBRATIONS))
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
        AABB boundingBox = new AABB(x - range, y - 12d, z - range, x + range, y + 10d, z + range);

        // For every zombie inside the bounds, make them walk towards the sound:
        List<WitheredZombieEntity> witheredNearby = serverWorld.getEntitiesOfClass(WitheredZombieEntity.class, boundingBox, EntitySelector.LIVING_ENTITY_STILL_ALIVE);
        boolean shouldMoveZombie;
        for (WitheredZombieEntity zombie : witheredNearby) {
            //if(zombie instanceof ZombifiedPiglinEntity) continue;
            if(zombie.getTarget() == null && zombie.getNavigation().isDone()) {

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
                        zombie.ambientSoundTime += 1000;
                    }
                }
                else {
                    shouldMoveZombie = true;
                    zombie.ambientSoundTime += isPriority ? 400 : 200;
                }

                if(shouldMoveZombie) {
                    Path path = zombie.getNavigation().createPath(BlockPos.containing(x, y, z), 1);
                    if (path != null) zombie.getNavigation().moveTo(path, speedMultiplier);
                }
            }
        }
        return true;
    }
}
