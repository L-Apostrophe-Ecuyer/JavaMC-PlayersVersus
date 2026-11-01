package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusSettings;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A watchling: an enderman the paleness took, with a creaking's yellow eyes and a tall head with three of them, big arms,
 * where the End's warping and the depths' paleness meet. Left alone it haunts, as a slender thing does. It doesn't walk then
 * and doesn't strike: it stands, and it watches. Looking at it is what kills (WatchlingStare): the screen fills with
 * static, and whoever looks too long dies into silverfish. And each time it's looked at and then left, it's gone a moment
 * later and back nearer, where the watcher isn't looking: closer and closer, until it's too near to live beside, which
 * kills as a stare does. Seen once, it hunts: left unlooked at, it still creeps nearer now and then, and it keeps on for
 * 90 seconds to 3 minutes before it gives up and vanishes. While it hunts, the view of whoever it hunts is drawn towards
 * it now and then (client WatchlingPull). But attacked, it stops haunting and fights, as an enderman does (it is a melee
 * mob with big arms): it goes for whoever hit it, teleporting up to them if they get away, until they're dead or gone;
 * only then does it haunt again. Looking at it still fills the screen with static, but being near it doesn't: that was
 * the haunting's. Like all the warped it shows only to a stare (Gaze), and a carved pumpkin hides you from its gaze as
 * from an enderman's.
 */
public class WatchlingEntity extends Enderman {
    /** Whether it hunts (it's been seen), for the clients that feel its pull. */
    private static final EntityDataAccessor<Boolean> DATA_HUNTING = SynchedEntityData.defineId(WatchlingEntity.class, EntityDataSerializers.BOOLEAN);
    /** Whether it fights (it was attacked): then it doesn't haunt. */
    private static final EntityDataAccessor<Boolean> DATA_ENRAGED = SynchedEntityData.defineId(WatchlingEntity.class, EntityDataSerializers.BOOLEAN);

    /** Whom it haunts: the last who looked at it, or hit it. */
    @Nullable private UUID haunted;
    /** Whether it was looked at last tick. */
    private boolean watchedLastTick;
    /** Ticks since last looked at; when it vanishes; ticks until it teleports (none if -1); ticks until it creeps nearer. */
    private int unseenTicks, vanishAfter, teleportIn = -1, stalkIn;
    /** Fighting: ticks without a target alive to fight, and ticks the target has been out of reach. */
    private int lostTicks, farTicks;

    public WatchlingEntity(EntityType<? extends Enderman> type, Level level) {
        super(type, level);
    }

    /** An enderman's (40 health, 7 damage, 0.3 speed), seeing a little less far. */
    public static AttributeSupplier.Builder createWatchlingAttributes() {
        return Enderman.createAttributes().add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HUNTING, false);
        builder.define(DATA_ENRAGED, false);
    }

    /** Whether it haunts: it has been seen, hasn't given up and isn't fighting. */
    public boolean isHunting() {
        return this.entityData.get(DATA_HUNTING) && !this.isEnraged();
    }

    /** Whether it fights: it was attacked, and has not yet lost or killed whom it fights. */
    public boolean isEnraged() {
        return this.entityData.get(DATA_ENRAGED);
    }

    /** Goals that only count while it fights: it walks, looks and strikes as an enderman does, but only then. */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false) {
            @Override
            public boolean canUse() {
                return WatchlingEntity.this.isEnraged() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return WatchlingEntity.this.isEnraged() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.0F) {
            @Override
            public boolean canUse() {
                return WatchlingEntity.this.isEnraged() && super.canUse();
            }
        });
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F) {
            @Override
            public boolean canUse() {
                return WatchlingEntity.this.isEnraged() && super.canUse();
            }
        });
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return WatchlingEntity.this.isEnraged() && super.canUse();
            }
        });
        // Whoever hurts it is whom it fights (hurtServer makes it fight at once; this keeps the target while it does)
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public boolean isPushable() {
        return this.isEnraged() && super.isPushable();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.isEnraged()) {
            this.fight(level);
            return;
        }
        Player watcher = WatchlingStare.watcher(this);
        if (watcher != null) {
            this.haunted = watcher.getUUID();
            this.entityData.set(DATA_HUNTING, true);
            this.unseenTicks = 0;
            this.teleportIn = -1;
        } else if (this.entityData.get(DATA_HUNTING)) {
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

    /**
     * Fighting: as an enderman, it teleports up to a target that's out of reach, and it calms (and haunts again) once it
     * has had no target alive for CALM_TICKS. The stare it still puts on the screen is WatchlingStare's.
     */
    private void fight(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            if (++this.lostTicks >= VersusSettings.Watchlings.CALM_TICKS) this.calm();
            return;
        }
        this.lostTicks = 0;
        this.watchedLastTick = false;
        double reach = VersusSettings.Watchlings.FIGHT_TELEPORT_RANGE;
        if (target.distanceToSqr(this) > reach * reach) {
            if (++this.farTicks >= VersusSettings.Watchlings.FIGHT_TELEPORT_TICKS && this.teleportNear(target)) this.farTicks = 0;
        } else {
            this.farTicks = 0;
        }
    }

    /** It fights whoever hit it, and stops haunting. */
    private void enrage(LivingEntity attacker) {
        if (attacker instanceof Player player) this.haunted = player.getUUID();
        this.entityData.set(DATA_ENRAGED, true);
        this.setTarget(attacker);
        this.lostTicks = 0;
        this.farTicks = 0;
        this.teleportIn = -1;
        this.unseenTicks = 0;
    }

    /** The fight is over: it haunts again whoever it fought, and gives up in the usual time if not looked at. */
    private void calm() {
        this.entityData.set(DATA_ENRAGED, false);
        this.entityData.set(DATA_HUNTING, true);
        this.setTarget(null);
        this.lostTicks = 0;
        this.unseenTicks = 0;
        this.vanishAfter = this.vanishTime();
        this.stalkIn = this.stalkTime();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        // Hit, or shot at (it dodges arrows, which doesn't make it any less angry), it fights whoever did it
        if (this.isAlive() && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof WatchlingEntity)
                && (hurt || source.is(DamageTypeTags.IS_PROJECTILE))) {
            this.enrage(attacker);
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

    /** Teleports up to a target, a few blocks off it, in the open. Whether it did. */
    private boolean teleportNear(LivingEntity target) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double reach = 3.0 + this.random.nextDouble() * 2.0;
            if (this.teleport(target.getX() + Math.cos(angle) * reach, target.getY() + this.random.nextInt(3) - 1, target.getZ() + Math.sin(angle) * reach)) return true;
        }
        return false;
    }

    /** Gone: a wisp of the End where it stood, and nothing. */
    private void vanish(ServerLevel level) {
        level.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY(0.5), this.getZ(), 24, 0.3, 1.0, 0.3, 0.2);
        this.discard();
    }
}
