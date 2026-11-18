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

    /**
     * A monster's spawn: in peace none; other than naturally, the mob's own checks only (no darkness asked); naturally,
     * the mod's rules for the overworld's (MobSpawning.overworldRules), then the darkness, from one read of the sky's
     * light for both (MobSpawning.darkEnoughToSpawn, vanilla's check given that read).
     */
    @Overwrite
    public static boolean checkMonsterSpawnRules(EntityType<? extends Monster> type, ServerLevelAccessor world, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        if(world.getDifficulty() == Difficulty.PEACEFUL) return false;
        if(spawnReason != EntitySpawnReason.NATURAL) return Monster.checkMobSpawnRules(type, world, spawnReason, pos, random);
        int skyLight = world.getBrightness(LightLayer.SKY, pos);
        if (MobSpawning.ruled(type, world.getLevel().dimension())) {
            boolean natural = world.getBlockState(pos.below()).is(type == EntityTypes.CREEPER ? MobSpawning.CREEPER_SPAWNABLE : MobSpawning.UNDEAD_OVERWORLD_SPAWNABLE);
            if (!MobSpawning.overworldRules(type, natural, skyLight, pos.getY(), MobSpawning.isMidnight(world))) return false;
        }
        return MobSpawning.darkEnoughToSpawn(world, pos, random, skyLight) && Monster.checkMobSpawnRules(type, world, spawnReason, pos, random);
    }
}
