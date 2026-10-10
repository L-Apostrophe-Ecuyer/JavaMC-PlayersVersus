package frootloops.versus.mod.items_and_effects.inventory.sorting.lists;

import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemType;
import java.util.LinkedList;
import java.util.Map;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;



public class SortedBlockItemList extends SortedItemList {

    protected final TagKey<Block> blockTagKey;
    protected final SoundType blockSoundGroup;
    protected final Map<Item, Integer> blockIndexMap;

    private final float minHardness, maxHardness;

    public SortedBlockItemList(TagKey<Block> blockTagKey) {
        this(blockTagKey, null, null, -1.0f, 128.0f, ItemType.BLOCK_FULL);
    }

    public SortedBlockItemList(Map<Item, Integer> blockIndexMap, ItemType type) {
        this(null, null, blockIndexMap, -1.0f, 128.0f, type);
    }

    public SortedBlockItemList(Map<Item, Integer> blockIndexMap) {
        this(null, null, blockIndexMap, -1.0f, 128.0f, ItemType.BLOCK_FULL);
    }

    public SortedBlockItemList(TagKey<Block> blockTagKey, Map<Item, Integer> blockIndexMap) {
        this(blockTagKey, null, blockIndexMap, -1.0f, 128.0f, ItemType.BLOCK_FULL);
    }

    public SortedBlockItemList(TagKey<Block> blockTagKey, SoundType blockSoundGroup, Map<Item, Integer> blockIndexMap) {
        this(blockTagKey, blockSoundGroup, blockIndexMap, -1.0f, 128.0f, ItemType.BLOCK_FULL);
    }

    public SortedBlockItemList(SoundType blockSoundGroup, Map<Item, Integer> blockIndexMap) {
        this(null, blockSoundGroup, blockIndexMap, -1.0f, 128.0f, ItemType.BLOCK_FULL);
    }

    public SortedBlockItemList(TagKey<Block> blockTagKey, float minHardness, float maxHardness) {
        this(blockTagKey, null, null, minHardness, maxHardness, ItemType.BLOCK_FULL);
    }

    public SortedBlockItemList(TagKey<Block> blockTagKey, SoundType blockSoundGroup, Map<Item, Integer> blockIndexMap, float minHardness, float maxHardness, ItemType type) {
        this.blockTagKey = blockTagKey;
        this.blockSoundGroup = blockSoundGroup;
        this.blockIndexMap = blockIndexMap;
        this.minHardness = minHardness;
        this.maxHardness = maxHardness;
        this.itemType = type;
    }

    @Override
    public int trySortedInsert(ItemSlot newSlot) {

        // First try, if the item is ranked in the map:
        int indexInMap = this.blockIndexMap != null ? this.blockIndexMap.getOrDefault(newSlot.stack().getItem(), -1) : -1;
        if(indexInMap != -1) {
            int otherValueInMap;
            for(int i = 0; i < this.size(); i++) {

                // If the item currently in the list isn't ranked as well in the map, then insert before:
                otherValueInMap = this.blockIndexMap == null ? Integer.MAX_VALUE : this.blockIndexMap.getOrDefault(slots.get(i).stack().getItem(), Integer.MAX_VALUE);
                if(indexInMap <= otherValueInMap) {
                    slots.add(i, newSlot);
                    return i;
                }
            }
            slots.add(newSlot);
            return this.size() - 1;
        }

        // Second try, if block and matches the required BlockTag or BlockSound
        if(newSlot.stack().getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();

            // Check if block meets hardness requirements:
            if(block.defaultDestroyTime() < minHardness || block.defaultDestroyTime() > maxHardness) return -1;

            // Check if block is valid for this group:
            boolean isInBlockTag = (this.blockTagKey != null && block.defaultBlockState().is(this.blockTagKey));
            boolean isInSoundGroup = (this.blockSoundGroup != null && block.defaultBlockState().getSoundType() == this.blockSoundGroup);
            if(!isInBlockTag && !isInSoundGroup && indexInMap == -1) return -1;

            // Special case: Gravel is both pickaxe mineable and shovel mineable. Privilege should go to shovels:
            if(block == Blocks.GRAVEL && this.size() == 0 && indexInMap == -1 && this.blockTagKey == BlockTags.MINEABLE_WITH_PICKAXE) return -1;

            // Insert and return true:
            int indexMapEnd = 0, otherValueInMap;
            for(int i = 0; i < this.size(); i++) {

                // Increment indexMapEnd if the other block is in the map:
                otherValueInMap = this.blockIndexMap == null ? Integer.MAX_VALUE : this.blockIndexMap.getOrDefault(slots.get(i).stack().getItem(), Integer.MAX_VALUE);
                if(otherValueInMap != Integer.MAX_VALUE) {
                    indexMapEnd = i;
                    continue;
                }

                // If the item currently in the list somehow isn't in the block tag, then insert before:
                Item otherItem = slots.get(i).stack().getItem();
                if(!(otherItem instanceof BlockItem otherBlockItem) || ((this.blockTagKey == null || !otherBlockItem.getBlock().defaultBlockState().is(this.blockTagKey)) && otherBlockItem.getBlock().defaultBlockState().getSoundType() != this.blockSoundGroup)) {
                    this.addBetween(newSlot, indexMapEnd, i);
                    return i;
                }
            }
            return this.addBetween(newSlot, indexMapEnd, this.size());
        }
        return -1;
    }

    @Override
    public LinkedList<ItemSlot> take(int count) {
        if(this.size() == 0 || count == 0) return new LinkedList<>();
        if(count == this.size()) return this.takeAll();

        LinkedList<ItemSlot> slotsToReturn = new LinkedList<>();
        for(int i = 0; i < this.size(); i++) {
            if(slots.get(i).stack().is(Items.GRAVEL)) {
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
