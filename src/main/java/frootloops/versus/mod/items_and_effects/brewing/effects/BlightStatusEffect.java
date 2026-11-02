package frootloops.versus.mod.items_and_effects.brewing.effects;

import frootloops.versus.mod.items_and_effects.blight.Undeath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Blight, the Wither's replacement (every Wither given is Blight), from staying in pale shrooms (very slowly), from the
 * pale's hits and from what gave the Wither: hearts are blighted, drawn as the Wither's black hearts, one per amplifier
 * + 1, and each dose adds to the last (LivingEntityBlightMixin). Food doesn't regenerate health into blighted hearts;
 * the blighted shake as more of their hearts are blighted. Fully blighted (as many hearts as the bar has), it deals a
 * heart every half second, as the Wither would, and whatever dies fully blighted is replaced by its pale form, a player
 * by a player zombie (Undeath). Healing and absorption push it back; it wears off, and milk cures it, as any effect.
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
