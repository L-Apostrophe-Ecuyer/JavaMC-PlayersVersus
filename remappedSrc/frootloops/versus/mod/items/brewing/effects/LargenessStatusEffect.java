package frootloops.versus.mod.items.brewing.effects;

import frootloops.versus.VersusMod;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;

public class LargenessStatusEffect extends StatusEffect  {
    private static final StatusEffectCategory category = StatusEffectCategory.NEUTRAL;
    private static final int color = 10359374;

    public LargenessStatusEffect(String id) {
        super(category, color);

        this.addAttributeModifier(
                EntityAttributes.SCALE, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                0.3, EntityAttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                EntityAttributes.STEP_HEIGHT, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                0.25, EntityAttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                EntityAttributes.ATTACK_DAMAGE, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                2.0, EntityAttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                EntityAttributes.ATTACK_SPEED, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                -0.2, EntityAttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                EntityAttributes.MOVEMENT_SPEED, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                -0.005, EntityAttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                EntityAttributes.ENTITY_INTERACTION_RANGE, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                1.0, EntityAttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                EntityAttributes.BLOCK_INTERACTION_RANGE, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                1.0, EntityAttributeModifier.Operation.ADD_VALUE
        );
    }


}
