package frootloops.versus.mod.items_and_effects.blight;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mixin.mobs.passive.AbstractHorseAccessor;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.mobs.hostile.overworld.PaleVillagerEntity;
import frootloops.versus.mod.mobs.hostile.overworld.PaleWanderingTraderEntity;
import frootloops.versus.mod.mobs.hostile.overworld.PlayerZombieEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.LevelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * What Blight does to the living as it blights their hearts (Blight replaces the Wither: {@link #asBlight}): they shake
 * (building from SHAKE_FROM, felt in a player's own view: client UndeathShaking and BlightCameraShakeMixin), and fully
 * blighted (as many hearts as the bar has) the Blight deals a heart every FULL_DAMAGE_TICKS, as the Wither would.
 * Whatever dies fully blighted, of that or of anything else, is replaced by its pale form ({@link #turn}; the death
 * hooks are LivingEntityBlightMixin's and, for players, ServerPlayerBlightedDeathMixin's): a player rises a player
 * zombie where they fell, wearing and carrying what they had (PlayerZombieEntity). A creative player can't die: they
 * live, and a copy of them rises. A totem of undying saves a player, and cures them. Skeletons, the nether's undead, the
 * pale, red mushroom creatures, warped things, golems and bosses don't take Blight at all ({@link #immune}).
 */
public final class Undeath {
    public static final TagKey<EntityType<?>> IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "blight_immune"));
    /** What turns into a pale zombie: illagers, witches. */
    public static final TagKey<EntityType<?>> RISE_AS_ZOMBIES = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "rise_as_zombies"));
    public static final ResourceKey<DamageType> TURNED_UNDEAD = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "turned_undead"));

    /** Since when each fully blighted thing has been so, and when it was last seen so (game time). */
    private record Turning(long started, long lastSeen) {}

    private static final Map<LivingEntity, Turning> TURNING = new WeakHashMap<>();

    private Undeath() {}

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(Undeath::tick);
    }

    /** Whether the entity never takes Blight. */
    public static boolean immune(LivingEntity entity) {
        return entity.is(IMMUNE);
    }

    /** The entity's blighted hearts. */
    public static int blightedHearts(LivingEntity entity) {
        MobEffectInstance blight = entity.getEffect(CustomStatusEffects.BLIGHT);
        return blight == null ? 0 : Blight.blightedHearts(blight.getAmplifier());
    }

    /** Whether the entity is fully blighted: as many hearts blighted as its bar has. */
    public static boolean fullyBlighted(LivingEntity entity) {
        return Blight.fullyBlighted(entity.getMaxHealth(), blightedHearts(entity));
    }

    /** What the Wither is now: the same time and strength (up to what a dose may blight), as Blight. Other effects as they are. */
    public static MobEffectInstance asBlight(MobEffectInstance effect) {
        if (!VersusSettings.Blight.REPLACE_WITHER || !effect.is(MobEffects.WITHER)) return effect;
        return new MobEffectInstance(CustomStatusEffects.BLIGHT, effect.getDuration(), Blight.fromWitherAmplifier(effect.getAmplifier()),
                effect.isAmbient(), effect.isVisible(), effect.showIcon());
    }

    /**
     * Purges blighted hearts: healing (a potion of Healing) and absorption push the Blight back, a heart for each heart
     * they give. Whatever's left of it keeps its time; none left, it's gone. Does nothing on an entity not blighted.
     */
    public static void purge(LivingEntity entity, int hearts) {
        MobEffectInstance blight = entity.getEffect(CustomStatusEffects.BLIGHT);
        if (blight == null || hearts <= 0 || entity.level().isClientSide()) return;
        int amplifier = Blight.purgedAmplifier(blight.getAmplifier(), hearts);
        entity.removeEffect(CustomStatusEffects.BLIGHT);
        if (amplifier >= 0) {
            entity.forceAddEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, blight.getDuration(), amplifier, blight.isAmbient(), blight.isVisible(), blight.showIcon()), null);
        }
    }

    /** The health food regenerates the entity up to. */
    public static float regenCap(LivingEntity entity) {
        return Blight.regenCap(entity.getMaxHealth(), blightedHearts(entity));
    }

    /**
     * Blight's tick on an entity: it notes whether it's fully blighted. What follows (the damage, and the turning a death
     * brings) happens at the end of the level's tick, out of the entity's effect ticking (which a turning mustn't change
     * under it). Spectators are left alone; creative players go through it too, and live.
     */
    public static void whileBlighted(ServerLevel level, LivingEntity entity, int amplifier) {
        if (entity instanceof Player player && player.isSpectator()) return;
        if (!Blight.fullyBlighted(entity.getMaxHealth(), Blight.blightedHearts(amplifier))) return;
        long now = level.getGameTime();
        Turning turning = TURNING.get(entity);
        TURNING.put(entity, turning == null || turning.lastSeen < now - 1 ? new Turning(now, now) : new Turning(turning.started, now));
    }

    private static void tick(ServerLevel level) {
        if (TURNING.isEmpty()) return;
        long now = level.getGameTime();
        List<LivingEntity> due = new ArrayList<>();
        for (Map.Entry<LivingEntity, Turning> entry : new ArrayList<>(TURNING.entrySet())) {
            LivingEntity entity = entry.getKey();
            if (entity.level() != level) continue;
            Turning turning = entry.getValue();
            // Not fully blighted this tick (cured, healed, gone): it's left alone
            if (turning.lastSeen < now || !entity.isAlive()) {
                TURNING.remove(entity);
                continue;
            }
            long ticks = now - turning.started;
            if (ticks == 0 && entity instanceof ServerPlayer player) {
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
            }
            if (ticks % 4 == 0) {
                level.sendParticles(ParticleTypes.WHITE_ASH, entity.getX(), entity.getY(0.5), entity.getZ(), 6,
                        entity.getBbWidth() / 2.0, entity.getBbHeight() / 3.0, entity.getBbWidth() / 2.0, 0.02);
            }
            if (entity instanceof ServerPlayer player && player.isCreative()) {
                // A creative player can't be blighted to death: after the shaking, a copy of them rises
                if (ticks >= VersusSettings.Mushrooms.TURNING_TICKS) due.add(entity);
            } else if (Blight.damagesNow(ticks)) {
                // As the Wither would: dying of it (or of anything else while fully blighted) turns it pale
                entity.hurtServer(level, entity.damageSources().wither(), VersusSettings.Blight.FULL_DAMAGE);
            }
        }
        for (LivingEntity entity : due) {
            TURNING.remove(entity);
            turn(level, entity);
        }
    }

    /**
     * Replaces the entity with its pale form, if it has one: whether it did. This is what dying fully blighted comes to
     * (LivingEntityBlightMixin; and for a creative player, who can't die of it, after the shaking). Its pale form first
     * ({@link #turnPale}): a villager a pale villager, a wandering trader a pale wandering trader, an illager or a witch
     * a pale zombie, zombies of every kind pale zombies, a creeper a pale creeper, spiders pale spiders. What has none
     * but an undead kind turns into that: piglins zombified piglins, a hoglin a zoglin, and animals with an undead kind
     * of their own, as calm as they were and their owner's still (a horse a zombie horse, a camel a camel husk, a
     * nautilus a zombie nautilus, a chicken a zombie chicken). A player rises a player zombie ({@link PlayerZombieEntity});
     * what has no such form (a pig, a cow...) just dies.
     */
    public static boolean turn(ServerLevel level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) return turnPlayer(level, player);
        // What it turns into takes no Blight along with the rest of its effects
        entity.removeEffect(CustomStatusEffects.BLIGHT);
        if (turnPale(level, entity)) {
            turned(level, entity);
            return true;
        }
        EntityType<? extends Mob> into = undeadKind(entity);
        if (into != null && entity instanceof Mob mob && mob.convertTo(into, ConversionParams.single(mob, true, true), converted -> {
            keepWhatWasKept(mob, converted);
            converted.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
        }) != null) {
            turned(level, mob);
            return true;
        }
        return false;
    }

    private static void turned(ServerLevel level, LivingEntity from) {
        level.playSound((Player) null, from.getX(), from.getY(), from.getZ(), SoundEvents.ZOMBIE_INFECT, from.getSoundSource(), 1.0F, 0.7F);
    }

    /**
     * A player turning: a survival player dies fully blighted and the player zombie rises as the death comes
     * (ServerPlayerBlightedDeathMixin), so there's nothing to do here for them. A creative player can't die: they live,
     * the Blight lifts, and a copy of them (wearing copies of what they wear, carrying nothing) rises beside them, so the
     * turning can be seen.
     */
    private static boolean turnPlayer(ServerLevel level, ServerPlayer player) {
        if (!player.isCreative()) return false;
        PlayerZombieEntity.rise(level, player, true);
        player.removeEffect(CustomStatusEffects.BLIGHT);
        turned(level, player);
        return true;
    }

    /**
     * The living that have a pale form turn into it: zombies of every kind into pale zombies, a creeper into a pale
     * creeper, spiders into pale spiders, a villager into a pale villager ({@link #turnPaleVillager}), a wandering trader
     * into a pale wandering trader ({@link #turnPaleTrader}), an illager or a witch into a pale zombie. Whether it turned.
     */
    public static boolean turnPale(ServerLevel level, LivingEntity entity) {
        if (immune(entity)) return false;
        if (entity instanceof Villager villager) return turnPaleVillager(level, villager, !villager.isSilent());
        if (entity instanceof WanderingTrader trader) return turnPaleTrader(level, trader, !trader.isSilent());
        if (!(entity instanceof Mob mob)) return false;
        EntityType<? extends Mob> into = paleKind(mob);
        if (into == null) return false;
        return mob.convertTo(into, ConversionParams.single(mob, true, true), turned -> {
            turned.setPersistenceRequired();
            if (!mob.isSilent()) level.levelEvent(null, LevelEvent.SOUND_ZOMBIE_TO_DROWNED, turned.blockPosition(), 0);
        }) != null;
    }

    /** The pale kind of a mob, or null if it has none. */
    @Nullable
    private static EntityType<? extends Mob> paleKind(Mob mob) {
        EntityType<?> type = mob.getType();
        if (mob instanceof Zombie && !(mob instanceof ZombifiedPiglin)) return ModEntities.PALE_ZOMBIE;
        if (mob instanceof Creeper && type == EntityTypes.CREEPER) return ModEntities.PALE_CREEPER;
        if (type == EntityTypes.SPIDER || type == EntityTypes.CAVE_SPIDER) return ModEntities.PALE_SPIDER;
        if (mob.is(RISE_AS_ZOMBIES)) return ModEntities.PALE_ZOMBIE;
        return null;
    }

    /**
     * A villager turns into a pale villager, keeping as a zombie villager does what it was (type, trade and level), its
     * trades, what the village thought of it and its experience, for when it's cured. Whether it turned.
     */
    public static boolean turnPaleVillager(ServerLevel level, Villager villager, boolean sound) {
        PaleVillagerEntity pale = villager.convertTo(ModEntities.PALE_VILLAGER, ConversionParams.single(villager, true, true), turned -> {
            turned.setVillagerData(villager.getVillagerData());
            turned.setGossips(villager.getGossips().copy());
            turned.setTradeOffers(villager.getOffers().copy());
            turned.setVillagerXp(villager.getVillagerXp());
            turned.setVillagerDataFinalized(villager.getVillagerDataFinalized());
            turned.setPersistenceRequired();
            if (sound) level.levelEvent(null, LevelEvent.SOUND_ZOMBIE_INFECTED, turned.blockPosition(), 0);
        });
        return pale != null;
    }

    /** A wandering trader turns into a pale wandering trader, keeping the trades it carried, for when it's cured. */
    public static boolean turnPaleTrader(ServerLevel level, WanderingTrader trader, boolean sound) {
        PaleWanderingTraderEntity pale = trader.convertTo(ModEntities.PALE_WANDERING_TRADER, ConversionParams.single(trader, true, true), turned -> {
            turned.setTradeOffers(trader.getOffers().copy());
            turned.setPersistenceRequired();
            if (sound) level.levelEvent(null, LevelEvent.SOUND_ZOMBIE_INFECTED, turned.blockPosition(), 0);
        });
        return pale != null;
    }

    /** What an animal turning undead keeps that its turning doesn't carry: being tame and whose, being a jockey's mount. */
    private static void keepWhatWasKept(Mob from, Mob into) {
        if (from instanceof AbstractHorse horse && into instanceof AbstractHorse undead) {
            undead.setTamed(horse.isTamed());
            ((AbstractHorseAccessor) undead).setOwnerKept(horse.getOwnerReference());
        }
        if (from instanceof TamableAnimal tame && into instanceof TamableAnimal undead) {
            undead.setTame(tame.isTame(), false);
            undead.setOwnerReference(tame.getOwnerReference());
        }
        if (from instanceof Chicken chicken && into instanceof Chicken undead) undead.setChickenJockey(chicken.isChickenJockey());
    }

    /** The undead kind a mob with no pale form turns into when it dies fully blighted, or null if it just dies. */
    @Nullable
    private static EntityType<? extends Mob> undeadKind(LivingEntity entity) {
        EntityType<?> type = entity.getType();
        // Animals with an undead kind of their own; donkeys, mules and the rest have none and die
        if (type == EntityTypes.HORSE) return EntityTypes.ZOMBIE_HORSE;
        if (type == EntityTypes.CAMEL) return EntityTypes.CAMEL_HUSK;
        if (type == EntityTypes.NAUTILUS) return EntityTypes.ZOMBIE_NAUTILUS;
        if (type == EntityTypes.CHICKEN) return ModEntities.ZOMBIE_CHICKEN;
        if (type == EntityTypes.PIGLIN || type == EntityTypes.PIGLIN_BRUTE) return EntityTypes.ZOMBIFIED_PIGLIN;
        if (type == EntityTypes.HOGLIN) return EntityTypes.ZOGLIN;
        return null;
    }
}
