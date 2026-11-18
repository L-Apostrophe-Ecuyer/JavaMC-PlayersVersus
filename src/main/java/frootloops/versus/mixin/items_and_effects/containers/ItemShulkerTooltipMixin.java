package frootloops.versus.mixin.items_and_effects.containers;

import frootloops.versus.mod.items_and_effects.containers.SelectedSlot;
import frootloops.versus.mod.items_and_effects.containers.ShulkerColors;
import frootloops.versus.mod.items_and_effects.containers.ShulkerItems;
import frootloops.versus.mod.items_and_effects.containers.ShulkerTooltip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** A shulker box with anything in it shows its slots as its tooltip image (unless its tooltip hides its contents). */
@Mixin(Item.class)
public abstract class ItemShulkerTooltipMixin {

    @Inject(method = "getTooltipImage", at = @At("HEAD"), cancellable = true)
    private void playersVersus$shulkerSlots(ItemStack stack, CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        if (!ShulkerItems.isBox(stack)) return;
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null || !contents.nonEmptyItems().iterator().hasNext()) return;
        if (!stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT).shows(DataComponents.CONTAINER)) return;
        DyeColor dye = Block.byItem(stack.getItem()) instanceof ShulkerBoxBlock box ? box.getColor() : null;
        cir.setReturnValue(Optional.of(new ShulkerTooltip(ShulkerItems.slots(contents), SelectedSlot.of(contents).playersVersus$selected(), ShulkerColors.of(dye))));
    }
}
