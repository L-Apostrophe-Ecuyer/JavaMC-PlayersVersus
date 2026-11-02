package frootloops.versus.mixin.mobs.hostile.end;

import frootloops.versus.mod.mobs.hostile.end.EndermanShulkerCarrier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A shulker's bullets pass through the enderman carrying it (EndermanShulkerCarrier): fired from within its arms, they'd
 * otherwise hit it as they left them, and an enderman hit by a projectile teleports away.
 */
@Mixin(Projectile.class)
public abstract class ShulkerBulletCarrierMixin {

    @Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
    private void playersVersus$passesItsCarrier(Entity target, CallbackInfoReturnable<Boolean> info) {
        if ((Object) this instanceof ShulkerBullet && EndermanShulkerCarrier.carriesShooter(target, ((Projectile) (Object) this).getOwner())) info.setReturnValue(false);
    }
}
