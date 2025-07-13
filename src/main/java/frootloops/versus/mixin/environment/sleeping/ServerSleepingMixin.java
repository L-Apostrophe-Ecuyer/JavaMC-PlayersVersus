package frootloops.versus.mixin.environment.sleeping;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.SleepManager;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.tick.TickManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.stream.Collectors;

import static frootloops.versus.VersusSettings.isFastForwardingTime;


@Mixin(ServerWorld.class)
public abstract class ServerSleepingMixin extends World {
    protected ServerSleepingMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates, List<ServerPlayerEntity> players, SleepManager sleepManager, MinecraftServer server, SleepManager sleepManager1) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
        this.server = server;
        this.sleepManager = sleepManager1;
    }

    @Shadow private final MinecraftServer server;
    @Shadow private final SleepManager sleepManager;
    @Shadow public TickManager getTickManager() {return this.server.getTickManager();}

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/SleepManager;canSkipNight(I)Z"))
    public boolean checkOnEepyPlayers(SleepManager sleepManager, int percentage) {
        if(VersusSettings.DO_SLEEP_OVERHAUL) percentage = 100;
        boolean canSkipNight = sleepManager.canSkipNight(percentage);
        if(VersusSettings.DO_SLEEP_OVERHAUL) {

            // If everyone is asleep, make time go by quick
            // And make nearby hostiles target players, to test their shelters
            List<ServerPlayerEntity> players = ((ServerWorld) ((Object)this)).getPlayers();
            if(players.size() == 0) return false;

            if(canSkipNight && !isFastForwardingTime && sleepManager.canResetTime(percentage, players)) startFastForwardingTime(players);
            else if(isFastForwardingTime && (!canSkipNight || !sleepManager.canResetTime(percentage, players))) stopFastForwardingTime(players);

            // Return false to cancel vanilla time skip
            return false;
        }
        return canSkipNight;
    }


    private void stopFastForwardingTime(List<ServerPlayerEntity> players) {
        isFastForwardingTime = false;
        ((ServerWorld) ((Object)this)).resetWeather();
        (players.stream().filter(LivingEntity::isSleeping).collect(Collectors.toList())).forEach(player -> player.wakeUp(false, false));
        this.sleepManager.clearSleeping();

        TickManager tickManager = this.server.getTickManager();
        if(tickManager.getTickRate() == VersusSettings.SLEEP_TICK_SPEED) tickManager.setTickRate(20.0f);
    }

    private void startFastForwardingTime(List<ServerPlayerEntity> players) {
        isFastForwardingTime = true;
        TickManager tickManager = this.server.getTickManager();
        if(tickManager.getTickRate() == 20.0f) {
            tickManager.setTickRate(VersusSettings.SLEEP_TICK_SPEED);

            for (PlayerEntity player : players) {
                Vec3d pos = player.getPos();
                Box boundingBox = new Box(pos.x - 24.0, pos.y - 12.0, pos.z - 24.0, pos.x + 24.0, pos.y + 12.0, pos.z + 24.0);
                List<HostileEntity> hostilesNearby = this.getEntitiesByClass(HostileEntity.class, boundingBox, EntityPredicates.VALID_LIVING_ENTITY);
                for (HostileEntity hostile : hostilesNearby) {
                    if (this.random.nextBoolean() && hostile.getNavigation().isIdle()) {
                        if (hostile.getTarget() == null && hostile.canSee(player)) hostile.setTarget(player);
                        else hostile.getNavigation().startMovingTo(pos.x, pos.y, pos.z, 1.5);
                    }
                }
            }
        }
    }
}
