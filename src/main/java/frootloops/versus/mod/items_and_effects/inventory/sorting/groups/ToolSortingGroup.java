package frootloops.versus.mod.items_and_effects.inventory.sorting.groups;

import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemType;
import frootloops.versus.mod.items_and_effects.inventory.sorting.SortingDebug;
import frootloops.versus.mod.items_and_effects.inventory.sorting.lists.SortedItemList;

import java.util.ArrayList;
import java.util.List;

/**
 * A group led by its tools, such as pickaxes, then the stacks those tools are for.
 */
public class ToolSortingGroup extends SimpleSortingGroup {

    private final SortedItemList sortedToolList;

    public ToolSortingGroup(SortedItemList sortedToolList, SortedItemList[] sortedItemLists, String name) {
        super(sortedItemLists, name);
        this.sortedToolList = sortedToolList;
    }

    @Override
    public int size() {
        return this.getNumTools() + this.numItems();
    }

    public int getNumTools() {
        return this.sortedToolList.size();
    }

    @Override
    public ItemType getItemType() {
        if(sortedToolList.size() > 0) return sortedToolList.getMainItemTypeOfList();
        return super.getItemType();
    }

    @Override
    public ItemSlot tryInsertingSlot(ItemSlot slot) {
        if(sortedToolList.trySortedInsert(slot) != -1) {
            SortingDebug.log(() -> "                   -> Inserting " + slot + " into " + this.GROUP_NAME + "'s tools");
            return null; // Inserted!
        }
        return super.tryInsertingSlot(slot);
    }

    @Override
    public void addSlot(ItemSlot slot) {
        ItemSlot slotLeft = this.tryInsertingSlot(slot);
        if(slotLeft != null) {
            if(slotLeft.isToolOrWeapon()) {
                SortingDebug.log(() -> "                   -> Forcibly adding " + slotLeft + " into " + this.GROUP_NAME + "'s tools");
                sortedToolList.add(slotLeft);
            }
            else super.addSlot(slotLeft);
        }
    }

    public boolean canGiveawayTools() {
        int numTools = this.getNumTools();
        if(numTools > 0 && this.numItems() < 1) return true;
        return numTools > 1;
    }

    public boolean hasOnlyTools() {
        return this.getNumTools() > 0 && this.numItems() == 0;
    }

    public ItemSlot takeWorstTool() {
        if(this.getNumTools() < 1) return null;
        return sortedToolList.removeLast();
    }

    public ItemSlot takeBestTool() {
        if(this.getNumTools() < 1) return null;
        return sortedToolList.removeFirst();
    }

    private List<ItemSlot> takeTools(int count) {
        if(this.getNumTools() < 1 || count == 0) return new ArrayList<>();
        if(this.getNumTools() <= count) return this.takeAllTools();
        return sortedToolList.take(count);
    }

    public List<ItemSlot> takeAllTools() {
        return sortedToolList.takeAll();
    }

    @Override
    public List<ItemSlot> takeAllItems() {
        return this.mergeAndTakeAllItems(true);
    }

    private List<ItemSlot> mergeAndTakeAllItems(boolean includeTools) {
        List<ItemSlot> items = new ArrayList<>();
        if(this.size() == 0) return items;
        if(includeTools && this.getNumTools() > 0) items.addAll(this.takeAllTools());
        for(int i = 1; i < sortedItemLists.length; i++) sortedItemLists[i].giveFirstSlotsTo(Integer.MAX_VALUE, sortedItemLists[0]);
        if(this.miscItems.size() > 0) miscItems.giveFirstSlotsTo(Integer.MAX_VALUE, sortedItemLists[0]);
        SortingDebug.log(() -> "                 " + this.GROUP_NAME + " - takeAllItems() - Took " + items.size() + " tools and " + sortedItemLists[0].size() + " items: " + sortedItemLists[0]);
        items.addAll(sortedItemLists[0].takeAll());
        return items;
    }

