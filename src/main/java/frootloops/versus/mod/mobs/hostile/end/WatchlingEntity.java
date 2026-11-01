package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusSettings;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
 * A watchling: an enderman the paleness took, with a creaking's yellow eyes and a tall head with three of them, where
 * the End's warping and the depths' paleness meet. It haunts, as a slender thing does. It never walks and never
 * strikes: it stands, and it watches. Looking at it is what kills (WatchlingStare): the screen fills with static, and
 * whoever looks too long dies into silverfish. And each time it's looked at and then left, it's gone a moment later and
 * back nearer, where the watcher isn't looking: closer and closer, until it's too near to live beside, which kills as
 * a stare does. Seen once, it hunts: left unlooked at, it still creeps nearer now and then, and it keeps on for 90
 * seconds to 3 minutes before it gives up and vanishes. While it hunts, the view of whoever it hunts is drawn towards it
 * now and then (client WatchlingPull). Hit, it slips behind whoever hit it. Like all the warped it shows only to a stare
 * (Gaze), and a carved pumpkin hides you from its gaze as from an enderman's.
 */
public class WatchlingEntity extends Enderman {
    /** Whether it hunts (it's been seen), for the clients that feel its pull. */
    private static final EntityDataAccessor<Boolean> DATA_HUNTING = SynchedEntityData.defineId(WatchlingEntity.class, EntityDataSerializers.BOOLEAN);

    /** Whom it haunts: the last who looked at it, or hit it. */
    @Nullable private UUID haunted;
    /** Whether it was looked at last tick. */
    private boolean watchedLastTick;
    /** Ticks since last looked at; when it vanishes; ticks until it teleports (none if -1); ticks until it creeps nearer. */
    private int unseenTicks, vanishAfter, teleportIn = -1, stalkIn;

    public WatchlingEntity(EntityType<? extends Enderman> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createWatchlingAttributes() {
        return Enderman.createAttributes()
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HUNTING, false);
    }

    /** Whether it hunts: it has been seen, and hasn't yet given up. */
    public boolean isHunting() {
        return this.entityData.get(DATA_HUNTING);
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
            this.entityData.set(DATA_HUNTING, true);
            this.unseenTicks = 0;
            this.teleportIn = -1;
        } else if (this.isHunting()) {
            this.unseenTicks++;
            if (this.watchedLastTick) {
                // Just looked away from: gone in a moment (and back nearer), and gone for good if not looked at for a while
                int delay = VersusSettings.Watchlings.TELEPORT_DELAY;
                this.teleportIn = delay + this.random.nextInt(delay + 1);
                this.vanishAfter = this.vanishTime();
                this.stalkIn = this.stalkTime();
            }
            if (this.teleportIn > 0 && --this.teleportIn == 0) {
                this.haunt(level);
            } else if (this.teleportIn < 0 && --this.stalkIn <= 0) {
                // Unlooked at, it still creeps nearer
                this.stalkIn = this.stalkTime();
                this.haunt(level);
            }
            if (this.vanishAfter > 0 && this.unseenTicks >= this.vanishAfter) {
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
            this.entityData.set(DATA_HUNTING, true);
            this.teleportIn = 1;
            this.stalkIn = this.stalkTime();
            if (this.vanishAfter <= 0) this.vanishAfter = VersusSettings.Watchlings.VANISH_MAX;
        }
        return hurt;
    }

    private int vanishTime() {
        int min = VersusSettings.Watchlings.VANISH_MIN, max = VersusSettings.Watchlings.VANISH_MAX;
        return min + this.random.nextInt(max - min + 1);
    }

    private int stalkTime() {
        int min = VersusSettings.Watchlings.STALK_MIN, max = VersusSettings.Watchlings.STALK_MAX;
        return min + this.random.nextInt(max - min + 1);
    }

    @Nullable
    private Player hauntedPlayer(ServerLevel level) {
        if (this.haunted == null) return null;
        Player player = level.getPlayerByUUID(this.haunted);
        return player != null && player.isAlive() && !player.isSpectator() && player.distanceToSqr(this) < 64.0 * 64.0 ? player : null;
    }

    /**
     * Teleports nearer to whom it haunts (WatchlingStare#approach), where they aren't looking: more than ASIDE_DEGREES
     * to either side of the way they face, which is mostly behind them.
     */
    private void haunt(ServerLevel level) {
        Player prey = this.hauntedPlayer(level);
        if (prey == null) {
            this.vanish(level);
            return;
        }
        double now = Math.sqrt(prey.distanceToSqr(this.getX(), prey.getY(), this.getZ()));
        double distance = WatchlingStare.approach(now, this.random.nextDouble());
        Vec3 view = prey.getViewVector(1.0F).multiply(1.0, 0.0, 1.0);
        view = view.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : view.normalize();
        double aside = Math.toRadians(VersusSettings.Watchlings.ASIDE_DEGREES);
        for (int attempt = 0; attempt < 20; attempt++) {
            // Anywhere but within aside of the way they face; a failed place is tried again a little further out
            double angle = aside + this.random.nextDouble() * (2.0 * Math.PI - 2.0 * aside);
            Vec3 way = view.yRot((float) angle);
            double reach = distance + attempt * 0.25;
            double y = prey.getY() + this.random.nextInt(5) - 1;
            if (this.teleport(prey.getX() + way.x * reach, y, prey.getZ() + way.z * reach)) return;
        }
    }

    /** Gone: a wisp of the End where it stood, and nothing. */
    private void vanish(ServerLevel level) {
        level.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY(0.5), this.getZ(), 24, 0.3, 1.0, 0.3, 0.2);
        this.discard();
    }
}
