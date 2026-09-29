package frootloops.versus.mixin.mobs.hostile.overworld;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.WorldTime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.village.VillageSiege;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VillageSiege.class)
public abstract class ZombieHordeMixin implements CustomSpawner {

    @Shadow
    private int zombiesToSpawn, nextSpawnTime, spawnX, spawnY, spawnZ;

    @Shadow
    private boolean hasSetupSiege;

    @Shadow @Nullable
    private Vec3 findRandomSpawnPos(ServerLevel world, BlockPos pos) {return null;}

    @Shadow
    private void trySpawn(ServerLevel world) {}


    @Override
    public void tick(ServerLevel world, boolean spawnMonsters) {

        if (world.isBrightOutside() || !spawnMonsters) {
            this.zombiesToSpawn = 15;
            this.nextSpawnTime = 1 + world.getRandom().nextInt(2);
            this.hasSetupSiege = false;
            return;
        }

        float skyAngle = WorldTime.timeOfDay(WorldTime.dayTime(world));
        if (skyAngle < 0.4f || skyAngle > 0.5f || world.getRandom().nextInt(20) != 0) return;
        if (!this.tryGettingSpawnLocation(world)) return;
        if (--this.nextSpawnTime > 0) return;

        int i;
        for(i = 0; i < Math.min(this.zombiesToSpawn, 10); i++) {
            this.trySpawn(world);
        }
        this.hasSetupSiege = true;
        this.zombiesToSpawn -= i;
    }

    private boolean tryGettingSpawnLocation(ServerLevel world) {
        for (Player playerEntity : world.players()) {
            BlockPos blockPos;
            if (playerEntity.isSpectator() || world.getBiome(blockPos = playerEntity.blockPosition()).is(BiomeTags.WITHOUT_ZOMBIE_SIEGES)) continue;

            boolean canSpawnZombieHorde = VersusSettings.Gameplay.DO_ZOMBIE_SEIGES_OUTSIDE_VILLAGES || world.isVillage(blockPos);
            if(!canSpawnZombieHorde) continue;
            for (int i = 0; i < 10; ++i) {
                float f = world.getRandom().nextFloat() * ((float)Math.PI * 2);
                this.spawnX = blockPos.getX() + Mth.floor(Mth.cos(f) * 32.0f);
                this.spawnY = blockPos.getY();
                this.spawnZ = blockPos.getZ() + Mth.floor(Mth.sin(f) * 32.0f);
                if(this.findRandomSpawnPos(world, new BlockPos(this.spawnX, this.spawnY, this.spawnZ)) == null) continue;
                if(!this.hasSetupSiege) this.zombiesToSpawn = canSpawnZombieHorde ? 32 : 20;
                break;
            }
            return true;
        }
        return false;
    }
}