    @Override
    public void tryFormingRows() {
        for(int i = 0; i < sortedItemLists.length; i++) {
            int sizeFirst = sortedItemLists[i].size() % 9;
            for(int j = i + 1; j < sortedItemLists.length && sizeFirst != 0; j++) {
                int sizeSecond = sortedItemLists[j].size() % 9;
                if(sizeSecond == 0) continue;
                int combinedSize = sizeFirst + sizeSecond;
                if(combinedSize == 9) sortedItemLists[j].giveFirstSlotsTo(sizeSecond, sortedItemLists[i]);
                else if(combinedSize < 9 && 9 - combinedSize <= this.getNumTools()) {
                    sortedItemLists[j].giveFirstSlotsTo(sizeSecond, sortedItemLists[i]);
                    sortedToolList.giveLastSlotsTo(9 - combinedSize, sortedItemLists[i]);
                }
                sizeFirst = sortedItemLists[i].size() % 9;
            }
        }
    }

    @Override
    public int getNextListSize() {
        if(this.size() == 0) return 0;
        if(this.sortedToolList.size() > 0) return this.sortedToolList.size();
        for(SortedItemList list : this.sortedItemLists) if(list.size() > 0) return list.size();
        return miscItems.size();
    }

    @Override
    public List<ItemSlot> takeFirstSlots(int numSlotsToTake, boolean splitUpSubGroups) {
        return this.takeFirstSlots(numSlotsToTake, splitUpSubGroups, false);
    }

    public List<ItemSlot> takeFirstSlots(int numSlotsToTake, final boolean splitUpSubGroups, final boolean prioritizeNonTools) {
        if(numSlotsToTake < 1 || this.size() < 1) return new ArrayList<>();

        // If there's enough to fill the quota when combining both tools and others, send both:
        numSlotsToTake = Math.min(this.size(), numSlotsToTake);
        int numItems = this.numItems();
        int numTools = this.getNumTools();
        if(numItems + numTools == numSlotsToTake) return this.mergeAndTakeAllItems(true);
        if(numItems < numSlotsToTake) {
            List<ItemSlot> slotList = this.mergeAndTakeAllItems(false);
            slotList.addAll(0, this.sortedToolList.take(numSlotsToTake - numItems));
            return slotList;
        }

        List<ItemSlot> slotList = new ArrayList<>();
        int numToolsTaken = 0, numItemsTaken = 0;

        // If has tools, try to include at least one, especially if not enough other blocks:
        int maxToolsToTake = prioritizeNonTools ? numSlotsToTake - numItems : Math.max(Math.min(numSlotsToTake - numItems, numTools), 1);
        if(maxToolsToTake > 0 && numTools > 1) {
            slotList.addAll(sortedToolList.take(maxToolsToTake));
            numToolsTaken = slotList.size();
            if(numToolsTaken >= numSlotsToTake) return slotList;
        }

        // Fill with otherItems:
        for(SortedItemList list : sortedItemLists) {
            if(list.size() > 0 && (splitUpSubGroups || list.size() <= numSlotsToTake - numToolsTaken - numItemsTaken)) {
                slotList.addAll(list.take(Math.min(list.size(), numSlotsToTake - numToolsTaken - numItemsTaken)));
                numItemsTaken = slotList.size() - numToolsTaken;
                if(numItemsTaken + numToolsTaken >= numSlotsToTake) return slotList;
            }
        }

        // Finally, if still some slots left, fill with misc:
        if(miscItems.size() > 0) slotList.addAll(miscItems.take(numSlotsToTake - numToolsTaken - numItemsTaken));
        return slotList;
    }

