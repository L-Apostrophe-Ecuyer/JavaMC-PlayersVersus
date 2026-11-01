package frootloops.versus.mixin.items_and_effects.blight;

import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.mobs.hostile.overworld.PlayerZombieEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A player who dies of the Wither while blighted (the Blight deals the Wither when all their hearts are white) is
 * converted: they rise where they died as a player zombie, wearing and carrying what they had, their things moving to it
 * before the death drops them (PlayerZombieEntity#rise). Dying blighted of anything else is dying.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerBlightedDeathMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void playersVersus$blightedDeathRises(DamageSource source, CallbackInfo info) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        if (self.isRemoved() || self.isSpectator() || !source.is(DamageTypes.WITHER) || !self.hasEffect(CustomStatusEffects.BLIGHT)) return;
        if (self.level() instanceof ServerLevel level) PlayerZombieEntity.rise(level, self, false);
    }
}
