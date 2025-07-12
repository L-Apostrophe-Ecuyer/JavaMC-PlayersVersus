package frootloops.versus.mod.items.inventory.sorting.lists;

import frootloops.versus.mod.items.inventory.sorting.ItemSlot;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

import java.util.LinkedList;
import java.util.Map;



public class SortedBlockItemList extends SortedItemList {

    protected final TagKey<Block> blockTagKey;
    protected final Map<Item, Integer> blockIndexMap;

    private final float minHardness, maxHardness;

    public SortedBlockItemList(TagKey<Block> blockTagKey) {
        this(blockTagKey, null, -1.0f, 128.0f);
    }

    public SortedBlockItemList(TagKey<Block> blockTagKey, Map<Item, Integer> blockIndexMap) {
        this(blockTagKey, blockIndexMap, -1.0f, 128.0f);
    }

    public SortedBlockItemList(TagKey<Block> blockTagKey, float minHardness, float maxHardness) {
        this(blockTagKey, null, minHardness, maxHardness);
    }

    public SortedBlockItemList(TagKey<Block> blockTagKey, Map<Item, Integer> blockIndexMap, float minHardness, float maxHardness) {
        this.blockTagKey = blockTagKey;
        this.blockIndexMap = blockIndexMap;
        this.minHardness = minHardness;
        this.maxHardness = maxHardness;
    }

    @Override
    public int trySortedInsert(ItemSlot newSlot) {
        if(newSlot.stack().getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();

            // Check if block meets hardness requirements:
            if(block.getHardness() < minHardness || block.getHardness() > maxHardness) return -1;

            // Check if block is valid for this group:
            boolean isInBlockTag = (this.blockTagKey != null && block.getDefaultState().isIn(this.blockTagKey));
            int indexInMap = this.blockIndexMap != null ? this.blockIndexMap.getOrDefault(blockItem, -1) : -1;
            if(!isInBlockTag && indexInMap == -1) return -1;

            // Special case: Gravel is both pickaxe mineable and shovel mineable. Privilege should go to shovels:
            if(block == Blocks.GRAVEL && this.size() == 0 && indexInMap == -1 && this.blockTagKey == BlockTags.PICKAXE_MINEABLE) return -1;

            // Insert and return true:
            int indexMapEnd = 0, otherValueInMap;
            for(int i = 0; i < this.size(); i++) {
                otherValueInMap = this.blockIndexMap == null ? Integer.MAX_VALUE : this.blockIndexMap.getOrDefault(slots.get(i).stack().getItem(), Integer.MAX_VALUE);
                if(indexInMap != -1) {
                    // If the item currently in the list isn't ranked as well in the map, then insert before:
                    if(indexInMap <= otherValueInMap) {
                        slots.add(i, newSlot);
                        return i;
                    }
                }
                else {
                    // Increment indexMapEnd if the other block is in the map:
                    if(otherValueInMap != Integer.MAX_VALUE) {
                        indexMapEnd = i;
                        continue;
                    }

                    // If the item currently in the list somehow isn't in the block tag, then insert before:
                    Item otherItem = slots.get(i).stack().getItem();
                    if(!(otherItem instanceof BlockItem otherBlockItem) || otherBlockItem.getBlock().getDefaultState().isIn(this.blockTagKey)) {
                        this.addBetween(newSlot, indexMapEnd, i, false);
                        return i;
                    }
                }
            }
            return this.addBetween(newSlot, indexMapEnd, this.size(), false);
        }
        return -1;
    }

    @Override
    public LinkedList<ItemSlot> take(int count) {
        if(this.size() == 0 || count == 0) return new LinkedList<>();
        if(count == this.size()) return this.takeAll();

        LinkedList<ItemSlot> slotsToReturn = new LinkedList<>();
        for(int i = 0; i < this.size(); i++) {
            if(slots.get(i).stack().isOf(Items.GRAVEL)) {
                slotsToReturn.add(this.slots.remove(i));
                count--;
                if(count == 0) return slotsToReturn;
            }
        }

        for(int i = 0; i < Math.min(this.size(), count); i++)
            slotsToReturn.add(this.removeLast());

        return slotsToReturn;
    }
}
