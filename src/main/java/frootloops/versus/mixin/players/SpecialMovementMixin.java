package frootloops.versus.mixin.players;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
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

    @Shadow public void incrementStat(Identifier stat) {}

    @Inject(method = "jump", at = @At(value = "HEAD"), cancellable = true)
    private void jump(CallbackInfo ci) {
        double velocityY = (double) this.getJumpVelocity() + this.getJumpBoostVelocityModifier();
        double velocityX = this.getVelocity().x;
        double velocityZ = this.getVelocity().z;
        double horizontalSpeedSquared = (velocityZ * velocityZ) + (velocityX * velocityX);
        boolean isSprinting = isSprinting();
        boolean hasBounded = false;
        double boundingStridesLevel = EnchantmentHelper.getLevel(BOUNDING_STRIDES, this.getEquippedStack(EquipmentSlot.FEET));
        boundingStridesLevel += EnchantmentHelper.getLevel(BOUNDING_STRIDES, this.getEquippedStack(EquipmentSlot.LEGS));

        // Sprint jump (regular):
        if (isSprinting) {
            float yawRads = this.getYaw() * ((float) Math.PI / 180);
            velocityX += -MathHelper.sin(yawRads) * 0.12; // Sprint jump speed reduced
            velocityZ += MathHelper.cos(yawRads) * 0.12;
        }

        // Dodging:
        else if(timeUntilRegen == 0 && horizontalSpeedSquared < 0.02d && horizontalSpeedSquared > 0.002d) {
            double dotProduct = this.getVelocity().dotProduct(this.getRotationVec(1.0F));
            boolean didSidewaysJump = dotProduct * dotProduct < 0.0008;
            if(didSidewaysJump) {
                timeUntilRegen = 12; // Invincible for two ticks
                velocityY += 0.02d;
                velocityX += velocityX * 0.4d;
                velocityZ += velocityZ * 0.4d;
            }
        }

        // Bounding strides:
        if(boundingStridesLevel > 0) {

            if (horizontalSpeedSquared < 0.005d) { // Extra High Jump:
                velocityY *= 1.3 + 0.125 * boundingStridesLevel;
                hasBounded = true;
            }
            else if (isSprinting) { // Extra Long Jump:
                velocityX *= (10.0 + boundingStridesLevel) / 8.5;
                velocityZ *= (10.0 + boundingStridesLevel) / 8.5;
                ((PlayerEntity) ((Object) this)).addExhaustion(0.2f);
                hasBounded = true;
            }

            // Bounding strides effect:
            if (hasBounded) {
                this.spawnSprintingParticles();
                this.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 8, 0, true, false));
                this.playBlockFallSound();
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
        }

        this.setVelocity(velocityX, velocityY, velocityZ);
        this.velocityDirty = true;
        this.incrementStat(Stats.JUMP);
        if (this.isSprinting()) ((PlayerEntity)((Object)this)).addExhaustion(0.15f);
        else ((PlayerEntity)((Object)this)).addExhaustion(0.075f);
        ci.cancel();
    }
}
