package frootloops.versus.mixin.players;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.enchantments.Enchants;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
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

import static frootloops.versus.VersusMod.DEBUG_MODE;
import static frootloops.versus.mod.enchantments.Enchants.BOUNDING_STRIDES;


@Mixin(PlayerEntity.class)
public abstract class SpecialMovementMixin extends LivingEntity {

    protected SpecialMovementMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow public void incrementStat(Identifier stat) {}

    @Inject(method = "jump", at = @At(value = "HEAD"), cancellable = true)
    private void jump(CallbackInfo info) {

        // Bounding strides:
        ItemStack leggings = this.getEquippedStack(EquipmentSlot.LEGS);
        if(leggings.isEmpty() || !leggings.hasEnchantments()) return;

        double boundingStridesLevel = Enchants.getLevel(getWorld(), leggings, BOUNDING_STRIDES);
        if(boundingStridesLevel > 0) {

            double velocityY = (double) this.getJumpVelocity() + this.getJumpBoostVelocityModifier();
            double velocityX = this.getVelocity().x;
            double velocityZ = this.getVelocity().z;
            double horizontalSpeedSquared = (velocityZ * velocityZ) + (velocityX * velocityX);
            boolean isSprinting = isSprinting();

            // Sprint jump (regular):
            if (isSprinting) {
                float yawRads = this.getYaw() * ((float) Math.PI / 180);
                velocityX += -MathHelper.sin(yawRads) * 0.12; // Sprint jump speed reduced
                velocityZ += MathHelper.cos(yawRads) * 0.12;
            }

            boolean hasBounded = false;
            if (isSprinting) { // Extra Long Jump:
                if(this.horizontalCollision || horizontalSpeedSquared < 0.02) velocityY *= 1.3 + 0.125 * boundingStridesLevel;
                velocityX *= (10.0 + boundingStridesLevel) / 8.5;
                velocityZ *= (10.0 + boundingStridesLevel) / 8.5;
                ((PlayerEntity) ((Object) this)).addExhaustion(0.2f);
                hasBounded = true;
            }

            // Bounding strides effect:
            if (hasBounded) {
                this.setVelocity(velocityX, velocityY, velocityZ);
                this.velocityDirty = true;
                this.incrementStat(Stats.JUMP);
                if (this.isSprinting()) ((PlayerEntity)((Object)this)).addExhaustion(0.15f);
                else ((PlayerEntity)((Object)this)).addExhaustion(0.075f);

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
                info.cancel();
            }
        }
    }
}
