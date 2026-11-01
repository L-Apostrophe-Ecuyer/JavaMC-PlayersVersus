package frootloops.versus.mod.items_and_effects.conversion;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.PaleLine;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.mobs.ModEntities;
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
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * All of a living thing's hearts white with Conversion: it shakes for TURNING_TICKS (others see it shake, a player
 * feels sick), then turns undead ({@link #turn}). A player dies of it and the zombie of the place rises where they
 * fell, wearing their armour (and dropping it). A totem of undying saves a player, and cures them. Skeletons, the
 * nether's undead, the pale, red mushroom creatures, warped things, golems and bosses don't take Conversion at all
 * ({@link #immune}).
 */
public final class Undeath {
    public static final TagKey<EntityType<?>> IMMUNE = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "conversion_immune"));
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

    /** Whether the entity never takes Conversion. */
    public static boolean immune(LivingEntity entity) {
        return entity.is(IMMUNE);
    }

    /** The entity's white hearts. */
    public static int whiteHearts(LivingEntity entity) {
        MobEffectInstance conversion = entity.getEffect(CustomStatusEffects.CONVERSION);
        return conversion == null ? 0 : Conversion.whiteHearts(conversion.getAmplifier());
    }

    /** The health food regenerates the entity up to. */
    public static float regenCap(LivingEntity entity) {
        return Conversion.regenCap(entity.getMaxHealth(), whiteHearts(entity));
    }

    /**
     * Conversion's tick on an entity: it notes whether all its hearts are white. What follows happens at the end of the
     * level's tick, out of the entity's effect ticking (which a conversion or a death mustn't change under it).
     */
    public static void whileConverting(ServerLevel level, LivingEntity entity, int amplifier) {
        boolean exempt = entity instanceof Player player && (player.isCreative() || player.isSpectator());
        if (exempt || !Conversion.allWhite(entity.getMaxHealth(), Conversion.whiteHearts(amplifier))) return;
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
     * Turns the entity undead, or kills it. The undead and their kin turn pale: zombies of every kind into pale
     * zombies, a creeper into a pale creeper, spiders into pale spiders; a horse becomes a zombie horse, a villager a
     * zombie villager, piglins zombified piglins, a hoglin a zoglin. A player, an illager, a witch or a wandering
     * trader rises as the zombie of where it turned ({@link #zombieOfThePlace}). Anything else dies of it.
     */
    public static void turn(ServerLevel level, LivingEntity entity) {
        level.playSound((Player) null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ZOMBIE_INFECT, entity.getSoundSource(), 1.0F, 0.7F);
        if (entity instanceof ServerPlayer player) {
            turnPlayer(level, player);
            return;
        }
        // What it turns into takes no Conversion along with the rest of its effects
        entity.removeEffect(CustomStatusEffects.CONVERSION);
        if (entity instanceof Villager villager) {
            Zombie zombie = new Zombie(EntityTypes.ZOMBIE, level);
            zombie.snapTo(villager.position());
            if (zombie.convertVillagerToZombieVillager(level, villager)) return;
        }
        EntityType<? extends Mob> into = undeadKind(level, entity);
        if (into != null && entity instanceof Mob mob && mob.convertTo(into, ConversionParams.single(mob, true, true),
                converted -> converted.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0))) != null) return;
        entity.hurtServer(level, source(level), Float.MAX_VALUE);
    }

    /** What a mob turns into, or null if it dies of it. */
    @Nullable
    private static EntityType<? extends Mob> undeadKind(ServerLevel level, LivingEntity entity) {
        EntityType<?> type = entity.getType();
        if (entity instanceof Zombie && !(entity instanceof ZombifiedPiglin)) return ModEntities.PALE_ZOMBIE;
        if (type == EntityTypes.CREEPER) return ModEntities.PALE_CREEPER;
        if (type == EntityTypes.SPIDER || type == EntityTypes.CAVE_SPIDER) return ModEntities.PALE_SPIDER;
        if (type == EntityTypes.HORSE) return EntityTypes.ZOMBIE_HORSE;
        if (type == EntityTypes.PIGLIN || type == EntityTypes.PIGLIN_BRUTE) return EntityTypes.ZOMBIFIED_PIGLIN;
        if (type == EntityTypes.HOGLIN) return EntityTypes.ZOGLIN;
        if (entity.is(RISE_AS_ZOMBIES)) return zombieOfThePlace(level, entity);
        return null;
    }

    /**
     * The zombie that rises where something turns: under water a drowned; where paleness holds (PaleLine) a pale
     * zombie; in the desert (tag {@code players-versus:undead_rise_as_husks}) a husk; elsewhere a zombie.
     */
    public static EntityType<? extends Mob> zombieOfThePlace(ServerLevel level, LivingEntity entity) {
        BlockPos pos = entity.blockPosition();
        if (entity.isUnderWater()) return EntityTypes.DROWNED;
        if (PaleLine.isPale(level, pos)) return ModEntities.PALE_ZOMBIE;
        if (level.getBiome(pos).is(HUSK_BIOMES)) return EntityTypes.HUSK;
        return EntityTypes.ZOMBIE;
    }

    /** The player dies of it, their armour taken off first; if they died, the zombie of the place rises in it where they fell. */
    private static void turnPlayer(ServerLevel level, ServerPlayer player) {
        Map<EquipmentSlot, ItemStack> armour = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : ARMOUR) {
            ItemStack worn = player.getItemBySlot(slot);
            if (worn.isEmpty()) continue;
            armour.put(slot, worn);
            player.setItemSlot(slot, ItemStack.EMPTY);
        }
        EntityType<? extends Mob> rises = zombieOfThePlace(level, player);
        player.hurtServer(level, source(level), Float.MAX_VALUE);
        if (!player.isDeadOrDying()) {
            // A totem saved them (and cured them): they keep their armour
            armour.forEach(player::setItemSlot);
            return;
        }
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
