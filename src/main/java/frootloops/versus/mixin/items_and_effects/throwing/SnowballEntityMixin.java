package frootloops.versus.mixin.items_and_effects.throwing;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableBallProjectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Since 26.4 snowballs and ice balls share their hit (ThrowableBallProjectile), and vanilla knocks players back on
// its own, which this used to add. Ice balls freeze through vanilla's Freezing effect; snowballs chill what they hit.
@Mixin(ThrowableBallProjectile.class)
public abstract class SnowballEntityMixin extends ThrowableItemProjectile {
    public SnowballEntityMixin(EntityType<? extends ThrowableItemProjectile> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "onHitEntity", at = @At("TAIL"))
    protected void onEntityHit(EntityHitResult entityHitResult, CallbackInfo info) {
        if (!((Object) this instanceof Snowball)) return;
        Entity entity = entityHitResult.getEntity();
        if (entity.canFreeze()) {
            entity.setTicksFrozen(entity.getTicksFrozen() + 80);
        }
    }
}
