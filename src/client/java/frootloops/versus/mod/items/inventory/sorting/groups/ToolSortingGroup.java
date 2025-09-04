package frootloops.versus.mod.items.inventory.sorting.groups;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.items.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items.inventory.sorting.ItemType;
import frootloops.versus.mod.items.inventory.sorting.lists.SortedItemList;

import java.util.Arrays;
import java.util.LinkedList;

import static frootloops.versus.mod.items.inventory.sorting.InventorySortingHelper.DEBUG_SORTING_GROUPS;


public class ToolSortingGroup extends SimpleSortingGroup {

    private SortedItemList sortedToolList;

    public ToolSortingGroup(SortedItemList sortedToolList, SortedItemList[] sortedItemLists, String name) {
        super(sortedItemLists, name);
        this.sortedToolList = sortedToolList;
    }

    protected int numTools = 0;

    @Override
    public ItemType getItemType() {
        if(sortedToolList.size() > 0) return sortedToolList.getMainItemTypeOfList();
        return super.getItemType();
    }

    public ItemSlot tryInsertingSlot(ItemSlot slot) {
        if(sortedToolList.trySortedInsert(slot) != -1) {
            if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("                   -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + "'s tools");
            this.numTools++;
            return null; // Inserted!
        }
        return super.tryInsertingSlot(slot);
    }

    public void addSlot(ItemSlot slot) {
        slot = this.tryInsertingSlot(slot);
        if(slot != null) {
            if(slot.isToolOrWeapon()) {
                if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("                   -> Forcibly adding " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + "'s tools");
                sortedToolList.add(slot);
                this.numTools++;
            }
            else super.addSlot(slot);
        }
    }

    public boolean canGiveawayTools() {
        if(numTools > 0 && numItems < 1) return true;
        if(numTools < 2) return false;

        // Check if giving away tools would ruin a perfect row of 9. If so, cancel:
        int sizeFirst, sizeSecond;
        for(int i = 0; i < sortedItemLists.length; i++) {
            sizeFirst = sortedItemLists[i].size() % 9;
            if(sizeFirst == 0) continue;

            for(int j = i; i < sortedItemLists.length; i++) {
                sizeSecond = sortedItemLists[j].size() % 9;
                if(sizeSecond != 0 && sizeFirst + sizeSecond == 8) {
                    return false;
                }
            }
        }
        return true;
    }

    public int getNumTools() {
        return numTools;
    }

    public boolean hasOnlyTools() {
        return numTools > 0 && this.size() == numTools;
    }

    public ItemSlot takeWorstTool() {
        if(numTools < 1) return null;
        this.numTools--;
        return sortedToolList.removeLast();
    }

    private LinkedList<ItemSlot> takeTools(int count) {
        if(numTools < 1 || count == 0) return new LinkedList<>();
        if(numTools <= count) return this.takeAllTools();
        this.numTools -= count;
        return sortedToolList.take(count);
    }

    public LinkedList<ItemSlot> takeAllTools() {
        this.numTools = 0;
        return sortedToolList.takeAll();
    }

    public LinkedList<ItemSlot> takeAllItems() {
        return this.mergeAndTakeAllItems(true);
    }

    private LinkedList<ItemSlot> mergeAndTakeAllItems(boolean includeTools) {
        LinkedList<ItemSlot> items = new LinkedList<>();
        if(this.size() == 0) {
            if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("                 " + this.GROUP_NAME + " - takeAllItems() - Returned nothing, thought it had size zero.");
            if(DEBUG_SORTING_GROUPS) this.debugCalculateActualSize();
            return items;
        }

        if(includeTools && numTools > 0) {
            items.addAll(this.takeAllTools());
            if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("                 " + this.GROUP_NAME + " - takeAllItems() - Took " + items.size() + " tools: " + items);
        }

        for(int i = 1; i < sortedItemLists.length; i++) sortedItemLists[i].giveFirstSlotsTo(Integer.MAX_VALUE, sortedItemLists[0]);
        if(this.miscItems.size() > 0) miscItems.giveFirstSlotsTo(Integer.MAX_VALUE, sortedItemLists[0]);
        if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("                 " + this.GROUP_NAME + " - takeAllItems() - Took " + sortedItemLists[0].size() + " items: " + sortedItemLists[0]);
        this.numItems = 0;
        items.addAll(sortedItemLists[0].takeAll());
        return items;
    }

    public void tryFormingRows() {
        int sizeFirst, sizeSecond;
        for(int i = 0; i < sortedItemLists.length; i++) {
            sizeFirst = sortedItemLists[i].size() % 9;
            if(sizeFirst == 0) continue;

            for(int j = i; i < sortedItemLists.length; i++) {
                sizeSecond = sortedItemLists[j].size() % 9;
                if(sizeSecond == 0) continue;

                int combinedSize = sizeFirst + sizeSecond;
                if(combinedSize == 9) sortedItemLists[j].giveFirstSlotsTo(sizeSecond, sortedItemLists[i]);
                else if(combinedSize < 9 && 9 - combinedSize <= this.numTools) {
                    sortedItemLists[j].giveFirstSlotsTo(sizeSecond, sortedItemLists[i]);
                    sortedToolList.giveLastSlotsTo(9 - combinedSize,  sortedItemLists[i]);
                    this.numTools = sortedToolList.size();
                }
            }
        }
    }

    /**
     * Take all items from the biggest list currently in the group.
     */
    @Override
    public int getNextListSize() {
        if(this.size() == 0) return 0;
        if(this.sortedToolList.size() > 0) return this.sortedToolList.size();
        for(SortedItemList list : this.sortedItemLists) if(list.size() > 0) return list.size();
        return miscItems.size();
    }


    /**
     * Take all items from the biggest list currently in the group.
     *
     * @param numSlotsToTake
     */
    @Override
    public LinkedList<ItemSlot> takeFirstSlots(int numSlotsToTake, boolean splitUpSubGroups) {
        if(numSlotsToTake < 1 || this.size() < 1) {
            if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("[ ITEM SORTING ] " + this.GROUP_NAME + " - TakeFirstSlots(" + numSlotsToTake + ", " + splitUpSubGroups + ") - Group was empty!! Has " + this.sortedToolList.size() + " tools and " + Arrays.stream(this.sortedItemLists).mapToInt(SortedItemList::size) + " items");
            return new LinkedList<>();
        }

        // If there's enough to fill the quota when combining both tools and others, send both:
        numSlotsToTake = Math.min(this.size(), numSlotsToTake);
        if(this.numItems + this.numTools == numSlotsToTake) {
            if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("[ ITEM SORTING ] " + this.GROUP_NAME + " - TakeFirstSlots(" + numSlotsToTake + ", " + splitUpSubGroups + ") - Returning everything.");
            return this.mergeAndTakeAllItems(true);
        }

        if(this.numItems < numSlotsToTake && this.numItems + this.numTools >= numSlotsToTake) {
            if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("[ ITEM SORTING ]  " + this.GROUP_NAME + " - TakeFirstSlots(" + numSlotsToTake+", " + splitUpSubGroups + ") - Returning " + this.numItems + " items and " + this.numTools + " tools");
            LinkedList<ItemSlot> slotList = this.mergeAndTakeAllItems(false);
            slotList.addAll(0, this.sortedToolList.take(numSlotsToTake - this.numItems));
            this.numTools = sortedToolList.size();
            return slotList;
        }

        LinkedList<ItemSlot> slotList = new LinkedList<>();
        int numToolsTaken = 0, numItemssTaken = 0;

        // If has tools, try to include at least one, especially if not enough other blocks:
        if(sortedToolList.size() > 1) {
            slotList.addAll(sortedToolList.take(Math.clamp(numSlotsToTake - numItems, 1, sortedToolList.size())));
            numToolsTaken = slotList.size();
            this.numTools -= numToolsTaken;
            if(numToolsTaken >= numSlotsToTake) {
                if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("                 " + this.GROUP_NAME + " - TakeFirstSlots(" + numSlotsToTake+", " + splitUpSubGroups + ") - Returning " + numToolsTaken + " tools");
                return slotList;
            }
        }

        // Fill with otherItems:
        for(int i = 0; i < sortedItemLists.length; i++) {
            if(sortedItemLists[i].size() > 0 && (splitUpSubGroups || sortedItemLists[i].size() <= numSlotsToTake - numToolsTaken - numItemssTaken)) {
                slotList.addAll(sortedItemLists[i].take(Math.min(sortedItemLists[i].size(), numSlotsToTake - numToolsTaken - numItemssTaken)));
                numItemssTaken = slotList.size() - numToolsTaken;
                if(numItemssTaken + numToolsTaken >= numSlotsToTake) {
                    if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("                 " + this.GROUP_NAME + " - TakeFirstSlots(" + numSlotsToTake+", " + splitUpSubGroups + ") - Returning " + numItemssTaken + " items and " + numToolsTaken + " tools");
                    this.numItems -= numItemssTaken;
                    return slotList;
                }
            }
        }

        // Finally, if still some slots left, fill with misc:
        if(miscItems.size() > 0) {
            slotList.addAll(miscItems.take(numSlotsToTake - numToolsTaken - numItemssTaken));
            numItemssTaken = slotList.size() - numToolsTaken;
        }

        if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("                 " + this.GROUP_NAME + " - TakeFirstSlots(" + numSlotsToTake+", " + splitUpSubGroups + ") - Returning " + numItemssTaken + " items (misc included) and " + numToolsTaken + " tools");
        this.numItems -= numItemssTaken;
        return slotList;
    }

    @Override
    public LinkedList<ItemSlot> tryTakingExactNumSlots(int numSlotsToTake, boolean withTool, boolean startFromEnd) {
        if(!withTool) return super.tryTakingExactNumSlots(numSlotsToTake, false, startFromEnd);

        if(this.size() == 0) return new LinkedList<>();
        withTool = this.numTools > 0;

        // First pass: trivial cases
        //  - Check if we should return nothing but the tools:
        if(withTool && numItems == 0 && numTools == numSlotsToTake) return this.takeAllTools();

        //  - Check if we should return nothing but the other items:
        if(!withTool && this.numItems == numSlotsToTake) return this.mergeAndTakeAllItems(false);

        //  - Finally, if we can return everything, tools and others included:
        if(withTool && this.numItems + this.numTools == numSlotsToTake) return this.mergeAndTakeAllItems(true);
        if(withTool && this.numItems < numSlotsToTake && this.numItems + this.numTools > numSlotsToTake) {
            LinkedList<ItemSlot> slotList = this.mergeAndTakeAllItems(false);
            slotList.addAll(this.takeTools(numSlotsToTake - this.numItems));
            return slotList;
        }

        // Second pass: if there's a perfect match with a single, return it
        final int startIndex = startFromEnd ? sortedItemLists.length - 1 : 0;
        final int endIndex = startFromEnd ? 0 : sortedItemLists.length - 1;
        final int increment = startFromEnd ? -1 : 1;

        for(int i = startIndex; i != endIndex; i += increment) {
            if(sortedItemLists[i].size() == 0) continue;
            int sizeOfList = sortedItemLists[i].size();
            if (withTool && sizeOfList % 9 < numSlotsToTake && (sizeOfList % 9) + this.numTools >= numSlotsToTake) {
                LinkedList<ItemSlot> slotList = sortedItemLists[i].takeAll();
                slotList.addAll(this.takeTools(numSlotsToTake - sizeOfList));
                if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("                 " + this.GROUP_NAME + " - TakeFirstSlots(" + numSlotsToTake+", " + withTool + ", " + startFromEnd + ") - Quick return of " + (sizeOfList) + " items and " + (numSlotsToTake - sizeOfList) + " tools");
                return slotList;
            }
            else if (sizeOfList == numSlotsToTake) {
                this.numItems -= numSlotsToTake;
                if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("                 " + this.GROUP_NAME + " - TakeFirstSlots(" + numSlotsToTake+", " + withTool + ", " + startFromEnd + ") -  Quick return of  " + (sizeOfList) + " items, found a perfect match!");
                return sortedItemLists[i].take(numSlotsToTake);
            }
        }

        // Fourth and final pass: find two lists that, when combined, equal num slots to take
        LinkedList<ItemSlot> slotList;
        int sizeFirst, sizeSecond;
        for(int i = startIndex; i != endIndex; i += increment) {
            sizeFirst = sortedItemLists[i].size() % 9;
            if(sizeFirst == 0) continue;

            for(int j = i; j != endIndex; j += increment) {
                sizeSecond = sortedItemLists[j].size() % 9;
                if(sizeSecond == 0) continue;

                int combinedSize = sizeFirst + sizeSecond;
                if(combinedSize == numSlotsToTake) {
                    slotList = new LinkedList<>();
                    slotList.addAll(sortedItemLists[i].take(sizeFirst));
                    slotList.addAll(sortedItemLists[j].take(sizeSecond));
                    this.numItems -= combinedSize;
                    return slotList;
                }
                else if(combinedSize < numSlotsToTake && numSlotsToTake - combinedSize <= this.numTools) {
                    slotList = new LinkedList<>();
                    slotList.addAll(this.takeTools(numSlotsToTake - combinedSize));
                    slotList.addAll(sortedItemLists[i].take(sizeFirst));
                    slotList.addAll(sortedItemLists[j].take(sizeSecond));
                    this.numItems -= combinedSize;
                    return slotList;
                }
            }
        }
        return new LinkedList<>();
    }

    @Override
    public void clear() {
        if(this.size() != 0 || this.sortedToolList.size() != 0) VersusMod.MOD_LOGGER.error("[ ITEM SORTING ERROR ] " + this.GROUP_NAME + " - Still had " + this.size() + " items left when clear() was run. (" + this.sortedToolList.size() + " tools" + (this.sortedToolList.size() == 0 ? "" : ": " + this.sortedToolList.toString()) + ")");
        this.sortedToolList.clear();
        for(SortedItemList list: sortedItemLists) list.clear();
        numTools = 0;
        numItems = 0;
    }

    public int size() {return this.numTools + this.numItems;}

    @Override
    protected int debugCalculateActualSize() {
        int realNumItems = miscItems.size() + sortedToolList.size();
        for(SortedItemList list: sortedItemLists) realNumItems += list.size();
        if(this.size() != realNumItems) VersusMod.MOD_LOGGER.error("                 " + this.GROUP_NAME + " - Thinks it has " + this.size() + " items (" + this.numTools + " tools, " + this.numItems + " other, " + this.miscItems.size() + " misc), but in reality has " + realNumItems);
        return realNumItems;
    }

    @Override
    public String toString() {
        if(this.size() == 0) return "\n              [ SORTED " + this.GROUP_NAME + " ] -> Empty!";
        String output = "\n              [ SORTED " + this.GROUP_NAME + " ] (" +this.numTools+ " Tools, " +this.numItems+ " Items)" ;
        if(sortedToolList.size() > 0) output += "\n                Tool List: " + this.sortedToolList;
        output += "\n";
        for(int i = 0; i < this.sortedItemLists.length; i++) {
            if(sortedItemLists[i].size() > 0) output += "                Item List " + i + ": " + this.sortedItemLists[i] + "\n";
        }
        if(this.miscItems.size() > 0) output += "              Other Items List: " + this.miscItems + "\n";
        return output;
    }

    @Override
    public int getMaxNumRows() {
        int numRows = super.getMaxNumRows();
        return (numTools > 0) ? numRows + 1 + numTools/9 : numRows;
    }

    @Override
    public LinkedList<ItemSlot> takeNextList() {
        if(this.size() == 0) return new LinkedList<>();
        if(this.numTools > 0) {
            LinkedList<ItemSlot> slotsTaken = this.takeAllTools();
            if(slotsTaken.size() + super.getNextListSize() <= 9) slotsTaken.addAll(super.takeNextList());
            return slotsTaken;
        }
        return super.takeNextList();
    }
}
