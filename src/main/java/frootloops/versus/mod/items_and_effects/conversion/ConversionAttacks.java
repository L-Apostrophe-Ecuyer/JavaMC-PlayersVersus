package frootloops.versus.mod.items_and_effects.conversion;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.mobs.hostile.overworld.PaleZombieEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Mobs whose hits convert: a pale zombie's and a creaking's always give a dose of Conversion; any other zombie's, by
 * {@link VersusSettings.Mushrooms#ZOMBIE_CONVERSION_CHANCE}. A hit that converts gives nothing else: whatever effect
 * the mob adds to what it hit in the same tick (a husk's hunger) is dropped (LivingEntityConversionMixin).
 */
public final class ConversionAttacks {
    /** Each mob's last hit that converted: what it hit (its id: the map holds no entity but its keys), and when. */
    private record Hit(int target, long tick) {}

    private static final Map<Mob, Hit> CONVERTING_HITS = new WeakHashMap<>();

    private ConversionAttacks() {}

    /** A dose of Conversion: one white heart more, for the effect's whole time. */
    public static MobEffectInstance dose() {
        return new MobEffectInstance(CustomStatusEffects.CONVERSION, VersusSettings.Mushrooms.EFFECT_TICKS, 0);
    }

    /** A mob's melee hit landed (Mob#doHurtTarget). */
    public static void afterHit(ServerLevel level, Mob attacker, LivingEntity target) {
        boolean always = attacker instanceof PaleZombieEntity || attacker instanceof Creaking;
        boolean rarely = attacker instanceof Zombie && attacker.getRandom().nextFloat() < VersusSettings.Mushrooms.ZOMBIE_CONVERSION_CHANCE;
        if (!always && !rarely) return;
        CONVERTING_HITS.put(attacker, new Hit(target.getId(), level.getGameTime()));
        target.addEffect(dose(), attacker);
    }

    /** Whether an effect from this source is one a converting hit replaces: anything but Conversion, on what it hit, this tick. */
    public static boolean replaced(MobEffectInstance effect, @Nullable Entity source, LivingEntity target) {
        if (!(source instanceof Mob mob) || effect.is(CustomStatusEffects.CONVERSION)) return false;
        Hit hit = CONVERTING_HITS.get(mob);
        return hit != null && hit.target == target.getId() && hit.tick == mob.level().getGameTime();
    }
}
