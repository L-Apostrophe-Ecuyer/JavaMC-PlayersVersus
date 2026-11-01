package frootloops.versus.mixin.items_and_effects.blight;

import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.mobs.hostile.overworld.PlayerZombieEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A player who dies blighted, of whatever (a hit, a fall, the Blight itself), rises where they died as a player zombie,
 * wearing and carrying what they had: their things move to it before the death drops them (PlayerZombieEntity#rise).
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerBlightedDeathMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void playersVersus$blightedDeathRises(DamageSource source, CallbackInfo info) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (self.isRemoved() || self.isSpectator() || !self.hasEffect(CustomStatusEffects.BLIGHT)) return;
        if (self.level() instanceof ServerLevel level) PlayerZombieEntity.rise(level, self, false);
    }
}
