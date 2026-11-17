package frootloops.versus.mod.environment.blocks.sulfur;

import frootloops.versus.VersusSettings;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * What a sulfur spike does to what it hurts: Poison for a whole number of seconds from {@code POISON_SECONDS} to the
 * damage it dealt, Nausea for one from {@code NAUSEA_SECONDS} to the damage dealt (VersusSettings.SulfurSpikes), the
 * damage being what got through: the health and absorption the hurt took, after armour, enchantments and Resistance. A
 * sulfur spike hurts by falling onto something (vanilla's falling stalactite: what the thing had as Fabric's
 * allow-damage hears of the hurt, against what it has as its after-damage does, whose own amount comes before armour)
 * and by being landed on (SulfurSpikeBlockMixin: vanilla gives the fall no spike damage of its own, so the fall's is the
 * spike's).
 */
public final class SulfurSpikes {

    private SulfurSpikes() {
    }

    /** What each living thing a falling sulfur spike is hurting had, health and absorption, as the hurt began. */
    private static final Map<LivingEntity, Float> BEFORE = new WeakHashMap<>();

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (isFallingSpike(source)) BEFORE.put(entity, entity.getHealth() + entity.getAbsorptionAmount());
            return true;
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            Float before = isFallingSpike(source) ? BEFORE.remove(entity) : null;
            if (before == null) return;
            float dealt = before - entity.getHealth() - entity.getAbsorptionAmount();
            if (dealt > 0.0F) afflict(entity, dealt);
        });
    }

    /** Whether this hurt is a falling sulfur spike's. */
    private static boolean isFallingSpike(DamageSource source) {
        return source.is(DamageTypes.FALLING_STALACTITE) && source.getDirectEntity() instanceof FallingBlockEntity falling
                && falling.getBlockState().is(Blocks.SULFUR_SPIKE);
    }

    /** Poisons and nauseates what a sulfur spike hurt this much. */
    public static void afflict(LivingEntity entity, float dealt) {
        RandomSource random = entity.getRandom();
        entity.addEffect(new MobEffectInstance(MobEffects.POISON, poisonTicks(dealt, random), 0));
        entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, nauseaTicks(dealt, random), 0));
    }

    /** Poison's length for this much damage dealt, in ticks. */
    public static int poisonTicks(float damage, RandomSource random) {
        return 20 * seconds(VersusSettings.SulfurSpikes.POISON_SECONDS, damage, random);
    }

    /** Nausea's length for this much damage dealt, in ticks. */
    public static int nauseaTicks(float damage, RandomSource random) {
        return 20 * seconds(VersusSettings.SulfurSpikes.NAUSEA_SECONDS, damage, random);
    }

    /** A whole number of seconds from {@code least} to the damage's whole part (just {@code least} when that's less). */
    private static int seconds(int least, float damage, RandomSource random) {
        return random.nextIntBetweenInclusive(least, Math.max(least, Mth.floor(damage)));
    }
}
