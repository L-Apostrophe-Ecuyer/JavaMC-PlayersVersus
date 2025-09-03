package frootloops.versus.mod.items.inventory.sorting.lists;

import frootloops.versus.mod.items.inventory.sorting.ItemComparaisonHelper;
import frootloops.versus.mod.items.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items.inventory.sorting.ItemType;
import net.minecraft.item.Item;

import java.util.Map;

public class SortedMappedItemList extends SortedItemList {
    protected final Map<Item, Integer> itemIndexMap;

    public SortedMappedItemList(Map<Item, Integer> itemIndexMap, ItemType type) {
        this.itemIndexMap = itemIndexMap;
        this.itemType = type;
    }

    @Override
    public int trySortedInsert(ItemSlot newSlot) {
        int indexInMap = this.itemIndexMap != null ? this.itemIndexMap.getOrDefault(newSlot.stack().getItem(), -1) : -1;
        if(indexInMap == -1) return -1;

        // If the item currently in the list isn't ranked as well in the map, then insert before:
        for(int i = 0; i < this.size(); i++) {
            if(indexInMap <= this.itemIndexMap.getOrDefault(slots.get(i).stack().getItem(), Integer.MAX_VALUE)) {
                slots.add(i, newSlot);
                return i;
            }
        }
        slots.add(newSlot); // Add to end of the list
        return this.size() - 1;
    }
}
