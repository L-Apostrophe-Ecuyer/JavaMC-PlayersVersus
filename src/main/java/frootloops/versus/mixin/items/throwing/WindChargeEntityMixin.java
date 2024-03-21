package frootloops.versus.mixin.items.throwing;

import frootloops.versus.VersusMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WindChargeEntity.class)
public abstract class WindChargeEntityMixin extends ExplosiveProjectileEntity {

    protected WindChargeEntityMixin(EntityType<? extends ExplosiveProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "createExplosion", at = @At("TAIL"))
    protected void afterExplosionCancelFallDamage(CallbackInfo info) {
        Entity owner = this.getOwner();
        if(owner != null && owner.getVelocity().y > 0.0) {
            owner.fallDistance = -1.0f - (float)owner.getVelocity().y * 6.0f;
        }
    }
}
