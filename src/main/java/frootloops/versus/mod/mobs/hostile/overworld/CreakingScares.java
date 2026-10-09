package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.mod.mobs.StandingSpots;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Scarier creakings. One stuck for a few seconds, chasing something it can't get to, teleports like an enderman once
 * nobody's looking at it: somewhere near its target that no player can see, within reach of its heart. And hitting one
 * is a gamble: {@link #VANISH_CHANCE} of the time the attacker goes blind and the creaking is gone; otherwise a
 * silverfish crawls out of it.
 */
public final class CreakingScares {
    public static final float VANISH_CHANCE = 0.4F;
    static final int BLINDNESS_TICKS = 80;
    /** Ticks without getting anywhere before a creaking counts as stuck, and before it may teleport again. */
    public static final int STUCK_TICKS = 60, TELEPORT_COOLDOWN = 100;
    /** Where a teleport lands: this far from the creaking's target (or anywhere this near itself), and this close to its heart. */
    static final double TELEPORT_NEAR = 4.0, TELEPORT_FAR = 12.0, WANDER_RANGE = 16.0, HOME_RANGE = 30.0;
    /** How far players are checked for a line of sight to a landing spot. */
    static final double SIGHT_RANGE = 64.0;
    static final int TELEPORT_ATTEMPTS = 24;

    private CreakingScares() {
    }

    /** A hit that went through: the attacker goes blind and the creaking vanishes, or a silverfish comes out of it. */
    public static void onHit(ServerLevel level, Creaking creaking, LivingEntity attacker) {
        if (creaking.getRandom().nextFloat() < VANISH_CHANCE) {
            attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_TICKS), creaking);
            creaking.discard();
            return;
        }
        Mob silverfish = EntityTypes.SILVERFISH.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (silverfish == null) return;
        silverfish.snapTo(creaking.getX(), creaking.getY() + 0.5, creaking.getZ(), creaking.getRandom().nextFloat() * 360.0F, 0.0F);
        silverfish.finalizeSpawn(level, level.getCurrentDifficultyAt(creaking.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
        if (attacker instanceof Player player && !player.isCreative() && !player.isSpectator()) silverfish.setTarget(player);
        level.addFreshEntity(silverfish);
    }

    /** Teleports a stuck creaking somewhere no player can see; returns whether it found such a place. */
    public static boolean teleportOutOfSight(ServerLevel level, Creaking creaking) {
        LivingEntity target = creaking.getTarget();
        Vec3 center = target != null ? target.position() : creaking.position();
        double near = target != null ? TELEPORT_NEAR : 0.0, far = target != null ? TELEPORT_FAR : WANDER_RANGE;
        BlockPos home = creaking.isHeartBound() ? creaking.getHomePos() : null;
        double height = creaking.getBbHeight();
        Vec3 spot = StandingSpots.around(level, creaking, center, near, far, 4, 6, TELEPORT_ATTEMPTS,
                place -> (home == null || place.closerThan(Vec3.atCenterOf(home), HOME_RANGE)) && !seen(level, place, height));
        if (spot == null) return false;
        creaking.teleportTo(spot.x, spot.y, spot.z);
        creaking.getNavigation().stop();
        return true;
    }

    /** Whether any player could see a creaking standing there: a clear line from their eyes to its feet, middle or head. */
    static boolean seen(ServerLevel level, Vec3 place, double height) {
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !player.isAlive() || !player.position().closerThan(place, SIGHT_RANGE)) continue;
            Vec3 eyes = player.getEyePosition();
            for (double up : new double[]{0.2, height * 0.5, height - 0.2}) {
                ClipContext sight = new ClipContext(eyes, place.add(0.0, up, 0.0), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player);
                if (level.clip(sight).getType() == HitResult.Type.MISS) return true;
            }
        }
        return false;
    }
}
