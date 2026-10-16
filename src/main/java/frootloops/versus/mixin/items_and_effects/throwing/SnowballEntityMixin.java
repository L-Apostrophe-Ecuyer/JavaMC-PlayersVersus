package frootloops.versus.mixin.items_and_effects.throwing;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Snowball.class)
public abstract class SnowballEntityMixin extends ThrowableItemProjectile {
    public SnowballEntityMixin(EntityType<? extends ThrowableItemProjectile> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "onHitEntity", at = @At("TAIL"))
    protected void onEntityHit(EntityHitResult entityHitResult, CallbackInfo info) {
        Entity entity = entityHitResult.getEntity();
        if (entity.canFreeze()) {
            entity.setTicksFrozen(entity.getTicksFrozen() + 80);
        }
        if (entity instanceof Player && !((Player) entity).getAbilities().invulnerable)
        {
            entity.setDeltaMovement(entity.getDeltaMovement().add(this.getDeltaMovement().normalize()));
            entity.syncVelocity = true;
        }
    }
}
