package frootloops.versus.mod.items.inventory.sorting.lists;

import frootloops.versus.mod.items.inventory.sorting.ItemComparaisonHelper;
import frootloops.versus.mod.items.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items.inventory.sorting.ItemType;

public class SortedTypedItemList extends SortedItemList {

    private final ItemType[] acceptableTypes;

    public SortedTypedItemList(final ItemType[] acceptableTypes) {
        this.acceptableTypes = acceptableTypes;
    }

    @Override
    public int trySortedInsert(ItemSlot newSlot) {
        for (ItemType validType:this.acceptableTypes) {
            if(newSlot.itemType() == validType) return this.addBetween(newSlot, 0, this.size());
        }
        return -1;
    }
}
