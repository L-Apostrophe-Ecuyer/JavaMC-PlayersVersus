package frootloops.versus.mod.items.inventory;

import frootloops.versus.mod.environment.CustomBlockItems;
import frootloops.versus.mod.items.brewing.ConcentrateItem;
import net.minecraft.block.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Rarity;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ItemSortingGroups {

    public static SortedItemGroup sortedCombatGroup = new CombatItems();
    public static SortedItemGroup sortedPickaxeGroup = new PickaxeMineableItems();
    public static SortedItemGroup sortedAxeGroup = new AxeMineableItems();
    public static SortedItemGroup sortedShovelGroup = new ShovelMineableItems();
    public static SortedItemGroup sortedHoesGroup = new HoeMineableItems();
    public static SortedItemGroup sortedShearsGroup = new ShearsMineableItems();
    public static SortedItemGroup sortedRedstoneGroup = new RedstoneItems();
    public static SortedItemGroup sortedRareGroup = new RareItems();
    public static SortedItemGroup sortedMiscGroup = new MiscItems();


    public record InventorySlot(int slodId, ItemStack stack) {}
    public static abstract class SortedItemGroup {
        public LinkedList<InventorySlot> inventorySlots = new LinkedList<>();

        public boolean tryInsert(InventorySlot slot) {
            for(int i = 0; i < inventorySlots.size(); i++) {
                if(inventorySlots.get(i).slodId == slot.slodId) return true;
            }
            inventorySlots.add(slot);
            return true;
        }

        protected void insertWithinBounds(InventorySlot slot, int startIndex, int maxIndex) {
            String itemName = slot.stack.getItem().getName().getString();
            for(int i = startIndex; i < maxIndex; i++) {
                InventorySlot otherSlot = inventorySlots.get(i);
                if(otherSlot.slodId == slot.slodId) return;
                if(otherSlot.stack.isOf(slot.stack.getItem()) && otherSlot.stack.getCount() > slot.stack.getCount()) continue;
                if(itemName.compareTo(otherSlot.stack.getItem().getName().getString()) < 1) {
                    inventorySlots.add(i, slot);
                    return;
                }
            }
            inventorySlots.add(maxIndex, slot);
        }

        protected void insertToolWithinBounds(InventorySlot slot, int startIndex, int maxIndex) {
            ItemStack otherStack = null, stack = slot.stack;
            int otherDurability = 0, durability = stack.getMaxDamage() - stack.getDamage();

            String itemName = stack.getName().getString();
            for(int i = startIndex; i < maxIndex; i++) {
                otherStack = inventorySlots.get(i).stack;
                otherDurability = otherStack.getMaxDamage() - otherStack.getDamage();
                if(durability > otherDurability) {
                    inventorySlots.add(i, slot);
                    return;
                }
                else if(durability == otherDurability && itemName.compareTo(otherStack.getItem().getName().getString()) < 1) {
                    inventorySlots.add(i, slot);
                    return;
                }
            }
            inventorySlots.add(maxIndex, slot);
        }

        private void forceInsert(InventorySlot slot) {
            if(!this.tryInsert(slot)) inventorySlots.add(slot);
        }

        public int size() {
            return inventorySlots.size();
        }

        public InventorySlot popFirst() {
            return inventorySlots.removeFirst();
        }

        public InventorySlot popLast() {
            return inventorySlots.removeLast();
        }

        public void merge(SortedItemGroup other) {
            inventorySlots.addAll(other.inventorySlots);
            other.clear();
        }

        public void clear() {
            inventorySlots.clear();
        }

        public void giveExtrasTo(SortedItemGroup other) {
            while(this.size() > 9) {
                other.forceInsert(this.popLast());
            }
        }
    }


    /**
     * REDSTONE
     */
    public static class RedstoneItems extends SortedItemGroup {
        @Override
        public boolean tryInsert(InventorySlot slot) {
            Item item = slot.stack.getItem();
            if(ItemSortingGroups.ITEMS_REDSTONE.containsKey(item)) {
                for(int i = 0; i < inventorySlots.size(); i++) {
                    if(ITEMS_REDSTONE.get(item) <= ITEMS_REDSTONE.getOrDefault(this.inventorySlots.get(i).stack.getItem(), 256)) {
                        this.inventorySlots.add(i, slot);
                        return true;
                    }
                }
                this.inventorySlots.add(slot);
                return true;
            }
            return false;
        }
    }


    /**
     * PICKAXES
     */
    public static class PickaxeMineableItems extends SortedItemGroup {
        int indexPickaxesEnd = 0;
        int indexSortedStonesEnd = 0;
        int indexSortedCopperEnd = 0;

        @Override
        public boolean tryInsert(InventorySlot slot) {
            Item item = slot.stack.getItem();
            if(item instanceof PickaxeItem) {
                this.insertToolWithinBounds(slot, 0, indexPickaxesEnd);
                indexPickaxesEnd++;
                indexSortedStonesEnd++;
                indexSortedCopperEnd++;
                return true;
            }
            else if(item instanceof BlockItem blockItem) {
                if(blockItem.getBlock().getDefaultState().isIn(BlockTags.PICKAXE_MINEABLE)) {
                    if(ItemSortingGroups.ITEMS_PICKAXE_MINEABLE.containsKey(item)) {
                        for(int i = indexPickaxesEnd + 1; i < indexSortedStonesEnd; i++) {
                            if(ITEMS_PICKAXE_MINEABLE.get(item) <= ITEMS_PICKAXE_MINEABLE.getOrDefault(this.inventorySlots.get(i).stack.getItem(), 256)) {
                                while(i < inventorySlots.size() && item == this.inventorySlots.get(i).stack.getItem() && i < indexSortedStonesEnd) {
                                    if(slot.stack.getCount() < this.inventorySlots.get(i).stack.getCount()) i++;
                                    else break;
                                }

                                this.inventorySlots.add(i, slot);
                                indexSortedStonesEnd++;
                                indexSortedCopperEnd++;
                                return true;
                            }
                        }
                        this.inventorySlots.add(indexSortedStonesEnd, slot);
                        indexSortedStonesEnd++;
                        indexSortedCopperEnd++;
                        return true;
                    }
                    else if(ItemSortingGroups.ITEMS_COPPER_BLOCKS.containsKey(item)) {
                        for(int i = indexSortedStonesEnd + 1; i < indexSortedCopperEnd; i++) {
                            if(ITEMS_COPPER_BLOCKS.get(item) <= ITEMS_COPPER_BLOCKS.get(this.inventorySlots.get(i).stack.getItem())) {
                                while(i < inventorySlots.size() && item == this.inventorySlots.get(i).stack.getItem() && i < indexSortedCopperEnd) {
                                    if(slot.stack.getCount() < this.inventorySlots.get(i).stack.getCount()) i++;
                                    else break;
                                }

                                this.inventorySlots.add(i, slot);
                                indexSortedCopperEnd++;
                                return true;
                            }
                        }
                        this.inventorySlots.add(indexSortedCopperEnd, slot);
                        indexSortedCopperEnd++;
                        return true;
                    }
                    else {
                        this.insertWithinBounds(slot, indexSortedCopperEnd + 1, this.inventorySlots.size());
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            indexPickaxesEnd = 0;
            indexSortedStonesEnd = 0;
            indexSortedCopperEnd = 0;
            inventorySlots.clear();
        }
    }


    /**
     * HOES
     */
    public static class HoeMineableItems extends SortedItemGroup {
        int indexHoesEnd = 0;

        @Override
        public boolean tryInsert(InventorySlot slot) {
            Item item = slot.stack.getItem();
            if(item instanceof HoeItem) {
                this.insertToolWithinBounds(slot, 0, indexHoesEnd);
                indexHoesEnd++;
                return true;
            }
            else if(item instanceof BlockItem blockItem) {
                if(blockItem.getBlock().getDefaultState().isIn(BlockTags.HOE_MINEABLE)) {
                    this.insertWithinBounds(slot, indexHoesEnd + 1, this.inventorySlots.size());
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            indexHoesEnd = 0;
            inventorySlots.clear();
        }
    }


    /**
     * SHEARS
     */
    public static class ShearsMineableItems extends SortedItemGroup {
        int indexShearsEnd = 0;
        int indexCobwebEnd = 0;
        int indexWoolEnd = 0;
        int indexLeavesEnd = 0;

        @Override
        public boolean tryInsert(InventorySlot slot) {
            Item item = slot.stack.getItem();
            if(item instanceof ShearsItem) {
                this.insertToolWithinBounds(slot, 0, indexShearsEnd);
                indexShearsEnd++;
                indexCobwebEnd++;
                indexWoolEnd++;
                indexLeavesEnd++;
                return true;
            }
            else if(item instanceof BlockItem blockItem) {
                BlockState state = blockItem.getBlock().getDefaultState();
                if(state.isOf(Blocks.COBWEB)) {
                    this.insertWithinBounds(slot, indexShearsEnd + 1, indexCobwebEnd);
                    indexCobwebEnd++;
                    indexWoolEnd++;
                    indexLeavesEnd++;
                    return true;
                }
                else if(state.isIn(BlockTags.WOOL)) {
                    this.insertWithinBounds(slot, indexCobwebEnd + 1, indexWoolEnd);
                    indexWoolEnd++;
                    indexLeavesEnd++;
                    return true;
                }
                else if(state.isIn(BlockTags.LEAVES)) {
                    this.insertWithinBounds(slot, indexWoolEnd + 1,indexLeavesEnd);
                    indexLeavesEnd++;
                    return true;
                }
                else if(blockItem.getBlock() instanceof PlantBlock){
                    this.insertWithinBounds(slot, indexLeavesEnd + 1, this.inventorySlots.size());
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            indexShearsEnd = 0;
            indexWoolEnd = 0;
            indexLeavesEnd = 0;
            inventorySlots.clear();
        }
    }


    /**
     * SHOVELS
     */
    public static class ShovelMineableItems extends SortedItemGroup {
        int indexShovelsEnd = 0;
        int indexBlocksEnd = 0;

        @Override
        public boolean tryInsert(InventorySlot slot) {
            Item item = slot.stack.getItem();
            if(item instanceof ShovelItem) {
                this.insertToolWithinBounds(slot, 0, indexShovelsEnd);
                indexShovelsEnd++;
                indexBlocksEnd++;
                return true;
            }
            else if(item instanceof BlockItem blockItem) {
                if(blockItem.getBlock().getDefaultState().isIn(BlockTags.SHOVEL_MINEABLE)) {
                    this.insertWithinBounds(slot, indexShovelsEnd + 1, indexBlocksEnd);
                    indexBlocksEnd++;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            indexShovelsEnd = 0;
            indexBlocksEnd = 0;
            inventorySlots.clear();
        }
    }


    /**
     * AXES
     */
    public static class AxeMineableItems extends SortedItemGroup {
        int indexAxesEnd = 0;
        int indexPlanksEnd = 0;
        int indexLogsEnd = 0;
        int indexSlabsAndStairsEnd = 0;

        @Override
        public boolean tryInsert(InventorySlot slot) {
            Item item = slot.stack.getItem();
            if(item instanceof AxeItem) {
                this.insertToolWithinBounds(slot, 0, indexAxesEnd);
                indexAxesEnd++;
                indexPlanksEnd++;
                indexLogsEnd++;
                indexSlabsAndStairsEnd++;
                return true;
            }
            else if(item instanceof BlockItem blockItem) {
                if(blockItem.getBlock().getDefaultState().isIn(BlockTags.AXE_MINEABLE)) {
                    if(item.getTranslationKey().endsWith("planks")) {
                        this.insertWithinBounds(slot, indexAxesEnd + 1, indexPlanksEnd);
                        indexPlanksEnd++;
                        indexLogsEnd++;
                        indexSlabsAndStairsEnd++;
                        return true;
                    }
                    else if(item.getTranslationKey().endsWith("log") || item.getTranslationKey().endsWith("wood")) {
                        this.insertWithinBounds(slot, indexPlanksEnd + 1, indexLogsEnd);
                        indexLogsEnd++;
                        indexSlabsAndStairsEnd++;
                        return true;
                    }
                    else if(blockItem.getBlock() instanceof StairsBlock || blockItem.getBlock() instanceof SlabBlock) {
                        this.insertWithinBounds(slot, indexLogsEnd + 1, indexSlabsAndStairsEnd);
                        indexSlabsAndStairsEnd++;
                        return true;
                    }
                    else {
                        this.insertWithinBounds(slot, indexSlabsAndStairsEnd + 1, this.size());
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void clear() {
            indexAxesEnd = 0;
            indexPlanksEnd = 0;
            indexLogsEnd = 0;
            indexSlabsAndStairsEnd = 0;
            inventorySlots.clear();
        }
    }


    /**
     * COMBAT
     */
    public static class CombatItems extends SortedItemGroup {
        int indexWeaponsEnd = 0;
        int indexShieldsAndRangedEnd = 0;
        int indexSpecialConsumables = 0;
        int indexFoods = 0;
        int indexMisc = 0;

        @Override
        public void merge(SortedItemGroup other) {
            if(other instanceof AxeMineableItems axeGroup) {
                for(int i = 0; i < axeGroup.indexAxesEnd; i++) {
                    inventorySlots.add(indexWeaponsEnd, axeGroup.popFirst());
                    indexWeaponsEnd++;
                    indexShieldsAndRangedEnd++;
                    indexSpecialConsumables++;
                    indexFoods++;
                    indexMisc++;
                }
            }
            else if(other instanceof PickaxeMineableItems pickaxeGroup) {
                for(int i = 0; i < pickaxeGroup.indexPickaxesEnd; i++) {
                    inventorySlots.add(indexWeaponsEnd, pickaxeGroup.popFirst());
                    indexWeaponsEnd++;
                    indexShieldsAndRangedEnd++;
                    indexSpecialConsumables++;
                    indexFoods++;
                    indexMisc++;
                }
            }
            else if(other instanceof HoeMineableItems hoeGroup) {
                for(int i = 0; i < hoeGroup.indexHoesEnd; i++) {
                    inventorySlots.add(indexWeaponsEnd, hoeGroup.popFirst());
                    indexWeaponsEnd++;
                    indexShieldsAndRangedEnd++;
                    indexSpecialConsumables++;
                    indexFoods++;
                    indexMisc++;
                }
            }
            else if(other instanceof ShovelMineableItems shovelGroup) {
                for(int i = 0; i < shovelGroup.indexShovelsEnd; i++) {
                    inventorySlots.add(indexWeaponsEnd, shovelGroup.popFirst());
                    indexWeaponsEnd++;
                    indexShieldsAndRangedEnd++;
                    indexSpecialConsumables++;
                    indexFoods++;
                    indexMisc++;
                }
            }
            inventorySlots.addAll(other.inventorySlots);
            other.clear();
        }

        @Override
        public boolean tryInsert(InventorySlot slot) {
            Item item = slot.stack.getItem();
            if(item instanceof SwordItem || item instanceof TridentItem || item instanceof MaceItem) {
                this.insertToolWithinBounds(slot, 0, indexWeaponsEnd);
                indexWeaponsEnd++;
                indexShieldsAndRangedEnd++;
                indexSpecialConsumables++;
                indexFoods++;
                indexMisc++;
                return true;
            }
            else if(item instanceof ShieldItem || item instanceof CrossbowItem || item instanceof BowItem) {
                this.insertToolWithinBounds(slot, indexWeaponsEnd + 1, indexShieldsAndRangedEnd);
                indexShieldsAndRangedEnd++;
                indexSpecialConsumables++;
                indexFoods++;
                indexMisc++;
                return true;
            }
            else if(item instanceof ConcentrateItem || item == Items.ROTTEN_FLESH || item == Items.SPIDER_EYE) {
                return false;
            }
            else if(item instanceof PotionItem || (item.getComponents().contains(DataComponentTypes.FOOD) && item.getComponents().get(DataComponentTypes.FOOD).effects().stream().anyMatch(statusEffectEntry -> statusEffectEntry.effect().getEffectType().value().getCategory() == StatusEffectCategory.BENEFICIAL))) {
                this.inventorySlots.add(Math.min(inventorySlots.size(), indexSpecialConsumables), slot);
                indexSpecialConsumables++;
                indexFoods++;
                indexMisc++;
                return true;
            }
            else if(item.getComponents().contains(DataComponentTypes.FOOD) && item.getComponents().get(DataComponentTypes.FOOD).nutrition() > 0) {
                this.inventorySlots.add(Math.min(inventorySlots.size(), indexFoods), slot);
                indexFoods++;
                indexMisc++;
                return true;
            }
            else if(item instanceof ProjectileItem || item instanceof FireChargeItem || item instanceof BucketItem || item instanceof ArmorItem || item == Items.TORCH || item == Items.FLINT_AND_STEEL) {
                this.inventorySlots.add(Math.min(inventorySlots.size(), indexMisc), slot);
                indexMisc++;
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            indexWeaponsEnd = 0;
            indexShieldsAndRangedEnd = 0;
            indexSpecialConsumables = 0;
            indexFoods = 0;
            indexMisc = 0;
            inventorySlots.clear();
        }
    }


    /**
     * GOODIES
     */
    public static class RareItems extends SortedItemGroup {
        int indexShulkersEnd = 0;
        int indexRessourcesEnd = 0;
        int indexConcentratesEnd = 0;

        @Override
        public boolean tryInsert(InventorySlot slot) {
            Item item = slot.stack.getItem();
            if(item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                this.insertWithinBounds(slot, 0, indexShulkersEnd);
                indexShulkersEnd++;
                indexRessourcesEnd++;
                indexConcentratesEnd++;
                return true;
            }
            else if(ItemSortingGroups.ITEMS_MINERAL_RESSOURCES.containsKey(item)) {
                for(int i = indexShulkersEnd + 1; i < indexRessourcesEnd; i++) {
                    if(ITEMS_MINERAL_RESSOURCES.get(item) <= ITEMS_MINERAL_RESSOURCES.getOrDefault(this.inventorySlots.get(i).stack.getItem(), 256)) {
                        this.inventorySlots.add(i, slot);
                        indexRessourcesEnd++;
                        indexConcentratesEnd++;
                        return true;
                    }
                }
                this.inventorySlots.add(indexRessourcesEnd, slot);
                indexRessourcesEnd++;
                indexConcentratesEnd++;
                return true;
            }
            else if(item instanceof ConcentrateItem) {
                this.insertWithinBounds(slot, indexRessourcesEnd + 1, indexConcentratesEnd);
                indexConcentratesEnd++;
                return true;
            }
            else if(item.getComponents().contains(DataComponentTypes.RARITY) && !item.getComponents().contains(DataComponentTypes.FOOD) && slot.stack.getOrDefault(DataComponentTypes.RARITY, Rarity.COMMON) != Rarity.COMMON) {
                this.insertWithinBounds(slot, indexConcentratesEnd + 1, this.size());
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            indexShulkersEnd = 0;
            indexConcentratesEnd = 0;
            indexRessourcesEnd = 0;
            inventorySlots.clear();
        }
    }


    /**
     * MISC
     */
    public static class MiscItems extends SortedItemGroup {
        int indexStorageEnd = 0;
        int indexToolsEnd = 0;
        int indexSingleStacksEnd = 0;
        int indexMonsterDropsEnd = 0;
        int indexIngredientsEnd = 0;
        int indexNatural = 0;
        int indexMisc = 0;

        @Override
        public boolean tryInsert(InventorySlot slot) {
            Item item = slot.stack.getItem();
            if(item instanceof BundleItem) {
                this.inventorySlots.add(0, slot);
                indexStorageEnd++;
                indexToolsEnd++;
                indexSingleStacksEnd++;
                indexIngredientsEnd++;
                indexMonsterDropsEnd++;
                indexNatural++;
                indexMisc++;
                return true;
            }
            else if(item.getComponents().contains(DataComponentTypes.DAMAGE)) {
                this.insertWithinBounds(slot, indexStorageEnd + 1, indexToolsEnd);
                indexToolsEnd++;
                indexSingleStacksEnd++;
                indexIngredientsEnd++;
                indexMonsterDropsEnd++;
                indexNatural++;
                indexMisc++;
                return true;
            }
            else if(item.getMaxCount() == 1) {
                this.insertWithinBounds(slot, indexToolsEnd + 1, indexSingleStacksEnd);
                indexSingleStacksEnd++;
                indexIngredientsEnd++;
                indexMonsterDropsEnd++;
                indexNatural++;
                indexMisc++;
                return true;
            }
            else if(item == Items.STICK || item == Items.PAPER || item == Items.BOOK || item == Items.SUGAR  || item == Items.SLIME_BALL) {
                this.insertWithinBounds(slot, indexSingleStacksEnd + 1, indexIngredientsEnd);
                indexIngredientsEnd++;
                indexMonsterDropsEnd++;
                indexNatural++;
                indexMisc++;
                return true;
            }
            else if(item == Items.GUNPOWDER ||  item == Items.SPIDER_EYE || item == Items.ROTTEN_FLESH || item == Items.BONE || item == Items.BONE_MEAL) {
                this.insertWithinBounds(slot, indexSingleStacksEnd + 1, indexMonsterDropsEnd);
                indexMonsterDropsEnd++;
                indexIngredientsEnd++;
                indexNatural++;
                indexMisc++;
                return true;
            }
            else if(item instanceof BlockItem blockItem && blockItem.getBlock().getDefaultState().hasRandomTicks()) {
                this.inventorySlots.add(Math.min(inventorySlots.size(), indexNatural), slot);
                indexNatural++;
                indexMisc++;
                return true;
            }
            else {
                this.inventorySlots.add(Math.min(inventorySlots.size(), indexMisc), slot);
                indexMisc++;
                return true;
            }
        }

        @Override
        public void clear() {
            indexStorageEnd = 0;
            indexToolsEnd = 0;
            indexSingleStacksEnd = 0;
            indexMonsterDropsEnd = 0;
            indexIngredientsEnd = 0;
            indexNatural = 0;
            indexMisc = 0;
            inventorySlots.clear();
        }
    }

    private static final Map<Item, Integer> ITEMS_REDSTONE = new HashMap<>();
    static {
        int indexRedstone = 1;
        ITEMS_REDSTONE.put(Items.TNT, indexRedstone++);
        ITEMS_REDSTONE.put(Items.REDSTONE, indexRedstone++);
        ITEMS_REDSTONE.put(Items.REDSTONE_BLOCK, indexRedstone++);
        ITEMS_REDSTONE.put(Items.REDSTONE_TORCH, indexRedstone++);
        ITEMS_REDSTONE.put(Items.TARGET, indexRedstone++);
        ITEMS_REDSTONE.put(Items.REPEATER, indexRedstone++);
        ITEMS_REDSTONE.put(Items.COMPARATOR, indexRedstone++);
        ITEMS_REDSTONE.put(Items.OBSERVER, indexRedstone++);
        ITEMS_REDSTONE.put(Items.PISTON, indexRedstone++);
        ITEMS_REDSTONE.put(Items.STICKY_PISTON, indexRedstone++);
        ITEMS_REDSTONE.put(Items.SLIME_BLOCK, indexRedstone++);
        ITEMS_REDSTONE.put(Items.HONEY_BLOCK, indexRedstone++);
        ITEMS_REDSTONE.put(Items.DISPENSER, indexRedstone++);
        ITEMS_REDSTONE.put(Items.DROPPER, indexRedstone++);
        ITEMS_REDSTONE.put(Items.HOPPER, indexRedstone++);
        ITEMS_REDSTONE.put(Items.CRAFTER, indexRedstone++);
        ITEMS_REDSTONE.put(Items.CHEST, indexRedstone++);
        ITEMS_REDSTONE.put(Items.TRAPPED_CHEST, indexRedstone++);
        ITEMS_REDSTONE.put(Items.BARREL, indexRedstone++);
        ITEMS_REDSTONE.put(Items.NOTE_BLOCK, indexRedstone++);
        ITEMS_REDSTONE.put(Items.COMPOSTER, indexRedstone++);
        ITEMS_REDSTONE.put(Items.CAULDRON, indexRedstone++);
        ITEMS_REDSTONE.put(Items.BREWING_STAND, indexRedstone++);
        ITEMS_REDSTONE.put(Items.RAIL, indexRedstone++);
        ITEMS_REDSTONE.put(Items.POWERED_RAIL, indexRedstone++);
        ITEMS_REDSTONE.put(Items.DETECTOR_RAIL, indexRedstone++);
        ITEMS_REDSTONE.put(Items.ACTIVATOR_RAIL, indexRedstone++);
        ITEMS_REDSTONE.put(Items.MINECART, indexRedstone++);
        ITEMS_REDSTONE.put(Items.HOPPER_MINECART, indexRedstone++);
        ITEMS_REDSTONE.put(Items.CHEST_MINECART, indexRedstone++);
        ITEMS_REDSTONE.put(Items.FURNACE_MINECART, indexRedstone++);
        ITEMS_REDSTONE.put(Items.TNT_MINECART, indexRedstone++);
        ITEMS_REDSTONE.put(Items.WAXED_COPPER_BULB, indexRedstone++);
        ITEMS_REDSTONE.put(Items.WAXED_EXPOSED_COPPER_BULB, indexRedstone++);
        ITEMS_REDSTONE.put(Items.WAXED_WEATHERED_COPPER_BULB, indexRedstone++);
        ITEMS_REDSTONE.put(Items.WAXED_OXIDIZED_COPPER_BULB, indexRedstone++);
        ITEMS_REDSTONE.put(Items.LEVER, indexRedstone++);
        ITEMS_REDSTONE.put(Items.STONE_PRESSURE_PLATE, indexRedstone++);
        ITEMS_REDSTONE.put(Items.SCULK_SENSOR, indexRedstone++);
        ITEMS_REDSTONE.put(Items.CALIBRATED_SCULK_SENSOR, indexRedstone++);
    }

    private static final Map<Item, Integer> ITEMS_PICKAXE_MINEABLE = new HashMap<>();
    static {

        int indexPickaxeBlocks = 1;
        ITEMS_PICKAXE_MINEABLE.put(Items.NETHERRACK, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.RED_NETHER_BRICKS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.RED_NETHER_BRICK_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.RED_NETHER_BRICK_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.NETHER_BRICKS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.NETHER_BRICK_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.NETHER_BRICK_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.NETHER_BRICK_FENCE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_BASALT, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_BLACKSTONE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_BLACKSTONE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_BLACKSTONE_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_BLACKSTONE_WALL, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_BLACKSTONE_BRICKS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_BLACKSTONE_BRICK_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_BLACKSTONE_BRICK_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_BLACKSTONE_BRICK_WALL, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.CRACKED_POLISHED_BLACKSTONE_BRICKS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.CHISELED_POLISHED_BLACKSTONE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.GILDED_BLACKSTONE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.BLACKSTONE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.BLACKSTONE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.BLACKSTONE_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.BLACKSTONE_WALL, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.SMOOTH_BASALT, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.BASALT, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.COBBLED_DEEPSLATE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.DEEPSLATE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.COBBLED_DEEPSLATE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.COBBLED_DEEPSLATE_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.COBBLED_DEEPSLATE_WALL, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.TUFF, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.TUFF_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.TUFF_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.TUFF_WALL, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.MOSSY_COBBLESTONE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.MOSSY_COBBLESTONE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.MOSSY_COBBLESTONE_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.MOSSY_COBBLESTONE_WALL, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.COBBLESTONE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.COBBLESTONE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.COBBLESTONE_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.COBBLESTONE_WALL, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.DEAD_TUBE_CORAL_BLOCK, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.DEAD_BRAIN_CORAL_BLOCK, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.DEAD_BUBBLE_CORAL_BLOCK, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.DEAD_FIRE_CORAL_BLOCK, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.DEAD_HORN_CORAL_BLOCK, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.STONE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.STONE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.STONE_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.ANDESITE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.ANDESITE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.ANDESITE_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.ANDESITE_WALL, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.DIORITE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.CALCITE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.DIORITE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.DIORITE_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.DIORITE_WALL, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.SANDSTONE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.SMOOTH_SANDSTONE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.SMOOTH_SANDSTONE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.SMOOTH_SANDSTONE_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.SANDSTONE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.SANDSTONE_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.PACKED_MUD, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.DRIPSTONE_BLOCK, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.GRANITE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.GRANITE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.GRANITE_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.GRANITE_WALL, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_GRANITE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_GRANITE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_GRANITE_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.BRICKS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.BRICK_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.BRICK_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.BRICK_WALL, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.TERRACOTTA, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.RED_SANDSTONE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.SMOOTH_RED_SANDSTONE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.SMOOTH_RED_SANDSTONE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.SMOOTH_RED_SANDSTONE_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.RED_SANDSTONE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.RED_SANDSTONE_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.ORANGE_TERRACOTTA, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.RED_TERRACOTTA, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.YELLOW_TERRACOTTA, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.WHITE_TERRACOTTA, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.LIGHT_GRAY_TERRACOTTA, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.GRAY_TERRACOTTA, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.BROWN_TERRACOTTA, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.GREEN_TERRACOTTA, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.LIME_TERRACOTTA, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.CYAN_TERRACOTTA, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.BLACK_TERRACOTTA, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.PINK_TERRACOTTA, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.MAGENTA_TERRACOTTA, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.PURPLE_TERRACOTTA, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.BLUE_TERRACOTTA, indexPickaxeBlocks++);

        ITEMS_PICKAXE_MINEABLE.put(Items.CRACKED_DEEPSLATE_TILES, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.DEEPSLATE_TILES, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.DEEPSLATE_TILE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.DEEPSLATE_TILE_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.DEEPSLATE_TILE_WALL, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_DEEPSLATE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_DEEPSLATE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_DEEPSLATE_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_DEEPSLATE_WALL, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.CRACKED_DEEPSLATE_BRICKS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.DEEPSLATE_BRICKS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.DEEPSLATE_BRICK_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.DEEPSLATE_BRICK_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.DEEPSLATE_BRICK_WALL, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.TUFF_BRICKS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.TUFF_BRICK_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.TUFF_BRICK_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.TUFF_BRICK_WALL, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.CHISELED_TUFF, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.CHISELED_TUFF_BRICKS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_TUFF, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_TUFF_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_TUFF_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_TUFF_WALL, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(CustomBlockItems.POLISHED_STONE_ITEM, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(CustomBlockItems.POLISHED_STONE_SLAB_ITEM, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(CustomBlockItems.POLISHED_STONE_STAIRS_ITEM, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_ANDESITE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_ANDESITE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_ANDESITE_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.MOSSY_STONE_BRICKS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.CRACKED_STONE_BRICKS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.STONE_BRICKS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.STONE_BRICK_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.STONE_BRICK_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.STONE_BRICK_WALL, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.CHISELED_STONE_BRICKS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_DIORITE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_DIORITE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_DIORITE_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.CUT_SANDSTONE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.CUT_SANDSTONE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.CHISELED_SANDSTONE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.CUT_RED_SANDSTONE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.CUT_RED_SANDSTONE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.CHISELED_RED_SANDSTONE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_GRANITE, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_GRANITE_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.POLISHED_GRANITE_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(CustomBlockItems.GRANITE_BRICKS_ITEM, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(CustomBlockItems.GRANITE_BRICK_SLAB_ITEM, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(CustomBlockItems.GRANITE_BRICK_STAIRS_ITEM, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(CustomBlockItems.GRANITE_TILES_ITEM, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(CustomBlockItems.GRANITE_TILES_SLAB_ITEM, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(CustomBlockItems.GRANITE_TILES_STAIRS_ITEM, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.MUD_BRICKS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.MUD_BRICK_SLAB, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.MUD_BRICK_STAIRS, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(Items.MUD_BRICK_WALL, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(CustomBlockItems.PACKED_MUD_TILES_ITEM, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(CustomBlockItems.PACKED_MUD_TILES_SLAB_ITEM, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(CustomBlockItems.PACKED_MUD_TILES_STAIRS_ITEM, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(CustomBlockItems.BROWN_MUD_BRICKS_ITEM, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(CustomBlockItems.BROWN_MUD_BRICK_SLAB_ITEM, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(CustomBlockItems.BROWN_MUD_BRICK_STAIRS_ITEM, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(CustomBlockItems.BROWN_MUD_TILES_ITEM, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(CustomBlockItems.BROWN_MUD_TILES_SLAB_ITEM, indexPickaxeBlocks++);
        ITEMS_PICKAXE_MINEABLE.put(CustomBlockItems.BROWN_MUD_TILES_STAIRS_ITEM, indexPickaxeBlocks++);
    }

    private static final Map<Item, Integer> ITEMS_SHOVEL_MINEABLE = new HashMap<>();
    static {
        int indexNaturalSoils = 1;
        ITEMS_SHOVEL_MINEABLE.put(Items.CLAY, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.GRAVEL, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.SUSPICIOUS_GRAVEL, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.SAND, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.SUSPICIOUS_SAND, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.RED_SAND, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.DIRT, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.DIRT_PATH, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.MYCELIUM, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.PODZOL, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(CustomBlockItems.BROWN_MUD_ITEM, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.FARMLAND, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.COARSE_DIRT, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.ROOTED_DIRT, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.GRASS_BLOCK, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.MOSS_BLOCK, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.MOSS_CARPET, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.MOSS_BLOCK, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.MOSS_CARPET, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.SOUL_SAND, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.SOUL_SOIL, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.SNOW_BLOCK, indexNaturalSoils++);
        ITEMS_SHOVEL_MINEABLE.put(Items.SNOW, indexNaturalSoils++);
    }

    private static final Map<Item, Integer> ITEMS_COPPER_BLOCKS = new HashMap<>();
    static {
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_CUT_COPPER, 1);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_EXPOSED_CUT_COPPER, 2);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_WEATHERED_CUT_COPPER, 3);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_OXIDIZED_CUT_COPPER, 4);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_COPPER_BLOCK, 5);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_EXPOSED_COPPER, 6);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_WEATHERED_COPPER, 7);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_OXIDIZED_COPPER, 8);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_CUT_COPPER_SLAB, 9);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_EXPOSED_CUT_COPPER_SLAB, 10);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_WEATHERED_CUT_COPPER_SLAB, 11);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_OXIDIZED_CUT_COPPER_SLAB, 12);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_CUT_COPPER_STAIRS, 13);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_EXPOSED_CUT_COPPER_STAIRS, 14);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_WEATHERED_CUT_COPPER_STAIRS, 15);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_OXIDIZED_CUT_COPPER_STAIRS, 16);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_COPPER_TRAPDOOR, 17);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_EXPOSED_COPPER_TRAPDOOR, 18);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_WEATHERED_COPPER_TRAPDOOR, 19);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_OXIDIZED_COPPER_TRAPDOOR, 20);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_COPPER_DOOR, 21);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_EXPOSED_COPPER_DOOR, 22);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_WEATHERED_COPPER_DOOR, 23);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_OXIDIZED_COPPER_DOOR, 24);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_COPPER_GRATE, 25);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_EXPOSED_COPPER_GRATE, 26);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_WEATHERED_COPPER_GRATE, 27);
        ITEMS_COPPER_BLOCKS.put(Items.WAXED_OXIDIZED_COPPER_GRATE, 28);
        ITEMS_COPPER_BLOCKS.put(Items.CUT_COPPER, 29);
        ITEMS_COPPER_BLOCKS.put(Items.EXPOSED_CUT_COPPER, 30);
        ITEMS_COPPER_BLOCKS.put(Items.WEATHERED_CUT_COPPER, 31);
        ITEMS_COPPER_BLOCKS.put(Items.OXIDIZED_CUT_COPPER, 32);
        ITEMS_COPPER_BLOCKS.put(Items.COPPER_BLOCK, 33);
        ITEMS_COPPER_BLOCKS.put(Items.EXPOSED_COPPER, 34);
        ITEMS_COPPER_BLOCKS.put(Items.WEATHERED_COPPER, 35);
        ITEMS_COPPER_BLOCKS.put(Items.OXIDIZED_COPPER, 36);
        ITEMS_COPPER_BLOCKS.put(Items.CUT_COPPER_SLAB, 37);
        ITEMS_COPPER_BLOCKS.put(Items.EXPOSED_CUT_COPPER_SLAB, 38);
        ITEMS_COPPER_BLOCKS.put(Items.WEATHERED_CUT_COPPER_SLAB, 39);
        ITEMS_COPPER_BLOCKS.put(Items.OXIDIZED_CUT_COPPER_SLAB, 40);
        ITEMS_COPPER_BLOCKS.put(Items.CUT_COPPER_STAIRS, 41);
        ITEMS_COPPER_BLOCKS.put(Items.EXPOSED_CUT_COPPER_STAIRS, 42);
        ITEMS_COPPER_BLOCKS.put(Items.WEATHERED_CUT_COPPER_STAIRS, 43);
        ITEMS_COPPER_BLOCKS.put(Items.OXIDIZED_CUT_COPPER_STAIRS, 44);
        ITEMS_COPPER_BLOCKS.put(Items.COPPER_TRAPDOOR, 45);
        ITEMS_COPPER_BLOCKS.put(Items.EXPOSED_COPPER_TRAPDOOR, 46);
        ITEMS_COPPER_BLOCKS.put(Items.WEATHERED_COPPER_TRAPDOOR, 47);
        ITEMS_COPPER_BLOCKS.put(Items.OXIDIZED_COPPER_TRAPDOOR, 48);
        ITEMS_COPPER_BLOCKS.put(Items.COPPER_DOOR, 49);
        ITEMS_COPPER_BLOCKS.put(Items.EXPOSED_COPPER_DOOR, 50);
        ITEMS_COPPER_BLOCKS.put(Items.WEATHERED_COPPER_DOOR, 51);
        ITEMS_COPPER_BLOCKS.put(Items.OXIDIZED_COPPER_DOOR, 52);
        ITEMS_COPPER_BLOCKS.put(Items.COPPER_GRATE, 53);
        ITEMS_COPPER_BLOCKS.put(Items.EXPOSED_COPPER_GRATE, 54);
        ITEMS_COPPER_BLOCKS.put(Items.WEATHERED_COPPER_GRATE, 55);
        ITEMS_COPPER_BLOCKS.put(Items.OXIDIZED_COPPER_GRATE, 56);
    }

    private static final Map<Item, Integer> ITEMS_MINERAL_RESSOURCES = new HashMap<>();
    static {
        ITEMS_MINERAL_RESSOURCES.put(Items.GOLD_BLOCK, 1);
        ITEMS_MINERAL_RESSOURCES.put(Items.GOLD_INGOT, 2);
        ITEMS_MINERAL_RESSOURCES.put(Items.GOLD_NUGGET, 3);
        ITEMS_MINERAL_RESSOURCES.put(Items.RAW_GOLD_BLOCK, 4);
        ITEMS_MINERAL_RESSOURCES.put(Items.RAW_GOLD, 5);
        ITEMS_MINERAL_RESSOURCES.put(Items.GOLD_ORE, 6);
        ITEMS_MINERAL_RESSOURCES.put(Items.DEEPSLATE_GOLD_ORE, 7);
        ITEMS_MINERAL_RESSOURCES.put(Items.NETHER_GOLD_ORE, 8);
        ITEMS_MINERAL_RESSOURCES.put(Items.IRON_BLOCK, 9);
        ITEMS_MINERAL_RESSOURCES.put(Items.IRON_INGOT, 10);
        ITEMS_MINERAL_RESSOURCES.put(Items.IRON_NUGGET, 11);
        ITEMS_MINERAL_RESSOURCES.put(Items.RAW_IRON_BLOCK, 12);
        ITEMS_MINERAL_RESSOURCES.put(Items.RAW_IRON, 13);
        ITEMS_MINERAL_RESSOURCES.put(Items.IRON_ORE, 14);
        ITEMS_MINERAL_RESSOURCES.put(Items.DEEPSLATE_IRON_ORE, 15);
        ITEMS_MINERAL_RESSOURCES.put(Items.EMERALD_BLOCK, 16);
        ITEMS_MINERAL_RESSOURCES.put(Items.EMERALD, 17);
        ITEMS_MINERAL_RESSOURCES.put(Items.EMERALD_ORE, 18);
        ITEMS_MINERAL_RESSOURCES.put(Items.DEEPSLATE_EMERALD_ORE, 19);
        ITEMS_MINERAL_RESSOURCES.put(Items.DIAMOND_BLOCK, 20);
        ITEMS_MINERAL_RESSOURCES.put(Items.DIAMOND, 21);
        ITEMS_MINERAL_RESSOURCES.put(Items.DIAMOND_ORE, 22);
        ITEMS_MINERAL_RESSOURCES.put(Items.DEEPSLATE_DIAMOND_ORE, 23);
        ITEMS_MINERAL_RESSOURCES.put(Items.LAPIS_BLOCK, 24);
        ITEMS_MINERAL_RESSOURCES.put(Items.LAPIS_LAZULI, 25);
        ITEMS_MINERAL_RESSOURCES.put(Items.LAPIS_ORE, 26);
        ITEMS_MINERAL_RESSOURCES.put(Items.DEEPSLATE_LAPIS_ORE, 27);
        ITEMS_MINERAL_RESSOURCES.put(Items.COPPER_INGOT, 28);
        ITEMS_MINERAL_RESSOURCES.put(Items.RAW_COPPER_BLOCK, 29);
        ITEMS_MINERAL_RESSOURCES.put(Items.RAW_COPPER, 30);
        ITEMS_MINERAL_RESSOURCES.put(Items.COPPER_ORE, 31);
        ITEMS_MINERAL_RESSOURCES.put(Items.DEEPSLATE_COPPER_ORE, 32);
        ITEMS_MINERAL_RESSOURCES.put(Items.AMETHYST_BLOCK, 33);
        ITEMS_MINERAL_RESSOURCES.put(Items.AMETHYST_SHARD, 34);
        ITEMS_MINERAL_RESSOURCES.put(Items.AMETHYST_CLUSTER, 35);
        ITEMS_MINERAL_RESSOURCES.put(Items.ANCIENT_DEBRIS, 36);
        ITEMS_MINERAL_RESSOURCES.put(Items.NETHERITE_SCRAP, 37);
        ITEMS_MINERAL_RESSOURCES.put(Items.NETHERITE_INGOT, 38);
        ITEMS_MINERAL_RESSOURCES.put(Items.NETHERITE_BLOCK, 39);
        ITEMS_MINERAL_RESSOURCES.put(Items.NETHER_STAR, 40);
        ITEMS_MINERAL_RESSOURCES.put(Items.BEACON, 41);
    }
}
