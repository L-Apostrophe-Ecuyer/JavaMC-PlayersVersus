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
    private static final int color = 1950417;

    public BuoyancyStatusEffect(String id) {
        super(category, color, ParticleTypes.ITEM_COBWEB);

        this.addAttributeModifier(
                EntityAttributes.GENERIC_WATER_MOVEMENT_EFFICIENCY, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                0.2, EntityAttributeModifier.Operation.ADD_VALUE
        );
    }


}
