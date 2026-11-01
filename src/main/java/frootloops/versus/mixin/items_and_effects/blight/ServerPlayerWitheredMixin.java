package frootloops.versus.mixin.items_and_effects.blight;

import frootloops.versus.mod.items_and_effects.blight.Undeath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A player withered to death rises as a pale zombie where they fell, in their armour (Undeath#risePale). */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerWitheredMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void playersVersus$risePale(DamageSource source, CallbackInfo info) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (source.is(DamageTypes.WITHER) && !self.isSpectator() && self.level() instanceof ServerLevel level) Undeath.risePale(level, self);
    }
}
