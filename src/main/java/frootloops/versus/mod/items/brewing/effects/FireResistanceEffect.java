package frootloops.versus.mod.items.brewing.effects;

import frootloops.versus.VersusMod;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;

public class FireResistanceEffect extends StatusEffect {

    private static final StatusEffectCategory category = StatusEffectCategory.BENEFICIAL;
    private static final int color = 0xFF9900;

    public FireResistanceEffect(String id) {
        super(category, color, ParticleTypes.SMOKE);

        this.addAttributeModifier(
                EntityAttributes.GENERIC_BURNING_TIME, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                -0.4, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }
}
