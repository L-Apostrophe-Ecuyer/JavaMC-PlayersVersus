package frootloops.versus.mixin.hostile_mobs;

import net.minecraft.block.BlockState;
import net.minecraft.block.CobwebBlock;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
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
        if(this.getBlockPos().getY() > 64 && !world.getBiome(this.getBlockPos()).isIn(BiomeTags.SPAWNS_WARM_VARIANT_FROGS)) return false;
        return super.canSpawn(world, spawnReason);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityTag) {

        EntityAttributeInstance instanceMvt = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (instanceMvt != null) instanceMvt.setBaseValue(0.28D);

        EntityAttributeInstance instanceDmg = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (instanceDmg != null) instanceDmg.setBaseValue(7.0D);

        EntityAttributeInstance instanceHP = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (instanceHP != null) {
            instanceHP.setBaseValue(36.0D);
            this.setHealth(this.getMaxHealth());
        }

        if(this.random.nextFloat() < 0.7F) this.setBaby(true);
        this.getNavigation().setCanSwim(true);
        return super.initialize(world, difficulty, spawnReason, entityData, entityTag);
    }


    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void addBabyData(CallbackInfo ci) {
        this.getDataTracker().startTracking(BABY, false);
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
                    "Baby spawn malus", -30.0D, EntityAttributeModifier.Operation.ADDITION));

            Objects.requireNonNull(speed).addPersistentModifier(new EntityAttributeModifier(
                    "Baby spawn malus", +0.08D, EntityAttributeModifier.Operation.ADDITION));
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