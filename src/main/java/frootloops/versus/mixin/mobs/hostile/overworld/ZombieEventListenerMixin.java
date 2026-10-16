package frootloops.versus.mixin.mobs.hostile.overworld;

import frootloops.versus.mod.mobs.hostile.overworld.ZombieSoundListener;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ZombieEventListenerMixin extends Level {

    protected ZombieEventListenerMixin(WritableLevelData properties, ResourceKey<Level> registryRef, RegistryAccess registryManager, Holder<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    @Inject(method = "gameEvent", at = @At("HEAD"))
    public void emitGameEvent(Holder<GameEvent> event, Vec3 emitterPos, GameEvent.Context emitter, CallbackInfo info) {
        ZombieSoundListener.OnGameEvent((ServerLevel) ((Object) this), event.value(), emitterPos, emitter);
    }
}
