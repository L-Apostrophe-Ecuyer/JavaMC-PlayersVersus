package frootloops.versus.mixin.items.trident;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(TridentEntity.class)
public abstract class TridentEntityMixin extends PersistentProjectileEntity {

    @Shadow
    private boolean dealtDamage;

    protected TridentEntityMixin(EntityType<? extends PersistentProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @ModifyVariable(method = "tick", at = @At("STORE"), ordinal = 0)
    private int alwaysLoyal(int loyaltyLevel) {
        return loyaltyLevel + 1;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = false)
    private void returnFromTheVoid(CallbackInfo ci) {
        if(this.getY() < this.world.getBottomY()) dealtDamage = true;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = false)
    private void sameBehaviorForDrowned(CallbackInfo ci) {
        if(this.getOwner() instanceof DrownedEntity drowned && this.getOwner().isAlive()) {
            if(!this.noClip) {
                drowned.getMainHandStack().setCount(0);
            }
            else if(this.squaredDistanceTo(drowned) < 9.0d) {
                drowned.equipStack(EquipmentSlot.MAINHAND, this.asItemStack());
                this.discard();
            }
        }
    }
}
