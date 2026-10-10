package frootloops.versus.mod.items_and_effects.inventory.sorting.lists;

import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class SortedTaggedItemList extends SortedItemList {

    protected final TagKey<Item> itemTagKey;

    public SortedTaggedItemList(TagKey<Item> itemTagKey, ItemType itemType) {
        this.itemTagKey = itemTagKey;
        this.itemType = itemType;
    }

    @Override
    public int trySortedInsert(ItemSlot newSlot) {
        boolean isInitemTag = (this.itemTagKey != null && newSlot.stack().is(this.itemTagKey));
        if(!isInitemTag) return -1;

        // If the item currently in the list somehow isn't in the item tag, then insert before:
        for(int i = 0; i < this.size(); i++) {
            if(!slots.get(i).stack().is(this.itemTagKey)) {
                this.addBetween(newSlot, 0, i);
                return i;
            }
        }

        // Otherwise, add in semi-sorted fashion anywhere in the list:
        this.addBetween(newSlot, 0, this.size());
        return this.size() - 1;
    }
}
