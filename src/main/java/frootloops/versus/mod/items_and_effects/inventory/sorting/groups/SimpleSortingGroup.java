package frootloops.versus.mod.items_and_effects.inventory.sorting.groups;

import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemOrdering;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemType;
import frootloops.versus.mod.items_and_effects.inventory.sorting.SortingDebug;
import frootloops.versus.mod.items_and_effects.inventory.sorting.lists.SortedItemList;

import java.util.ArrayList;
import java.util.List;

public class SimpleSortingGroup extends SortingGroup {

    protected final SortedItemList[] sortedItemLists;

    public SimpleSortingGroup(SortedItemList[] sortedItemLists, String name) {
        super(name);
        this.sortedItemLists = sortedItemLists;
    }

    /**
     * The stacks in the sorted lists and the misc list, tools aside.
     */
    protected int numItems() {
        int numItems = this.miscItems.size();
        for(SortedItemList list : sortedItemLists) numItems += list.size();
        return numItems;
    }

    @Override
    public int size() {
        return this.numItems();
    }

    @Override
    public ItemType getItemType() {
        for(SortedItemList sortedList: sortedItemLists) {
            if(sortedList.size() > 0) return sortedList.getMainItemTypeOfList();
        }
        return ItemType.MISC;
    }

    @Override
    public int getMaxNumRows() {
        int numRows = this.miscItems.size() > 0 ? 1 + this.miscItems.size()/9 : 0;
        for(SortedItemList sortedList: sortedItemLists)
            if(sortedList.size() > 0) numRows += 1 + sortedList.size()/9;
        return numRows;
    }

    @Override
    public ItemSlot tryInsertingSlot(ItemSlot slot) {
        for(SortedItemList sortedList: sortedItemLists) {
            if (sortedList.trySortedInsert(slot) != -1) {
                SortingDebug.log(() -> "                   -> " + this.GROUP_NAME + ": Inserting " + slot + " of type " + slot.itemType() + " into list: " + sortedList);
                return null; // Inserted!
            }
        }
        return slot;
    }

    @Override
    public int getNextListSize() {
        if(this.size() == 0) return 0;
        for(SortedItemList list : this.sortedItemLists) if(list.size() > 0) return list.size();
        return 0;
    }

    @Override
    public List<ItemSlot> takeNextList() {
        for(SortedItemList list : this.sortedItemLists) {
            if(list.size() > 0) return list.takeAll();
        }
        if(this.miscItems.size() > 0) return miscItems.takeAll();
        return new ArrayList<>();
    }

    @Override
    public void addSlot(ItemSlot slot) {
        ItemSlot slotLeft = this.tryInsertingSlot(slot);
        if(slotLeft != null) {
            if(slotLeft.isToolOrWeapon()) sortedItemLists[0].add(slotLeft);
            else this.miscItems.add(slotLeft);
            SortingDebug.log(() -> "                   -> Forcibly adding " + slotLeft + " into " + this.GROUP_NAME);
        }
    }

    @Override
    public List<ItemSlot> takeAllItems() {
        if(this.size() == 0) return new ArrayList<>();
        for(int i = 1; i < sortedItemLists.length; i++) sortedItemLists[0].appendListToEnd(sortedItemLists[i]);
        if(this.miscItems.size() > 0) {
            if(sortedItemLists[0].size() > 0 && ItemOrdering.shouldGoBefore(miscItems.getSlot(0), sortedItemLists[0].getSlot(0), false, true, false)) {
                sortedItemLists[0].addListToStart(miscItems);
            }
            else sortedItemLists[0].appendListToEnd(miscItems);
        }
        SortingDebug.log(() -> "                 " + this.GROUP_NAME + " - takeAllItems() - Took " + sortedItemLists[0].size() + " items: " + sortedItemLists[0]);
        return sortedItemLists[0].takeAll();
    }

    @Override
    public void tryFormingRows() {
        for(int i = 0; i < sortedItemLists.length; i++) {
            int sizeFirst = sortedItemLists[i].size() % 9;
            for(int j = i + 1; j < sortedItemLists.length && sizeFirst != 0; j++) {
                int sizeSecond = sortedItemLists[j].size() % 9;
                if(sizeSecond == 0) continue;
                if(sizeFirst + sizeSecond == 9) sortedItemLists[j].giveFirstSlotsTo(sizeSecond, sortedItemLists[i]);
                sizeFirst = sortedItemLists[i].size() % 9;
            }
        }
    }

