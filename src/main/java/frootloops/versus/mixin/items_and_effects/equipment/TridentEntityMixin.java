package frootloops.versus.mixin.items_and_effects.equipment;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ThrownTrident.class)
public abstract class TridentEntityMixin extends AbstractArrow {

    @Shadow
    private boolean dealtDamage;

    protected TridentEntityMixin(EntityType<? extends AbstractArrow> entityType, Level world) {
        super(entityType, world);
    }


    @ModifyVariable(method = "tick", at = @At("STORE"), ordinal = 0)
    private int alwaysLoyal(int loyaltyLevel) {
        return loyaltyLevel + 1;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = false)
    private void returnFromTheVoid(CallbackInfo ci) {
        if(this.getY() < this.level().getMinY()) dealtDamage = true;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = false)
    private void sameBehaviorForDrowned(CallbackInfo ci) {
        if(this.getOwner() instanceof Drowned drowned && this.getOwner().isAlive()) {
            if(!this.noPhysics) {
                drowned.getMainHandItem().setCount(0);
            }
            else if(this.distanceToSqr(drowned) < 9.0d) {
                drowned.setItemSlot(EquipmentSlot.MAINHAND, this.getPickupItem());
                this.discard();
            }
        }
    }
}
