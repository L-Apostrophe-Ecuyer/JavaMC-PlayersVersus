package frootloops.versus.mixin.mobs.hostile.overworld;

import frootloops.versus.mod.Combat;
import frootloops.versus.mod.mobs.hostile.overworld.PhantomMoveControlRevamp;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Phantom.class)
public abstract class PhantomMixin extends Mob {

    @Shadow
    BlockPos anchorPoint = BlockPos.ZERO;

    protected PhantomMixin(EntityType<? extends Mob> entityType, Level world) {
        super(entityType, world);
    }


    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        boolean wasDamaged = super.hurtServer(world, source, amount);
        if(!source.is(DamageTypes.MOB_PROJECTILE) && source.getEntity() != null && this.getTarget() != null && !Combat.isLookingTowards(this, this.getTarget().position(), true)) {
            // Keep attacking, instead of flying back up, to give a chance to melee attackers:
            this.hurtTime = 0;
            for(WrappedGoal goal : this.goalSelector.getAvailableGoals()) {
                if(goal.getPriority() == 1) {
                    goal.start();
                    return wasDamaged;
                }
            }
        }
        return wasDamaged;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor world, EntitySpawnReason spawnReason) {
        if(spawnReason == EntitySpawnReason.NATURAL) {

            long dayTime = world.dayTime() % 24000l;
            if(dayTime < 18000l || dayTime > 20000l) return false;

            int moonPhase = world.getMoonPhase();
            if((moonPhase + 2) % 8 < 6) return false;
            if(moonPhase == 7 && world.getRandom().nextFloat() > 0.2f) return false;

        }
        return super.checkSpawnRules(world, spawnReason);
    }

    @Override
    public boolean isInvisibleTo(Player player) {
        return super.isInvisible() ? super.isInvisibleTo(player) : false;
    }

    @Override
    public boolean isInvisible() {
        return this.hurtTime == 0;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData entityData) {
        this.moveControl = new PhantomMoveControlRevamp((Phantom) ((Object)this));
        this.anchorPoint = this.blockPosition().above(16);
        this.noPhysics = true;

        AttributeInstance instanceHp = this.getAttributes().getInstance(Attributes.MAX_HEALTH);
        if (instanceHp != null) {
            instanceHp.setBaseValue(10.0D);
            this.setHealth(this.getMaxHealth());
        }

        AttributeInstance instanceDmg = this.getAttributes().getInstance(Attributes.ATTACK_DAMAGE);
        if (instanceDmg != null) {
            instanceDmg.setBaseValue(4.0D);
        }

        return super.finalizeSpawn(world, difficulty, spawnReason, entityData);
    }

    @Override
    public void move(MoverType movementType, Vec3 movement) {
        boolean isInsideBlock = false;
        if(this.noPhysics) {
            float width = this.getBbWidth() * 0.8f;
            AABB box = AABB.ofSize(this.getEyePosition(), width, 1.0E-6, width);
            isInsideBlock = BlockPos.betweenClosedStream(box).anyMatch(pos -> {
                BlockState blockState = this.level().getBlockState((BlockPos)pos);
                return !blockState.isAir() && Shapes.joinIsNotEmpty(blockState.getCollisionShape(this.level(), (BlockPos)pos).move(pos.getX(), pos.getY(), pos.getZ()), Shapes.create(box), BooleanOp.AND);
            });
        }
        this.noPhysics = (this.getXRot() > 1.0f || movement.y > 0.1d || isInsideBlock || movement.distanceToSqr(0d,0d,0d) < 0.01d);
        if(this.horizontalCollision) {
            movement = movement.scale(-1.0d);
            this.setYRot(-this.getViewYRot(1f));
            this.horizontalCollision = false;
        }
        if(this.verticalCollision) {
            movement = movement.add(0d, 0.3d, 0d);
            this.setXRot(60.0f);
            this.verticalCollision = false;
        }
        super.move(movementType,movement);
        if(this.isInWater() && !this.level().isClientSide()) this.hurtServer((ServerLevel) this.level(), damageSources().drown(), 2.0F);
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (!this.isSilent()) {
            volume = 0.8f;
            pitch /= 4.0f;
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), sound, this.getSoundSource(), volume, pitch);
        }
    }
}
