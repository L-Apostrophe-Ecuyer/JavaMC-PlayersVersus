package frootloops.versus.mixin.items_and_effects.conversion;

import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.items_and_effects.conversion.Conversion;
import frootloops.versus.mod.items_and_effects.conversion.ConversionAttacks;
import frootloops.versus.mod.items_and_effects.conversion.Shroomify;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Conversion stacks: a dose on one had adds its amplifier + 1 to it, refreshing it without shortening it. A hit that
 * converted gives nothing else. And the immune (undead, mushroom creatures, golems, bosses) never take it.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityConversionMixin {

    @Shadow @Nullable public abstract MobEffectInstance getEffect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect);

    @ModifyVariable(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), argsOnly = true)
    private MobEffectInstance playersVersus$stackConversion(MobEffectInstance dose) {
        if (!dose.is(CustomStatusEffects.CONVERSION)) return dose;
        MobEffectInstance had = this.getEffect(CustomStatusEffects.CONVERSION);
        if (had == null) return dose;
        return new MobEffectInstance(dose.getEffect(), Conversion.stackedDuration(had.getDuration(), dose.getDuration()),
                Conversion.stackedAmplifier(had.getAmplifier(), dose.getAmplifier()),
                dose.isAmbient(), dose.isVisible(), dose.showIcon());
    }

    /** A hit that converted gives nothing else (ConversionAttacks): the mob's other effects on what it hit are dropped. */
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void playersVersus$onlyConversion(MobEffectInstance effect, @Nullable Entity source, CallbackInfoReturnable<Boolean> info) {
        if (ConversionAttacks.replaced(effect, source, (LivingEntity) (Object) this)) info.setReturnValue(false);
    }

    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void playersVersus$immuneToConversion(MobEffectInstance effect, CallbackInfoReturnable<Boolean> info) {
        if (effect.is(CustomStatusEffects.CONVERSION) && Shroomify.immune((LivingEntity) (Object) this)) info.setReturnValue(false);
    }
}
