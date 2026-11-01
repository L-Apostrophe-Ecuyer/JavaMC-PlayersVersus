package frootloops.versus.mod.items_and_effects.brewing.effects;

import frootloops.versus.mod.items_and_effects.blight.Undeath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Blight, the Wither's replacement and the pale shrooms' and the pale's: hearts turn white, one per amplifier + 1, and
 * each dose adds to the last (LivingEntityBlightMixin). Food doesn't regenerate health into white hearts; the blighted
 * shake as more of their hearts whiten, and with all white they turn undead (Undeath): a player dies of it and rises as
 * a player zombie. It wears off, and milk cures it, as any effect.
 */
public class BlightStatusEffect extends MobEffect {
    public static final int COLOR = 0xD4D4CA;

    public BlightStatusEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        Undeath.whileBlighted(level, entity, amplifier);
        return true;
    }
}
