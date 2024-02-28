package frootloops.versus.mixin.mobs.hostile.end;

import frootloops.versus.mod.Combat;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndermanEntity.class)
public abstract class EndermanMixin extends HostileEntity implements Angerable {

    @Shadow public boolean teleportTo(Entity entity) {return false;}

    protected EndermanMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityTag) {
        EntityAttributeInstance instanceKnockback = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (instanceKnockback != null) instanceKnockback.setBaseValue(0.6D);
        return super.initialize(world, difficulty, spawnReason, entityData, entityTag);
    }

    @Override
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        if(!super.canSpawn(world, spawnReason)) return false;
        if(spawnReason == SpawnReason.NATURAL && world.getDimension().hasSkyLight()) {
            int moonPhase = world.getMoonPhase();
            if((moonPhase + 2) % 8 < 6 && this.random.nextInt(4) < 1) return false; // Fewer endermen when not nearing new moons
        }
        return true;
    }

    @Override
    protected int computeFallDamage(float fallDistance, float damageMultiplier) {
        return super.computeFallDamage(fallDistance - 4.0f, damageMultiplier) - 5;
    }

    @Overwrite
    public boolean isPlayerStaring(PlayerEntity player) {
        if (player.getEquippedStack(EquipmentSlot.HEAD).isOf(Blocks.CARVED_PUMPKIN.asItem())) return false;
        double squaredDistance = this.squaredDistanceTo(player);

        // Targetted players will be attacked if returns false. We invert to make it so endermen only attack when you're looking, making chases more panicky.
        if(this.getTarget() == player) {
            if(this.getAngerTime() < 10) return false;
            if(this.hurtTime > 0 && this.lastDamageTaken > 5.0f) return true;
            if(squaredDistance > 16.0) return false;
            else return !(Combat.isLookingTowards(player, this.getEyePos(), 0.4));
        }
        // Untargetted players, Endermen will teleport up to them until they're in range for aggro:
        else {
            if (player.prevHeadYaw != player.headYaw) return false;
            else if(squaredDistance > 2304.0) return false;
            else if(!Combat.isLookingTowards((LivingEntity)((Object)this), player.getPos())) return false;

            Vec3d playerRotationVect = player.getRotationVec(1.0f).normalize();
            Vec3d directionVect = new Vec3d(this.getX() - player.getX(), this.getEyeY() - player.getEyeY(), this.getZ() - player.getZ());
            double distance = directionVect.length();
            double dotProduct = (playerRotationVect).dotProduct(directionVect.normalize());
            if (dotProduct > 1.0 - 0.035 / distance) {
                if (squaredDistance > 64.0f) {
                    if(this.age % 20 < 10) teleportTo(player);
                    this.playAmbientSound();
                    return false;
                }
                else return player.canSee(this);
            }
        }
        return false;
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (!this.isSilent()) {
            volume *= 0.8f;
            pitch *= 0.75f;
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(), sound, this.getSoundCategory(), volume, pitch);
        }
    }

    @Inject(method = "setTarget", at = @At("TAIL"))
    private void darknessWhenAngered(@Nullable LivingEntity target, CallbackInfo ci) {
        if(target != null) target.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 90, 0, false, false));
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
    public int getMinAmbientSoundDelay() {
        return 200;
    }
}