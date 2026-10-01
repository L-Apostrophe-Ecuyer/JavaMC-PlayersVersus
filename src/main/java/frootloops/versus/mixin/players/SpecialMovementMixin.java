package frootloops.versus.mixin.players;

import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static frootloops.versus.mod.enchantments.CustomEnchants.BOUNDING_STRIDES;


@Mixin(Player.class)
public abstract class SpecialMovementMixin extends LivingEntity {

    protected SpecialMovementMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Shadow public void awardStat(Identifier stat) {}



    @Override
    public void jumpFromGround() {

        // Bounding strides:
        ItemStack leggings = this.getItemBySlot(EquipmentSlot.LEGS);
        if(leggings.isEmpty()) {
            super.jumpFromGround();
            return;
        }

        double boundingStridesLevel = EnchantRegistryHelper.getLevel(this.level(), leggings, BOUNDING_STRIDES);
        if(boundingStridesLevel <= 0) {
            super.jumpFromGround();
        }
        else {
            double velocityY = (double) this.getJumpPower() + this.getJumpBoostPower();
            double velocityX = this.getDeltaMovement().x;
            double velocityZ = this.getDeltaMovement().z;
            double horizontalSpeedSquared = (velocityZ * velocityZ) + (velocityX * velocityX);
            boolean isSprinting = isSprinting();

            // Sprint jump (regular):
            if (isSprinting) {
                float yawRads = this.getYRot() * ((float) Math.PI / 180);
                velocityX += -Mth.sin(yawRads) * 0.12; // Sprint jump speed reduced
                velocityZ += Mth.cos(yawRads) * 0.12;
            }

            boolean hasBounded = false;
            if (isSprinting) { // Extra Long Jump:
                if(this.horizontalCollision || horizontalSpeedSquared < 0.02) velocityY *= 1.3 + 0.125 * boundingStridesLevel;
                velocityX *= (10.0 + boundingStridesLevel) / 8.5;
                velocityZ *= (10.0 + boundingStridesLevel) / 8.5;
                ((Player) ((Object) this)).causeFoodExhaustion(0.2f);
                hasBounded = true;
            }

            // Bounding strides effect:
            if (hasBounded) {
                this.setDeltaMovement(velocityX, velocityY, velocityZ);
                this.needsSync = true;
                this.awardStat(Stats.JUMP);
                if (this.isSprinting()) ((Player)((Object)this)).causeFoodExhaustion(0.15f);
                else ((Player)((Object)this)).causeFoodExhaustion(0.075f);

                this.spawnSprintParticle();
                this.addEffect(new MobEffectInstance(MobEffects.SPEED, 8, 0, true, false));
                this.playBlockFallSound();
                this.playSound(SoundEvents.AMETHYST_BLOCK_FALL, 1.0f, 1.0f);
                this.playSound(SoundEvents.DISPENSER_LAUNCH, 1.0f, 1.0f);
                for (int i = 0; i < 6; ++i) {
                    double d = this.random.nextGaussian() * 0.02 - velocityX;
                    double e = this.random.nextGaussian() * 0.02 + 0.01;
                    double f = this.random.nextGaussian() * 0.02 - velocityZ;
                    this.level().addParticle(ParticleTypes.POOF, this.getRandomX(1.0), this.getRandomY(), this.getRandomZ(1.0), d, e, f);
                }
                this.spawnSprintParticle();
                this.playBlockFallSound();
            }
        }
    }
}
