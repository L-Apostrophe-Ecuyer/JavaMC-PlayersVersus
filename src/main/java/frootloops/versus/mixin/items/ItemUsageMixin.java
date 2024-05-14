package frootloops.versus.mixin.items;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;


@Mixin(Item.class)
public class ItemUsageMixin {

    private static final Map<Item,Integer> MAX_USE_TIME_MAP = new HashMap<>();
    static {
        MAX_USE_TIME_MAP.put(Items.ROTTEN_FLESH, 40);
        MAX_USE_TIME_MAP.put(Items.SPIDER_EYE, 40);
        MAX_USE_TIME_MAP.put(Items.COOKED_BEEF, 36);
        MAX_USE_TIME_MAP.put(Items.COOKED_PORKCHOP, 36);
        MAX_USE_TIME_MAP.put(Items.COOKED_CHICKEN, 32);
        MAX_USE_TIME_MAP.put(Items.COOKED_MUTTON, 32);
        MAX_USE_TIME_MAP.put(Items.COOKED_RABBIT, 32);
        MAX_USE_TIME_MAP.put(Items.COOKED_SALMON, 28);
        MAX_USE_TIME_MAP.put(Items.COOKED_COD, 28);
        MAX_USE_TIME_MAP.put(Items.BEETROOT, 12);
        MAX_USE_TIME_MAP.put(Items.POTATO, 32);
        MAX_USE_TIME_MAP.put(Items.BAKED_POTATO, 24);
        MAX_USE_TIME_MAP.put(Items.POISONOUS_POTATO, 32);
        MAX_USE_TIME_MAP.put(Items.BREAD, 24);
        MAX_USE_TIME_MAP.put(Items.COOKIE, 12);
        MAX_USE_TIME_MAP.put(Items.DRIED_KELP, 12);
        MAX_USE_TIME_MAP.put(Items.PUMPKIN_PIE, 16);
        MAX_USE_TIME_MAP.put(Items.APPLE, 24);
        MAX_USE_TIME_MAP.put(Items.CARROT, 24);
        MAX_USE_TIME_MAP.put(Items.GOLDEN_CARROT, 28);
        MAX_USE_TIME_MAP.put(Items.GOLDEN_APPLE, 28);
        MAX_USE_TIME_MAP.put(Items.ENCHANTED_GOLDEN_APPLE, 28);
        MAX_USE_TIME_MAP.put(Items.SUSPICIOUS_STEW, 16);
        MAX_USE_TIME_MAP.put(Items.BEETROOT_SOUP, 12);
        MAX_USE_TIME_MAP.put(Items.MUSHROOM_STEW, 16);
        MAX_USE_TIME_MAP.put(Items.RABBIT_STEW, 16);
        MAX_USE_TIME_MAP.put(Items.HONEY_BOTTLE, 16);
        MAX_USE_TIME_MAP.put(Items.POTION, 36);
        MAX_USE_TIME_MAP.put(Items.MILK_BUCKET, 12);
        MAX_USE_TIME_MAP.put(Items.RECOVERY_COMPASS, 32);
    }

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    public void getMaxUseTime(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        Item item = stack.getItem();
        if(MAX_USE_TIME_MAP.containsKey(item)) {
            cir.setReturnValue(MAX_USE_TIME_MAP.get(item));
        }
        else if(item instanceof SwordItem || item instanceof HoeItem) {
            UseAction useAction = item.getUseAction(stack);
            if(useAction == UseAction.BLOCK) cir.setReturnValue(72000);
            else if(useAction == UseAction.BRUSH) cir.setReturnValue(6);
            else cir.setReturnValue(0);
        }
        else if (stack.contains(DataComponentTypes.FOOD)) {
            FoodComponent foodComponent = stack.getComponents().get(DataComponentTypes.FOOD);
            if (item.getTranslationKey().contains("cooked")) cir.setReturnValue(32);
            else if (foodComponent.eatSeconds() == 0.8f) cir.setReturnValue(10);
            else if (foodComponent.nutrition() < 5) cir.setReturnValue(14);
            else cir.setReturnValue(24);
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
                if (stackToPutOnCooldown.getMaxUseTime(user) > 4) return;

                int timeToSetCooldown = 4;
                Item itemToPutOnCooldown = stackToPutOnCooldown.getItem();
                if(itemToPutOnCooldown instanceof BlockItem) timeToSetCooldown = 6;
                else if(itemToPutOnCooldown instanceof WindChargeItem || itemToPutOnCooldown instanceof FireChargeItem || itemToPutOnCooldown instanceof EnderPearlItem || itemToPutOnCooldown instanceof ThrowablePotionItem) timeToSetCooldown = 8;
                player.getItemCooldownManager().set(itemToPutOnCooldown, timeToSetCooldown);
            }
        }
    }
}
