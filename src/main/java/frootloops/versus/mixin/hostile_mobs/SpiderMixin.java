package frootloops.versus.mixin.hostile_mobs;

import net.minecraft.block.AirBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CobwebBlock;
import net.minecraft.data.client.BlockStateSupplier;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BiomeTags;
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

    protected SpiderMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        int y = this.getBlockPos().getY();
        int ySpawnBonus = world.getBiome(this.getBlockPos()).isIn(BiomeTags.SPAWNS_WARM_VARIANT_FROGS) ? 32 : 0;
        if(y > 84 + ySpawnBonus) return false;
        if(y > 56 + ySpawnBonus && world.getLightLevel(LightType.SKY, this.getBlockPos()) > 1) return false;
        return super.canSpawn(world, spawnReason);
    }

    @Override
    protected int computeFallDamage(float fallDistance, float damageMultiplier) {
        return super.computeFallDamage(fallDistance, damageMultiplier) - 10;
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        super.dropLoot(source, causedByPlayer);
        if(!this.isBaby()) {
            super.dropLoot(source, causedByPlayer); // Triple loot for the big boys!
            super.dropLoot(source, causedByPlayer);
        }
    }

    @Override
    public int getXpToDrop() {
        if (!this.isBaby()) this.experiencePoints = 17;
        return super.getXpToDrop();
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityTag) {

        EntityAttributeInstance instanceMvt = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (instanceMvt != null) instanceMvt.setBaseValue(0.3D);

        EntityAttributeInstance instanceDmg = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (instanceDmg != null) instanceDmg.setBaseValue(7.0D);

        EntityAttributeInstance instanceHP = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (instanceHP != null) {
            instanceHP.setBaseValue(50.0f);
            this.setHealth(50.0f);
        }

        if(this.random.nextFloat() < 0.85F) this.setBaby(true);
        this.getNavigation().setCanSwim(true);
        return super.initialize(world, difficulty, spawnReason, entityData, entityTag);
    }


    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void addBabyData(CallbackInfo ci) {
        this.getDataTracker().startTracking(BABY, false);
    }

    @Override
    public boolean tryAttack(Entity target) {
        if (super.tryAttack(target)) {
            if (target instanceof LivingEntity && !this.isBaby()) {
                int i = 0;
                if (this.world.getDifficulty() == Difficulty.NORMAL) i = 3;
                else if (this.world.getDifficulty() == Difficulty.HARD) i = 6;
                if (i > 0) {
                    ((LivingEntity)target).addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, i * 10, 0), this);
                    ((LivingEntity)target).addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, i * 10, 0), this);
                }
                if(world.getTime() % 5 == 0) {
                    if(world.getBlockState(target.getBlockPos()) == Blocks.AIR.getDefaultState()) {
                        if (Blocks.COBWEB.getDefaultState().canPlaceAt(world, target.getBlockPos())) {
                            world.setBlockState(target.getBlockPos(), Blocks.COBWEB.getDefaultState());
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
        if (baby) {
            EntityAttributeInstance attackDamage = this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            EntityAttributeInstance maxHealth = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
            EntityAttributeInstance speed = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);

            Objects.requireNonNull(attackDamage).addPersistentModifier(new EntityAttributeModifier(
                    "Baby spawn malus", -4.0D, EntityAttributeModifier.Operation.ADDITION));

            Objects.requireNonNull(maxHealth).addPersistentModifier(new EntityAttributeModifier(
                    "Baby spawn malus", -40.0D, EntityAttributeModifier.Operation.ADDITION));

            Objects.requireNonNull(speed).addPersistentModifier(new EntityAttributeModifier(
                    "Baby spawn malus", +0.06D, EntityAttributeModifier.Operation.ADDITION));

            this.setHealth(10.0f);
        }
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        if (BABY.equals(data)) {
            this.calculateDimensions();
        }

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
    public float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 0.65F * this.getScaleFactor();
    }

    @Override
    public void slowMovement(BlockState state, Vec3d multiplier) {
        if (!(state.getBlock() instanceof CobwebBlock)) {
            super.slowMovement(state, multiplier);
        }
    }
}