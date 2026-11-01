package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * A watchling's gaze. A player looking at one (in plain sight, near the middle of the view, a carved pumpkin not over
 * their eyes) has their screen fill with static; full, they die, and silverfish crawl out where they stood. So does one
 * too near them, looked at or not: within NEAR_RANGE blocks the static builds on its own, faster the nearer, and a stare
 * adds to it. Looking away, or backing off, it clears (slowly). Creative and spectating players don't see it.
 */
public final class WatchlingStare {
    public static final ResourceKey<DamageType> WATCHED = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "watched"));

    /** How full each player's screen is of static, 0 to 1. */
    private static final Map<ServerPlayer, Float> STARE = new WeakHashMap<>();

    private WatchlingStare() {}

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(WatchlingStare::tick);
    }

    /** Whether the player looks at it: in reach and plain sight, at least half seen (Gaze), nothing over their eyes. */
    public static boolean looksAt(Player player, WatchlingEntity watchling) {
        if (!player.isAlive() || player.isSpectator() || player.isCreative() || !watchling.isAlive()) return false;
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(ItemTags.GAZE_DISGUISE_EQUIPMENT)) return false;
        Vec3 towards = watchling.getBoundingBox().getCenter().subtract(player.getEyePosition());
        if (towards.length() > VersusSettings.Watchlings.WATCH_RANGE) return false;
        return Gaze.watched(Gaze.visibility(player.getViewVector(1.0F), towards, watchling.getBbHeight() / 2.0)) && player.hasLineOfSight(watchling);
    }

    /** The player looking at it, if any. */
    @Nullable
    public static Player watcher(WatchlingEntity watchling) {
        for (Player player : watchling.level().players()) {
            if (looksAt(player, watchling)) return player;
        }
        return null;
    }

    /**
     * How much faster than a stare the static builds for one this far off, looked at or not: nothing beyond NEAR_RANGE,
     * rising evenly to PROXIMITY_RATE at KILL_RANGE and nearer.
     */
    public static float proximity(double distance) {
        double near = VersusSettings.Watchlings.NEAR_RANGE, kill = VersusSettings.Watchlings.KILL_RANGE;
        if (distance >= near) return 0.0F;
        if (distance <= kill) return VersusSettings.Watchlings.PROXIMITY_RATE;
        return (float) ((near - distance) / (near - kill)) * VersusSettings.Watchlings.PROXIMITY_RATE;
    }

    /** How fast the static builds, in stares' worth: one for a stare, and what nearness adds. */
    public static float rate(boolean looking, double distance) {
        return (looking ? 1.0F : 0.0F) + proximity(distance);
    }

    /** The static a stare builds up to after a tick: fuller while looking, clearing while not. */
    public static float next(float had, boolean looking) {
        return next(had, looking ? 1.0F : 0.0F);
    }

    /** The static after a tick at this rate (see {@link #rate}): fuller at any rate above nothing, clearing at none. */
    public static float next(float had, float rate) {
        return rate > 0.0F ? had + rate / VersusSettings.Watchlings.STARE_TICKS : Math.max(0.0F, had - 1.0F / VersusSettings.Watchlings.CLEAR_TICKS);
    }

    /**
     * How a distance leaves it after a sighting: nearer, to APPROACH_MIN to APPROACH_MAX of what it was (the roll, 0 to 1,
     * picks where in that), never nearer than CLOSEST, and never further than it was.
     */
    public static double approach(double distance, double roll) {
        double factor = VersusSettings.Watchlings.APPROACH_MIN + (VersusSettings.Watchlings.APPROACH_MAX - VersusSettings.Watchlings.APPROACH_MIN) * roll;
        return Math.min(distance, Math.max(VersusSettings.Watchlings.CLOSEST, distance * factor));
    }

    /** The fastest static a player is under from the watchlings about them, and which one it's from. */
    private record Pressure(float rate, @Nullable WatchlingEntity from) {}

    private static Pressure pressure(ServerLevel level, ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || player.isCreative()) return new Pressure(0.0F, null);
        double range = Math.max(VersusSettings.Watchlings.WATCH_RANGE, VersusSettings.Watchlings.NEAR_RANGE);
        float best = 0.0F;
        WatchlingEntity from = null;
        for (WatchlingEntity watchling : level.getEntitiesOfClass(WatchlingEntity.class, player.getBoundingBox().inflate(range))) {
            if (!watchling.isAlive()) continue;
            double distance = player.getEyePosition().distanceTo(watchling.getBoundingBox().getCenter());
            float rate = rate(looksAt(player, watchling), distance);
            if (rate > best) {
                best = rate;
                from = watchling;
            }
        }
        return new Pressure(best, from);
    }

    private static void tick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            float had = STARE.getOrDefault(player, 0.0F);
            Pressure pressure = pressure(level, player);
            float now = next(had, pressure.rate());
            if (now == had) continue;
            if (now >= 1.0F && pressure.from() != null) {
                now = 0.0F;
                die(level, player, pressure.from());
            }
            STARE.put(player, now);
            if (ServerPlayNetworking.canSend(player, WatchedPayload.TYPE.id())) ServerPlayNetworking.send(player, new WatchedPayload(now));
        }
    }

    /** Looked too long, or too near: the player dies of it, and silverfish crawl out where they stood. A totem still saves them. */
    private static void die(ServerLevel level, ServerPlayer player, WatchlingEntity watchling) {
        Vec3 at = player.position();
        DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(WATCHED), watchling);
        player.hurtServer(level, source, Float.MAX_VALUE);
        if (!player.isDeadOrDying()) return;
        int count = VersusSettings.Watchlings.SILVERFISH_MIN + level.getRandom().nextInt(VersusSettings.Watchlings.SILVERFISH_MAX - VersusSettings.Watchlings.SILVERFISH_MIN + 1);
        for (int i = 0; i < count; i++) {
            Mob silverfish = EntityTypes.SILVERFISH.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (silverfish == null) return;
            silverfish.snapTo(at.x + (level.getRandom().nextDouble() - 0.5) * 0.6, at.y + 0.2, at.z + (level.getRandom().nextDouble() - 0.5) * 0.6,
                    level.getRandom().nextFloat() * 360.0F, 0.0F);
            silverfish.finalizeSpawn(level, level.getCurrentDifficultyAt(silverfish.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
            level.addFreshEntity(silverfish);
        }
    }
}
