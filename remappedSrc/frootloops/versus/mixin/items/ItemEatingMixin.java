package frootloops.versus.mixin.items;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Item.class)
public class ItemEatingMixin {

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    public void getMaxUseTime(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.contains(DataComponentTypes.FOOD)) {

            FoodComponent foodComponent = stack.getComponents().get(DataComponentTypes.FOOD);
            boolean isCookedMeat = stack.getItem().getTranslationKey().contains("cooked");

            if(isCookedMeat) cir.setReturnValue(28);
            else if(foodComponent.eatSeconds() == 0.8f) cir.setReturnValue(10);
            else if(stack.isOf(Items.PUMPKIN_PIE)) cir.setReturnValue(14);
            else if(stack.isOf(Items.POTATO)) cir.setReturnValue(32);
            else if(stack.isOf(Items.GOLDEN_APPLE)) cir.setReturnValue(24);
            else if(stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) cir.setReturnValue(24);
            else if(foodComponent.nutrition() < 5)  cir.setReturnValue(14);
            else cir.setReturnValue(18);

        }
    }

    @Inject(method = "finishUsing", at = @At("RETURN"), cancellable = false)
    public void dontUseOffhandItemAfterExhausted(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable cir) {
        // This should hopefully stop players from accidentally using an item in their offhand right after eating:
        if(user instanceof PlayerEntity player) {;
            boolean canPlayerStillUseItem = !player.getItemCooldownManager().isCoolingDown(stack.getItem());
            if(stack.contains(DataComponentTypes.FOOD) && canPlayerStillUseItem) {
                canPlayerStillUseItem = stack.getComponents().get(DataComponentTypes.FOOD).canAlwaysEat();
                canPlayerStillUseItem = player.canConsume(canPlayerStillUseItem);
            }

            if(!canPlayerStillUseItem) {

                ItemStack stackToPutOnCooldown = player.getOffHandStack();
                if (stack == user.getOffHandStack()) stackToPutOnCooldown = player.getMainHandStack();
                if (stackToPutOnCooldown.getMaxUseTime() > 4) return;

                int timeToSetCooldown = 4;
                Item itemToPutOnCooldown = stackToPutOnCooldown.getItem();
                if(itemToPutOnCooldown instanceof BlockItem) timeToSetCooldown = 6;
                else if(itemToPutOnCooldown instanceof WindChargeItem || itemToPutOnCooldown instanceof FireChargeItem || itemToPutOnCooldown instanceof EnderPearlItem || itemToPutOnCooldown instanceof ThrowablePotionItem) timeToSetCooldown = 8;
                player.getItemCooldownManager().set(itemToPutOnCooldown, timeToSetCooldown);
            }
        }
    }
}
