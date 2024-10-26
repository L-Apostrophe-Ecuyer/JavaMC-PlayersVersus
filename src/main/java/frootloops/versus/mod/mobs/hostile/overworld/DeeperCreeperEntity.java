package frootloops.versus.mod.mobs.hostile.overworld;


import frootloops.versus.VersusMod;
import frootloops.versus.mod.mobs.hostile.ai.CreepingAndExplodingGoal;
import frootloops.versus.mod.mobs.hostile.ai.FollowTargetThroughWallsGoal;
import net.minecraft.block.BlockState;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;


public class DeeperCreeperEntity extends CreeperEntity {
    //private static final TrackedData<Integer> FUSE_SPEED = DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.INTEGER);
    //private static final TrackedData<Boolean> IGNITED = DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private int lastFuseTime, currentFuseTime, fuseTime = 29, explosionRadius = 4;
    public static final SoundEvent DREEPER_AMBIENCE_SOUND = SoundEvent.of(Identifier.of("ambient.cave"), 32);

    public DeeperCreeperEntity(EntityType<? extends CreeperEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 29;
    }

    public static DefaultAttributeContainer.Builder createDeeperCreeperAttributes() {
        return HostileEntity.createHostileAttributes().add(EntityAttributes.MOVEMENT_SPEED, 0.36).add(EntityAttributes.FOLLOW_RANGE, 40.0).add(EntityAttributes.ARMOR, 10.0).add(EntityAttributes.ARMOR_TOUGHNESS, 3.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new CreepingAndExplodingGoal(this, 1.0));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(6, new LookAroundGoal(this));
        this.targetSelector.add(1, new FollowTargetThroughWallsGoal<PlayerEntity>((MobEntity)this, PlayerEntity.class, true));
    }


    @Override
    public boolean damage(ServerWorld serverWorld, DamageSource source, float amount) {
        if(source.isOf(DamageTypes.WITHER)) return false;
        return super.damage(serverWorld, source, amount);
    }


    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putShort("Fuse", (short)this.fuseTime);
        nbt.putByte("ExplosionRadius", (byte)this.explosionRadius);
        nbt.putBoolean("ignited", this.isIgnited());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("Fuse", NbtElement.NUMBER_TYPE)) this.fuseTime = nbt.getShort("Fuse");
        if (nbt.contains("ExplosionRadius", NbtElement.NUMBER_TYPE)) this.explosionRadius = nbt.getByte("ExplosionRadius");
        if (nbt.getBoolean("ignited")) this.ignite();
    }

    @Override
    public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
        super.onStruckByLightning(world, lightning);
    }
    @Override
    public boolean shouldDropHead() {
        return false;
    }

    @Override
    public void tick() {
        if (this.isAlive()) {
            int i;
            this.lastFuseTime = this.currentFuseTime;
            if (this.isIgnited()) {
                this.setFuseSpeed(1);
            }
            if ((i = this.getFuseSpeed()) > 0 && this.currentFuseTime == 0) {
                this.playSound(SoundEvents.ENTITY_CREEPER_PRIMED, 1.0f, 0.5f);
                this.emitGameEvent(GameEvent.PRIME_FUSE);
            }
            this.currentFuseTime += i;
            if (this.currentFuseTime < 0) {
                this.currentFuseTime = 0;
            }
            if (this.currentFuseTime >= this.fuseTime) {
                this.currentFuseTime = this.fuseTime;
                this.explode();
            }
        }
        super.tick();
    }

    private void explode() {
        if (!this.getWorld().isClient) {
            float explosionMultiplier = this.isCharged() ? 2.0f : this.hurtTime > 0 ? 0.5f : 1.0f;
            this.dead = true;
            this.getWorld().createExplosion(this, this.getX(), this.getY(), this.getZ(), (float)this.explosionRadius * explosionMultiplier, World.ExplosionSourceType.MOB);
            this.spawnEffectsCloud();
            this.onRemoval((ServerWorld) this.getWorld(), Entity.RemovalReason.KILLED);
            this.discard();
        }
    }

    private void spawnEffectsCloud() {
        AreaEffectCloudEntity areaEffectCloudEntity = new AreaEffectCloudEntity(this.getWorld(), this.getX(), this.getY(), this.getZ());
        areaEffectCloudEntity.setRadius(5f);
        areaEffectCloudEntity.setRadiusOnUse(-0.5f);
        areaEffectCloudEntity.setWaitTime(10);
        areaEffectCloudEntity.setDuration(300);
        areaEffectCloudEntity.setRadiusGrowth(-areaEffectCloudEntity.getRadius() / 300.0f);
        areaEffectCloudEntity.addEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 600, 0, false, false));
        areaEffectCloudEntity.addEffect(new StatusEffectInstance(StatusEffects.WITHER, 600, 0, false, true));
        this.getWorld().spawnEntity(areaEffectCloudEntity);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.BLOCK_STONE_STEP, 1.2f, 1.0f + 0.2f * random.nextFloat());
        this.playSound(SoundEvents.ENTITY_CREEPER_HURT, 0.1f, 1.4f);
        this.playSound(SoundEvents.BLOCK_MANGROVE_ROOTS_STEP, 0.2f, 0.8F);
    }

    @Override
    protected void playHurtSound(DamageSource source) {
        this.playSound(SoundEvents.ENTITY_CREEPER_HURT, this.getSoundVolume(), this.getSoundPitch());
        this.playSound(SoundEvents.BLOCK_MANGROVE_ROOTS_BREAK, 0.4f, 0.8F);
    }

    @Override
    public int getMinAmbientSoundDelay() {
        return 500;
    }

    @Override
    public void playAmbientSound() {
        if (this.getTarget() != null) return;
        this.playSound(DREEPER_AMBIENCE_SOUND, 1.2f, 0.6f + this.random.nextFloat() * 0.8f);
    }
}
