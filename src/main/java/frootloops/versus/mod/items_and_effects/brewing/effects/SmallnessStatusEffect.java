package frootloops.versus.mod.items_and_effects.brewing.effects;

import frootloops.versus.VersusMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class SmallnessStatusEffect extends MobEffect  {
    private static final MobEffectCategory category = MobEffectCategory.NEUTRAL;
    public static final int COLOR = 22790024;

    public SmallnessStatusEffect(String id) {
        super(category, COLOR);

        this.addAttributeModifier(
                Attributes.SCALE, ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "effect." + id),
                -0.3, AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                Attributes.STEP_HEIGHT, ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "effect." + id),
                -0.25, AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                Attributes.ATTACK_DAMAGE, ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "effect." + id),
                -2.0, AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                Attributes.ATTACK_SPEED, ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "effect." + id),
                0.2, AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED, ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "effect." + id),
                0.005, AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                Attributes.ENTITY_INTERACTION_RANGE, ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "effect." + id),
                -1.0, AttributeModifier.Operation.ADD_VALUE
        );

        this.addAttributeModifier(
                Attributes.BLOCK_INTERACTION_RANGE, ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "effect." + id),
                -1.0, AttributeModifier.Operation.ADD_VALUE
        );
    }


}
