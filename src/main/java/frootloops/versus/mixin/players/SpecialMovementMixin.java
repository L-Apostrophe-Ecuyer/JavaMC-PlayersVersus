package frootloops.versus.mixin.players;

import frootloops.versus.VersusMod;
import net.minecraft.client.sound.Sound;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static frootloops.versus.util.Enchants.DASH_ENCHANTMENT;


@Mixin(PlayerEntity.class)
public abstract class SpecialMovementMixin extends LivingEntity {

    protected SpecialMovementMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

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

        if (ticksLeftToDash > 0 && isSprinting()) {
            this.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 8, 0,true, false));
            int dashEnchantmentLevel = EnchantmentHelper.getLevel(DASH_ENCHANTMENT, this.getEquippedStack(EquipmentSlot.LEGS));
            if(dashEnchantmentLevel > 0) {
                this.world.playSound(null, this.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_FALL, SoundCategory.PLAYERS);
                for (int i = 0; i < 6; ++i) {
                    double d = this.random.nextGaussian() * 0.02 - velocityX/2;
                    double e = this.random.nextGaussian() * 0.02 + 0.01;
                    double f = this.random.nextGaussian() * 0.02 - velocityZ/2;
                    this.world.addParticle(ParticleTypes.POOF, this.getParticleX(1.0), this.getRandomBodyY(), this.getParticleZ(1.0), d, e, f);
                }
            }
            velocityX *= 1.25 + 0.5f * dashEnchantmentLevel;
            velocityZ *= 1.25 + 0.5f * dashEnchantmentLevel;
            this.spawnSprintingParticles();
            this.playBlockFallSound();
            ticksLeftToDash = -1;
        }

        if(ticksLeftToLeap > 0 && !this.isSneaking()) {
            velocityY *= 1.35;
            this.playBlockFallSound();
            this.setSneaking(false);
            ticksLeftToLeap = -1;
        }

        this.setVelocity(velocityX, velocityY, velocityZ);
        this.velocityDirty = true;
        ci.cancel();
    }
}
