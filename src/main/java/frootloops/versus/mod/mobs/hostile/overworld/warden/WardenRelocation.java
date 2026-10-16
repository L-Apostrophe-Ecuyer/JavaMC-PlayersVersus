package frootloops.versus.mod.mobs.hostile.overworld.warden;

import frootloops.versus.mod.mobs.StandingSpots;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.warden.WardenAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A warden that rises far from everyone, the nearest player {@link #FAR} blocks away or more, goes to them: once it has
 * emerged it sniffs the air, digs back down, and comes out again about 16 blocks from that player, whose spot it then
 * investigates. Its brain waits while it sniffs and digs; a hit while it sniffs ends the search, and if the player has
 * gone by the time it's dug down, so has the warden.
 */
public final class WardenRelocation {
    public static final double FAR = 28.0, SEARCH_RANGE = 64.0;
    /** Where it comes back out: this far from the player across, at most this far above or below them. */
    static final double RESURFACE_NEAR = 12.0, RESURFACE_FAR = 20.0;
    static final int RESURFACE_UP = 6, RESURFACE_DOWN = 8, RESURFACE_ATTEMPTS = 32;
    /** How long the warden sniffs and digs: its animations' lengths. */
    static final int SNIFF_TICKS = 84, DIG_TICKS = 100;

    private enum Phase { AWAIT_EMERGING, SNIFF, DIG, DONE }

    private final UUID player;
    private Phase phase = Phase.AWAIT_EMERGING;
    private int ticks;

    private WardenRelocation(UUID player) {
        this.player = player;
    }

    /** For a newly spawned warden: a search for the nearest player within reach if they're far off, else null. */
    @Nullable
    public static WardenRelocation forSpawn(Warden warden) {
        Player nearest = warden.level().getNearestPlayer(warden, SEARCH_RANGE);
        return nearest != null && !nearest.closerThan(warden, FAR) ? new WardenRelocation(nearest.getUUID()) : null;
    }

    public boolean isDone() {
        return this.phase == Phase.DONE;
    }

    /** Runs a tick of the search; returns true while the warden is busy with it and its brain should wait. */
    public boolean tick(ServerLevel level, Warden warden) {
        switch (this.phase) {
            case AWAIT_EMERGING -> {
                if (warden.hasPose(Pose.EMERGING) || warden.getBrain().hasMemoryValue(MemoryModuleType.IS_EMERGING)) return false;
                this.begin(warden, Phase.SNIFF, SNIFF_TICKS, Pose.SNIFFING, SoundEvents.WARDEN_SNIFF);
                return true;
            }
            case SNIFF -> {
                holdStill(warden);
                if (warden.hurtTime > 0) {
                    warden.setPose(Pose.STANDING);
                    this.phase = Phase.DONE;
                    return false;
                }
                if (--this.ticks <= 0) this.begin(warden, Phase.DIG, DIG_TICKS, Pose.DIGGING, SoundEvents.WARDEN_DIG);
                return true;
            }
            case DIG -> {
                holdStill(warden);
                if (--this.ticks > 0) return true;
                this.phase = Phase.DONE;
                this.resurface(level, warden);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void begin(Warden warden, Phase phase, int ticks, Pose pose, SoundEvent sound) {
        this.phase = phase;
        this.ticks = ticks;
        warden.setPose(pose);
        warden.playSound(sound, 5.0F, 1.0F);
        holdStill(warden);
    }

    /** Comes back out near the player, emerging as it did when it spawned, and investigates where they stand. */
    private void resurface(ServerLevel level, Warden warden) {
        Player target = level.getPlayerByUUID(this.player);
        Vec3 spot = target == null || !target.isAlive() ? null
                : StandingSpots.around(level, warden, target.position(), RESURFACE_NEAR, RESURFACE_FAR, RESURFACE_UP, RESURFACE_DOWN, RESURFACE_ATTEMPTS, place -> true);
        if (spot == null) {
            warden.discard();
            return;
        }
        warden.teleportTo(spot.x, spot.y, spot.z);
        warden.setPose(Pose.EMERGING);
        warden.getBrain().setMemoryWithExpiry(MemoryModuleType.IS_EMERGING, Unit.INSTANCE, WardenAi.EMERGE_DURATION);
        WardenAi.setDisturbanceLocation(warden, target.blockPosition());
    }

    private static void holdStill(Warden warden) {
        warden.getNavigation().stop();
        warden.setZza(0.0F);
        warden.setXxa(0.0F);
    }
}
