package frootloops.versus.mixin.players;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.Combat;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static frootloops.versus.mod.enchantments.Enchants.BOUNDING_STRIDES;


@Mixin(PlayerEntity.class)
public abstract class SpecialMovementMixin extends LivingEntity {

    protected SpecialMovementMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow
    protected HungerManager hungerManager;

    private int ticksLeftToLeap = 0;
    private int ticksLeftToDash = 0;

    @Inject(method = "tick", at = @At(value = "TAIL"))
    private void tickMovement(CallbackInfo ci) {
        if(!isCrawling() && !isSwimming()) {

            if(isSneaking()) ticksLeftToLeap = 8;
            else if(ticksLeftToLeap > 0) ticksLeftToLeap--;

            if(!isSprinting()) ticksLeftToDash = 8;
            else if(ticksLeftToDash > 0) ticksLeftToDash--;
        }
    }

    @Inject(method = "jump", at = @At(value = "HEAD"), cancellable = true)
    private void jump(CallbackInfo ci) {
        double velocityY = (double) this.getJumpVelocity() + this.getJumpBoostVelocityModifier();
        double velocityX = this.getVelocity().x;
        double velocityZ = this.getVelocity().z;

        if (isSprinting()) {
            float yaw = this.getYaw() * ((float) Math.PI / 180);
            float horizontalVelocity = 0.2f;
            velocityX += -MathHelper.sin(yaw) * horizontalVelocity;
            velocityZ += MathHelper.cos(yaw) * horizontalVelocity;
        }

        // Bounding strides:
        int boundingStridesLevel = EnchantmentHelper.getLevel(BOUNDING_STRIDES, this.getEquippedStack(EquipmentSlot.FEET));
        boundingStridesLevel += EnchantmentHelper.getLevel(BOUNDING_STRIDES, this.getEquippedStack(EquipmentSlot.LEGS));
        boolean hasBounded = false;

        // Sprint-jump:
        if (ticksLeftToDash > 0 && boundingStridesLevel > 0 && isSprinting()) {
            velocityX *= 1.5 + velocityX * boundingStridesLevel;
            velocityZ *= 1.5 + velocityZ * boundingStridesLevel;
            timeUntilRegen = 12; // Invincible for two ticks
            hungerManager.addExhaustion(0.5f); // Increases hunger by a lot
            ticksLeftToDash = -1;
            hasBounded = true;
        }

        // Dodging:
        if(velocityX != 0d && velocityZ != 0d && this.isOnGround() && !this.isSprinting() && !this.isSneaking()) {
            double dotProduct = this.getVelocity().dotProduct(this.getRotationVector());
            boolean isPlayerDodging = (dotProduct * dotProduct) < 0.01;
            if (isPlayerDodging) {
                double horizontalVelocityTotal = Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
                double horizontalDodgeVelocity = 0.45 + 0.1 * boundingStridesLevel;

                velocityX = (velocityX / horizontalVelocityTotal) * horizontalDodgeVelocity;
                velocityZ = (velocityZ / horizontalVelocityTotal) * horizontalDodgeVelocity;
                velocityY *= 0.75;

                hungerManager.addExhaustion(0.5f);
                this.spawnSprintingParticles();
                this.playBlockFallSound();
                ticksLeftToLeap = -1;
                ticksLeftToDash = -1;
                hasBounded = true;
            }
        }

        // Crouch-jump:
        if(ticksLeftToLeap > 0 && !this.isSneaking()) {
            velocityY *= 1.25 + 0.15 * boundingStridesLevel;
            this.playBlockFallSound();
            ticksLeftToLeap = -1;
            hasBounded = true;
        }

        // Bounding strides effect:
        if(boundingStridesLevel > 0 && hasBounded) {
            this.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 8, 0,true, false));
            this.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_FALL, 1.0f, 1.0f);
            this.playSound(SoundEvents.BLOCK_DISPENSER_LAUNCH, 1.0f, 1.0f);
            for (int i = 0; i < 6; ++i) {
                double d = this.random.nextGaussian() * 0.02 - velocityX;
                double e = this.random.nextGaussian() * 0.02 + 0.01;
                double f = this.random.nextGaussian() * 0.02 - velocityZ;
                this.getWorld().addParticle(ParticleTypes.POOF, this.getParticleX(1.0), this.getRandomBodyY(), this.getParticleZ(1.0), d, e, f);
            }
            this.spawnSprintingParticles();
            this.playBlockFallSound();
        }

        this.setVelocity(velocityX, velocityY, velocityZ);
        this.velocityDirty = true;
        ci.cancel();
    }
}
