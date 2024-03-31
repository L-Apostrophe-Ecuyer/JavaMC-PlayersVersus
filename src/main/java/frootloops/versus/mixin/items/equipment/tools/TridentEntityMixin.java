package frootloops.versus.mixin.items.equipment.tools;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(TridentEntity.class)
public abstract class TridentEntityMixin extends PersistentProjectileEntity {

    @Shadow
    private boolean dealtDamage;

    protected TridentEntityMixin(EntityType<? extends PersistentProjectileEntity> type, World world, ItemStack stack) {
        super(type, world, stack);
    }


    @ModifyVariable(method = "tick", at = @At("STORE"), ordinal = 0)
    private int alwaysLoyal(int loyaltyLevel) {
        return loyaltyLevel + 1;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = false)
    private void returnFromTheVoid(CallbackInfo ci) {
        if(this.getY() < this.getWorld().getBottomY()) dealtDamage = true;
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