    @Override
    public List<ItemSlot> takeFirstSlots(int numSlotsToTake, boolean splitUpSubgroups) {
        if(numSlotsToTake == 0 || this.size() == 0) return new ArrayList<>();
        List<ItemSlot> slotList = new ArrayList<>();
        int numItemsTaken = 0;
        for(SortedItemList list : sortedItemLists) {
            if(list.size() > 0 && (splitUpSubgroups || list.size() <= numSlotsToTake - numItemsTaken)) {
                int numItemsToTakeFromList = Math.min(list.size(), numSlotsToTake - numItemsTaken);
                if(numItemsToTakeFromList < 1) continue;
                slotList.addAll(list.take(numItemsToTakeFromList));
                numItemsTaken = slotList.size();
                if(numItemsTaken >= numSlotsToTake) return slotList;
            }
        }

        // Finally, if still some slots left, fill with misc:
        if(miscItems.size() > 0) slotList.addAll(miscItems.take(numSlotsToTake - numItemsTaken));
        SortingDebug.log(() -> "                 " + this.GROUP_NAME + " - takeFirstSlots(" + numSlotsToTake + ", " + splitUpSubgroups + ") - Took " + slotList.size() + " items, misc included");
        return slotList;
    }

    @Override
    public List<ItemSlot> tryTakingExactNumSlots(int numSlotsToTake, boolean withTools, boolean startFromEnd) {
        int numItems = this.numItems();
        if(numItems == 0) return new ArrayList<>();

        // First pass: trivial cases
        //  - Check if we should return nothing but the other items:
        if(numItems == numSlotsToTake) return this.takeAllItems();

        // Second pass: if there's a perfect match with a single, return it
        final int startIndex = startFromEnd ? sortedItemLists.length - 1 : 0;
        final int endIndex = startFromEnd ? -1 : sortedItemLists.length;
        final int increment = startFromEnd ? -1 : 1;
        for(int i = startIndex; i != endIndex; i += increment) {
            int sizeOfList = sortedItemLists[i].size() % 9;
            if (sizeOfList == numSlotsToTake) {
                SortingDebug.log(() -> "                 " + this.GROUP_NAME + " - tryTakingExactNumSlots(" + numSlotsToTake + ") - Found a perfect match");
                return sortedItemLists[i].take(numSlotsToTake);
            }
        }
        if(this.miscItems.size() == numSlotsToTake) return this.miscItems.takeAll();

        // Third and final pass: find two lists that, when combined, equal num slots to take
        for(int i = startIndex; i != endIndex; i += increment) {
            int sizeFirst = sortedItemLists[i].size() % 9;
            if(sizeFirst == 0) continue;
            for(int j = i; j != endIndex; j += increment) {
                int sizeSecond = sortedItemLists[j].size() % 9;
                if(sizeSecond == 0) continue;
                if(sizeFirst + sizeSecond == numSlotsToTake) {
                    List<ItemSlot> slotList = new ArrayList<>();
                    slotList.addAll(sortedItemLists[i].take(sizeFirst));
                    slotList.addAll(sortedItemLists[j].take(sizeSecond));
                    SortingDebug.log(() -> "                 " + this.GROUP_NAME + " - tryTakingExactNumSlots(" + numSlotsToTake + ") - Found a combination of two lists");
                    return slotList;
                }
            }
        }
        return new ArrayList<>();
    }

    @Override
    public String toString() {
        if(this.size() == 0) return "\n              [ SORTED " + this.GROUP_NAME + " ] -> Empty!";
        StringBuilder output = new StringBuilder("\n              [ SORTED " + this.GROUP_NAME + " ] (" + this.numItems() + " Items) -> Max Rows:  " + this.getMaxNumRows() + "\n");
        for(int i = 0; i < this.sortedItemLists.length; i++) {
            if(sortedItemLists[i].size() > 0) output.append("                List ").append(i).append(": ").append(this.sortedItemLists[i]).append("\n");
        }
        if(this.miscItems.size() > 0) output.append("                Other Items List: ").append(this.miscItems).append("\n");
        return output.toString();
    }
}