    @Override
    public List<ItemSlot> tryTakingExactNumSlots(int numSlotsToTake, boolean withTool, boolean startFromEnd) {
        if(!withTool) return super.tryTakingExactNumSlots(numSlotsToTake, false, startFromEnd);
        if(this.size() == 0) return new ArrayList<>();
        int numItems = this.numItems();
        int numTools = this.getNumTools();
        withTool = numTools > 0;

        // First pass: trivial cases
        //  - Check if we should return nothing but the tools:
        if(withTool && numItems == 0 && numTools == numSlotsToTake) return this.takeAllTools();
        //  - Check if we should return nothing but the other items:
        if(!withTool && numItems == numSlotsToTake) return this.mergeAndTakeAllItems(false);
        //  - Finally, if we can return everything, tools and others included:
        if(withTool && numItems + numTools == numSlotsToTake) return this.mergeAndTakeAllItems(true);
        if(withTool && numItems < numSlotsToTake && numItems + numTools > numSlotsToTake) {
            List<ItemSlot> slotList = this.mergeAndTakeAllItems(false);
            slotList.addAll(this.takeTools(numSlotsToTake - numItems));
            return slotList;
        }

        // Second pass: if there's a perfect match with a single, return it
        final int startIndex = startFromEnd ? sortedItemLists.length - 1 : 0;
        final int endIndex = startFromEnd ? 0 : sortedItemLists.length - 1;
        final int increment = startFromEnd ? -1 : 1;
        for(int i = startIndex; i != endIndex; i += increment) {
            int sizeOfList = sortedItemLists[i].size();
            if(sizeOfList == 0) continue;
            if (withTool && sizeOfList + this.getNumTools() == numSlotsToTake) {
                List<ItemSlot> slotList = sortedItemLists[i].takeAll();
                slotList.addAll(this.takeTools(numSlotsToTake - sizeOfList));
                return slotList;
            }
            else if (sizeOfList == numSlotsToTake) {
                return sortedItemLists[i].take(numSlotsToTake);
            }
        }

        // Fourth and final pass: find two lists that, when combined, equal num slots to take
        for(int i = startIndex; i != endIndex; i += increment) {
            int sizeFirst = sortedItemLists[i].size() % 9;
            if(sizeFirst == 0) continue;
            if(sizeFirst + this.getNumTools() == numSlotsToTake) {
                List<ItemSlot> slotList = new ArrayList<>(sortedToolList.takeAll());
                slotList.addAll(sortedItemLists[i].take(sizeFirst));
                return slotList;
            }
            for(int j = i; j != endIndex; j += increment) {
                int sizeSecond = sortedItemLists[j].size() % 9;
                if(sizeSecond == 0) continue;
                int combinedSize = sizeFirst + sizeSecond;
                if(combinedSize == numSlotsToTake) {
                    List<ItemSlot> slotList = new ArrayList<>(sortedItemLists[i].take(sizeFirst));
                    slotList.addAll(sortedItemLists[j].take(sizeSecond));
                    return slotList;
                }
                else if(combinedSize < numSlotsToTake && numSlotsToTake - combinedSize <= this.getNumTools()) {
                    List<ItemSlot> slotList = new ArrayList<>(this.takeTools(numSlotsToTake - combinedSize));
                    slotList.addAll(sortedItemLists[i].take(sizeFirst));
                    slotList.addAll(sortedItemLists[j].take(sizeSecond));
                    return slotList;
                }
            }
        }
        return new ArrayList<>();
    }

    @Override
    public String toString() {
        if(this.size() == 0) return "\n              [ SORTED " + this.GROUP_NAME + " ] -> Empty!";
        StringBuilder output = new StringBuilder("\n              [ SORTED " + this.GROUP_NAME + " ] (" + this.getNumTools() + " Tools, " + this.numItems() + " Items) - Max Rows:" + this.getMaxNumRows());
        if(sortedToolList.size() > 0) output.append("\n                Tool List: ").append(this.sortedToolList);
        output.append("\n");
        for(int i = 0; i < this.sortedItemLists.length; i++) {
            if(sortedItemLists[i].size() > 0) output.append("                Item List ").append(i).append(": ").append(this.sortedItemLists[i]).append("\n");
        }
        if(this.miscItems.size() > 0) output.append("              Other Items List: ").append(this.miscItems).append("\n");
        return output.toString();
    }

    @Override
    public int getMaxNumRows() {
        int numRows = super.getMaxNumRows();
        int numTools = this.getNumTools();
        return (numTools > 0) ? numRows + 1 + numTools/9 : numRows;
    }

    @Override
    public List<ItemSlot> takeNextList() {
        if(this.size() == 0) return new ArrayList<>();
        if(this.getNumTools() > 0) {
            List<ItemSlot> slotsTaken = this.takeAllTools();
            if(slotsTaken.size() + super.getNextListSize() <= 9) slotsTaken.addAll(super.takeNextList());
            return slotsTaken;
        }
        return super.takeNextList();
    }
}
