package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusSettings;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A watchling: an enderman the paleness took, with a creaking's yellow eyes, where the End's warping and the depths'
 * paleness meet. It haunts. It never moves and never strikes: it stands, and it watches. Looking at it is what kills
 * (WatchlingStare): the screen fills with static, and whoever looks too long dies into silverfish. Looked at and then
 * left, it's gone a moment later, up behind whoever looked or far off; left unlooked at for 15 to 30 seconds, it
 * vanishes and leaves them be. Hit, it slips behind whoever hit it. Like all the warped it shows only to a stare (Gaze),
 * and a carved pumpkin hides you from its gaze as from an enderman's.
 */
public class WatchlingEntity extends Enderman {
    /** Whom it haunts: the last who looked at it, or hit it. */
    @Nullable private UUID haunted;
    /** Whether it's been looked at yet, and whether it was last tick. */
    private boolean seen, watchedLastTick;
    /** Ticks since last looked at; when it vanishes; ticks until it teleports (none if -1). */
    private int unseenTicks, vanishAfter, teleportIn = -1;

    public WatchlingEntity(EntityType<? extends Enderman> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createWatchlingAttributes() {
        return Enderman.createAttributes()
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    /** No goals at all: no walking, no carrying blocks, no anger at a stare, no attacking. */
    @Override
    protected void registerGoals() {}

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        Player watcher = WatchlingStare.watcher(this);
        if (watcher != null) {
            this.haunted = watcher.getUUID();
            this.seen = true;
            this.unseenTicks = 0;
            this.teleportIn = -1;
        } else if (this.seen) {
            this.unseenTicks++;
            if (this.watchedLastTick) {
                // Just looked away from: gone in a moment, and gone for good if not looked at again for a while
                int delay = VersusSettings.Watchlings.TELEPORT_DELAY;
                this.teleportIn = delay + this.random.nextInt(delay + 1);
                this.vanishAfter = VersusSettings.Watchlings.VANISH_MIN + this.random.nextInt(VersusSettings.Watchlings.VANISH_MAX - VersusSettings.Watchlings.VANISH_MIN + 1);
            }
            if (this.teleportIn > 0 && --this.teleportIn == 0) this.haunt(level);
            if (this.unseenTicks >= this.vanishAfter && this.vanishAfter > 0) {
                this.vanish(level);
                return;
            }
        }
        this.watchedLastTick = watcher != null;
        Player prey = this.hauntedPlayer(level);
        if (prey != null) this.getLookControl().setLookAt(prey, 30.0F, 30.0F);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && this.isAlive() && source.getEntity() instanceof Player player) {
            this.haunted = player.getUUID();
            this.seen = true;
            this.teleportIn = 1;
            if (this.vanishAfter <= 0) this.vanishAfter = VersusSettings.Watchlings.VANISH_MAX;
        }
        return hurt;
    }

    @Nullable
    private Player hauntedPlayer(ServerLevel level) {
        if (this.haunted == null) return null;
        Player player = level.getPlayerByUUID(this.haunted);
        return player != null && player.isAlive() && !player.isSpectator() && player.distanceToSqr(this) < 64.0 * 64.0 ? player : null;
    }

    /** Teleports half the time up behind whom it haunts, where they aren't looking, half the time far off. */
    private void haunt(ServerLevel level) {
        Player prey = this.hauntedPlayer(level);
        if (prey == null) {
            this.vanish(level);
            return;
        }
        boolean behind = this.random.nextBoolean();
        Vec3 view = prey.getViewVector(1.0F).multiply(1.0, 0.0, 1.0);
        view = view.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : view.normalize();
        for (int attempt = 0; attempt < 16; attempt++) {
            Vec3 way;
            double distance;
            if (behind) {
                way = view.scale(-1.0).yRot((float) ((this.random.nextDouble() - 0.5) * 1.4));
                distance = VersusSettings.Watchlings.BEHIND_NEAR + this.random.nextDouble() * (VersusSettings.Watchlings.BEHIND_FAR - VersusSettings.Watchlings.BEHIND_NEAR);
            } else {
                way = new Vec3(1.0, 0.0, 0.0).yRot((float) (this.random.nextDouble() * Math.PI * 2.0));
                distance = VersusSettings.Watchlings.AWAY_NEAR + this.random.nextDouble() * (VersusSettings.Watchlings.AWAY_FAR - VersusSettings.Watchlings.AWAY_NEAR);
            }
            double y = prey.getY() + this.random.nextInt(9) - 4;
            if (this.teleport(prey.getX() + way.x * distance, y, prey.getZ() + way.z * distance)) return;
        }
    }

    /** Gone: a wisp of the End where it stood, and nothing. */
    private void vanish(ServerLevel level) {
        level.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY(0.5), this.getZ(), 24, 0.3, 1.0, 0.3, 0.2);
        this.discard();
    }
}
