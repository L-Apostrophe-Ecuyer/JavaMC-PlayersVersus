package frootloops.versus.mod.items_and_effects.inventory.sorting.lists;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemComparaisonHelper;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemType;
import net.minecraft.item.Item;

import java.util.LinkedList;

import static frootloops.versus.mod.items_and_effects.inventory.InventorySorting.DEBUG_SORTING_GROUPS;


public abstract class SortedItemList {
    protected LinkedList<ItemSlot> slots = new LinkedList<>();
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
                boolean shouldGoBefore = slot.shouldAlwaysGoBefore(otherSlot, doSortedInsert) || (doSortedInsert && ItemComparaisonHelper.shouldGoBefore(slot, slots.get(i), skipNonToolTypes, false, true));
                if (shouldGoBefore) {
                    if(DEBUG_SORTING_GROUPS && startIndex == 0 && doSortedInsert) VersusMod.MOD_LOGGER.warn("                            - Found a better spot for " + slot + " -> Inserting it at pos " + i + " -> List: " + this);
                    slots.add(i, slot);
                    return i;
                }
            }
        }
        if(endIndex >= this.size() - 1) slots.addLast(slot);
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


    public LinkedList<ItemSlot> take(int count) {
        if(this.size() == 0) return new LinkedList<>();
        if(count == this.size()) return this.takeAll();

        LinkedList<ItemSlot> slotsToReturn = new LinkedList<>();
        for(int i = 0; i < Math.min(this.size(), count); i++)
            slotsToReturn.add(this.removeLast());
        return slotsToReturn;
    }

    public LinkedList<ItemSlot> takeAll() {
        LinkedList<ItemSlot> allSlots = (LinkedList<ItemSlot>) this.slots.clone();
        this.clear();
        return allSlots;
    }

    @Override
    public String toString() {
        String output = "";
        for(int i = 0; i < this.size(); i++) output += this.slots.get(i) + ", ";
        return output.substring(0, Math.max(0, output.length() - 2));
    }

    public boolean containsItem(Item item) {
        for(ItemSlot slot : slots) if(slot.stack().getItem() == item) return true;
        return false;
    }
}
