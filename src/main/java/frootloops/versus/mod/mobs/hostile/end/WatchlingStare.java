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
 * their eyes) has their screen fill with static; full, they die, and silverfish crawl out where they stood. Looking away,
 * it clears. Creative and spectating players don't see it.
 */
public final class WatchlingStare {
    public static final ResourceKey<DamageType> WATCHED = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "watched"));

    /** How full each player's screen is of static, 0 to 1. */
    private static final Map<ServerPlayer, Float> STARE = new WeakHashMap<>();

    private WatchlingStare() {}

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(WatchlingStare::tick);
    }

    /** Whether the player looks at it: in reach and plain sight, near the middle of their view, nothing over their eyes. */
    public static boolean looksAt(Player player, WatchlingEntity watchling) {
        if (!player.isAlive() || player.isSpectator() || player.isCreative() || !watchling.isAlive()) return false;
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(ItemTags.GAZE_DISGUISE_EQUIPMENT)) return false;
        Vec3 towards = watchling.getEyePosition().subtract(player.getEyePosition());
        double distance = towards.length();
        if (distance > VersusSettings.Watchlings.WATCH_RANGE || distance < 1.0E-3) return false;
        return looking(player.getViewVector(1.0F), towards.scale(1.0 / distance)) && player.hasLineOfSight(watchling);
    }

    /** Whether a view looks along a direction (both unit vectors): within the watching cone. */
    static boolean looking(Vec3 view, Vec3 direction) {
        return view.dot(direction) > VersusSettings.Watchlings.WATCH_CONE;
    }

    /** Whether any player looks at it now. */
    public static boolean watchedByAnyone(WatchlingEntity watchling) {
        for (Player player : watchling.level().players()) {
            if (looksAt(player, watchling)) return true;
        }
        return false;
    }

    /** The static a stare builds up to after a tick: fuller while looking, clearing while not. */
    public static float next(float had, boolean looking) {
        return looking ? had + 1.0F / VersusSettings.Watchlings.STARE_TICKS : Math.max(0.0F, had - 1.0F / VersusSettings.Watchlings.CLEAR_TICKS);
    }

    private static void tick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            float had = STARE.getOrDefault(player, 0.0F);
            WatchlingEntity watched = watched(level, player);
            float now = next(had, watched != null);
            if (now == had) continue;
            if (now >= 1.0F && watched != null) {
                now = 0.0F;
                die(level, player, watched);
            }
            STARE.put(player, now);
            if (ServerPlayNetworking.canSend(player, WatchedPayload.TYPE.id())) ServerPlayNetworking.send(player, new WatchedPayload(now));
        }
    }

    @Nullable
    private static WatchlingEntity watched(ServerLevel level, ServerPlayer player) {
        double range = VersusSettings.Watchlings.WATCH_RANGE;
        for (WatchlingEntity watchling : level.getEntitiesOfClass(WatchlingEntity.class, player.getBoundingBox().inflate(range))) {
            if (looksAt(player, watchling)) return watchling;
        }
        return null;
    }

    /** Looked too long: the player dies of it, and silverfish crawl out where they stood. A totem still saves them. */
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
