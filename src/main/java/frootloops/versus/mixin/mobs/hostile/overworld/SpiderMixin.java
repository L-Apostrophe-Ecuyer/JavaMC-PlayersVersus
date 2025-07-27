package frootloops.versus.mixin.mobs.hostile.overworld;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CobwebBlock;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(SpiderEntity.class)
public class SpiderMixin extends HostileEntity {
    private static final TrackedData<Boolean> BABY = DataTracker.registerData(SpiderEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private static final Identifier BABY_SCALE_MODIFIER_ID = Identifier.ofVanilla("baby");
    private static final EntityAttributeModifier BABY_SCALE_MODIFIER  = new EntityAttributeModifier(BABY_SCALE_MODIFIER_ID, -0.5, EntityAttributeModifier.Operation.ADD_VALUE);

    protected SpiderMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void playAmbientSound() {
        // Skip! Spiders should be quieter. Also, their ambient noise is pretty grating
    }

    @Override
    public float getSoundPitch() {
        return this.isBaby() ? (this.random.nextFloat() - this.random.nextFloat()) * 0.3F + 1.5F : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 0.6F;
    }

    @Override
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        if(spawnReason != SpawnReason.NATURAL) return super.canSpawn(world, spawnReason);

        int y = this.getBlockPos().getY();
        if(y < -8) return false;

        int ySpawnBonus = world.getBiome(this.getBlockPos()).isIn(BiomeTags.SPAWNS_WARM_VARIANT_FROGS) ? 32 : 0;
        if(y > 96 + ySpawnBonus) return false;
        if(y > 64 + ySpawnBonus && world.getLightLevel(LightType.SKY, this.getBlockPos()) > 2) return false;
        return super.canSpawn(world, spawnReason);
    }

    @Override
    protected int computeFallDamage(float fallDistance, float damageMultiplier) {
        return super.computeFallDamage(fallDistance, damageMultiplier) - 10;
    }

    @Override
    protected void dropLoot(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        super.dropLoot(world, source, causedByPlayer);
        if(!this.isBaby()) {
            super.dropLoot(world, source, causedByPlayer); // Triple loot for the big boys!
            super.dropLoot(world, source, causedByPlayer);
        }
    }

    @Override
    public int getXpToDrop(ServerWorld world) {
        if (!this.isBaby()) this.experiencePoints = 17;
        return super.getXpToDrop(world);
    }

    @Override
    @Nullable
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
        entityData = super.initialize(world, difficulty, spawnReason, entityData);
        if (entityData == null) {
            entityData = new SpiderEntity.SpiderData();
            if ((world.getDifficulty() == Difficulty.HARD || world.getMoonPhase() == 7 || this.getY() < 32.0) && random.nextFloat() < 0.3f * difficulty.getClampedLocalDifficulty()) {
                ((SpiderEntity.SpiderData)entityData).setEffect(random);
            }
        }
        if (entityData instanceof SpiderEntity.SpiderData) {
            SpiderEntity.SpiderData spiderData = (SpiderEntity.SpiderData)entityData;
            if (spiderData.effect != null && spiderData.effect.hasKeyAndValue()) {
                this.addStatusEffect(new StatusEffectInstance(spiderData.effect, -1));
            }
        }

        EntityAttributeInstance instanceMvt = this.getAttributes().getCustomInstance(EntityAttributes.MOVEMENT_SPEED);
        EntityAttributeInstance instanceDmg = this.getAttributes().getCustomInstance(EntityAttributes.ATTACK_DAMAGE);
        EntityAttributeInstance instanceHP = this.getAttributes().getCustomInstance(EntityAttributes.MAX_HEALTH);
        EntityAttributeInstance instanceScale = this.getAttributes().getCustomInstance(EntityAttributes.SCALE);

        if(random.nextFloat() < 0.85F) {
            this.setBaby(true);
            if (instanceMvt != null) instanceMvt.setBaseValue(0.36D);
            if (instanceDmg != null) instanceDmg.setBaseValue(3.0D);
            if (instanceScale != null) instanceScale.setBaseValue(0.7D);
            if (instanceHP != null) {
                instanceHP.setBaseValue(12.0f);
                this.setHealth(12.0f);
            }
        }
        else {
            if (instanceMvt != null) instanceMvt.setBaseValue(0.32D);
            if (instanceDmg != null) instanceDmg.setBaseValue(6.0D);
            if (instanceScale != null) instanceScale.setBaseValue(1.1D);
            if (instanceHP != null) {
                instanceHP.setBaseValue(24.0f);
                this.setHealth(24.0f);
            }
            SkeletonEntity skeletonEntity;
            if (random.nextInt(60) == 0 && (skeletonEntity = EntityType.SKELETON.create(this.getWorld(), SpawnReason.JOCKEY)) != null) {
                skeletonEntity.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), this.getYaw(), 0.0f);
                skeletonEntity.initialize(world, difficulty, spawnReason, null);
                skeletonEntity.startRiding(this);
            }
        }

        this.getNavigation().setCanSwim(true);
        return entityData;
    }


    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void addBabyData(DataTracker.Builder builder, CallbackInfo ci) {
        builder.add(BABY, false);
    }

    @Override
    public boolean tryAttack(ServerWorld world, Entity target) {
        if (super.tryAttack(world, target)) {
            if (target instanceof LivingEntity && !this.isBaby()) {
                int i = 0;
                if (this.getWorld().getDifficulty() == Difficulty.NORMAL) i = 3;
                else if (this.getWorld().getDifficulty() == Difficulty.HARD) i = 6;
                if (i > 0) {
                    ((LivingEntity)target).addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, i * 10, 0), this);
                    ((LivingEntity)target).addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, i * 10, 0), this);
                }
                if(this.getWorld().getTime() % 5 == 0) {
                    if(this.getWorld().getBlockState(target.getBlockPos()) == Blocks.AIR.getDefaultState()) {
                        if (Blocks.COBWEB.getDefaultState().canPlaceAt(this.getWorld(), target.getBlockPos())) {
                            this.getWorld().setBlockState(target.getBlockPos(), Blocks.COBWEB.getDefaultState());
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isBaby() {
        return this.getDataTracker().get(BABY);
    }

    @Override
    public void setBaby(boolean baby) {
        this.getDataTracker().set(BABY, baby);
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        if (BABY.equals(data)) {this.calculateDimensions();}
        super.onTrackedDataSet(data);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound tag) {
        super.readCustomDataFromNbt(tag);
        this.setBaby(tag.getBoolean("IsBaby"));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound tag) {
        super.writeCustomDataToNbt(tag);
        tag.putBoolean("IsBaby", this.isBaby());
    }

    @Override
    public float getScaleFactor() {
        return this.isBaby() ? 0.85F : 1.3F;
    }

    @Override
    public void slowMovement(BlockState state, Vec3d multiplier) {
        if (!(state.getBlock() instanceof CobwebBlock)) {
            super.slowMovement(state, multiplier);
        }
    }
}