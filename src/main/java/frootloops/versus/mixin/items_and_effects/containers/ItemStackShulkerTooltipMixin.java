package frootloops.versus.mixin.items_and_effects.containers;

import frootloops.versus.mod.items_and_effects.containers.ShulkerItems;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/** A shulker box's contents aren't also listed as lines of text: its tooltip image shows them. */
@Mixin(ItemStack.class)
public abstract class ItemStackShulkerTooltipMixin {

    @Inject(method = "addToTooltip(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V",
            at = @At("HEAD"), cancellable = true)
    private void playersVersus$noShulkerLines(DataComponentType<?> type, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> consumer,
                                              TooltipFlag flag, CallbackInfo ci) {
        if (type == DataComponents.CONTAINER && ShulkerItems.isBox((ItemStack) (Object) this)) ci.cancel();
    }
}
