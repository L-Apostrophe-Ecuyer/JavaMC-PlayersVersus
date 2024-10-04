package frootloops.versus.mod.items.brewing.effects;

import frootloops.versus.VersusMod;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;

public class BuoyancyStatusEffect extends StatusEffect  {
    private static final StatusEffectCategory category = StatusEffectCategory.NEUTRAL;
    public static final int COLOR = 15985696;

    public BuoyancyStatusEffect(String id) {
        super(category, COLOR, ParticleTypes.BUBBLE_COLUMN_UP);

        this.addAttributeModifier(
                EntityAttributes.WATER_MOVEMENT_EFFICIENCY, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                0.2, EntityAttributeModifier.Operation.ADD_VALUE
        );
    }
}
