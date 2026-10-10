package frootloops.versus.gametest;

import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemComparaisonHelper;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items_and_effects.inventory.sorting.SortingHelper;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Inventories for the sorting game tests, and the one call that sorts them.
 */
public final class TestInventories {

    private TestInventories() {
    }

    /**
     * Sorts the stacks as an inventory of {@code numRows} rows. {@code result[i]} is the index in {@code stacks} of the
     * stack laid at position {@code i} ({@code row * 9 + column}; row 0 is a player's hotbar), or -1 for an empty slot.
     * Empty when the sort gives up.
     */
    public static Optional<int[]> sortIds(List<ItemStack> stacks, int numRows, boolean playerInventory, boolean inDeepDark, boolean inNether, boolean inWater) {
        LinkedList<ItemSlot> slots = new LinkedList<>();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            slots.add(new ItemSlot(i, stack, ItemComparaisonHelper.getItemTypeOf(stack)));
        }
        ItemSlot[] sorted = SortingHelper.getOptimalInventoryRows(slots, numRows, playerInventory, inDeepDark, inNether, inWater);
        if (sorted == null) return Optional.empty();
        int[] ids = new int[sorted.length];
        for (int i = 0; i < sorted.length; i++) ids[i] = sorted[i] == null ? -1 : sorted[i].slodId();
        return Optional.of(ids);
    }
}
