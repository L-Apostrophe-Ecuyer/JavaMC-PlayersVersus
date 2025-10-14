package frootloops.versus.mixin.items_and_effects.equipment;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.entity.LivingEntity.canGlideWith;


@Mixin(LivingEntity.class)
public abstract class ElytraGlidingMixin extends Entity {
    public ElytraGlidingMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Shadow private int lastAttackedTime;
    @Shadow	public ItemStack getEquippedStack(EquipmentSlot slot) {
        return null;
    }

    @ModifyConstant(method = "tickGliding", constant = @Constant(intValue = 10))
    private int lessDurabilityLossWhileGliding(int numTicksToUntilDurabilityLoss) {
        if(this.getVelocity().lengthSquared() < 2.0) return 360;
        return 80;
    }

    @Inject(method = "canGlide", at = @At("HEAD"), cancellable = true)
    private void stopGlidingWhenAttacked(CallbackInfoReturnable<Boolean> cir) {
        if(this.age - this.lastAttackedTime < 10) cir.setReturnValue(false);
    }
}
