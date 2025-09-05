package frootloops.versus.mixin.items;

import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import frootloops.versus.mod.items.VanillaItemsAndStacks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Item.class)
public abstract class ItemUsageMixin {

    @Inject(method = "getUseAction", at = @At("HEAD"), cancellable = true)
    public void getUseAction(ItemStack stack, CallbackInfoReturnable<UseAction> cir) {
        if(cir.getReturnValue() == UseAction.NONE && stack.isOf(Items.RECOVERY_COMPASS)) cir.setReturnValue(UseAction.BOW);
    }

    @Inject(method = "use", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setCurrentHand(Lnet/minecraft/util/Hand;)V"), cancellable = true)
    public void canOnlyBlockIfAttackIsCharged(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if(user.getAttackCooldownProgress(0.0f) < 0.5f) cir.setReturnValue(ActionResult.PASS);
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
