package frootloops.versus.mod.items_and_effects.brewing.effects;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.CustomSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class BuoyancyStatusEffect extends MobEffect  {
    private static final MobEffectCategory category = MobEffectCategory.NEUTRAL;
    public static final int COLOR = 15985696;

    public BuoyancyStatusEffect(String id) {
        super(category, COLOR, CustomSpecialEffects.BUYANCY_EFFECT_PARTICLE);

        this.addAttributeModifier(
                Attributes.WATER_MOVEMENT_EFFICIENCY, ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "effect." + id),
                0.2, AttributeModifier.Operation.ADD_VALUE
        );
    }
}
