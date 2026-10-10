package frootloops.versus.mod.items_and_effects.inventory.sorting.groups;

import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemOrdering;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemType;
import frootloops.versus.mod.items_and_effects.inventory.sorting.lists.SortedItemList;
import frootloops.versus.mod.items_and_effects.inventory.sorting.lists.SortedMiscItemList;

import java.util.ArrayList;
import java.util.List;

/**
 * Sorted lists that the layout keeps together, such as the pickaxes with the blocks they mine. Its size is what its
 * lists hold.
 */
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

    /**
     * Inserts each slot where it belongs, and the ones no list takes with the misc items.
     */
    public boolean addSlots(List<ItemSlot> newSlots) {
        if(newSlots == null || newSlots.isEmpty()) return false;
        List<ItemSlot> leftOver = new ArrayList<>();
        for(ItemSlot slot : newSlots) {
            slot = tryInsertingSlot(slot);
            if(slot != null) leftOver.add(slot);
        }
        this.addSlotsToMisc(leftOver);
        return true;
    }

    protected void addSlotsToMisc(List<ItemSlot> newSlots) {
        if(newSlots.isEmpty()) return;

        // Step 1: Figure out where to insert into the misc list:
        int indexMiscStart = 0, indexMiscEnd;
        for(int i = miscItems.size() - 1; i >= 0; i--) {
            indexMiscStart = i;
            if(ItemOrdering.shouldGoBefore(miscItems.getSlot(i), newSlots.getFirst(), false, false, false)) break;
        }

        // Step 2: Insert there, keeping the new slots together:
        indexMiscEnd = indexMiscStart;
        for (ItemSlot slot : newSlots) {
            miscItems.addBetween(slot, indexMiscStart, indexMiscEnd);
            indexMiscEnd++;
        }
    }

    /**
     * Merge with another group, by taking all of their slots and placing them in this group.
     */
    public void mergeWithOtherGroup(SortingGroup otherGroup) {
        while(otherGroup.size() > 0)
            this.addSlots(otherGroup.takeNextList());
    }

    /**
     * Take all items from this group, in the form of an ordered list.
     */
    public abstract List<ItemSlot> takeAllItems();

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
    public abstract List<ItemSlot> takeNextList();

    /**
     * Takes up to {@code numSlotsToTake} items, from the first lists on.
     */
    public abstract List<ItemSlot> takeFirstSlots(int numSlotsToTake, boolean splitUpSubgroups);

    /**
     * Looks through the group's sorted lists to find a match of exactly numSlotsToTake items, or returns an empty list.
     */
    public abstract List<ItemSlot> tryTakingExactNumSlots(int numSlotsToTake, boolean withTool, boolean startFromEnd);

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
