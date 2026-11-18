package frootloops.versus.mod.items_and_effects.containers;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;
import java.util.function.IntPredicate;

/**
 * The item selected in a shulker box, by the wheel: from item to item in the box's order, the empty slots skipped, round
 * and round. Up (a positive wheel) goes back through the slots and down forward, as the hotbar and the bundle do; from
 * nothing, down selects the first item and up the last.
 */
public final class ShulkerSelection {

    private ShulkerSelection() {
    }

    /** The slot the wheel selects next, or -1 when the box holds nothing. */
    public static int next(List<ItemStack> slots, int selected, int wheel) {
        return next(slots.size(), slot -> !slots.get(slot).isEmpty(), selected, wheel);
    }

    /** The slot the wheel selects next among a box's slots, those that hold an item, or -1 when none does. */
    public static int next(int size, IntPredicate holds, int selected, int wheel) {
        if (wheel == 0) return selected;
        // From nothing, going back starts past the end
        int from = selected < 0 && wheel > 0 ? size : selected;
        for (int step = 1; step <= size; step++) {
            int slot = Math.floorMod(wheel > 0 ? from - step : from + step, size);
            if (holds.test(slot)) return slot;
        }
        return -1;
    }

    public static int selected(ItemStack box) {
        ItemContainerContents contents = box.get(DataComponents.CONTAINER);
        return contents == null ? -1 : SelectedSlot.of(contents).playersVersus$selected();
    }

    /** Selects this slot of a single box (none for a slot without an item). */
    public static void select(ItemStack box, int slot) {
        ItemContainerContents contents = ShulkerItems.ofSingleBox(box);
        if (contents == null) return;
        SelectedSlot selection = SelectedSlot.of(contents);
        int valid = selection.playersVersus$holds(slot) ? slot : -1;
        if (valid != selection.playersVersus$selected()) box.set(DataComponents.CONTAINER, selection.playersVersus$withSelected(valid));
    }

    /** The server's side of the client's scroll: selects in the box in this slot of the player's menu, if it holds one. */
    public static void selectInMenu(AbstractContainerMenu menu, int menuSlot, int boxSlot) {
        if (menuSlot >= 0 && menuSlot < menu.slots.size()) select(menu.getSlot(menuSlot).getItem(), boxSlot);
    }

    /** Unselects every box in a menu's slots and on its cursor (the menu closing: nothing selected outlives it). */
    public static void clearIn(AbstractContainerMenu menu) {
        for (Slot slot : menu.slots) select(slot.getItem(), -1);
        select(menu.getCarried(), -1);
    }
}
