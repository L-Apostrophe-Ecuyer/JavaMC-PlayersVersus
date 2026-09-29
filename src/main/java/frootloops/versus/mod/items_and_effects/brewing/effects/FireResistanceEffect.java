package frootloops.versus.mod.items_and_effects.brewing.effects;

import frootloops.versus.VersusMod;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class FireResistanceEffect extends MobEffect {

    private static final MobEffectCategory category = MobEffectCategory.BENEFICIAL;
    private static final int color = 0xFF9900;

    public FireResistanceEffect(String id) {
        super(category, color, ParticleTypes.SMOKE);

        this.addAttributeModifier(
                Attributes.BURNING_TIME, ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "effect." + id),
                -0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
    }
}
