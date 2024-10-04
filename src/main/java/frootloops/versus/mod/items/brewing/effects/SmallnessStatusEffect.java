package frootloops.versus.mod.items.brewing.effects;

import frootloops.versus.VersusMod;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;

public class SmallnessStatusEffect extends StatusEffect  {
    private static final StatusEffectCategory category = StatusEffectCategory.NEUTRAL;
    public static final int COLOR = 22790024;

    public SmallnessStatusEffect(String id) {
        super(category, COLOR, ParticleTypes.SMOKE);

        this.addAttributeModifier(
                EntityAttributes.GENERIC_SCALE, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                -0.3, EntityAttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                EntityAttributes.GENERIC_STEP_HEIGHT, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                -0.25, EntityAttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                EntityAttributes.GENERIC_ATTACK_DAMAGE, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                -2.0, EntityAttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                EntityAttributes.GENERIC_ATTACK_SPEED, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                0.2, EntityAttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                EntityAttributes.GENERIC_MOVEMENT_SPEED, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                0.005, EntityAttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                -1.0, EntityAttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                -1.0, EntityAttributeModifier.Operation.ADD_VALUE
        );
    }


}
