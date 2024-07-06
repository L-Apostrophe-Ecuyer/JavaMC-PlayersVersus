package frootloops.versus.mod.items.inventory;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.CustomBlockItems;
import frootloops.versus.mod.items.brewing.ConcentrateItem;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ItemSortingGroups {

    public static SortedItemGroup sortedCombatGroup = new CombatItems();
    public static SortedItemGroup sortedPickaxeGroup = new PickaxeMineableItems();
    public static SortedItemGroup sortedAxeGroup = new AxeMineableItems();
    public static SortedItemGroup sortedShovelGroup = new ShovelMineableItems();
    public static SortedItemGroup sortedRedstoneGroup = new RedstoneItems();
    public static SortedItemGroup sortedMiscGroup = new MiscItems();


    public static boolean isItemInGroup(RegistryKey<ItemGroup> itemGroupKey, Item item) {
        return Registries.ITEM_GROUP.getOrThrow(itemGroupKey).getDisplayStacks().stream().anyMatch(stack -> stack.getItem().equals(item));
    }


    public record InventorySlot(int slodId, ItemStack stack) {}
    public static abstract class SortedItemGroup {
        public LinkedList<InventorySlot> inventorySlots = new LinkedList<>();

        public boolean tryInsert(InventorySlot slot) {
            inventorySlots.add(slot);
            return true;
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

    public static class RedstoneItems extends SortedItemGroup {
        @Override
        public boolean tryInsert(InventorySlot slot) {
            Item item = slot.stack.getItem();
            if(item != Items.OAK_BUTTON && item != Items.OAK_DOOR && ItemSortingGroups.isItemInGroup(net.minecraft.item.ItemGroups.REDSTONE, item)) {
                if(inventorySlots.size() == 0) inventorySlots.add(slot);
                else {
                    for(int i = 0; i < inventorySlots.size(); i++) {
                        if(item.getName().getString().compareTo(inventorySlots.get(i).stack.getItem().getName().getString()) < 1) {
                            inventorySlots.add(Math.min(inventorySlots.size(), i), slot);
                            return true;
                        }
                    }
                    inventorySlots.add(slot);
                }
                return true;
            }
            return false;
        }
    }

    public static class PickaxeMineableItems extends SortedItemGroup {
        int indexPickaxes = 0;
        int indexBuildingBlocks = 0;
        int indexNaturalBlocks = 0;

        @Override
        public boolean tryInsert(InventorySlot slot) {
            Item item = slot.stack.getItem();
            if(item instanceof PickaxeItem) {
                this.inventorySlots.add(Math.min(inventorySlots.size(), indexPickaxes), slot);
                indexPickaxes++;
                indexBuildingBlocks++;
                indexNaturalBlocks++;
                return true;
            }
            else if(item instanceof BlockItem blockItem) {
                if(blockItem.getBlock().getDefaultState().isIn(BlockTags.PICKAXE_MINEABLE)) {
                    if(isItemInGroup(net.minecraft.item.ItemGroups.BUILDING_BLOCKS, blockItem)) {
                        for(int i = indexBuildingBlocks; i <= indexNaturalBlocks; i++) {
                            if(i == indexNaturalBlocks || item.getName().getString().compareTo(inventorySlots.get(i).stack.getItem().getName().getString()) < 1) {
                                this.inventorySlots.add(Math.min(inventorySlots.size(), i), slot);
                            }
                        }
                        indexBuildingBlocks++;
                        indexNaturalBlocks++;
                    }
                    if(isItemInGroup(net.minecraft.item.ItemGroups.NATURAL, blockItem)) {
                        this.inventorySlots.add(Math.min(inventorySlots.size(), indexNaturalBlocks), slot);
                        indexNaturalBlocks++;
                    }
                    else {
                        this.inventorySlots.add(slot);
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            indexPickaxes = 0;
            indexBuildingBlocks = 0;
            indexNaturalBlocks = 0;
            inventorySlots.clear();
        }
    }

    public static class ShovelMineableItems extends SortedItemGroup {
        int indexShovels = 0;
        int indexBlocks = 0;

        @Override
        public boolean tryInsert(InventorySlot slot) {
            Item item = slot.stack.getItem();
            if(item instanceof ShovelItem) {
                this.inventorySlots.add(Math.min(inventorySlots.size(), indexShovels), slot);
                indexShovels++;
                indexBlocks++;
                return true;
            }
            else if(item instanceof BlockItem blockItem) {
                if(blockItem.getBlock().getDefaultState().isIn(BlockTags.SHOVEL_MINEABLE)) {
                    for(int i = indexBlocks; i <= this.inventorySlots.size(); i++) {
                        if(i == this.inventorySlots.size() || item.getName().getString().compareTo(inventorySlots.get(i).stack.getItem().getName().getString()) < 1) {
                            this.inventorySlots.add(Math.min(inventorySlots.size(), i), slot);
                        }
                    }
                    indexBlocks++;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            indexShovels = 0;
            indexBlocks = 0;
            inventorySlots.clear();
        }
    }

    public static class AxeMineableItems extends SortedItemGroup {
        int indexAxes = 0;
        int indexBuildingBlocks = 0;
        int indexNaturalBlocks = 0;

        @Override
        public boolean tryInsert(InventorySlot slot) {
            Item item = slot.stack.getItem();
            if(item instanceof PickaxeItem) {
                this.inventorySlots.add(Math.min(inventorySlots.size(), indexAxes), slot);
                indexAxes++;
                indexBuildingBlocks++;
                indexNaturalBlocks++;
                return true;
            }
            else if(item instanceof BlockItem blockItem) {
                if(blockItem.getBlock().getDefaultState().isIn(BlockTags.AXE_MINEABLE)) {
                    if(isItemInGroup(net.minecraft.item.ItemGroups.BUILDING_BLOCKS, blockItem)) {
                        for(int i = indexBuildingBlocks; i <= indexNaturalBlocks; i++) {
                            if(i == indexNaturalBlocks || item.getName().getString().compareTo(inventorySlots.get(i).stack.getItem().getName().getString()) < 1) {
                                this.inventorySlots.add(Math.min(inventorySlots.size(), i), slot);
                            }
                        }
                        indexBuildingBlocks++;
                        indexNaturalBlocks++;
                    }
                    if(isItemInGroup(net.minecraft.item.ItemGroups.NATURAL, blockItem)) {
                        this.inventorySlots.add(Math.min(inventorySlots.size(), indexNaturalBlocks), slot);
                        indexNaturalBlocks++;
                    }
                    else {
                        this.inventorySlots.add(slot);
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            indexAxes = 0;
            indexBuildingBlocks = 0;
            indexNaturalBlocks = 0;
            inventorySlots.clear();
        }
    }

    public static class CombatItems extends SortedItemGroup {
        int indexWeapons = 0;
        int indexShieldsAndRanged = 0;
        int indexConsumables = 0;
        int indexFoods = 0;
        int indexMisc = 0;

        @Override
        public boolean tryInsert(InventorySlot slot) {
            Item item = slot.stack.getItem();
            if(item instanceof SwordItem || item instanceof TridentItem || item instanceof MaceItem) {
                this.inventorySlots.add(Math.min(inventorySlots.size(), indexWeapons), slot);
                indexWeapons++;
                indexShieldsAndRanged++;
                indexConsumables++;
                indexFoods++;
                indexMisc++;
                return true;
            }
            else if(item instanceof ShieldItem || item instanceof CrossbowItem || item instanceof BowItem) {
                this.inventorySlots.add(Math.min(inventorySlots.size(), indexShieldsAndRanged), slot);
                indexShieldsAndRanged++;
                indexConsumables++;
                indexFoods++;
                indexMisc++;
                return true;
            }
            else if(item instanceof ConcentrateItem || item == Items.ROTTEN_FLESH || item == Items.SPIDER_EYE) {
                return false;
            }
            else if(item instanceof PotionItem || (!(item instanceof ConcentrateItem) && item.getComponents().contains(DataComponentTypes.FOOD) && !item.getComponents().get(DataComponentTypes.FOOD).effects().stream().anyMatch(statusEffectEntry -> statusEffectEntry.effect().getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL))) {
                this.inventorySlots.add(Math.min(inventorySlots.size(), indexConsumables), slot);
                indexConsumables++;
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
            indexWeapons = 0;
            indexShieldsAndRanged = 0;
            indexConsumables = 0;
            indexFoods = 0;
            indexMisc = 0;
            inventorySlots.clear();
        }
    }

    public static class MiscItems extends SortedItemGroup {
        int indexStorage = 0;
        int indexTools = 0;
        int indexShears = 0;
        int indexWool = 0;
        int indexIron = 0;
        int indexCopper = 0;
        int indexGoldAndOther = 0;
        int indexNatural = 0;
        int indexMisc = 0;

        @Override
        public boolean tryInsert(InventorySlot slot) {
            Item item = slot.stack.getItem();
            if(item instanceof BundleItem || (item instanceof BlockItem && ((BlockItem)item).getBlock() instanceof ShulkerBoxBlock)) {
                this.inventorySlots.add(Math.min(inventorySlots.size(), indexStorage), slot);
                indexStorage++;
                indexTools++;
                indexShears++;
                indexWool++;
                indexIron++;
                indexCopper++;
                indexGoldAndOther++;
                indexNatural++;
                indexMisc++;
                return true;
            }
            else if(!(item instanceof ShearsItem) && (item.getMaxCount() == 1 ||(item.getComponents().contains(DataComponentTypes.DAMAGE)))) {
                this.inventorySlots.add(Math.min(inventorySlots.size(), indexTools), slot);
                indexTools++;
                indexShears++;
                indexWool++;
                indexIron++;
                indexCopper++;
                indexGoldAndOther++;
                indexNatural++;
                indexMisc++;
                return true;
            }
            else if(item instanceof ShearsItem) {
                this.inventorySlots.add(Math.min(inventorySlots.size(), indexShears), slot);
                indexShears++;
                indexWool++;
                indexIron++;
                indexCopper++;
                indexGoldAndOther++;
                indexNatural++;
                indexMisc++;
                return true;
            }
            else if(item instanceof BlockItem && ((BlockItem)item).getBlock().getTranslationKey().contains("wool")) {
                this.inventorySlots.add(Math.min(inventorySlots.size(), indexWool), slot);
                indexWool++;
                indexIron++;
                indexCopper++;
                indexGoldAndOther++;
                indexNatural++;
                indexMisc++;
                return true;
            }
            else if(item.getTranslationKey().contains("iron")) {
                this.inventorySlots.add(Math.min(inventorySlots.size(), indexIron), slot);
                indexIron++;
                indexCopper++;
                indexGoldAndOther++;
                indexNatural++;
                indexMisc++;
                return true;
            }
            else if(item.getTranslationKey().contains("copper")) {
                this.inventorySlots.add(Math.min(inventorySlots.size(), indexCopper), slot);
                indexCopper++;
                indexGoldAndOther++;
                indexNatural++;
                indexMisc++;
                return true;
            }
            else if(item.getTranslationKey().contains("gold") || item.getTranslationKey().contains("diamond") || item.getTranslationKey().contains("netherite") || item == Items.ANCIENT_DEBRIS) {
                this.inventorySlots.add(Math.min(inventorySlots.size(), indexGoldAndOther), slot);
                indexGoldAndOther++;
                indexNatural++;
                indexMisc++;
                return true;
            }
            else if(isItemInGroup(net.minecraft.item.ItemGroups.NATURAL, item)) {
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
            indexStorage = 0;
            indexTools = 0;
            indexShears = 0;
            indexWool = 0;
            indexIron = 0;
            indexCopper = 0;
            indexGoldAndOther = 0;
            indexNatural = 0;
            indexMisc = 0;
            inventorySlots.clear();
        }
    }

    private static final Map<Item, Integer> ITEMS_REDSTONE = new HashMap<>();
    static {
        ITEMS_REDSTONE.put(Items.TNT, 0);
        ITEMS_REDSTONE.put(Items.REDSTONE, 1);
        ITEMS_REDSTONE.put(Items.REDSTONE_BLOCK, 2);
        ITEMS_REDSTONE.put(Items.REDSTONE_TORCH, 2);
        ITEMS_REDSTONE.put(Items.TARGET, 3);
        ITEMS_REDSTONE.put(Items.REPEATER, 3);
        ITEMS_REDSTONE.put(Items.COMPARATOR, 4);
        ITEMS_REDSTONE.put(Items.OBSERVER, 5);
        ITEMS_REDSTONE.put(Items.PISTON, 6);
        ITEMS_REDSTONE.put(Items.STICKY_PISTON, 7);
        ITEMS_REDSTONE.put(Items.SLIME_BLOCK, 8);
        ITEMS_REDSTONE.put(Items.HONEY_BLOCK, 9);
        ITEMS_REDSTONE.put(Items.DISPENSER, 10);
        ITEMS_REDSTONE.put(Items.DROPPER, 11);
        ITEMS_REDSTONE.put(Items.HOPPER, 12);
        ITEMS_REDSTONE.put(Items.CRAFTER, 13);
        ITEMS_REDSTONE.put(Items.CHEST, 14);
        ITEMS_REDSTONE.put(Items.TRAPPED_CHEST, 15);
        ITEMS_REDSTONE.put(Items.BARREL, 16);
        ITEMS_REDSTONE.put(Items.NOTE_BLOCK, 17);
        ITEMS_REDSTONE.put(Items.COMPOSTER, 18);
        ITEMS_REDSTONE.put(Items.CAULDRON, 19);
        ITEMS_REDSTONE.put(Items.BREWING_STAND, 20);
        ITEMS_REDSTONE.put(Items.RAIL, 21);
        ITEMS_REDSTONE.put(Items.POWERED_RAIL, 22);
        ITEMS_REDSTONE.put(Items.DETECTOR_RAIL, 23);
        ITEMS_REDSTONE.put(Items.ACTIVATOR_RAIL, 24);
        ITEMS_REDSTONE.put(Items.MINECART, 25);
        ITEMS_REDSTONE.put(Items.HOPPER_MINECART, 26);
        ITEMS_REDSTONE.put(Items.CHEST_MINECART, 27);
        ITEMS_REDSTONE.put(Items.FURNACE_MINECART, 28);
        ITEMS_REDSTONE.put(Items.TNT_MINECART, 29);
        ITEMS_REDSTONE.put(Items.WAXED_COPPER_BULB, 30);
        ITEMS_REDSTONE.put(Items.WAXED_EXPOSED_COPPER_BULB, 31);
        ITEMS_REDSTONE.put(Items.WAXED_WEATHERED_COPPER_BULB, 32);
        ITEMS_REDSTONE.put(Items.WAXED_OXIDIZED_COPPER_BULB, 33);
        ITEMS_REDSTONE.put(Items.LEVER, 34);
        ITEMS_REDSTONE.put(Items.STONE_PRESSURE_PLATE, 35);
        ITEMS_REDSTONE.put(Items.SCULK_SENSOR, 36);
        ITEMS_REDSTONE.put(Items.CALIBRATED_SCULK_SENSOR, 37);

    }

    private static final Map<Item, Integer> ITEMS_STONES = new HashMap<>();
    static {
        ITEMS_REDSTONE.put(Items.COBBLESTONE, 1);
        ITEMS_REDSTONE.put(Items.GRANITE, 4);
        ITEMS_REDSTONE.put(Items.POLISHED_GRANITE, 4);
        ITEMS_REDSTONE.put(CustomBlockItems.GRANITE_BRICKS_ITEM, 4);
        ITEMS_REDSTONE.put(CustomBlockItems.GRANITE_TILES_ITEM, 4);
        ITEMS_REDSTONE.put(Items.POLISHED_ANDESITE, 4);
        ITEMS_REDSTONE.put(Items.STONE_BRICKS, 2);
        ITEMS_REDSTONE.put(Items.CRACKED_STONE_BRICKS, 3);
        ITEMS_REDSTONE.put(Items.MOSSY_STONE_BRICKS, 4);
        ITEMS_REDSTONE.put(Items.CHISELED_STONE_BRICKS, 3);
    }

    private static final Map<Item, Integer> ITEMS_NATURAL_STONES = new HashMap<>();
    static {
        ITEMS_REDSTONE.put(Items.SANDSTONE, 2);
        ITEMS_REDSTONE.put(Items.SMOOTH_SANDSTONE, 2);
        ITEMS_REDSTONE.put(Items.STONE, 2);
        ITEMS_REDSTONE.put(Items.DRIPSTONE_BLOCK, 2);
        ITEMS_REDSTONE.put(Items.POINTED_DRIPSTONE, 2);
        ITEMS_REDSTONE.put(Items.DRIPSTONE_BLOCK, 2);
    }

    private static final Map<Item, Integer> ITEMS_NATURAL_OTHER = new HashMap<>();
    static {
        ;
        ITEMS_NATURAL_OTHER.put(Items.MOSSY_COBBLESTONE, -10);
        ITEMS_NATURAL_OTHER.put(Items.MOSS_BLOCK, -8);
        ITEMS_NATURAL_OTHER.put(Items.MOSS_CARPET, -7);

        ITEMS_NATURAL_OTHER.put(Items.SAND, 1);
        ITEMS_NATURAL_OTHER.put(Items.GRASS_BLOCK, 2);
        ITEMS_NATURAL_OTHER.put(Items.PODZOL, 3);
        ITEMS_NATURAL_OTHER.put(Items.MYCELIUM, 4);
        ITEMS_NATURAL_OTHER.put(Items.DIRT_PATH, 5);
        ITEMS_NATURAL_OTHER.put(Items.DIRT, 6);
        ITEMS_NATURAL_OTHER.put(CustomBlockItems.BROWN_MUD_ITEM, 6);
        ITEMS_NATURAL_OTHER.put(Items.COARSE_DIRT, 8);
        ITEMS_NATURAL_OTHER.put(Items.ROOTED_DIRT, 9);
        ITEMS_NATURAL_OTHER.put(Items.FARMLAND, 10);
    }

    private static final Map<Item, Integer> ITEMS_RESSOURCES = new HashMap<>();
    static {
        ITEMS_REDSTONE.put(Items.ANDESITE, 1);
        ITEMS_REDSTONE.put(Items.STONE, 2);
        ITEMS_REDSTONE.put(Items.REDSTONE_ORE, 2);
        ITEMS_REDSTONE.put(Items.COPPER_ORE, 3);
        ITEMS_REDSTONE.put(Items.IRON_ORE, 3);
        ITEMS_REDSTONE.put(Items.GOLD_ORE, 4);
        ITEMS_REDSTONE.put(Items.EMERALD_ORE, 5);
        ITEMS_REDSTONE.put(Items.PISTON, 6);
        ITEMS_REDSTONE.put(Items.STICKY_PISTON, 7);
        ITEMS_REDSTONE.put(Items.SLIME_BLOCK, 8);
    }
}
