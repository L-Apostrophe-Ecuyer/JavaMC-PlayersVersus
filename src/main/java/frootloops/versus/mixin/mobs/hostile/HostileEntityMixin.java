package frootloops.versus.mixin.mobs.hostile;

import frootloops.versus.mod.mobs.MobSpawning;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Monster.class)
public class HostileEntityMixin {

    @Overwrite
    public static boolean checkMonsterSpawnRules(EntityType<? extends Monster> type, ServerLevelAccessor world, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        if(world.getDifficulty() == Difficulty.PEACEFUL) return false;
        if(spawnReason != EntitySpawnReason.NATURAL) return Monster.checkMobSpawnRules(type, world, spawnReason, pos, random);
        if (type == EntityTypes.ZOMBIE || type == EntityTypes.SKELETON || type == EntityTypes.CREEPER || type == EntityTypes.SPIDER) {
            boolean natural = world.getBlockState(pos.below()).is(type == EntityTypes.CREEPER ? MobSpawning.CREEPER_SPAWNABLE : MobSpawning.UNDEAD_OVERWORLD_SPAWNABLE);
            if (!MobSpawning.overworldRules(type, natural, world.getBrightness(LightLayer.SKY, pos), pos.getY(), world.canSeeSky(pos), MobSpawning.isMidnight(world))) return false;
        }
        return (Monster.isDarkEnoughToSpawn(world, pos, random)) && Monster.checkMobSpawnRules(type, world, spawnReason, pos, random);
    }
}
