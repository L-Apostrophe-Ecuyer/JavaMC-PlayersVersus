package frootloops.versus.mod.items_and_effects.inventory.sorting.lists;

import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemOrdering;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemType;
import frootloops.versus.mod.items_and_effects.inventory.sorting.SortingDebug;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An ordered list of stacks that go together, such as pickaxes or wooden blocks. Each subclass decides which stacks
 * it accepts and where they go.
 */
public abstract class SortedItemList {
    protected final List<ItemSlot> slots = new ArrayList<>();
    protected ItemType itemType;

    public int trySortedInsert(ItemSlot newSlot) {
        return -1;
    }

    public ItemType getMainItemTypeOfList() {
        return this.itemType;
    }

    public void clear() {slots.clear();}

    public int size() {return slots.size();}

    public ItemSlot removeFirst() {return slots.removeFirst();}
    public ItemSlot removeLast() {return slots.removeLast();}

    public int add(ItemSlot slot) {
        return this.addBetween(slot, 0, this.size() - 1, true);
    }

    public int addWithMinimalSorting(ItemSlot slot) {
        return this.addBetween(slot, 0, this.size() - 1, false);
    }

    public int addWithoutSorting(ItemSlot slot) {
        this.slots.add(slot);
        return slots.size();
    }

    public void addAt(ItemSlot slot, int index) {
        slots.add(index, slot);
    }

    public int addBetween(ItemSlot slot, int startIndex, int endIndex) {
        endIndex = Math.min(endIndex, this.size() - 1);
        startIndex = Math.max(0, Math.min(startIndex, endIndex));
        return this.addBetween(slot, startIndex, endIndex, true);
    }

    private int addBetween(ItemSlot slot, int startIndex, int endIndex, boolean doSortedInsert) {
        if(this.size() > 0) {
            boolean skipNonToolTypes = false;
            if(startIndex > 0 && slot.isToolOrWeapon()) {
                skipNonToolTypes = true;
                startIndex = 0;
            }
            for (int i = startIndex; i < endIndex + 1; i++) {
                ItemSlot otherSlot = slots.get(i);
                boolean shouldGoBefore = slot.shouldAlwaysGoBefore(otherSlot, doSortedInsert) || (doSortedInsert && ItemOrdering.shouldGoBefore(slot, otherSlot, skipNonToolTypes, false, true));
                if (shouldGoBefore) {
                    if(startIndex == 0 && doSortedInsert) {
                        int position = i;
                        SortingDebug.log(() -> "                            - Found a better spot for " + slot + " -> Inserting it at pos " + position + " -> List: " + this);
                    }
                    slots.add(i, slot);
                    return i;
                }
            }
        }
        if(endIndex >= this.size() - 1) slots.add(slot);
        else slots.add(endIndex, slot);
        return endIndex + 1;
    }

    public void giveLastSlotsTo(int count, SortedItemList other) {
        count = Math.min(count, this.size());
        for(int i = 0; i < count; i++) other.addWithMinimalSorting(this.removeLast());
    }

    public void giveFirstSlotsTo(int count, SortedItemList other) {
        count = Math.min(count, this.size());
        for(int i = 0; i < count; i++) other.addWithMinimalSorting(this.removeFirst());
    }

    public void appendListToEnd(SortedItemList other) {
        this.slots.addAll(other.takeAll());
    }

    public void addListToStart(SortedItemList other) {
        this.slots.addAll(0, other.takeAll());
    }

    public ItemSlot getSlot(int index) {
        return this.slots.get(index);
    }

    /**
     * Takes {@code count} stacks (or all of them, if fewer), from the end of the list, last first.
     */
    public List<ItemSlot> take(int count) {
        if(this.size() == 0) return new ArrayList<>();
        if(count == this.size()) return this.takeAll();
        return this.takeLast(count);
    }

    protected List<ItemSlot> takeLast(int count) {
        int numToTake = Math.min(this.size(), count);
        List<ItemSlot> slotsToReturn = new ArrayList<>(numToTake);
        for(int i = 0; i < numToTake; i++) slotsToReturn.add(this.removeLast());
        return slotsToReturn;
    }

    public List<ItemSlot> takeAll() {
        List<ItemSlot> allSlots = new ArrayList<>(this.slots);
        this.clear();
        return allSlots;
    }

    @Override
    public String toString() {
        return this.slots.stream().map(ItemSlot::toString).collect(Collectors.joining(", "));
    }

    public boolean containsItem(Item item) {
        for(ItemSlot slot : slots) if(slot.stack().getItem() == item) return true;
        return false;
    }
}
