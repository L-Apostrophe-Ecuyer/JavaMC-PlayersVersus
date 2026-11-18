package frootloops.versus.mod.items_and_effects.containers;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * A shulker box in the inventory, clicked as a bundle is: an item clicked onto the box goes in, the box clicked onto an
 * item takes it in, a right click takes out the item the wheel selected. Anything else (nothing selected, an item that
 * can't go in a shulker box, a box with no room, a stack of boxes) is the click vanilla's: the two swap, or the box is
 * picked up. Runs on both sides (the client predicting).
 */
public final class ShulkerClicks {

    private ShulkerClicks() {
    }

    /** The box on the cursor clicked onto a slot. */
    public static boolean boxOntoSlot(ItemStack box, Slot slot, ClickAction action, Player player) {
        ItemContainerContents contents = ShulkerItems.ofSingleBox(box);
        if (contents == null || action != ClickAction.PRIMARY) return false;
        ItemStack other = slot.getItem();
        NonNullList<ItemStack> slots = ShulkerItems.slots(contents);
        int room = ShulkerItems.room(slots, other);
        if (room <= 0) return false;
        ItemStack taken = slot.safeTake(other.getCount(), room, player);
        if (taken.isEmpty()) return false;
        ShulkerItems.insert(slots, taken);
        if (!taken.isEmpty()) slot.safeInsert(taken);
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
        insertSound(player);
        changed(player);
        return true;
    }

    /** A slot's box clicked with what's on the cursor; a click left vanilla's (a pick-up, a swap) unselects the box first. */
    public static boolean slotOntoBox(ItemStack box, ItemStack carried, Slot slot, ClickAction action, Player player, SlotAccess cursor) {
        if (ShulkerItems.ofSingleBox(box) == null) return false;
        if (intoOrOutOfBox(box, carried, slot, action, player, cursor)) return true;
        // Picked up or swapped away: unselected, as a bundle is, so no selection goes with the box
        ShulkerSelection.select(box, -1);
        return false;
    }

    private static boolean intoOrOutOfBox(ItemStack box, ItemStack carried, Slot slot, ClickAction action, Player player, SlotAccess cursor) {
        ItemContainerContents contents = ShulkerItems.ofSingleBox(box);
        if (action == ClickAction.PRIMARY && carried.isEmpty()) return false;
        if (!slot.allowModification(player)) return false;
        NonNullList<ItemStack> slots = ShulkerItems.slots(contents);
        if (action == ClickAction.PRIMARY) {
            if (ShulkerItems.room(slots, carried) <= 0) return false;
            ShulkerItems.insert(slots, carried);
            box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
            insertSound(player);
            changed(player);
            return true;
        }
        if (action == ClickAction.SECONDARY && carried.isEmpty()) {
            ItemStack taken = ShulkerItems.take(slots, ShulkerSelection.selected(box));
            if (taken.isEmpty()) return false;
            // New contents: nothing selected
            box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
            cursor.set(taken);
            removeSound(player);
            changed(player);
            return true;
        }
        return false;
    }

    private static void insertSound(Player player) {
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
    }

    private static void removeSound(Player player) {
        player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
    }

    private static void changed(Player player) {
        if (player.containerMenu != null) player.containerMenu.slotsChanged(player.getInventory());
    }
}
