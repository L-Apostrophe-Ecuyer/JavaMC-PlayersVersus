
package frootloops.versus.mixin.items.brewing;


import frootloops.versus.mod.items.brewing.CustomStatusEffects;
import frootloops.versus.mod.items.brewing.effects.BadOmenStatusEffect;
import frootloops.versus.mod.items.brewing.effects.FireResistanceEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(StatusEffects.class)
public abstract class VanillaStatusEffectsMixin {

    @Shadow
    public static final RegistryEntry<StatusEffect> FIRE_RESISTANCE = CustomStatusEffects.registerOverhauledVanillaEffect("fire_resistance", new FireResistanceEffect("fire_resistance"));

    @Shadow
    public static final RegistryEntry<StatusEffect> BAD_OMEN = CustomStatusEffects.registerOverhauledVanillaEffect("bad_omen", new BadOmenStatusEffect("bad_omen"));

}

