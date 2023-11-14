package frootloops.versus.mixin.items;

import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Item.class)
public class ItemEatingMixin {
    public ItemEatingMixin(@Nullable FoodComponent foodComponent) {
        this.foodComponent = foodComponent;
    }

    @Shadow @Nullable
    private final FoodComponent foodComponent;


    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    public void getMaxUseTime(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.getItem().isFood()) {

            if(foodComponent.isMeat()) cir.setReturnValue(32);
            else if(foodComponent.isSnack()) cir.setReturnValue(10);
            else if(stack.isOf(Items.POTATO)) cir.setReturnValue(32);
            else if(stack.isOf(Items.GOLDEN_APPLE)) cir.setReturnValue(24);
            else if(stack.isOf(Items.ENCHANTED_GOLDEN_APPLE)) cir.setReturnValue(24);
            else if(foodComponent.getHunger() < 3)  cir.setReturnValue(14);
            else if(foodComponent.getHunger() < 5)  cir.setReturnValue(18);
            else if(foodComponent.getSaturationModifier() == 0.3F)  cir.setReturnValue(18);
            else cir.setReturnValue(24);

        }
    }
}
