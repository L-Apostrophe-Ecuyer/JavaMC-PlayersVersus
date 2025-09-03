package frootloops.versus.mod.items.inventory.sorting.lists;

import frootloops.versus.mod.items.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items.inventory.sorting.ItemType;
import net.minecraft.item.Item;

import java.util.Map;

public class SortedMiscItemList extends SortedItemList {
    protected final Map<Item, Integer> optionalExclusionMap;

    public SortedMiscItemList(Map<Item, Integer> optionalExclusionMap) {
        this(optionalExclusionMap, ItemType.MISC);
    }

    public SortedMiscItemList(Map<Item, Integer> optionalExclusionMap, ItemType itemType) {
        this.optionalExclusionMap = optionalExclusionMap;
        this.itemType = itemType;
    }

    @Override
    public int trySortedInsert(ItemSlot newSlot) {
        if(this.optionalExclusionMap != null && (this.optionalExclusionMap.getOrDefault(newSlot.stack().getItem(), -1) == -1)) return -1;
        return this.addBetween(newSlot, 0, this.size());
    }
}
