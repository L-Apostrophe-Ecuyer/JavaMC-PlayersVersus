
package frootloops.versus.mixin.items.brewing;


import frootloops.versus.mod.items.brewing.CustomStatusEffects;
import frootloops.versus.mod.items.brewing.effects.FireResistanceEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatusEffects.class)
public abstract class VanillaStatusEffectsMixin {

    //@Shadow public static final RegistryEntry<StatusEffect> FIRE_RESISTANCE = CustomStatusEffects.registerOverhauledVanillaEffect("fire_resistance", new FireResistanceEffect("fire_resistance"));

    @Inject(method = "register", at = @At("HEAD"), cancellable = true)
    private static void register(String id, StatusEffect statusEffect, CallbackInfoReturnable<RegistryEntry<StatusEffect>> cir) {
        if(id == "fire_resistance") cir.setReturnValue(CustomStatusEffects.registerOverhauledVanillaEffect("fire_resistance", new FireResistanceEffect("fire_resistance")));
    }
}

