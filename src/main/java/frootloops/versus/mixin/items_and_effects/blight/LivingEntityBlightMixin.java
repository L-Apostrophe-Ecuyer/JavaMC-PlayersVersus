package frootloops.versus.mixin.items_and_effects.blight;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.items_and_effects.blight.Blight;
import frootloops.versus.mod.items_and_effects.blight.BlightAttacks;
import frootloops.versus.mod.items_and_effects.blight.Undeath;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blight replaces the Wither effect: any Wither given is Blight (the same time, the same strength up to a limit), by
 * whatever gave it (a wither skeleton's hit, a wither rose, the wither's skulls, a command). Blight stacks: a dose on
 * one had adds its amplifier + 1 to it, refreshing it without shortening it, however it was given (a hit, a cloud, a
 * shroom, a command). Absorption gained pushes it back. A hit that blighted gives nothing else. The immune (undead, the
 * pale, pillagers, vindicators, witches, wolves, mushroom creatures, golems, bosses) never take it, nor the Wither. Whatever dies fully blighted is replaced by its pale form.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityBlightMixin {

    @Shadow @Nullable public abstract MobEffectInstance getEffect(Holder<MobEffect> effect);

    @ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), argsOnly = true)
    private MobEffectInstance playersVersus$blightStacks(MobEffectInstance dose) {
        dose = Undeath.asBlight(dose);
        if (!dose.is(CustomStatusEffects.BLIGHT)) return dose;
        MobEffectInstance had = this.getEffect(CustomStatusEffects.BLIGHT);
        if (had == null) return dose;
        return new MobEffectInstance(dose.getEffect(), Blight.stackedDuration(had.getDuration(), dose.getDuration()),
                Blight.stackedAmplifier(had.getAmplifier(), dose.getAmplifier()),
                dose.isAmbient(), dose.isVisible(), dose.showIcon());
    }

    /** Forced on, too: a Wither is Blight whichever way it's given (no stacking: forcing sets it as it is). */
    @ModifyVariable(method = "forceAddEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), argsOnly = true)
    private MobEffectInstance playersVersus$forcedWitherIsBlight(MobEffectInstance effect) {
        return Undeath.asBlight(effect);
    }

    /** A Wither already on (a world saved before, or put there some way unforeseen) is turned into Blight at the next tick. */
    @Inject(method = "tick", at = @At("HEAD"))
    private void playersVersus$noWither(CallbackInfo info) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!VersusSettings.Blight.REPLACE_WITHER || self.level().isClientSide()) return;
        MobEffectInstance wither = self.getEffect(MobEffects.WITHER);
        if (wither == null) return;
        self.removeEffect(MobEffects.WITHER);
        self.addEffect(wither);
    }

    /** Absorption gained pushes the Blight back, a blighted heart for each heart of it. */
    @Inject(method = "internalSetAbsorptionAmount", at = @At("HEAD"))
    private void playersVersus$absorptionPurgesBlight(float amount, CallbackInfo info) {
        LivingEntity self = (LivingEntity) (Object) this;
        float gained = amount - self.getAbsorptionAmount();
        if (gained > 0.0F && !self.level().isClientSide()) Undeath.purge(self, Blight.absorptionPurge(gained));
    }

    /** A hit that blighted gives nothing else (BlightAttacks): the mob's other effects on what it hit are dropped. */
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void playersVersus$onlyBlight(MobEffectInstance effect, @Nullable Entity source, CallbackInfoReturnable<Boolean> info) {
        if (BlightAttacks.replaced(effect, source, (LivingEntity) (Object) this)) info.setReturnValue(false);
    }

    /** Blight, and the Wither it replaces, never take on the immune. */
    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void playersVersus$immuneToBlight(MobEffectInstance effect, CallbackInfoReturnable<Boolean> info) {
        boolean blight = effect.is(CustomStatusEffects.BLIGHT) || VersusSettings.Blight.REPLACE_WITHER && effect.is(MobEffects.WITHER);
        if (blight && Undeath.immune((LivingEntity) (Object) this)) info.setReturnValue(false);
    }

    /**
     * Dying fully blighted, of the Blight's own damage or of anything else, the living are replaced by their pale form
     * (Undeath#turn) instead: the death doesn't happen. What has none dies as it would. Players die, and rise a player
     * zombie (ServerPlayerBlightedDeathMixin).
     */
    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void playersVersus$diesPale(DamageSource source, CallbackInfo info) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player || self.isRemoved() || !(self.level() instanceof ServerLevel level)) return;
        if (Undeath.fullyBlighted(self) && Undeath.turn(level, self)) info.cancel();
    }
}
