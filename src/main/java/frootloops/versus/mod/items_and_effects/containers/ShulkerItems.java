package frootloops.versus.mod.items_and_effects.containers;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * A shulker box item's 27 slots, put into and taken out of as the box's own slots would be: onto stacks of the same
 * item first, then the first empty slots, never past an item's stack size, never another shulker box.
 */
public final class ShulkerItems {

    public static final int SIZE = ShulkerBoxBlockEntity.CONTAINER_SIZE;

    private ShulkerItems() {
    }

    public static boolean isBox(ItemStack stack) {
        return stack.is(ItemTags.SHULKER_BOXES);
    }

    /** The contents of a single shulker box (a stack of one) whose slots are a box's, or null for anything else. */
    public static @Nullable ItemContainerContents ofSingleBox(ItemStack stack) {
        if (!isBox(stack) || stack.getCount() != 1) return null;
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        return contents.size() <= SIZE ? contents : null;
    }

    public static NonNullList<ItemStack> slots(ItemContainerContents contents) {
        NonNullList<ItemStack> slots = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        contents.copyInto(slots);
        return slots;
    }

    /** Whether this may go in a shulker box at all (not a shulker box, nothing that refuses container items). */
    public static boolean fits(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem().canFitInsideContainerItems();
    }

    /** How many of this stack would go in. */
    public static int room(NonNullList<ItemStack> slots, ItemStack from) {
        if (!fits(from)) return 0;
        int room = 0, most = from.getMaxStackSize();
        for (ItemStack held : slots) {
            if (held.isEmpty()) room += most;
            else if (ItemStack.isSameItemSameComponents(held, from)) room += Math.max(0, Math.min(most, held.getMaxStackSize()) - held.getCount());
            if (room >= from.getCount()) return from.getCount();
        }
        return room;
    }

    /** Moves as much of this stack in as fits (it shrinks by as much); how many went in. */
    public static int insert(NonNullList<ItemStack> slots, ItemStack from) {
        if (!fits(from)) return 0;
        int moved = 0, most = from.getMaxStackSize();
        for (ItemStack held : slots) {
            if (from.isEmpty()) break;
            if (held.isEmpty() || !ItemStack.isSameItemSameComponents(held, from)) continue;
            int added = Math.min(from.getCount(), Math.min(most, held.getMaxStackSize()) - held.getCount());
            if (added <= 0) continue;
            held.grow(added);
            from.shrink(added);
            moved += added;
        }
        for (int slot = 0; slot < slots.size() && !from.isEmpty(); slot++) {
            if (!slots.get(slot).isEmpty()) continue;
            int added = Math.min(from.getCount(), most);
            slots.set(slot, from.split(added));
            moved += added;
        }
        return moved;
    }

    /** The whole stack in this slot, the slot left empty (nothing for a slot past the box's). */
    public static ItemStack take(NonNullList<ItemStack> slots, int slot) {
        if (slot < 0 || slot >= slots.size()) return ItemStack.EMPTY;
        ItemStack taken = slots.get(slot);
        slots.set(slot, ItemStack.EMPTY);
        return taken;
    }
}
