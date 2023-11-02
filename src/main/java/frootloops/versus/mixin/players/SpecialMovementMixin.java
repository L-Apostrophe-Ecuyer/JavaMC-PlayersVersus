package frootloops.versus.mixin.players;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static frootloops.versus.mod.Enchants.BOUNDING_STRIDES;


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

        int boundingStridesLevel = EnchantmentHelper.getLevel(BOUNDING_STRIDES, this.getEquippedStack(EquipmentSlot.FEET));
        boolean hasBounded = false;

        if (ticksLeftToDash > 0 && isSprinting() && boundingStridesLevel > 0) {
            velocityX *= 1.5 + 0.5 * boundingStridesLevel;
            velocityZ *= 1.5 + 0.5 * boundingStridesLevel;
            timeUntilRegen = 12; // Invincible for two ticks
            hungerManager.addExhaustion(0.5f); // Increases hunger by a lot
            ticksLeftToDash = -1;
            hasBounded = true;
        }

        if(ticksLeftToLeap > 0 && !this.isSneaking()) {
            velocityY *= 1.25 + 0.15 * boundingStridesLevel;
            this.playBlockFallSound();
            ticksLeftToLeap = -1;
            hasBounded = true;
        }

        if(boundingStridesLevel > 0 && hasBounded) {
            this.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 8, 0,true, false));
            this.world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_FALL, SoundCategory.PLAYERS);
            this.world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_DISPENSER_LAUNCH, SoundCategory.PLAYERS);
            for (int i = 0; i < 6; ++i) {
                double d = this.random.nextGaussian() * 0.02 - velocityX;
                double e = this.random.nextGaussian() * 0.02 + 0.01;
                double f = this.random.nextGaussian() * 0.02 - velocityZ;
                this.world.addParticle(ParticleTypes.POOF, this.getParticleX(1.0), this.getRandomBodyY(), this.getParticleZ(1.0), d, e, f);
            }
            this.spawnSprintingParticles();
            this.playBlockFallSound();
        }

        this.setVelocity(velocityX, velocityY, velocityZ);
        this.velocityDirty = true;
        ci.cancel();
    }
}
