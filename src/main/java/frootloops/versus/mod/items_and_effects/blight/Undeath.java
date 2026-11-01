package frootloops.versus.mod.items_and_effects.blight;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mixin.mobs.passive.AbstractHorseAccessor;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.mobs.hostile.overworld.PaleVillagerEntity;
import frootloops.versus.mod.mobs.hostile.overworld.PaleWanderingTraderEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * All of a living thing's hearts white with Blight: it shakes for TURNING_TICKS (others see it shake, a player
 * feels sick), then turns undead ({@link #turn}). A player dies of it and the zombie of the place rises where they
 * fell, wearing their armour (and dropping it). A totem of undying saves a player, and cures them. Skeletons, the
 * nether's undead, the pale, red mushroom creatures, warped things, golems and bosses don't take Blight at all
 * ({@link #immune}).
 */
public final class Undeath {
    public static final TagKey<EntityType<?>> IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "blight_immune"));
    /** What rises as the zombie of the place it turned in: illagers, witches, wandering traders (players do too). */
    public static final TagKey<EntityType<?>> RISE_AS_ZOMBIES = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "rise_as_zombies"));
    public static final TagKey<Biome> HUSK_BIOMES = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "undead_rise_as_husks"));
    public static final ResourceKey<DamageType> TURNED_UNDEAD = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "turned_undead"));
    private static final EquipmentSlot[] ARMOUR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    /** How often (ticks) clients hear that something still shakes, and for how long more it does. */
    private static final int SHAKE_NEWS = 10, SHAKE_FOR = 15;

    /** Since when each all-white thing has shaken, and when it was last seen all white (game time). */
    private record Shaking(long started, long lastSeen) {}

    private static final Map<LivingEntity, Shaking> SHAKING = new WeakHashMap<>();

    private Undeath() {}

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(Undeath::tick);
    }

    /** Whether the entity never takes Blight. */
    public static boolean immune(LivingEntity entity) {
        return entity.is(IMMUNE);
    }

    /** The entity's white hearts. */
    public static int whiteHearts(LivingEntity entity) {
        MobEffectInstance blight = entity.getEffect(CustomStatusEffects.BLIGHT);
        return blight == null ? 0 : Blight.whiteHearts(blight.getAmplifier());
    }

    /** The health food regenerates the entity up to. */
    public static float regenCap(LivingEntity entity) {
        return Blight.regenCap(entity.getMaxHealth(), whiteHearts(entity));
    }

    /**
     * Blight's tick on an entity: it notes whether all its hearts are white. What follows happens at the end of the
     * level's tick, out of the entity's effect ticking (which a turning or a death mustn't change under it).
     */
    public static void whileConverting(ServerLevel level, LivingEntity entity, int amplifier) {
        boolean exempt = entity instanceof Player player && (player.isCreative() || player.isSpectator());
        if (exempt || !Blight.allWhite(entity.getMaxHealth(), Blight.whiteHearts(amplifier))) return;
        long now = level.getGameTime();
        Shaking shaking = SHAKING.get(entity);
        SHAKING.put(entity, shaking == null || shaking.lastSeen < now - 1 ? new Shaking(now, now) : new Shaking(shaking.started, now));
    }

    private static void tick(ServerLevel level) {
        if (SHAKING.isEmpty()) return;
        long now = level.getGameTime();
        List<LivingEntity> due = new ArrayList<>();
        for (Map.Entry<LivingEntity, Shaking> entry : new ArrayList<>(SHAKING.entrySet())) {
            LivingEntity entity = entry.getKey();
            if (entity.level() != level) continue;
            Shaking shaking = entry.getValue();
            // Not all white this tick (cured, healed, gone): it stops shaking
            if (shaking.lastSeen < now || !entity.isAlive()) {
                SHAKING.remove(entity);
                continue;
            }
            long ticks = now - shaking.started;
            if (ticks == 0 && entity instanceof ServerPlayer player) {
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, VersusSettings.Mushrooms.TURNING_TICKS + 60, 0));
            }
            if (ticks % SHAKE_NEWS == 0) shake(entity);
            if (ticks % 4 == 0) {
                level.sendParticles(ParticleTypes.WHITE_ASH, entity.getX(), entity.getY(0.5), entity.getZ(), 6,
                        entity.getBbWidth() / 2.0, entity.getBbHeight() / 3.0, entity.getBbWidth() / 2.0, 0.02);
            }
            if (ticks >= VersusSettings.Mushrooms.TURNING_TICKS) due.add(entity);
        }
        for (LivingEntity entity : due) {
            SHAKING.remove(entity);
            turn(level, entity);
        }
    }

    /** Tells the clients seeing it (and a player itself) that it shakes. */
    private static void shake(LivingEntity entity) {
        UndeathShakingPayload payload = new UndeathShakingPayload(entity.getId(), SHAKE_FOR);
        List<ServerPlayer> watching = new ArrayList<>(PlayerLookup.tracking(entity));
        if (entity instanceof ServerPlayer player && !watching.contains(player)) watching.add(player);
        for (ServerPlayer player : watching) {
            if (ServerPlayNetworking.canSend(player, UndeathShakingPayload.TYPE.id())) ServerPlayNetworking.send(player, payload);
        }
    }

    /**
     * Turns the entity undead, or kills it. Blight makes zombies of the living: a villager a zombie villager (as a
     * zombie's kill does), piglins zombified piglins, a hoglin a zoglin; animals with an undead kind of their own turn
     * into it, as calm as they were and their owner's still (a horse a zombie horse, a camel a camel husk, a nautilus a
     * zombie nautilus, a chicken a zombie chicken). A player, an illager, a witch or a wandering trader rises as the
     * zombie of where it turned ({@link #zombieOfThePlace}). The undead and their kin it turns pale: zombies of every kind
     * into pale zombies, a creeper into a pale creeper, spiders into pale spiders. Anything else dies of it. (Withering is
     * what turns the living pale: {@link #wither}.)
     */
    public static void turn(ServerLevel level, LivingEntity entity) {
        level.playSound((Player) null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ZOMBIE_INFECT, entity.getSoundSource(), 1.0F, 0.7F);
        if (entity instanceof ServerPlayer player) {
            turnPlayer(level, player);
            return;
        }
        // What it turns into takes no Blight along with the rest of its effects
        entity.removeEffect(CustomStatusEffects.BLIGHT);
        if (entity instanceof Villager villager) {
            // As a zombie's kill: Zombie#convertVillagerToZombieVillager
            Zombie zombie = new Zombie(EntityTypes.ZOMBIE, level);
            zombie.snapTo(villager.position());
            if (zombie.convertVillagerToZombieVillager(level, villager)) return;
        }
        EntityType<? extends Mob> into = undeadKind(level, entity);
        if (into != null && entity instanceof Mob mob && mob.convertTo(into, ConversionParams.single(mob, true, true), converted -> {
            keepWhatWasKept(mob, converted);
            converted.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
        }) != null) return;
        entity.hurtServer(level, source(level), Float.MAX_VALUE);
    }

    /**
     * Withering turns the living pale, as it turns zombies and creepers (ZombieMixin, CreeperMixin): wither damage leaving
     * them below 8 health, a villager turns into a pale villager ({@link #turnPaleVillager}), a wandering trader into a
     * pale wandering trader ({@link #turnPaleTrader}), a spider into a pale spider, an illager or a witch into a pale
     * zombie. (A player withered to death rises as a pale zombie:
     * {@link #risePale}.) Whether it turned.
     */
    public static boolean wither(ServerLevel level, LivingEntity entity) {
        if (entity instanceof Villager villager) return turnPaleVillager(level, villager, !villager.isSilent());
        if (entity instanceof WanderingTrader trader) return turnPaleTrader(level, trader, !trader.isSilent());
        if (!(entity instanceof Mob mob)) return false;
        EntityType<?> type = mob.getType();
        EntityType<? extends Mob> into = type == EntityTypes.SPIDER || type == EntityTypes.CAVE_SPIDER ? ModEntities.PALE_SPIDER
                : mob.is(RISE_AS_ZOMBIES) ? ModEntities.PALE_ZOMBIE : null;
        if (into == null) return false;
        return mob.convertTo(into, ConversionParams.single(mob, true, true), turned -> {
            turned.setPersistenceRequired();
            if (!mob.isSilent()) level.levelEvent(null, LevelEvent.SOUND_ZOMBIE_INFECTED, turned.blockPosition(), 0);
        }) != null;
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

    /** What a mob turns into, or null if it dies of it. */
    @Nullable
    private static EntityType<? extends Mob> undeadKind(ServerLevel level, LivingEntity entity) {
        EntityType<?> type = entity.getType();
        if (entity instanceof Zombie && !(entity instanceof ZombifiedPiglin)) return ModEntities.PALE_ZOMBIE;
        if (type == EntityTypes.CREEPER) return ModEntities.PALE_CREEPER;
        if (type == EntityTypes.SPIDER || type == EntityTypes.CAVE_SPIDER) return ModEntities.PALE_SPIDER;
        // Animals with an undead kind of their own; donkeys, mules and the rest have none and die
        if (type == EntityTypes.HORSE) return EntityTypes.ZOMBIE_HORSE;
        if (type == EntityTypes.CAMEL) return EntityTypes.CAMEL_HUSK;
        if (type == EntityTypes.NAUTILUS) return EntityTypes.ZOMBIE_NAUTILUS;
        if (type == EntityTypes.CHICKEN) return ModEntities.ZOMBIE_CHICKEN;
        if (type == EntityTypes.PIGLIN || type == EntityTypes.PIGLIN_BRUTE) return EntityTypes.ZOMBIFIED_PIGLIN;
        if (type == EntityTypes.HOGLIN) return EntityTypes.ZOGLIN;
        if (entity.is(RISE_AS_ZOMBIES)) return zombieOfThePlace(level, entity);
        return null;
    }

    /**
     * The zombie that rises where something turns: under water a drowned; in the desert (tag
     * {@code players-versus:undead_rise_as_husks}) a husk; elsewhere a zombie. (Pale zombies rise of withering, not of
     * Blight: {@link #wither}, {@link #risePale}.)
     */
    public static EntityType<? extends Mob> zombieOfThePlace(ServerLevel level, LivingEntity entity) {
        BlockPos pos = entity.blockPosition();
        if (entity.isUnderWater()) return EntityTypes.DROWNED;
        if (level.getBiome(pos).is(HUSK_BIOMES)) return EntityTypes.HUSK;
        return EntityTypes.ZOMBIE;
    }

    /** The player dies of it, their armour taken off first; if they died, the zombie of the place rises in it where they fell. */
    private static void turnPlayer(ServerLevel level, ServerPlayer player) {
        Map<EquipmentSlot, ItemStack> armour = takeArmour(player);
        EntityType<? extends Mob> rises = zombieOfThePlace(level, player);
        player.hurtServer(level, source(level), Float.MAX_VALUE);
        if (!player.isDeadOrDying()) {
            // A totem saved them (and cured them): they keep their armour
            armour.forEach(player::setItemSlot);
            return;
        }
        rise(level, player, rises, armour);
    }

    /**
     * A player withered to death (ServerPlayerWitheredMixin, as they die): a pale zombie rises where they fell, in their
     * armour (which it always drops), as the zombie of the place rises of a player Blight turned.
     */
    public static void risePale(ServerLevel level, ServerPlayer player) {
        rise(level, player, ModEntities.PALE_ZOMBIE, takeArmour(player));
    }

    private static Map<EquipmentSlot, ItemStack> takeArmour(ServerPlayer player) {
        Map<EquipmentSlot, ItemStack> armour = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : ARMOUR) {
            ItemStack worn = player.getItemBySlot(slot);
            if (worn.isEmpty()) continue;
            armour.put(slot, worn);
            player.setItemSlot(slot, ItemStack.EMPTY);
        }
        return armour;
    }

    private static void rise(ServerLevel level, ServerPlayer player, EntityType<? extends Mob> rises, Map<EquipmentSlot, ItemStack> armour) {
        Mob zombie = rises.create(level, EntitySpawnReason.CONVERSION);
        if (zombie == null) {
            armour.values().forEach(stack -> Block.popResource(level, player.blockPosition(), stack));
            return;
        }
        zombie.snapTo(player.position(), player.getYRot(), 0.0F);
        zombie.setYHeadRot(player.getYHeadRot());
        armour.forEach((slot, stack) -> {
            zombie.setItemSlot(slot, stack);
            zombie.setGuaranteedDrop(slot);
        });
        zombie.setPersistenceRequired();
        level.addFreshEntity(zombie);
    }

    private static DamageSource source(ServerLevel level) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(TURNED_UNDEAD));
    }
}
