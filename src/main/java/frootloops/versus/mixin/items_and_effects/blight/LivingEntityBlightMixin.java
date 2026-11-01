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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Blight replaces the Wither effect: any Wither given is Blight (the same time, the same strength up to a limit), by
 * whatever gave it (a wither skeleton's hit, a wither rose, the wither's skulls, a command). Blight stacks: a dose on
 * one had adds its amplifier + 1 to it, refreshing it without shortening it, however it was given (a hit, a cloud, a
 * shroom, a command). A hit that blighted gives nothing else. The immune (undead, mushroom creatures, golems, bosses)
 * never take it. And blighted, hurt low, a zombie, creeper, spider, villager, trader, illager or witch turns pale.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityBlightMixin {

    @Shadow @Nullable public abstract MobEffectInstance getEffect(Holder<MobEffect> effect);

    @ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), argsOnly = true)
    private MobEffectInstance playersVersus$blightStacks(MobEffectInstance dose) {
        if (VersusSettings.Blight.REPLACE_WITHER && dose.is(MobEffects.WITHER)) {
            dose = new MobEffectInstance(CustomStatusEffects.BLIGHT, dose.getDuration(), Blight.fromWitherAmplifier(dose.getAmplifier()),
                    dose.isAmbient(), dose.isVisible(), dose.showIcon());
        }
        if (!dose.is(CustomStatusEffects.BLIGHT)) return dose;
        MobEffectInstance had = this.getEffect(CustomStatusEffects.BLIGHT);
        if (had == null) return dose;
        return new MobEffectInstance(dose.getEffect(), Blight.stackedDuration(had.getDuration(), dose.getDuration()),
                Blight.stackedAmplifier(had.getAmplifier(), dose.getAmplifier()),
                dose.isAmbient(), dose.isVisible(), dose.showIcon());
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

    /** Hurt while blighted, to below the limit, what can turn pale does (players don't: they rise a player zombie if they die). */
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void playersVersus$blightedAndWoundedTurnPale(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!info.getReturnValueZ() || self instanceof Player || self.isRemoved() || !self.isAlive()) return;
        if (!self.hasEffect(CustomStatusEffects.BLIGHT) || !Blight.paleOnWound(self.getHealth())) return;
        Undeath.turnPale(level, self);
    }
}
