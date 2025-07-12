package frootloops.versus.mod.items.inventory.sorting.lists;

import frootloops.versus.mod.items.inventory.sorting.ItemComparaisonHelper;
import frootloops.versus.mod.items.inventory.sorting.ItemSlot;

import java.util.LinkedList;



public abstract class SortedItemList {
    protected LinkedList<ItemSlot> slots = new LinkedList<>();

    public int trySortedInsert(ItemSlot newSlot) {
        return -1;
    }

    public void clear() {slots.clear();}

    public int size() {return slots.size();}

    public ItemSlot removeFirst() {return slots.removeFirst();}
    public ItemSlot removeLast() {return slots.removeLast();}

    public int add(ItemSlot slot) {
        return this.addBetween(slot, 0, this.size(), true);
    }

    protected int addBetween(ItemSlot slot, int startIndex, int endIndex) {
        return this.addBetween(slot, startIndex, endIndex, true);
    }

    protected int addBetween(ItemSlot slot, int startIndex, int endIndex, boolean doSortedInsert) {
        if(this.size() > 0) {
            endIndex = Math.min(endIndex, this.size() - 1);
            startIndex = Math.max(0, Math.min(startIndex, endIndex));

            boolean skipNonToolTypes = !doSortedInsert; // doSortedInsert is usually true, unless merging two groups together, where they should stay distinct
            if(startIndex > 0 && slot.isToolOrWeapon()) {
                skipNonToolTypes = true;
                startIndex = 0;
            }
            for (int i = endIndex - 1; i >= startIndex; i--) {
                if (ItemComparaisonHelper.shouldGoBefore(slot, slots.get(i), skipNonToolTypes, !doSortedInsert)) {
                    slots.add(i, slot);
                    return i;
                }
            }
        }
        slots.add(slot);
        return endIndex;
    }

    public void giveLastSlotsTo(int count, SortedItemList other) {
        count = Math.min(count, this.size());
        for(int i = 0; i < count; i++)
            other.addBetween(this.removeLast(), 0, this.size(), false);
    }

    public void giveFirstSlotsTo(int count, SortedItemList other) {
        count = Math.min(count, this.size());
        for(int i = 0; i < count; i++)
            other.addBetween(this.removeFirst(), 0, this.size(), false);
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
}
