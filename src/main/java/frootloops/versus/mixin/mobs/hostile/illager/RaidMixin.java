package frootloops.versus.mixin.mobs.hostile.illager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.phys.AABB;
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

    @Shadow private final ServerBossEvent raidEvent;
    @Shadow private int groupsSpawned;
    @Shadow private Optional<BlockPos> waveSpawnPos;
    private int numPlayerDeaths = 0;
    private boolean isRaidingVillage = false;
    private boolean isRaidingBase = false;
    private ServerLevel serverWorld = null;

    protected RaidMixin(ServerBossEvent bar, Optional<BlockPos> preCalculatedRaidersSpawnLocation) {
        this.raidEvent = bar;
        this.waveSpawnPos = preCalculatedRaidersSpawnLocation;
    }


    @Inject(method = "getNumGroups", at = @At("HEAD"), cancellable = true)
    public void getMaxWaves(Difficulty difficulty, CallbackInfoReturnable<Integer> cir) {
        numPlayerDeaths = 0;
        if(this.serverWorld == null) return;
        if(this.groupsSpawned > 0) return;
        if(waveSpawnPos.isEmpty()) return;

        BlockPos center = waveSpawnPos.get();
        AABB boundingBox = new AABB(center.getX() - 48.0, center.getY() - 24.0, center.getZ() - 48.0, center.getX() + 48.0, center.getY() + 24.0, center.getZ() + 48.0);
        int numVillagers = serverWorld.getEntitiesOfClass(Villager.class, boundingBox, EntitySelector.LIVING_ENTITY_STILL_ALIVE).size();
        int difficultyBonus = difficulty == Difficulty.EASY ? 1 : difficulty == Difficulty.NORMAL ? 2 : 3;

        if(numVillagers == 0) {
            isRaidingBase = true;
            cir.setReturnValue(difficultyBonus);
        }
        else {
            isRaidingVillage = true;
            groupsSpawned = 1;
            cir.setReturnValue(Math.min(7, 2 + difficultyBonus + numVillagers/8));
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;isVillage(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean shouldContinueRaid(ServerLevel world, BlockPos pos) {
        if(this.serverWorld == null) this.serverWorld = world;
        if (this.groupsSpawned == 0) return true;
        boolean hasSomeoneDied = false;

        int numParticipatingPlayers = this.raidEvent.getPlayers().size();
        if(numParticipatingPlayers == 0 && world.getGameTime() % 120L == 0) this.numPlayerDeaths++;
        for (ServerPlayer player : this.raidEvent.getPlayers()) {
            if(!player.isCreative() && !player.isSpectator()) {
                if(player.deathTime == 2 && player.getLastAttacker() instanceof Raider) {
                    this.numPlayerDeaths++;
                    hasSomeoneDied = true;
                }
            }
        }

        if(numPlayerDeaths > 5 + numParticipatingPlayers) {
            if(hasSomeoneDied)  raidEvent.setName(Component.nullToEmpty("Raid - " + (5 + numParticipatingPlayers - numPlayerDeaths) + " attempts remain"));
            return false;
        }
        else if(world.sectionsToVillage(SectionPos.of(pos)) <= 2) {
            if(hasSomeoneDied)  raidEvent.setName(Component.nullToEmpty("Raid - " + (5 + numParticipatingPlayers - numPlayerDeaths) + " attempts remain"));
            return true;
        }
        if(hasSomeoneDied)  raidEvent.setName(Component.nullToEmpty("Raid - " + (3 + numParticipatingPlayers - numPlayerDeaths) + " attempts remain"));
        return numPlayerDeaths < 3 + numParticipatingPlayers;
    }


    @Inject(method = "moveRaidCenterToNearbyVillageSection", at = @At("HEAD"), cancellable = true)
    private void moveRaidCenter(CallbackInfo info) {
        if(isRaidingBase || !isRaidingVillage) info.cancel(); // This is so that the game doesn't keep trying to find a village where there isn't one
    }
}
