package frootloops.versus.mixin.mobs.hostile.overworld;

import frootloops.versus.VersusSettings;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.ZombieSiegeManager;
import net.minecraft.world.spawner.SpecialSpawner;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ZombieSiegeManager.class)
public class ZombieHordeMixin implements SpecialSpawner {

    @Shadow
    private int remaining, countdown, startX, startY, startZ;

    @Shadow
    private boolean spawned;

    @Shadow @Nullable
    private Vec3d getSpawnVector(ServerWorld world, BlockPos pos) {return null;}

    @Shadow
    private void trySpawnZombie(ServerWorld world) {}


    @Override
    public void spawn(ServerWorld world, boolean spawnMonsters, boolean spawnAnimals) {

        if (world.isDay() || !spawnMonsters) {
            this.remaining = 15;
            this.countdown = 1 + world.random.nextInt(2);
            this.spawned = false;
            return;
        }

        float skyAngle = world.getSkyAngle(0.0f);
        if (skyAngle < 0.4f || skyAngle > 0.5f || world.random.nextInt(20) != 0) return;
        if (!this.tryGettingSpawnLocation(world)) return;
        if (--this.countdown > 0) return;

        int i;
        for(i = 0; i < Math.min(this.remaining, 10); i++) {
            this.trySpawnZombie(world);
        }
        this.spawned = true;
        this.remaining -= i;
    }

    private boolean tryGettingSpawnLocation(ServerWorld world) {
        for (PlayerEntity playerEntity : world.getPlayers()) {
            BlockPos blockPos;
            if (playerEntity.isSpectator() || world.getBiome(blockPos = playerEntity.getBlockPos()).isIn(BiomeTags.WITHOUT_ZOMBIE_SIEGES)) continue;

            boolean canSpawnZombieHorde = VersusSettings.DO_ZOMBIE_SEIGES_OUTSIDE_VILLAGES || world.isNearOccupiedPointOfInterest(blockPos);
            if(!canSpawnZombieHorde) continue;
            for (int i = 0; i < 10; ++i) {
                float f = world.random.nextFloat() * ((float)Math.PI * 2);
                this.startX = blockPos.getX() + MathHelper.floor(MathHelper.cos(f) * 32.0f);
                this.startY = blockPos.getY();
                this.startZ = blockPos.getZ() + MathHelper.floor(MathHelper.sin(f) * 32.0f);
                if(this.getSpawnVector(world, new BlockPos(this.startX, this.startY, this.startZ)) == null) continue;
                if(!this.spawned) this.remaining = canSpawnZombieHorde ? 32 : 20;
                break;
            }
            return true;
        }
        return false;
    }
}
