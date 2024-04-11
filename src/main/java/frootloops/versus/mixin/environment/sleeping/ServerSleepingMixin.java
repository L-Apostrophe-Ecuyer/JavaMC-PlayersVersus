package frootloops.versus.mixin.environment.sleeping;

import frootloops.versus.VersusSettings;
import net.minecraft.entity.EntityPose;
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
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.tick.TickManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Mixin(ServerWorld.class)
public abstract class ServerSleepingMixin extends World {
    @Shadow public void setTimeOfDay(long timeOfDay) {}
    @Shadow final List<ServerPlayerEntity> players;
    private static SleepManager cachedSleepManager = null;

    @Shadow private final MinecraftServer server;

    @Shadow public TickManager getTickManager() {return this.server.getTickManager();}

    protected ServerSleepingMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long biomeAccess, int maxChainedNeighborUpdates, MutableWorldProperties properties1, List<ServerPlayerEntity> players, SleepManager sleepManager, List<ServerPlayerEntity> players1, MinecraftServer server) {
        super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
        this.players = players1;
        this.server = server;
    }


    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/SleepManager;canResetTime(ILjava/util/List;)Z"))
    public boolean checkOnEepyPlayers(SleepManager sleepManager, int percentage, List<ServerPlayerEntity> players) {
        if(VersusSettings.DO_SLEEP_OVERHAUL) {
            cachedSleepManager = sleepManager;
            return false; // Return false to cancel vanilla time skip
        }
        else {
            return sleepManager.canResetTime(percentage,players);
        }
    }

    @Inject(method = "tick", at = @At(value = "TAIL"))
    public void stopIfPlayerWokeUp(BooleanSupplier shouldKeepTicking, CallbackInfo info) {
        if(VersusSettings.DO_SLEEP_OVERHAUL == false) {
            return;
        }
        if(players.size() == 0) return;

        long timeOfDay = this.properties.getTimeOfDay();
        if(timeOfDay % 20l != 0) return;
        timeOfDay = timeOfDay % 24000l;

        boolean startSleepingThroughNight = true;
        if(timeOfDay > 12999l && timeOfDay < 23300l) {
            for (PlayerEntity player : players) {
                if (player.hurtTime > 0 || player.getPose() != EntityPose.SLEEPING) {
                    startSleepingThroughNight = false;
                    break;
                }
            }
        }
        else {
            startSleepingThroughNight = false;
        }

        // Check if we need to update tick rate based off of sleeping players:
        TickManager tickManager = this.getTickManager();
        if(startSleepingThroughNight) {
            // If everyone is asleep, make time go by quick
            // And make nearby hostiles target players, to test their shelters
            if(tickManager.getTickRate() == 20.0f) {
                tickManager.setTickRate(800.0f);

                for (PlayerEntity player : players) {
                    Vec3d pos = player.getPos();
                    Box boundingBox = new Box(pos.x - 32.0, pos.y - 12.0, pos.z - 32.0, pos.x + 32.0, pos.y + 12.0, pos.z + 32.0);
                    List<HostileEntity> hostilesNearby = this.getEntitiesByClass(HostileEntity.class, boundingBox, EntityPredicates.VALID_LIVING_ENTITY);
                    for (HostileEntity hostile : hostilesNearby) {
                        if (hostile.getNavigation().isIdle()) {
                            if (hostile.getTarget() == null) hostile.setTarget(player);
                            else hostile.getNavigation().startMovingTo(pos.x, pos.y, pos.z, 1.5);
                        }
                    }
                }
            }
        }
        else if(tickManager.getTickRate() == 800.0f) {
            // Otherwise, wake everyone up and reset tick rate
            tickManager.setTickRate(20.0f);
            if(cachedSleepManager != null) cachedSleepManager.clearSleeping();
            (this.getPlayers().stream().filter(LivingEntity::isSleeping).collect(Collectors.toList())).forEach((PlayerEntity player) -> {player.wakeUp(false, false);});
        }
    }
}
