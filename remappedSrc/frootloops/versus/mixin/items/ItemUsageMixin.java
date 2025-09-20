package frootloops.versus.mixin.items_and_effects;

import frootloops.versus.mod.items_and_effects.ItemsAndStacks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.consume.UseAction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;


@Mixin(Item.class)
public class ItemUsageMixin {

    @Inject(method = "getUseAction", at = @At("HEAD"), cancellable = true)
    public void getUseAction(ItemStack stack, CallbackInfoReturnable<UseAction> cir) {
        if(cir.getReturnValue() == UseAction.NONE && stack.isOf(Items.RECOVERY_COMPASS)) cir.setReturnValue(UseAction.BOW);
    }

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    public void getMaxUseTime(ItemStack stack, LivingEntity user, CallbackInfoReturnable<Integer> cir) {
        Item item = stack.getItem();
        int maxUseTime = ItemsAndStacks.getOverhauledMaxUseTime(item);
        if(maxUseTime > 0) {
            cir.setReturnValue(maxUseTime);
        }
        else if(item instanceof SwordItem || item instanceof HoeItem) {
            UseAction useAction = item.getUseAction(stack);
            if(useAction == UseAction.BLOCK) cir.setReturnValue(72000);
            else if(useAction == UseAction.BRUSH) cir.setReturnValue(6);
            else cir.setReturnValue(0);
        }
        else if (stack.contains(DataComponentTypes.FOOD)) {
            cir.setReturnValue(28);
        }
    }

    @Inject(method = "finishUsing", at = @At("RETURN"), cancellable = false)
    public void dontUseOffhandItemAfterExhausted(ItemStack stack, World world, LivingEntity user, CallbackInfoReturnable cir) {
        // This should hopefully stop players from accidentally using an item in their offhand right after eating:
        if(user instanceof PlayerEntity player) {;
            boolean canPlayerStillUseItem = !player.getItemCooldownManager().isCoolingDown(stack);
            if(stack.contains(DataComponentTypes.FOOD) && canPlayerStillUseItem) {
                canPlayerStillUseItem = stack.getComponents().get(DataComponentTypes.FOOD).canAlwaysEat();
                canPlayerStillUseItem = player.canConsume(canPlayerStillUseItem);
            }

            if(!canPlayerStillUseItem) {

                ItemStack stackToPutOnCooldown = player.getOffHandStack();
                if (stack == user.getOffHandStack()) stackToPutOnCooldown = player.getMainHandStack();
                if (stackToPutOnCooldown.getMaxUseTime(user) > 4) return;

                int timeToSetCooldown = 4;
                Item itemToPutOnCooldown = stackToPutOnCooldown.getItem();
                if(itemToPutOnCooldown instanceof BlockItem) timeToSetCooldown = 6;
                else if(itemToPutOnCooldown instanceof WindChargeItem || itemToPutOnCooldown instanceof FireChargeItem || itemToPutOnCooldown instanceof EnderPearlItem || itemToPutOnCooldown instanceof ThrowablePotionItem) timeToSetCooldown = 8;
                player.getItemCooldownManager().set(stackToPutOnCooldown, timeToSetCooldown);
            }
        }
    }
}
