package frootloops.versus.mod.items.brewing.effects;

import frootloops.versus.VersusMod;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;

public class BignessStatusEffect extends StatusEffect  {
    private static final StatusEffectCategory category = StatusEffectCategory.NEUTRAL;
    private static final int color = 0;

    public BignessStatusEffect(String id) {
        super(category, color);

        this.addAttributeModifier(
                EntityAttributes.GENERIC_SCALE, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                0.5, EntityAttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                EntityAttributes.GENERIC_STEP_HEIGHT, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                1.0, EntityAttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                EntityAttributes.GENERIC_ATTACK_DAMAGE, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                1.0, EntityAttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                EntityAttributes.GENERIC_ATTACK_SPEED, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                -0.4, EntityAttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                EntityAttributes.GENERIC_MOVEMENT_SPEED, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                -0.01, EntityAttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                1.0, EntityAttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                EntityAttributes.PLAYER_BLOCK_INTERACTION_RANGE, Identifier.of(VersusMod.MOD_ID, "effect." + id),
                1.0, EntityAttributeModifier.Operation.ADD_VALUE
        );
    }


}
