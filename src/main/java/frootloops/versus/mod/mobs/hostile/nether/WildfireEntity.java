package frootloops.versus.mod.mobs.hostile.nether;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

public class WildfireEntity extends Monster {

    private float eyeOffset = 0.5F;
    private int eyeOffsetCooldown;
    private static final EntityDataAccessor<Byte> FIRE_ACTIVE = SynchedEntityData.defineId(WildfireEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> ACTIVE_SHIELDS_BYTEMASK = SynchedEntityData.defineId(WildfireEntity.class, EntityDataSerializers.BYTE);

    public WildfireEntity(EntityType<? extends Monster> entityType, Level world) {
        super(entityType, world);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.LAVA, 8.0F);
        this.setPathfindingMalus(PathType.FIRE_IN_NEIGHBOR, 0.0F);
        this.setPathfindingMalus(PathType.FIRE, 0.0F);
        this.xpReward = 40;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FIRE_ACTIVE, (byte)1);
        builder.define(ACTIVE_SHIELDS_BYTEMASK, (byte)17);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(4, new WildfireShootFireBallsGoal(this));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.0F));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Player.class, true));
    }

    public static AttributeSupplier.Builder createWildfireAttributes() {
        return Monster.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.5F)
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    @Override
    protected void customServerAiStep(ServerLevel world) {
        this.eyeOffsetCooldown--;
        if (this.eyeOffsetCooldown <= 0) {
            this.eyeOffsetCooldown = 100;
            this.eyeOffset = (float)this.random.triangle(0.5, 6.891);
        }

        LivingEntity livingEntity = this.getTarget();
        if (livingEntity != null && livingEntity.getEyeY() > this.getEyeY() + (double)this.eyeOffset && this.canAttack(livingEntity)) {
            Vec3 vec3d = this.getDeltaMovement();
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, (0.2F - vec3d.y) * 0.2F, 0.0));
            this.needsSync = true;
        }

        super.customServerAiStep(world);
    }

    @Override
    public void aiStep() {
        if (!this.onGround() && this.getDeltaMovement().y < 0.0) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.6, 1.0));
            this.fallDistance = 0.0f;
        }

        if (this.level().isClientSide()) {
            if (this.random.nextInt(24) == 0 && !this.isSilent()) {
                this.playSound(SoundEvents.BLAZE_BURN, 1.0F + this.random.nextFloat(), this.random.nextFloat() * 0.7F + 0.3F);
            }

            for (int i = 0; i < (this.isFireActive() ? 4 : 2); i++) {
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 0.0, 0.0, 0.0);
            }
        }

        super.aiStep();
    }

    public void addSoulFlameParticles(int count) {
        if(this.level() instanceof ServerLevel serverWorld) {
            for (int i = 0; i < count; i++) {
                double d = this.random.nextGaussian() * 0.02;
                double e = this.random.nextGaussian() * 0.02;
                double f = this.random.nextGaussian() * 0.02;
                serverWorld.addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getRandomX(1.0) - d * 10.0, this.getRandomY() - e * 10.0, this.getRandomZ(1.0) - f * 10.0, d, e, f);
            }
        }
    }



    @Override public boolean isSensitiveToWater() {
        return true;
    }
    @Override public boolean isOnFire() {return this.isFireActive();}
    private boolean isFireActive() {
        return (this.entityData.get(FIRE_ACTIVE) & 1) != 0;
    }
    void setFireActive(boolean fireActive) {
        byte b = this.entityData.get(FIRE_ACTIVE);
        if (fireActive) b = (byte)(b | 1);
        else b = (byte)(b & -2);
        this.entityData.set(FIRE_ACTIVE, b);
    }

    @Override protected SoundEvent getAmbientSound() {
        return SoundEvents.BLAZE_AMBIENT;
    }
    @Override protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }
    @Override protected SoundEvent getDeathSound() {
        return SoundEvents.BLAZE_DEATH;
    }
}
