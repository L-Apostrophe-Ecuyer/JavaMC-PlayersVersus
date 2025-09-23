package frootloops.versus.mixin.mobs.hostile.overworld;

import net.minecraft.entity.mob.Angriness;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.GameEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

// Use fully qualified name with '$' for inner class
@Mixin(targets = "net.minecraft.entity.mob.WardenEntity$VibrationCallback")
public abstract class WardenVibrationListenerMixin {

    @Shadow @Final private WardenEntity field_44600;

    @Overwrite
    public int getRange() {
        return 36;
    }

    @Inject(method = "accepts", at = @At("RETURN"), cancellable = true)
    public void noDistractions(ServerWorld world, BlockPos pos, RegistryEntry<GameEvent> event, GameEvent.Emitter emitter, CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue()) {
            WardenEntity warden = field_44600;
            if(emitter.sourceEntity() == null) cir.setReturnValue(warden.getAnger() < 1);
            else if(!(emitter.sourceEntity() instanceof PlayerEntity)) {
                if(warden.getTarget() instanceof PlayerEntity) cir.setReturnValue(false);
                else {
                    List<ServerPlayerEntity> list = world.getPlayers(player -> player.interactionManager.isSurvivalLike() && warden.isInRange(player, 8.0));
                    if (list.size() != 0) cir.setReturnValue(false);
                }
            }
        }
    }
}