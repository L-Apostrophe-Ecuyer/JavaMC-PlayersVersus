package frootloops.versus.mod.mobs.hostile.nether;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class WildfireEntity extends HostileEntity {

    private float eyeOffset = 0.5F;
    private int eyeOffsetCooldown;
    private static final TrackedData<Byte> FIRE_ACTIVE = DataTracker.registerData(WildfireEntity.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Byte> ACTIVE_SHIELDS_BYTEMASK = DataTracker.registerData(WildfireEntity.class, TrackedDataHandlerRegistry.BYTE);

    public WildfireEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
        this.setPathfindingPenalty(PathNodeType.LAVA, 8.0F);
        this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, 0.0F);
        this.setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, 0.0F);
        this.experiencePoints = 40;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(FIRE_ACTIVE, (byte)1);
        builder.add(ACTIVE_SHIELDS_BYTEMASK, (byte)17);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(4, new WildfireShootFireBallsGoal(this));
        this.goalSelector.add(5, new GoToWalkTargetGoal(this, 1.0));
        this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0, 0.0F));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal(this, PlayerEntity.class, true));
    }

    public static DefaultAttributeContainer.Builder createWildfireAttributes() {
        return HostileEntity.createMobAttributes()
                .add(EntityAttributes.MOVEMENT_SPEED, 0.5F)
                .add(EntityAttributes.MAX_HEALTH, 120.0)
                .add(EntityAttributes.FOLLOW_RANGE, 40.0)
                .add(EntityAttributes.ATTACK_DAMAGE, 6.0);
    }

    @Override
    protected void mobTick(ServerWorld world) {
        this.eyeOffsetCooldown--;
        if (this.eyeOffsetCooldown <= 0) {
            this.eyeOffsetCooldown = 100;
            this.eyeOffset = (float)this.random.nextTriangular(0.5, 6.891);
        }

        LivingEntity livingEntity = this.getTarget();
        if (livingEntity != null && livingEntity.getEyeY() > this.getEyeY() + (double)this.eyeOffset && this.canTarget(livingEntity)) {
            Vec3d vec3d = this.getVelocity();
            this.setVelocity(this.getVelocity().add(0.0, (0.2F - vec3d.y) * 0.2F, 0.0));
            this.velocityDirty = true;
        }

        super.mobTick(world);
    }

    @Override
    public void tickMovement() {
        if (!this.isOnGround() && this.getVelocity().y < 0.0) {
            this.setVelocity(this.getVelocity().multiply(1.0, 0.6, 1.0));
            this.fallDistance = 0.0f;
        }

        if (this.getEntityWorld().isClient()) {
            if (this.random.nextInt(24) == 0 && !this.isSilent()) {
                this.playSound(SoundEvents.ENTITY_BLAZE_BURN, 1.0F + this.random.nextFloat(), this.random.nextFloat() * 0.7F + 0.3F);
            }

            for (int i = 0; i < (this.isFireActive() ? 4 : 2); i++) {
                this.getEntityWorld().addParticleClient(ParticleTypes.SOUL_FIRE_FLAME, this.getParticleX(0.5), this.getRandomBodyY(), this.getParticleZ(0.5), 0.0, 0.0, 0.0);
            }
        }

        super.tickMovement();
    }

    public void addSoulFlameParticles(int count) {
        if(this.getEntityWorld() instanceof ServerWorld serverWorld) {
            for (int i = 0; i < count; i++) {
                double d = this.random.nextGaussian() * 0.02;
                double e = this.random.nextGaussian() * 0.02;
                double f = this.random.nextGaussian() * 0.02;
                serverWorld.addParticleClient(ParticleTypes.SOUL_FIRE_FLAME, this.getParticleX(1.0) - d * 10.0, this.getRandomBodyY() - e * 10.0, this.getParticleZ(1.0) - f * 10.0, d, e, f);
            }
        }
    }



    @Override public boolean hurtByWater() {
        return true;
    }
    @Override public boolean isOnFire() {return this.isFireActive();}
    private boolean isFireActive() {
        return (this.dataTracker.get(FIRE_ACTIVE) & 1) != 0;
    }
    void setFireActive(boolean fireActive) {
        byte b = this.dataTracker.get(FIRE_ACTIVE);
        if (fireActive) b = (byte)(b | 1);
        else b = (byte)(b & -2);
        this.dataTracker.set(FIRE_ACTIVE, b);
    }

    @Override protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_BLAZE_AMBIENT;
    }
    @Override protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_BLAZE_HURT;
    }
    @Override protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_BLAZE_DEATH;
    }
}
