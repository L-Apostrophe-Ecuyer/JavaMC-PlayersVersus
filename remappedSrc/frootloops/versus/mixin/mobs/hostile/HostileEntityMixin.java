package frootloops.versus.mixin.mobs.hostile;

import frootloops.versus.mod.mobs.MobSpawning;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LightType;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(HostileEntity.class)
public class HostileEntityMixin {

    @Overwrite
    public static boolean canSpawnInDark(EntityType<? extends HostileEntity> type, ServerWorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
        if(world.getDifficulty() == Difficulty.PEACEFUL) return false;
        if(spawnReason != SpawnReason.NATURAL) return HostileEntity.canMobSpawn(type, world, spawnReason, pos, random);
        if(!MobSpawning.isMidnight(world)) {
            if ((type == EntityType.ZOMBIE || type == EntityType.SKELETON) && (pos.getY() < 0 || !world.getBlockState(pos.down()).isIn(MobSpawning.UNDEAD_OVERWORLD_SPAWNABLE) || (pos.getY() > 64 && world.isSkyVisible(pos))))
                return false;
            else if (!MobSpawning.isNewMoon(world) && type == EntityType.CREEPER && (pos.getY() > 128 || world.getLightLevel(LightType.SKY, pos) > 7 || !world.getBlockState(pos.down()).isIn(MobSpawning.CREEPER_SPAWNABLE)))
                return false;
        }
        return (HostileEntity.isSpawnDark(world, pos, random)) && HostileEntity.canMobSpawn(type, world, spawnReason, pos, random);
    }
}
