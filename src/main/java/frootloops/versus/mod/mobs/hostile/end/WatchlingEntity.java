package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.mod.mobs.hostile.overworld.FreezeWhenWatchedGoal;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A watchling: an enderman the paleness took, with a creaking's yellow eyes, where the End's warping and the depths'
 * paleness meet. Like the creaking it moves only while no one looks at it; unwatched it teleports up behind its prey,
 * closer each time. Looking at it is what kills: the screen fills with static, and whoever looks too long dies into
 * silverfish (WatchlingStare). Its own hits convert (ConversionAttacks). A carved pumpkin hides you from its gaze as from
 * an enderman's.
 */
public class WatchlingEntity extends Enderman {
    /** At most this often (ticks, doubled at random) it teleports up behind its prey, landing this near behind them. */
    static final int CREEP_COOLDOWN = 80;
    static final double CREEP_NEAR = 6.0, CREEP_FAR = 10.0;

    public WatchlingEntity(EntityType<? extends Enderman> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createWatchlingAttributes() {
        return Enderman.createAttributes().add(Attributes.FOLLOW_RANGE, 48.0);
    }

    /** No block carrying, no anger at a stare: it hunts, freezing while watched (as a pale spider or a creaking). */
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FreezeWhenWatchedGoal(this));
        this.goalSelector.addGoal(2, new CreepBehindGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.0F));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** Unwatched and far from its prey, it teleports up behind them: where they aren't looking. */
    static class CreepBehindGoal extends Goal {
        private final WatchlingEntity watchling;
        private int cooldown;

        CreepBehindGoal(WatchlingEntity watchling) {
            this.watchling = watchling;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.cooldown > 0) {
                this.cooldown--;
                return false;
            }
            LivingEntity target = this.watchling.getTarget();
            return target instanceof Player && this.watchling.distanceToSqr(target) > CREEP_FAR * CREEP_FAR
                    && !WatchlingStare.watchedByAnyone(this.watchling);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            this.cooldown = CREEP_COOLDOWN + this.watchling.getRandom().nextInt(CREEP_COOLDOWN);
            LivingEntity target = this.watchling.getTarget();
            if (target == null) return;
            Vec3 behind = target.getViewVector(1.0F).multiply(-1.0, 0.0, -1.0);
            if (behind.lengthSqr() < 1.0E-4) behind = new Vec3(1.0, 0.0, 0.0);
            behind = behind.normalize();
            for (int attempt = 0; attempt < 8; attempt++) {
                double distance = CREEP_NEAR + this.watchling.getRandom().nextDouble() * (CREEP_FAR - CREEP_NEAR);
                Vec3 way = behind.yRot((float) ((this.watchling.getRandom().nextDouble() - 0.5) * 1.2));
                double y = target.getY() + this.watchling.getRandom().nextInt(5) - 2;
                if (this.watchling.teleport(target.getX() + way.x * distance, y, target.getZ() + way.z * distance)) return;
            }
        }
    }
}
