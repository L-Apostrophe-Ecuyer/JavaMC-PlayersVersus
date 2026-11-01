package frootloops.versus.mod.environment.blocks.mushrooms;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Staying in pale shrooms: each DOSE_TICKS a living thing spends in them gives it a dose of Blight. Leaving them
 * starts the count over; several shrooms around it count once a tick.
 */
public final class BlightExposure {
    /** A gap this long in a stay (ticks) ends it. */
    private static final int GAP = 3;
    private static final Map<Entity, Stay> STAYS = new WeakHashMap<>();

    private record Stay(long lastTick, int ticks) {}

    private BlightExposure() {}

    public static void inside(ServerLevel level, Entity entity) {
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || entity.isSpectator()) return;
        long now = level.getGameTime();
        Stay stay = STAYS.get(entity);
        if (stay != null && stay.lastTick == now) return;
        int ticks = stay != null && stay.lastTick >= now - GAP ? stay.ticks + 1 : 1;
        if (ticks >= VersusSettings.Mushrooms.DOSE_TICKS) {
            living.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, VersusSettings.Mushrooms.EFFECT_TICKS, 0));
            ticks = 0;
        }
        STAYS.put(entity, new Stay(now, ticks));
    }
}
