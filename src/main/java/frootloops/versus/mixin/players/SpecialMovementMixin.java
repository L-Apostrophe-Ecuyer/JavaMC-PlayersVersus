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
        if(velocityX != 0d && velocityZ != 0d && this.isOnGround() && !this.isSprinting()) {
            double sideStepAmount = (this.prevBodyYaw -  this.getHeadYaw());
            boolean isPlayerDodging = (sideStepAmount * sideStepAmount) > 1600d;
            VersusMod.MOD_LOGGER.warn("Side-step amount squared: " + (sideStepAmount * sideStepAmount));
            if(isPlayerDodging) {
                velocityX *= 3.5 + velocityX * 1.5 * boundingStridesLevel;
                velocityZ *= 3.5 + velocityZ * 1.5 * boundingStridesLevel;
                velocityX = MathHelper.clamp(velocityX, -0.36 - 0.1 * boundingStridesLevel, 0.36 + 0.1 * boundingStridesLevel);
                velocityZ = MathHelper.clamp(velocityZ, -0.36 - 0.1 * boundingStridesLevel, 0.36 + 0.1 * boundingStridesLevel);
                velocityY *= 0.85;
                this.playBlockFallSound();
                ticksLeftToLeap = -1;
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
            this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_FALL, SoundCategory.PLAYERS);
            this.getWorld().playSound(null, this.getBlockPos(), SoundEvents.BLOCK_DISPENSER_LAUNCH, SoundCategory.PLAYERS);
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
