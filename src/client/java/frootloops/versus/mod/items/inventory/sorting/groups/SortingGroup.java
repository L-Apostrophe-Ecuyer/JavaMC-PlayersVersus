package frootloops.versus.mod.items.inventory.sorting.groups;

import frootloops.versus.mod.items.inventory.sorting.ItemComparaisonHelper;
import frootloops.versus.mod.items.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items.inventory.sorting.ItemType;
import frootloops.versus.mod.items.inventory.sorting.lists.SortedItemList;
import frootloops.versus.mod.items.inventory.sorting.lists.SortedMiscItemList;


import java.util.LinkedList;

public abstract class SortingGroup implements Comparable<SortingGroup>{

    public final String GROUP_NAME;
    protected final SortedItemList miscItems = new SortedMiscItemList(null);

    protected SortingGroup(String group_name) {
        GROUP_NAME = group_name;
    }

    public abstract ItemType getItemType();
    public abstract int getMaxNumRows();

    /**
     * Either accepts and inserts slot into itself, or returns it back. Also has the opportunity to take the slot and return another.
     */
    public abstract ItemSlot tryInsertingSlot(ItemSlot slot);

    /**
     * Forcibly adds the item slot somewhere to this group, wherever the group thinks is best.
     */
    public void addSlot(ItemSlot slot) {
        slot = tryInsertingSlot(slot);
        if(slot != null) this.miscItems.add(slot);
    }

    public void addSlots(LinkedList<ItemSlot> newSlots) {
        for(int i = 0; i < newSlots.size(); i++) {
            ItemSlot slot = newSlots.pop();
            slot = tryInsertingSlot(slot);
            if(slot != null) newSlots.add(slot);
        }
        this.addSlotsToMisc(newSlots);
    }

    protected void addSlotsToMisc(LinkedList<ItemSlot> newSlots) {
        if(newSlots.size() < 1) return;
        int indexMiscEnd;
        for(indexMiscEnd = miscItems.size() - 1; indexMiscEnd >= 0; indexMiscEnd--) {
            if(ItemComparaisonHelper.shouldGoBefore(miscItems.getSlot(indexMiscEnd), newSlots.getFirst())) break;
        }
        int indexMiscStart = indexMiscEnd;
        for (ItemSlot slot : newSlots) {
            miscItems.addBetween(slot, indexMiscStart, indexMiscEnd, false);
            indexMiscEnd++;
        }
    }

    /**
     * Take all items from this group, in the form of an ordered list.
     */
    public abstract LinkedList<ItemSlot> takeAllItems();

    /**
     * Try to merge lists together so that they make neat rows of 9 when possible.
     */
    public abstract void tryFormingRows();

    /**
     * Return size of next non-empty list to return.
     */
    public abstract int getNextListSize();

    /**
     * Take all items from the next non-empty list currently in the group.
     */
    public abstract LinkedList<ItemSlot> takeNextList();

    /**
     * Take all items from the biggest list currently in the group.
     */
    public abstract LinkedList<ItemSlot> takeFirstSlots(int numSlotsToTake, boolean splitUpSubgroups);

    /**
     * Looks through the group's sorted lists to find a match of exactly numSlotsToTake items.
     * Note: This can be fairly expensive.
     */
    public abstract LinkedList<ItemSlot> tryTakingExactNumSlots(int numSlotsToTake, boolean withTool, boolean startFromEnd);

    public abstract void clear();

    public abstract int size();

    @Override
    public int compareTo(SortingGroup other) {
        int size = this.size();
        int otherSize = other.size();
        if(size == otherSize) return 0;
        if(size % 9 == otherSize % 9) return size > otherSize ? -1 : 1;
        return size % 9 > otherSize % 9 ? 1 : -1;
    }
}

