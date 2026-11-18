package frootloops.versus.mixin.items_and_effects.containers;

import frootloops.versus.mod.items_and_effects.containers.ShulkerClicks;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A shulker box clicked in the inventory as a bundle is (ShulkerClicks); items that override these aren't touched. */
@Mixin(Item.class)
public abstract class ItemShulkerClicksMixin {

    @Inject(method = "overrideStackedOnOther", at = @At("HEAD"), cancellable = true)
    private void playersVersus$boxOntoSlot(ItemStack self, Slot slot, ClickAction clickAction, Player player, CallbackInfoReturnable<Boolean> cir) {
        if (ShulkerClicks.boxOntoSlot(self, slot, clickAction, player)) cir.setReturnValue(true);
    }

    @Inject(method = "overrideOtherStackedOnMe", at = @At("HEAD"), cancellable = true)
    private void playersVersus$slotOntoBox(ItemStack self, ItemStack other, Slot slot, ClickAction clickAction, Player player, SlotAccess carriedItem,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (ShulkerClicks.slotOntoBox(self, other, slot, clickAction, player, carriedItem)) cir.setReturnValue(true);
    }
}
