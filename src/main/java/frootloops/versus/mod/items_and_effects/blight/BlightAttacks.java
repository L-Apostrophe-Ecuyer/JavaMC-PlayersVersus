package frootloops.versus.mod.items_and_effects.blight;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.mobs.hostile.overworld.PaleVillagerEntity;
import frootloops.versus.mod.mobs.hostile.overworld.PaleZombieEntity;
import frootloops.versus.mod.mobs.hostile.end.WatchlingEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Mobs whose hits convert: the pale (a pale zombie, a creaking, a watchling) convert anything living, every hit; other
 * zombies only their own kind (people for zombies: tag {@code players-versus:zombie_kin}; piglins for zombified
 * piglins), by {@link VersusSettings.Mushrooms#ZOMBIE_BLIGHT_CHANCE}. A hit that converts gives nothing else:
 * whatever effect the mob adds to what it hit in the same tick (a husk's hunger) is dropped (LivingEntityBlightMixin).
 */
public final class BlightAttacks {
    /** Each mob's last hit that converted: what it hit (its id: the map holds no entity but its keys), and when. */
    private record Hit(int target, long tick) {}

    private static final Map<Mob, Hit> CONVERTING_HITS = new WeakHashMap<>();
    public static final TagKey<EntityType<?>> ZOMBIE_KIN = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "zombie_kin"));

    private BlightAttacks() {}

    /** A dose of Blight: one white heart more, for the effect's whole time. */
    public static MobEffectInstance dose() {
        return new MobEffectInstance(CustomStatusEffects.BLIGHT, VersusSettings.Mushrooms.EFFECT_TICKS, 0);
    }

    /** A mob's melee hit landed (Mob#doHurtTarget). */
    public static void afterHit(ServerLevel level, Mob attacker, LivingEntity target) {
        boolean always = attacker instanceof PaleZombieEntity || attacker instanceof Creaking || attacker instanceof WatchlingEntity;
        // A pale villager's hits wither instead (PaleVillagerEntity): withering is what turns villagers pale
        boolean rarely = attacker instanceof Zombie && !(attacker instanceof PaleVillagerEntity) && kin(attacker, target) && attacker.getRandom().nextFloat() < VersusSettings.Mushrooms.ZOMBIE_BLIGHT_CHANCE;
        if (!always && !rarely) return;
        CONVERTING_HITS.put(attacker, new Hit(target.getId(), level.getGameTime()));
        target.addEffect(dose(), attacker);
    }

    /** Whether the zombie's hit falls on its own kind: piglins for a zombified piglin, people for the rest. */
    static boolean kin(Mob zombie, LivingEntity target) {
        if (zombie instanceof ZombifiedPiglin) return target.getType() == EntityTypes.PIGLIN || target.getType() == EntityTypes.PIGLIN_BRUTE;
        return target.is(ZOMBIE_KIN);
    }

    /** Whether an effect from this source is one a converting hit replaces: anything but Blight, on what it hit, this tick. */
    public static boolean replaced(MobEffectInstance effect, @Nullable Entity source, LivingEntity target) {
        if (!(source instanceof Mob mob) || effect.is(CustomStatusEffects.BLIGHT)) return false;
        Hit hit = CONVERTING_HITS.get(mob);
        return hit != null && hit.target == target.getId() && hit.tick == mob.level().getGameTime();
    }
}
