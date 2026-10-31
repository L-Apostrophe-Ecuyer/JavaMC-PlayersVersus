package frootloops.versus.mod.items_and_effects.brewing.effects;

import frootloops.versus.mod.items_and_effects.conversion.Shroomify;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Conversion, from staying in pale shrooms: hearts turn white, one per amplifier + 1, and each dose adds to the last
 * (LivingEntityConversionMixin). Food doesn't regenerate health into white hearts; all white, the entity shakes and
 * shroomifies (Shroomify). It wears off, and milk cures it, as any effect.
 */
public class ConversionStatusEffect extends MobEffect {
    public static final int COLOR = 0xD4D4CA;

    public ConversionStatusEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        Shroomify.whileConverting(level, entity, amplifier);
        return true;
    }
}
