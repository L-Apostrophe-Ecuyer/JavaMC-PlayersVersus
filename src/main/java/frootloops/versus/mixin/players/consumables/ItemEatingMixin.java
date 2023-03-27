package frootloops.versus.mixin.players.consumables;

import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

            if(foodComponent.isMeat()) cir.setReturnValue(44);
            else if(foodComponent.isSnack()) cir.setReturnValue(16);
            else if(foodComponent.getHunger() == 1 && foodComponent.getSaturationModifier() == 0.3f) cir.setReturnValue(44);
            else if(foodComponent.getHunger() < 4)  cir.setReturnValue(16);
            else if(foodComponent.getSaturationModifier() == 0.3F)  cir.setReturnValue(24);
            else cir.setReturnValue(32);

        }
    }
}
