package frootloops.versus.mod.items_and_effects.inventory.sorting.groups;

import frootloops.versus.mod.items_and_effects.inventory.sorting.SortingDebug;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemComparaisonHelper;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemType;
import frootloops.versus.mod.items_and_effects.inventory.sorting.lists.SortedItemList;

import java.util.LinkedList;



public class SimpleSortingGroup extends SortingGroup {
    protected SortedItemList[] sortedItemLists;

    public SimpleSortingGroup(SortedItemList[] sortedItemLists, String name) {
        super(name);
        this.sortedItemLists = sortedItemLists;
    }

    protected int numItems = 0;

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
            int insertPos = sortedList.trySortedInsert(slot);
            if (insertPos != -1) {
                this.numItems++;
                if(SortingDebug.ENABLED) {
                    VersusMod.MOD_LOGGER.warn("                   -> " + this.GROUP_NAME + ": Inserting " + slot.stack().getHoverName().getString() + " of type " + slot.itemType() + " into list: " + sortedList);
                }
                return null; // Inserted!
            }
        }
        return slot;
    }

    /**
     * Take all items from the biggest list currently in the group.
     */
    @Override
    public int getNextListSize() {
        if(this.size() == 0) return 0;
        for(SortedItemList list : this.sortedItemLists) if(list.size() > 0) return list.size();
        return 0;
    }

    @Override
    public LinkedList<ItemSlot> takeNextList() {
        if(this.size() == 0 && this.size() == this.debugCalculateActualSize()) return new LinkedList<>();
        for(SortedItemList list : this.sortedItemLists) {
            if(list.size() > 0) {
                this.numItems -= list.size();
                return list.takeAll();
            }
        }
        if(this.miscItems.size() > 0) {
            this.numItems -= this.miscItems.size();
            return miscItems.takeAll();
        }
        return new LinkedList<>();
    }

    @Override
    public void addSlot(ItemSlot slot) {
        slot = this.tryInsertingSlot(slot);
        if(slot != null) {
            if(slot.isToolOrWeapon()) sortedItemLists[0].add(slot);
            else this.miscItems.add(slot);
            this.numItems++;
            if(SortingDebug.ENABLED) VersusMod.MOD_LOGGER.warn("                   -> Forcibly adding " + slot.stack().getHoverName().getString() + " into " + this.GROUP_NAME + "");
            if(SortingDebug.ENABLED) this.debugCalculateActualSize();
        }
    }

    @Override
    protected void addSlotsToMisc(LinkedList<ItemSlot> newSlots) {
        this.numItems += newSlots.size();
        super.addSlotsToMisc(newSlots);
    }

    @Override
    public LinkedList<ItemSlot> takeAllItems() {
        if(this.size() == 0 && SortingDebug.ENABLED) VersusMod.MOD_LOGGER.warn("                 " + this.GROUP_NAME + " - takeAllItems() - Returned nothing, thought it had size zero.");
        if(SortingDebug.ENABLED) this.debugCalculateActualSize();
        if(this.size() == 0) return new LinkedList<>();
        for(int i = 1; i < sortedItemLists.length; i++) sortedItemLists[0].appendListToEnd(sortedItemLists[i]);
        if(this.miscItems.size() > 0) {
            if(sortedItemLists[0].size() > 0 && ItemComparaisonHelper.shouldGoBefore(miscItems.getSlot(0), sortedItemLists[0].getSlot(0), false, true, false)) {
                sortedItemLists[0].addListToStart(miscItems);
            }
            else sortedItemLists[0].appendListToEnd(miscItems);
        }
        if(SortingDebug.ENABLED) VersusMod.MOD_LOGGER.warn("                 " + this.GROUP_NAME + " - takeAllItems() - Took " + sortedItemLists[0].size() + " items: " + sortedItemLists[0]);
        this.numItems = 0;
        return sortedItemLists[0].takeAll();
    }

    @Override
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
            }
        }
    }

    /**
     * Take all items from the biggest list currently in the group.
     *
     * @param numSlotsToTake
     */
    @Override
    public LinkedList<ItemSlot> takeFirstSlots(int numSlotsToTake, boolean splitUpSubgroups) {
        if(numSlotsToTake == 0 || this.size() == 0) return new LinkedList<>();;

        LinkedList<ItemSlot> slotList = new LinkedList<>();
        int numItemsTaken = 0;
        for(int i = 0; i < sortedItemLists.length; i++) {
            if(sortedItemLists[i].size() > 0 && (splitUpSubgroups || sortedItemLists[i].size() <= numSlotsToTake - numItemsTaken)) {
                int numItemsToTakeFromList = Math.min(sortedItemLists[i].size(), numSlotsToTake - numItemsTaken);
                if( numItemsToTakeFromList < 1) continue;
                if( SortingDebug.ENABLED) VersusMod.MOD_LOGGER.warn("                 " + this.GROUP_NAME + " - takeFirstSlots(" + numSlotsToTake + ", " + splitUpSubgroups + ") - Current Size: " + this.numItems + " - Taking " +numItemsToTakeFromList + " items from: " + sortedItemLists[i].toString());
                slotList.addAll(sortedItemLists[i].take(numItemsToTakeFromList));

                numItemsTaken = slotList.size();
                if(numItemsTaken >= numSlotsToTake) {
                    this.numItems -= numItemsTaken;
                    if(SortingDebug.ENABLED) VersusMod.MOD_LOGGER.warn("                 " + this.GROUP_NAME + " - takeFirstSlots(" + numSlotsToTake + ", " + splitUpSubgroups + ") - Quick return, took " + numItemsTaken + " items");
                    if(SortingDebug.ENABLED) this.debugCalculateActualSize();
                    return slotList;
                }
            }
        }

        // Finally, if still some slots left, fill with misc:
        if(miscItems.size() > 0) {
            slotList.addAll(miscItems.take(numSlotsToTake - numItemsTaken));
            numItemsTaken = slotList.size();
        }

        this.numItems -= numItemsTaken;
        if(SortingDebug.ENABLED) {
            VersusMod.MOD_LOGGER.warn("                 " + this.GROUP_NAME + "   takeFirstSlots(" + numSlotsToTake + ", " + splitUpSubgroups + ") - Took " + numItemsTaken + " items, including possibly misc (actual list is of size " + slotList.size() + ")");
            if(this.debugCalculateActualSize() != this.numItems) {
                String namesItemsTaken = "";
                for (ItemSlot slot:slotList) namesItemsTaken += slot.stack().getHoverName().getString() + ", ";
                VersusMod.MOD_LOGGER.error("                                     slotList: " + namesItemsTaken);
            }
        }
        return slotList;
    }

    public LinkedList<ItemSlot> tryTakingExactNumSlots(int numSlotsToTake, boolean withTools, boolean startFromEnd) {
        if(this.numItems == 0) return null;

        // First pass: trivial cases
        //  - Check if we should return nothing but the other items:
        if(this.numItems == numSlotsToTake) return this.takeAllItems();

        // Second pass: if there's a perfect match with a single, return it
        final int startIndex = startFromEnd ? sortedItemLists.length - 1 : 0;
        final int endIndex = startFromEnd ? -1 : sortedItemLists.length;
        final int increment = startFromEnd ? -1 : 1;

        for(int i = startIndex; i != endIndex; i += increment) {
            int sizeOfList = sortedItemLists[i].size() % 9;
            if (sizeOfList == numSlotsToTake) {
                this.numItems -= numSlotsToTake;
                if(SortingDebug.ENABLED) VersusMod.MOD_LOGGER.warn("                 " + this.GROUP_NAME + " - tryTakingExactNumSlots(" + numSlotsToTake + ", " + withTools + ", " + startFromEnd + ") - Found perfect match, taking " + numSlotsToTake + " items (had " + (this.numItems + numSlotsToTake) + ")");
                if(SortingDebug.ENABLED) this.debugCalculateActualSize();
                return sortedItemLists[i].take(numSlotsToTake);
            }
        }
        if(this.miscItems.size() == numSlotsToTake) return this.miscItems.takeAll();

        // Third and final pass: find two lists that, when combined, equal num slots to take
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
                    if(SortingDebug.ENABLED) VersusMod.MOD_LOGGER.warn("                 " + this.GROUP_NAME + " - tryTakingExactNumSlots(" + numSlotsToTake + ", " + withTools + ", " + startFromEnd + ") - Found combination of two groups, taking " + sizeFirst + " and " + sizeSecond + " items (had " + (this.numItems + combinedSize) + ")");
                    if(SortingDebug.ENABLED) this.debugCalculateActualSize();
                    return slotList;
                }
            }
        }
        return null;
    }

    @Override
    public void clear() {
        if(this.size() != 0) VersusMod.MOD_LOGGER.error("[ ITEM SORTING ] " + this.GROUP_NAME + " - Still had " + this.size() + " items left when clear() was run.");
        for(SortedItemList list: sortedItemLists) list.clear();
        this.numItems = 0;
    }

    @Override
    public int size() {
        return this.numItems;
    }

    public int recalculateActualSize() {
        int realNumItems = miscItems.size();
        for(SortedItemList list: sortedItemLists) realNumItems += list.size();
        return this.numItems = realNumItems;
    }

    protected int debugCalculateActualSize() {
        int realNumItems = miscItems.size();
        for(SortedItemList list: sortedItemLists) realNumItems += list.size();
        if(this.size() != realNumItems) VersusMod.MOD_LOGGER.error("                " + this.GROUP_NAME + " - Thinks it has " + this.size() + " items, but in reality has " + realNumItems);
        return realNumItems;
    }

    @Override
    public String toString() {
        if(this.size() == 0) return "\n              [ SORTED " + this.GROUP_NAME + " ] -> Empty!";
        String output = "\n              [ SORTED " + this.GROUP_NAME + " ] (" +this.numItems+ " Items) -> Max Rows:  " + this.getMaxNumRows() + "\n";
        for(int i = 0; i < this.sortedItemLists.length; i++) {
            if(sortedItemLists[i].size() > 0) output += "                List " + i + ": " + this.sortedItemLists[i].toString() + "\n";
        }
        if(this.miscItems.size() > 0) output += "                Other Items List: " + this.miscItems.toString() + "\n";
        return output;
    }
}
