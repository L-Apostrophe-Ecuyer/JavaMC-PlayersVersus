package frootloops.versus.mixin.mobs.hostile.end;

import frootloops.versus.mod.Combat;
import frootloops.versus.mod.mobs.hostile.end.EndermanHideAndWaitGoal;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
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

    private int angerTime = 0;
    protected EndermanMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow
    private boolean teleportTo(double x, double y, double z) {return false;}

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData) {
        EntityAttributeInstance instanceKnockback = this.getAttributes().getCustomInstance(EntityAttributes.KNOCKBACK_RESISTANCE);
        if (instanceKnockback != null) instanceKnockback.setBaseValue(0.6D);
        return super.initialize(world, difficulty, spawnReason, entityData);
    }

    @Inject(method = "initGoals", at = @At("HEAD"))
    private void addWaitForPlayerGoal(CallbackInfo ci) {
        this.goalSelector.add(0, new EndermanHideAndWaitGoal((EndermanEntity) ((Object)this)));
    }

    @Override
    public boolean canSpawn(WorldAccess worldAccess, SpawnReason spawnReason) {
        if(!super.canSpawn(worldAccess, spawnReason)) return false;
        if(spawnReason == SpawnReason.NATURAL && worldAccess.getDimension().hasSkyLight()) {
            if(worldAccess instanceof World world && world.isRaining() && !world.isThundering()) { // Endermen more common when foggy or during new moons
                return true;
            }
            if((worldAccess.getMoonPhase() + 2) % 8 < 6 && this.random.nextInt(4) < 1) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected int computeFallDamage(double fallDistance, float damagePerDistance) {
        return super.computeFallDamage(fallDistance - 4.0f, damagePerDistance) - 5;
    }


    @Overwrite
    public boolean isPlayerStaring(PlayerEntity player) {
        if (player.getEquippedStack(EquipmentSlot.HEAD).isOf(Blocks.CARVED_PUMPKIN.asItem())) return false;
        double squaredDistance = this.squaredDistanceTo(player);

        // Targeted players will be attacked if returns false. We make it so endermen only attack when you're looking, making chases more panicky.
        if(this.getTarget() == player) {
            if(this.getAngerTime() < 10) return false;
            if(this.hurtTime > 0 && this.lastDamageTaken > 5.0f) return true;
            if(squaredDistance > 64.0) return false;
            else if(squaredDistance > 16.0) return !(Combat.isLookingTowards(player, this.getEyePos(), -0.3));
            else return false;
        }
        // Untargeted players, Endermen will teleport up to them until they're in range for aggro:
        else {
            if (player.lastHeadYaw != player.headYaw) return false;
            else if(squaredDistance > 8192.0) return false;

            Vec3d playerRotationVect = player.getRotationVec(1.0f).normalize();
            Vec3d directionVect = new Vec3d(this.getX() - player.getX(), this.getBodyY(0.6) - player.getEyeY(), this.getZ() - player.getZ());
            double distance = directionVect.length();
            double dotProduct = (playerRotationVect).dotProduct(directionVect.multiply(1.0/distance));
            double dotProductThreshold = distance < 10.0 ? 0.95 : 1.0 - 0.05 / (distance - 9.0);
            if (dotProduct > dotProductThreshold) {
                if (squaredDistance > 256.0 && player.canSee(this)) {
                    angerTime = 0;
                    Vec3d target = this.getEntityPos().add(directionVect.multiply(0.5));
                    teleportTo(target.x + (this.random.nextDouble() - 0.5) * 4.0, target.y + (double) this.random.nextInt(16) - 8.0, target.z + (this.random.nextDouble() - 0.5) * 4.0);
                    this.lookAtEntity(player, 100f, 100f);
                    this.playAmbientSound();
                    return false;
                }
                else if(!player.canSee(this)) {
                    angerTime = Math.max(0, angerTime - 8);
                }
                else {
                    angerTime += distance < 8.0 ? 16 : 8;
                    if(angerTime > 64) {
                        return true;
                    }
                }
            }
        }
        return false;
    }



    boolean teleportToEntity(Entity entity, double distance) {
        Vec3d direction = new Vec3d(this.getX() - entity.getX(), this.getBodyY(0.5) - entity.getEyeY(), this.getZ() - entity.getZ());
        direction = direction.normalize();
        double e = this.getX() + (this.random.nextDouble() - 0.5) * 4.0 - direction.x * distance;
        double f = this.getY() + (double)(this.random.nextInt(16) - 8) - direction.y * distance;
        double g = this.getZ() + (this.random.nextDouble() - 0.5) * 4.0 - direction.z * distance;
        return this.teleportTo(e, f, g);
    }

    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        if (!this.isSilent()) {
            volume *= 0.8f;
            pitch *= 0.75f;
            this.getEntityWorld().playSound(null, this.getX(), this.getY(), this.getZ(), sound, this.getSoundCategory(), volume, pitch);
        }
    }

    @Inject(method = "setTarget", at = @At("TAIL"))
    private void darknessWhenAngered(@Nullable LivingEntity target, CallbackInfo ci) {
        if(target != null) target.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 90, 0, false, false));
    }

    @Override
    public int getMinAmbientSoundDelay() {
        return 300;
    }
}
