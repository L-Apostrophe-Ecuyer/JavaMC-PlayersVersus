package frootloops.versus.mixin.items_and_effects;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Item.class)
public abstract class ItemUsageMixin {

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    public void getUseAction(ItemStack stack, CallbackInfoReturnable<ItemUseAnimation> cir) {
        if(cir.getReturnValue() == ItemUseAnimation.NONE && stack.is(Items.RECOVERY_COMPASS)) cir.setReturnValue(ItemUseAnimation.BOW);
    }

    /*
    @Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setCurrentHand(Lnet/minecraft/util/Hand;)V"), cancellable = true)
    public void canOnlyBlockIfAttackIsCharged(PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if(user.getAttackCooldownProgress(0.0f) < 0.5f) cir.setReturnValue(ActionResult.PASS);
    }*/

    @Inject(method = "finishUsingItem", at = @At("RETURN"), cancellable = false)
    public void dontUseOffhandItemAfterExhausted(ItemStack stack, Level world, LivingEntity user, CallbackInfoReturnable cir) {
        // This should hopefully stop players from accidentally using an item in their offhand right after eating:
        if(user instanceof Player player) {;
            boolean canPlayerStillUseItem = !player.getCooldowns().isOnCooldown(stack);
            if(stack.has(DataComponents.FOOD) && canPlayerStillUseItem) {
                canPlayerStillUseItem = stack.getComponents().get(DataComponents.FOOD).canAlwaysEat();
                canPlayerStillUseItem = player.canEat(canPlayerStillUseItem);
            }

            if(!canPlayerStillUseItem) {
                ItemStack stackToPutOnCooldown = player.getOffhandItem();
                if (stack == user.getOffhandItem()) stackToPutOnCooldown = player.getMainHandItem();
                if (stackToPutOnCooldown.getUseDuration(user) > 4) return;

                int timeToSetCooldown = 4;
                Item itemToPutOnCooldown = stackToPutOnCooldown.getItem();
                if(itemToPutOnCooldown instanceof BlockItem) timeToSetCooldown = 6;
                else if(itemToPutOnCooldown instanceof WindChargeItem || itemToPutOnCooldown instanceof FireChargeItem || itemToPutOnCooldown instanceof EnderpearlItem || itemToPutOnCooldown instanceof ThrowablePotionItem) timeToSetCooldown = 8;
                player.getCooldowns().addCooldown(stackToPutOnCooldown, timeToSetCooldown);
            }
        }
    }
}
