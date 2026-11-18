package frootloops.versus.mixin.client.items_and_effects.containers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import frootloops.versus.mod.items_and_effects.containers.ShulkerPeek;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** While the shulker box peek shows, the wheel selects among the box's items rather than the hotbar's slots. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerPeekMixin {

    @WrapOperation(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V"))
    private void playersVersus$peekWheel(Inventory inventory, int slot, Operation<Void> original, @Local int wheel) {
        if (!ShulkerPeek.scroll(wheel)) original.call(inventory, slot);
    }
}
