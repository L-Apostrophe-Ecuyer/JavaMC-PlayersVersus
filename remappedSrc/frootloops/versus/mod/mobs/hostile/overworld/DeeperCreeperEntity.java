package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.mod.mobs.hostile.ai.CreepingAndExplodingGoal;
import frootloops.versus.mod.mobs.hostile.ai.FollowTargetThroughWallsGoal;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.*;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

public class DeeperCreeperEntity extends CreeperEntity {
    //private static final TrackedData<Integer> FUSE_SPEED = DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.INTEGER);
    //private static final TrackedData<Boolean> IGNITED = DataTracker.registerData(CreeperEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private int lastFuseTime, currentFuseTime, fuseTime = 34, explosionRadius = 5;
    public static final SoundEvent DREEPER_AMBIENCE_SOUND = SoundEvent.of(Identifier.of("ambient.cave"), 32);

    public DeeperCreeperEntity(EntityType<? extends CreeperEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 29;
    }

    public static DefaultAttributeContainer.Builder createDeeperCreeperAttributes() {
        return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.36).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0).add(EntityAttributes.GENERIC_ARMOR, 10.0).add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 3.0);
    }

    @Override
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        if(spawnReason == SpawnReason.NATURAL && (this.getBlockPos().getY() > -16 || this.method_48926().getLightLevel(this.getBlockPos()) > 10)) return false;
        if(!this.getSteppingBlockState().isOf(Blocks.DEEPSLATE)) return false;
        return this.getPathfindingFavor(this.getBlockPos(), world) >= 0.0f;
    }

    public static boolean canSpawn(EntityType<RabbitEntity> entity, WorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
        return pos.getY() < 0 && world.getBlockState(pos.down()).isIn(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
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
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        //builder.add(FUSE_SPEED, -1);
        //builder.add(CHARGED, false);
        //builder.add(IGNITED, false);
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
        if (!this.method_48926().isClient) {
            this.dead = true;
            this.method_48926().createExplosion(this, this.getX(), this.getY(), this.getZ(), (float)this.explosionRadius, World.ExplosionSourceType.MOB);
            this.discard();
            this.spawnEffectsCloud();
        }
    }

    private void spawnEffectsCloud() {
        AreaEffectCloudEntity areaEffectCloudEntity = new AreaEffectCloudEntity(this.method_48926(), this.getX(), this.getY(), this.getZ());
        areaEffectCloudEntity.setRadius(5f);
        areaEffectCloudEntity.setRadiusOnUse(-0.5f);
        areaEffectCloudEntity.setWaitTime(10);
        areaEffectCloudEntity.setDuration(areaEffectCloudEntity.getDuration()/2);
        areaEffectCloudEntity.setRadiusGrowth(-areaEffectCloudEntity.getRadius() / (float)areaEffectCloudEntity.getDuration());
        areaEffectCloudEntity.addEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 120, 0, true, false));
        areaEffectCloudEntity.addEffect(new StatusEffectInstance(StatusEffects.WITHER, 120, 0, true, false));
        this.method_48926().spawnEntity(areaEffectCloudEntity);
    }

    @Override
    public boolean canHaveStatusEffect(StatusEffectInstance effect) {
        if (effect.getEffectType() == StatusEffects.WITHER) return false;
        else return super.canHaveStatusEffect(effect);
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
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
