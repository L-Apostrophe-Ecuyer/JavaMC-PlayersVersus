package frootloops.versus.mixin.mobs.hostile.overworld;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;

// Use fully qualified name with '$' for inner class
@Mixin(targets = "net.minecraft.world.entity.monster.warden.Warden$VibrationUser")
public abstract class WardenVibrationListenerMixin {

    @Shadow @Final private Warden field_44600;

    @Overwrite
    public int getListenerRadius() {
        return 36;
    }

    @Inject(method = "canReceiveVibration", at = @At("RETURN"), cancellable = true)
    public void noDistractions(ServerLevel world, BlockPos pos, Holder<GameEvent> event, GameEvent.Context emitter, CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue()) {
            Warden warden = field_44600;
            if(emitter.sourceEntity() == null) cir.setReturnValue(warden.getClientAngerLevel() < 1);
            else if(!(emitter.sourceEntity() instanceof Player)) {
                if(warden.getTarget() instanceof Player) cir.setReturnValue(false);
                else {
                    List<ServerPlayer> list = world.getPlayers(player -> player.gameMode.isSurvival() && warden.closerThan(player, 8.0));
                    if (list.size() != 0) cir.setReturnValue(false);
                }
            }
        }
    }
}