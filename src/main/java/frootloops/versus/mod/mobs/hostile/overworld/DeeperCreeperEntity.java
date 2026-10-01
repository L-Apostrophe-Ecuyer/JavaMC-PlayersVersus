package frootloops.versus.mod.mobs.hostile.overworld;


import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;


public class DeeperCreeperEntity extends Creeper {
    //private static final TrackedData<Integer> FUSE_SPEED = DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.INTEGER);
    //private static final TrackedData<Boolean> IGNITED = DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private int lastFuseTime, currentFuseTime, fuseTime = 29, explosionRadius = 4;
    public static final SoundEvent DREEPER_AMBIENCE_SOUND = SoundEvent.createFixedRangeEvent(Identifier.parse("ambient.cave"), 32);

    public DeeperCreeperEntity(EntityType<? extends Creeper> entityType, Level world) {
        super(entityType, world);
        this.xpReward = 29;
    }

    public static AttributeSupplier.Builder createDeeperCreeperAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.36).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.ARMOR, 10.0).add(Attributes.ARMOR_TOUGHNESS, 3.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new CreepingAndExplodingGoal(this, 1.0));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new CreeperFollowTargetThroughWallsGoal<Player>((Mob)this, Player.class, true));
    }


    @Override
    public boolean hurtServer(ServerLevel serverWorld, DamageSource source, float amount) {
        if(source.is(DamageTypes.WITHER)) return false;
        return super.hurtServer(serverWorld, source, amount);
    }


    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        super.addAdditionalSaveData(view);
        view.putShort("Fuse", (short)this.fuseTime);
        view.putByte("ExplosionRadius", (byte)this.explosionRadius);
        view.putBoolean("ignited", this.isIgnited());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        super.readAdditionalSaveData(view);
        this.fuseTime = view.getShortOr("Fuse", (short)30);
        this.explosionRadius = view.getByteOr("ExplosionRadius", (byte)3);
        if (view.getBooleanOr("ignited", false)) {
            this.ignite();
        }
    }


    @Override
    public void thunderHit(ServerLevel world, LightningBolt lightning) {
        super.thunderHit(world, lightning);
    }

    @Override
    public void tick() {
        if (this.isAlive()) {
            int i;
            this.lastFuseTime = this.currentFuseTime;
            if (this.isIgnited()) {
                this.setSwellDir(1);
            }
            if ((i = this.getSwellDir()) > 0 && this.currentFuseTime == 0) {
                this.playSound(SoundEvents.CREEPER_PRIMED, 1.0f, 0.5f);
                this.gameEvent(GameEvent.PRIME_FUSE);
            }
            this.currentFuseTime += i;
            if (this.currentFuseTime < 0) {
                this.currentFuseTime = 0;
            }
            if (this.currentFuseTime >= this.fuseTime) {
                this.currentFuseTime = this.fuseTime;
                this.explodeCreeper();
            }
        }
        super.tick();
    }

    private void explodeCreeper() {
        if (!this.level().isClientSide()) {
            float explosionMultiplier = this.isPowered() ? 2.0f : this.hurtTime > 0 ? 0.5f : 1.0f;
            this.dead = true;
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), (float)this.explosionRadius * explosionMultiplier, Level.ExplosionInteraction.MOB);
            this.spawnLingeringCloud();
            this.triggerOnDeathMobEffects((ServerLevel) this.level(), Entity.RemovalReason.KILLED);
            this.discard();
        }
    }

    private void spawnLingeringCloud() {
        AreaEffectCloud areaEffectCloudEntity = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
        areaEffectCloudEntity.setRadius(5f);
        areaEffectCloudEntity.setRadiusOnUse(-0.5f);
        areaEffectCloudEntity.setWaitTime(10);
        areaEffectCloudEntity.setDuration(300);
        areaEffectCloudEntity.setRadiusPerTick(-areaEffectCloudEntity.getRadius() / 300.0f);
        areaEffectCloudEntity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 600, 0, false, false));
        areaEffectCloudEntity.addEffect(new MobEffectInstance(MobEffects.WITHER, 600, 0, false, true));
        this.level().addFreshEntity(areaEffectCloudEntity);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.STONE_STEP, 1.2f, 1.0f + 0.2f * random.nextFloat());
        this.playSound(SoundEvents.MANGROVE_ROOTS_STEP, 0.2f, 0.8F);
    }

    @Override
    protected void playHurtSound(DamageSource source) {
        this.playSound(SoundEvents.CREEPER_HURT, this.getSoundVolume(), this.getVoicePitch());
        this.playSound(SoundEvents.MANGROVE_ROOTS_BREAK, 0.4f, 0.8F);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 500;
    }

    @Override
    public void playAmbientSound() {
        if (this.getTarget() != null) return;
        this.playSound(DREEPER_AMBIENCE_SOUND, 1.2f, 0.6f + this.random.nextFloat() * 0.8f);
    }
}
