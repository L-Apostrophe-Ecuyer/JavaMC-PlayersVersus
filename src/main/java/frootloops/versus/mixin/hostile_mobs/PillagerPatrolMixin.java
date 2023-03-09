package frootloops.versus.mixin.hostile_mobs;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.PatrolEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.spawner.PatrolSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PatrolSpawner.class)
public class PillagerPatrolMixin {

    @Inject(method = "spawnPillager", at = @At("RETURN"), cancellable = true)
    private void moreDiversePatrol(ServerWorld world, BlockPos pos, Random random, boolean captain, CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue() == true) {
            int rand = random.nextInt(100);

            PatrolEntity patrolEntity = null;
            if(rand < 15) patrolEntity = EntityType.VINDICATOR.create(world);
            else if(rand < 40) patrolEntity = EntityType.WITCH.create(world);
            else if(rand < 70) patrolEntity = EntityType.PILLAGER.create(world);
            else if(rand < 73) patrolEntity = EntityType.RAVAGER.create(world);

            if (patrolEntity != null) {
                patrolEntity.setPosition(pos.getX(), pos.getY(), pos.getZ());
                patrolEntity.initialize(world, world.getLocalDifficulty(pos), SpawnReason.PATROL, null, null);
                world.spawnEntityAndPassengers(patrolEntity);
            }
        }
    }
}
