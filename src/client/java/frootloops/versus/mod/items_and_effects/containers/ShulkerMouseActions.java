package frootloops.versus.mod.items_and_effects.containers;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.joml.Vector2i;

/**
 * The wheel over a shulker box in a slot selects among its items (ShulkerSelection), here and on the server, as vanilla's
 * BundleMouseActions does for bundles; leaving the slot, or shift-clicking, swapping or throwing it, unselects.
 */
public class ShulkerMouseActions implements ItemSlotMouseAction {

    private final ScrollWheelHandler scrollWheelHandler = new ScrollWheelHandler();

    @Override
    public boolean matches(Slot slot) {
        ItemContainerContents contents = ShulkerItems.ofSingleBox(slot.getItem());
        return contents != null && contents.nonEmptyItems().iterator().hasNext();
    }

    @Override
    public boolean onMouseScrolled(double scrollX, double scrollY, int slotIndex, ItemStack box) {
        ItemContainerContents contents = ShulkerItems.ofSingleBox(box);
        if (contents == null) return false;
        Vector2i wheelXY = this.scrollWheelHandler.onMouseScroll(scrollX, scrollY);
        int wheel = wheelXY.y == 0 ? -wheelXY.x : wheelXY.y;
        if (wheel != 0) {
            int selected = ShulkerSelection.selected(box);
            int next = ShulkerSelection.next(ShulkerItems.slots(contents), selected, wheel);
            if (next != selected) select(box, slotIndex, next);
        }
        return true;
    }

    @Override
    public void onStopHovering(Slot slot) {
        if (ShulkerSelection.selected(slot.getItem()) >= 0) select(slot.getItem(), slot.index, -1);
    }

    @Override
    public void onSlotClicked(Slot slot, ContainerInput input) {
        if ((input == ContainerInput.QUICK_MOVE || input == ContainerInput.SWAP || input == ContainerInput.THROW) && ShulkerSelection.selected(slot.getItem()) >= 0) {
            select(slot.getItem(), slot.index, -1);
        }
    }

    private static void select(ItemStack box, int slotIndex, int boxSlot) {
        ShulkerSelection.select(box, boxSlot);
        if (ClientPlayNetworking.canSend(SelectShulkerSlotPayload.TYPE)) ClientPlayNetworking.send(new SelectShulkerSlotPayload(slotIndex, boxSlot));
    }
}
