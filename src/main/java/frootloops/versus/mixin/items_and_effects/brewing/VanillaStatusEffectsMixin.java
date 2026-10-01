
package frootloops.versus.mixin.items_and_effects.brewing;


import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.items_and_effects.brewing.effects.FireResistanceEffect;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffects.class)
public abstract class VanillaStatusEffectsMixin {

    //@Shadow public static final RegistryEntry<StatusEffect> FIRE_RESISTANCE = CustomStatusEffects.registerOverhauledVanillaEffect("fire_resistance", new FireResistanceEffect("fire_resistance"));

    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private static void register(String id, MobEffect statusEffect, CallbackInfoReturnable<Holder<MobEffect>> cir) {
        if(id == "fire_resistance") cir.setReturnValue(CustomStatusEffects.registerOverhauledVanillaEffect("fire_resistance", new FireResistanceEffect("fire_resistance")));
    }
}

