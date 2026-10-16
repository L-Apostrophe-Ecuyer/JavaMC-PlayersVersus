package frootloops.versus.mixin.items_and_effects.equipment;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.world.entity.LivingEntity.canGlideUsing;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


@Mixin(LivingEntity.class)
public abstract class ElytraGlidingMixin extends Entity {
    public ElytraGlidingMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Shadow private int lastHurtByMobTimestamp;
    @Shadow	public ItemStack getItemBySlot(EquipmentSlot slot) {
        return null;
    }

    @ModifyConstant(method = "updateFallFlying", constant = @Constant(intValue = 10))
    private int lessDurabilityLossWhileGliding(int numTicksToUntilDurabilityLoss) {
        if(this.getDeltaMovement().lengthSqr() < 2.0) return 360;
        return 80;
    }

    @Inject(method = "canGlide", at = @At("HEAD"), cancellable = true)
    private void stopGlidingWhenAttacked(CallbackInfoReturnable<Boolean> cir) {
        if(this.tickCount - this.lastHurtByMobTimestamp < 2) cir.setReturnValue(false);
    }
}
