package frootloops.versus.mixin.environment.sleeping;

import frootloops.versus.VersusSettings;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.stream.Collectors;

import static frootloops.versus.VersusSettings.Gameplay.isFastForwardingTime;


@Mixin(ServerLevel.class)
public abstract class ServerSleepingMixin extends Level {
    protected ServerSleepingMixin(WritableLevelData properties, ResourceKey<Level> registryRef, RegistryAccess registryManager, Holder<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates, List<ServerPlayer> players, SleepStatus sleepManager, MinecraftServer server, SleepStatus sleepManager1) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
        this.server = server;
        this.sleepStatus = sleepManager1;
    }

    @Shadow private final MinecraftServer server;
    @Shadow private final SleepStatus sleepStatus;
    @Shadow public TickRateManager tickRateManager() {return this.server.tickRateManager();}

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/SleepStatus;areEnoughSleeping(I)Z"))
    public boolean checkOnEepyPlayers(SleepStatus sleepManager, int percentage) {
        if(VersusSettings.Gameplay.DO_SLEEP_OVERHAUL) percentage = 100;
        boolean canSkipNight = sleepManager.areEnoughSleeping(percentage);
        if(VersusSettings.Gameplay.DO_SLEEP_OVERHAUL) {

            // If everyone is asleep, make time go by quick
            // And make nearby hostiles target players, to test their shelters
            List<ServerPlayer> players = ((ServerLevel) ((Object)this)).players();
            if(players.size() == 0) return false;

            if(canSkipNight && !isFastForwardingTime && sleepManager.areEnoughDeepSleeping(percentage, players)) startFastForwardingTime(players);
            else if(isFastForwardingTime && (!canSkipNight || !sleepManager.areEnoughDeepSleeping(percentage, players))) stopFastForwardingTime(players);

            // Return false to cancel vanilla time skip
            return false;
        }
        return canSkipNight;
    }


    private void stopFastForwardingTime(List<ServerPlayer> players) {
        isFastForwardingTime = false;
        ((ServerLevel) ((Object)this)).resetWeatherCycle();
        (players.stream().filter(LivingEntity::isSleeping).collect(Collectors.toList())).forEach(player -> player.stopSleepInBed(false, false));
        this.sleepStatus.removeAllSleepers();

        TickRateManager tickManager = this.server.tickRateManager();
        if(tickManager.tickrate() == VersusSettings.Gameplay.SLEEP_TICK_SPEED) tickManager.setTickRate(20.0f);
    }

    private void startFastForwardingTime(List<ServerPlayer> players) {
        isFastForwardingTime = true;
        TickRateManager tickManager = this.server.tickRateManager();
        if(tickManager.tickrate() == 20.0f) {
            tickManager.setTickRate(VersusSettings.Gameplay.SLEEP_TICK_SPEED);

            for (Player player : players) {
                Vec3 pos = player.position();
                AABB boundingBox = new AABB(pos.x - 24.0, pos.y - 12.0, pos.z - 24.0, pos.x + 24.0, pos.y + 12.0, pos.z + 24.0);
                List<Monster> hostilesNearby = this.getEntitiesOfClass(Monster.class, boundingBox, EntitySelector.LIVING_ENTITY_STILL_ALIVE);
                for (Monster hostile : hostilesNearby) {
                    if (this.random.nextBoolean() && hostile.getNavigation().isDone()) {
                        if (hostile.getTarget() == null && hostile.hasLineOfSight(player)) hostile.setTarget(player);
                        else hostile.getNavigation().moveTo(pos.x, pos.y, pos.z, 1.5);
                    }
                }
            }
        }
    }
}
