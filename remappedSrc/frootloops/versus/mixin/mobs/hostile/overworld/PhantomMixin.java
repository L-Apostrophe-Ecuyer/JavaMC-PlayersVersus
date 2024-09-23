package frootloops.versus.mixin.mobs.hostile.overworld;

import EntityData;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.mobs.hostile.ai.PhantomMoveControlRevamp;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.FlyingEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PhantomEntity.class)
public abstract class PhantomMixin extends FlyingEntity {

    @Shadow
    BlockPos circlingCenter = BlockPos.ORIGIN;

    protected PhantomMixin(EntityType<? extends FlyingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean wasDamaged = super.damage(source, amount);
        if(!source.isOf(DamageTypes.MOB_PROJECTILE) && source.getAttacker() != null && this.getTarget() != null && !Combat.isLookingTowards(this, this.getTarget().getPos(), true)) {
            // Keep attacking, instead of flying back up, to give a chance to melee attackers:
            this.hurtTime = 0;
            for(PrioritizedGoal goal : this.goalSelector.getGoals()) {
                if(goal.getPriority() == 1) {
                    goal.start();
                    return wasDamaged;
                }
            }
        }
        return wasDamaged;
    }

    @Override
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        if(spawnReason == SpawnReason.NATURAL) {

            long dayTime = world.getLunarTime() % 24000l;
            if(dayTime < 18000l || dayTime > 20000l) return false;

            int moonPhase = world.getMoonPhase();
            if((moonPhase + 2) % 8 < 6) return false;
            if(moonPhase == 7 && world.getRandom().nextFloat() > 0.2f) return false;

        }
        return super.canSpawn(world, spawnReason);
    }

    @Override
    public boolean isInvisibleTo(PlayerEntity player) {
        return super.isInvisible() ? super.isInvisibleTo(player) : false;
    }

    @Override
    public boolean isInvisible() {
        return this.hurtTime == 0;
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
        this.moveControl = new PhantomMoveControlRevamp((PhantomEntity) ((Object)this));
        this.circlingCenter = this.getBlockPos().up(16);
        this.noClip = true;

        EntityAttributeInstance instanceHp = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (instanceHp != null) {
            instanceHp.setBaseValue(10.0D);
            this.setHealth(this.getMaxHealth());
        }

        EntityAttributeInstance instanceDmg = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (instanceDmg != null) {
            instanceDmg.setBaseValue(4.0D);
        }

        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    @Override
    public void move(MovementType movementType, Vec3d movement) {
        boolean isInsideBlock = false;
        if(this.noClip) {
            float width = this.getWidth() * 0.8f;
            Box box = Box.of(this.getEyePos(), width, 1.0E-6, width);
            isInsideBlock = BlockPos.stream(box).anyMatch(pos -> {
                BlockState blockState = this.getWorld().getBlockState((BlockPos)pos);
                return !blockState.isAir() && VoxelShapes.matchesAnywhere(blockState.getCollisionShape(this.getWorld(), (BlockPos)pos).offset(pos.getX(), pos.getY(), pos.getZ()), VoxelShapes.cuboid(box), BooleanBiFunction.AND);
            });
        }
        this.noClip = (this.getPitch() > 1.0f || movement.y > 0.1d || isInsideBlock || movement.squaredDistanceTo(0d,0d,0d) < 0.01d);
        if(this.horizontalCollision) {
            movement = movement.multiply(-1.0d);
            this.setYaw(-this.getYaw(1f));
            this.horizontalCollision = false;
        }
        if(this.verticalCollision) {
            movement = movement.add(0d, 0.3d, 0d);
            this.setPitch(60.0f);
            this.verticalCollision = false;
        }
        super.move(movementType,movement);
        if(this.isTouchingWater()) this.damage(getDamageSources().drown(), 2.0F);
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (!this.isSilent()) {
            volume = 0.8f;
            pitch /= 4.0f;
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), sound, this.getSoundCategory(), volume, pitch);
        }
    }
}
