package frootloops.versus.mixin.mobs.hostile.overworld;

import frootloops.versus.mod.Combat;
import frootloops.versus.mod.mobs.hostile.ai.PhantomMoveControlRevamp;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.FlyingEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;

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
            int moonPhase = world.getMoonPhase();
            if((moonPhase + 2) % 8 < 6) return false;
            if(moonPhase == 7 && world.getRandom().nextFloat() > 0.2f) return false;
        }
        return super.canSpawn(world, spawnReason);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        this.moveControl = new PhantomMoveControlRevamp((PhantomEntity) ((Object)this));
        this.circlingCenter = this.getBlockPos().up(16);

        EntityAttributeInstance instanceHp = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (instanceHp != null) {
            instanceHp.setBaseValue(12.0D);
            this.setHealth(12.0f);
        }
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
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
