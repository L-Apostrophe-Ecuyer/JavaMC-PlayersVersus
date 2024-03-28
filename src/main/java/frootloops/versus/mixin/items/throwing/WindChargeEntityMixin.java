package frootloops.versus.mixin.items.throwing;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static frootloops.versus.backported.items.FutureItems.LAST_WIND_CHARGE_USE_TIME;
import static net.minecraft.entity.projectile.WindChargeEntity.EXPLOSION_BEHAVIOR;

@Mixin(WindChargeEntity.class)
public abstract class WindChargeEntityMixin extends ExplosiveProjectileEntity {

    protected WindChargeEntityMixin(EntityType<? extends ExplosiveProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "createExplosion", at = @At("HEAD"), cancellable = true)
    protected void afterExplosionCancelFallDamage(CallbackInfo info) {
        Entity owner = this.getOwner();
        if(owner == null || !(owner instanceof PlayerEntity) || this.squaredDistanceTo(owner) > 36.0d) {
            this.getWorld().createExplosion(this, null, EXPLOSION_BEHAVIOR, this.getX(), this.getY(), this.getZ(), 3.0F + this.random.nextFloat(), false, World.ExplosionSourceType.BLOW, ParticleTypes.GUST, ParticleTypes.GUST_EMITTER, SoundEvents.ENTITY_GENERIC_WIND_BURST);
            return;
        }

        double initialVelocity = owner.getVelocity().y;
        Explosion windBurst = this.getWorld().createExplosion(this, null, EXPLOSION_BEHAVIOR, this.getX(), this.getY(), this.getZ(), 2.0F, false, World.ExplosionSourceType.BLOW, ParticleTypes.GUST, ParticleTypes.GUST_EMITTER, SoundEvents.ENTITY_GENERIC_WIND_BURST);
        if(windBurst.getAffectedPlayers().containsKey(owner)) {

            // Clutching:
            owner.fallDistance = -1.0f;

            // Fall damage negation, when landing:
            if(owner.getVelocity().y > 0.1) {
                // double vy = windBurst.getAffectedPlayers().get(owner).y + initialVelocity;
                // int jumpHeightMax = 1 + (int) (vy / 0.08 - (vy * vy * vy) / 1.5);
                LAST_WIND_CHARGE_USE_TIME.put(owner.getUuid(), owner.getWorld().getTime());
            }
        }
        info.cancel();
    }


}
