package frootloops.versus.mixin.mobs.hostile.illager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PatrolSpawner.class)
public class PillagerPatrolMixin {

    @Inject(method = "spawnPatrolMember", at = @At("RETURN"), cancellable = true)
    private void moreDiversePatrol(ServerLevel world, BlockPos pos, RandomSource random, boolean captain, CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue()) {
            int rand = random.nextInt(100);

            PatrollingMonster patrolEntity = null;
            if(rand < 15) patrolEntity = EntityTypes.VINDICATOR.create(world, EntitySpawnReason.PATROL);
            else if(rand < 40) patrolEntity = EntityTypes.WITCH.create(world, EntitySpawnReason.PATROL);
            else if(rand < 65){
                patrolEntity = EntityTypes.VINDICATOR.create(world, EntitySpawnReason.PATROL);
                patrolEntity.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
                patrolEntity.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                patrolEntity.getItemBySlot(EquipmentSlot.MAINHAND).setDamageValue(180);
                patrolEntity.getItemBySlot(EquipmentSlot.MAINHAND).setDamageValue(120);
            }

            if (patrolEntity != null) {
                patrolEntity.setPos(pos.getX(), pos.getY(), pos.getZ());
                patrolEntity.finalizeSpawn(world, world.getCurrentDifficultyAt(pos), EntitySpawnReason.PATROL, null);
                world.addFreshEntityWithPassengers(patrolEntity);
            }
        }
    }
}
