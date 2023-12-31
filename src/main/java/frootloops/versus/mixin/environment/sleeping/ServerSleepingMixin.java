package frootloops.versus.mixin.environment.sleeping;

import frootloops.versus.ServerSettings;
import frootloops.versus.VersusMod;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.SleepManager;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
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
    private static SleepManager cachedSleepManager = null;

    protected ServerSleepingMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long biomeAccess, int maxChainedNeighborUpdates, MutableWorldProperties properties1, List<ServerPlayerEntity> players, SleepManager sleepManager) {
        super(properties, registryRef, registryManager, dimensionEntry, profiler, isClient, debugWorld, biomeAccess, maxChainedNeighborUpdates);
    }


    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/SleepManager;canResetTime(ILjava/util/List;)Z"))
    public boolean checkOnEepyPlayers(SleepManager sleepManager, int percentage, List<ServerPlayerEntity> players) {
        cachedSleepManager = sleepManager;
        if(this.isEveryoneTuckedIn()) { // If everyone is asleep, make time go by quick:
            ServerSettings.isTimeFastForwarding = true;
            this.setTimeOfDay((this.properties.getTimeOfDay() + 1) % 24000);
            if(this.properties.getTimeOfDay() % 40 == 0) VersusMod.MOD_LOGGER.warn("Time should be moving quickly...");
        }
        else if(ServerSettings.isTimeFastForwarding) { // Otherwise, check if we need to wake up:
            this.riseAndGrind();
        }
        return false; // Return false to cancel vanilla time skip:
    }

    @Inject(method = "tick", at = @At(value = "TAIL"))
    public void stopIfPlayerWokeUp(BooleanSupplier shouldKeepTicking, CallbackInfo info) {
        if(ServerSettings.isTimeFastForwarding && !this.isEveryoneTuckedIn()) this.riseAndGrind();
    }

    private void riseAndGrind() {
        if(this.properties.getTimeOfDay() >= 23300) {
            if(cachedSleepManager != null) cachedSleepManager.clearSleeping();
            (this.getPlayers().stream().filter(LivingEntity::isSleeping).collect(Collectors.toList())).forEach((PlayerEntity player) -> {
                player.wakeUp(false, false);
            });
        }
        ServerSettings.isTimeFastForwarding = false;
    }

    private boolean isEveryoneTuckedIn() {
        if(this.properties.getTimeOfDay() > 12999 && this.properties.getTimeOfDay() < 23300) {
            for (PlayerEntity player : this.getPlayers()) {
                if (player.hurtTime > 0 || !player.canResetTimeBySleeping()) return false;
            }
            return true;
        }
        return false;
    }
}
