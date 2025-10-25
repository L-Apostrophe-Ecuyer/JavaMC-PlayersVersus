package frootloops.versus.mixin.mobs.hostile.illager;

import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Raid.class)
public abstract class RaidMixin {

    @Shadow private final ServerBossBar bar;
    @Shadow private int wavesSpawned;
    @Shadow private Optional<BlockPos> preCalculatedRaidersSpawnLocation;
    private int numPlayerDeaths = 0;
    private boolean isRaidingVillage = false;
    private boolean isRaidingBase = false;
    private ServerWorld serverWorld = null;

    protected RaidMixin(ServerBossBar bar, Optional<BlockPos> preCalculatedRaidersSpawnLocation) {
        this.bar = bar;
        this.preCalculatedRaidersSpawnLocation = preCalculatedRaidersSpawnLocation;
    }


    @Inject(method = "getMaxWaves", at = @At("HEAD"), cancellable = true)
    public void getMaxWaves(Difficulty difficulty, CallbackInfoReturnable<Integer> cir) {
        numPlayerDeaths = 0;
        if(this.serverWorld == null) return;
        if(this.wavesSpawned > 0) return;
        if(preCalculatedRaidersSpawnLocation.isEmpty()) return;

        BlockPos center = preCalculatedRaidersSpawnLocation.get();
        Box boundingBox = new Box(center.getX() - 48.0, center.getY() - 24.0, center.getZ() - 48.0, center.getX() + 48.0, center.getY() + 24.0, center.getZ() + 48.0);
        int numVillagers = serverWorld.getEntitiesByClass(VillagerEntity.class, boundingBox, EntityPredicates.VALID_LIVING_ENTITY).size();
        int difficultyBonus = difficulty == Difficulty.EASY ? 1 : difficulty == Difficulty.NORMAL ? 2 : 3;

        if(numVillagers == 0) {
            isRaidingBase = true;
            cir.setReturnValue(difficultyBonus);
        }
        else {
            isRaidingVillage = true;
            wavesSpawned = 1;
            cir.setReturnValue(Math.min(7, 2 + difficultyBonus + numVillagers/8));
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;isNearOccupiedPointOfInterest(Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean shouldContinueRaid(ServerWorld world, BlockPos pos) {
        if(this.serverWorld == null) this.serverWorld = world;
        if (this.wavesSpawned == 0) return true;
        boolean hasSomeoneDied = false;

        int numParticipatingPlayers = this.bar.getPlayers().size();
        if(numParticipatingPlayers == 0 && world.getTime() % 120L == 0) this.numPlayerDeaths++;
        for (ServerPlayerEntity player : this.bar.getPlayers()) {
            if(!player.isCreative() && !player.isSpectator()) {
                if(player.deathTime == 2 && player.getLastAttacker() instanceof RaiderEntity) {
                    this.numPlayerDeaths++;
                    hasSomeoneDied = true;
                }
            }
        }

        if(numPlayerDeaths > 5 + numParticipatingPlayers) {
            if(hasSomeoneDied)  bar.setName(Text.of("Raid - " + (5 + numParticipatingPlayers - numPlayerDeaths) + " attempts remain"));
            return false;
        }
        else if(world.getOccupiedPointOfInterestDistance(ChunkSectionPos.from(pos)) <= 2) {
            if(hasSomeoneDied)  bar.setName(Text.of("Raid - " + (5 + numParticipatingPlayers - numPlayerDeaths) + " attempts remain"));
            return true;
        }
        if(hasSomeoneDied)  bar.setName(Text.of("Raid - " + (3 + numParticipatingPlayers - numPlayerDeaths) + " attempts remain"));
        return numPlayerDeaths < 3 + numParticipatingPlayers;
    }


    @Inject(method = "moveRaidCenter", at = @At("HEAD"), cancellable = true)
    private void moveRaidCenter(CallbackInfo info) {
        if(isRaidingBase || !isRaidingVillage) info.cancel(); // This is so that the game doesn't keep trying to find a village where there isn't one
    }
}
