package frootloops.versus.mod.items_and_effects.inventory.sorting.groups;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.items_and_effects.CustomPotions;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemComparaisonHelper;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;

import java.util.LinkedList;
import java.util.Optional;

import static frootloops.versus.mod.items_and_effects.inventory.sorting.SortingHelper.DEBUG_SORTING_GROUPS;


public class MainHotbarGroup extends SortingGroup {

    private static final float MAX_BLOCK_HARDNESS = 6.0F;
    ItemSlot mainWeaponSlot = null, pickaxeSlot = null, axeSlot = null, extraWeaponSlot = null, foodSlot = null, potionSlot = null, clutchItem = null, lightSlot = null, blockSlot = null;
    private boolean hasGoodPotion = false;
    private boolean hasExcellentPotion = false;
    private boolean isBlockSlotLocked = false;

    public MainHotbarGroup() {super("GROUP: MAIN_HOTBAR");}

    public boolean hasAtLeastOneConsumable(){
        return (this.foodSlot != null || this.potionSlot != null || (this.clutchItem != null && this.clutchItem.itemType() == ItemType.TOTEM));
    }

    public boolean hasCombatItems(){
        boolean hasAtLeastOneWeapon = (this.mainWeaponSlot != null || this.extraWeaponSlot != null || this.axeSlot != null);
        if(!hasAtLeastOneWeapon) return false;

        boolean hasAtLeastTwoWeapons = (this.mainWeaponSlot != null && this.extraWeaponSlot != null) || (this.axeSlot != null && this.extraWeaponSlot != null) || (this.mainWeaponSlot != null && this.axeSlot != null) || (this.pickaxeSlot != null && this.axeSlot != null);
        return this.hasAtLeastOneConsumable() || hasAtLeastTwoWeapons;
    }

    public boolean hasBuildingItems(){
        if(isBlockSlotLocked) return true;
        if(this.blockSlot == null) return false;
        BlockState blockState = ((BlockItem)blockSlot.stack().getItem()).getBlock().getDefaultState();
        if(blockState.getBlock().getHardness() > MAX_BLOCK_HARDNESS && !(blockState.getBlock().getBlastResistance() == Blocks.OBSIDIAN.getBlastResistance() && this.canHaveObsidianAsBlock())) {
            return false;
        }
        if(this.pickaxeSlot != null) {
            ToolComponent pickaxeComponent = pickaxeSlot.stack().get(DataComponentTypes.TOOL);
            if(pickaxeComponent.isCorrectForDrops(blockState)) return true;
        }
        if(this.axeSlot != null) {
            ToolComponent axeComponent = axeSlot.stack().get(DataComponentTypes.TOOL);
            if(axeComponent.isCorrectForDrops(blockState)) return true;
        }
        return false;
    }
    private boolean canHaveObsidianAsBlock() {
        if(this.pickaxeSlot == null || pickaxeSlot.stack().getMaxDamage() < 256) return false;
        if(miscItems.size() > 0 && miscItems.containsItem(Items.END_CRYSTAL)) return true;
        return false;
    }

    public boolean hasPickaxe(){
        return (this.pickaxeSlot != null);
    }

    public boolean hasAxe(){
        return (this.axeSlot != null && axeSlot.itemType() == ItemType.AXE);
    }

    @Override
    public void addSlot(ItemSlot slot) {
        slot = tryAddingSlot(slot);
        if(slot != null) this.miscItems.add(slot);
    }

    @Override
    public void addSlots(LinkedList<ItemSlot> newSlots) {
        for (ItemSlot slot : newSlots) this.addSlot(slot);
    }

    @Override
    public ItemType getItemType() {
        return ItemType.SWORD;
    }

    @Override
    public int getMaxNumRows() {
        int size = this.size();
        return size > 0 ? 1 + size/9 : 0;
    }

    public ItemSlot tryInsertingSlot(ItemSlot slot) {
        return this.tryInsertingSlot(slot, false, false, false);
    }

    public ItemSlot tryInsertingSlot(ItemSlot slot, boolean isInDeepDark, boolean isInNether, boolean isInWater) {
        if(slot.stack().isIn(ItemTags.SWORDS) || (isInDeepDark && slot.stack().isIn(ItemTags.HOES))) {
            boolean isBetterWeapon = this.mainWeaponSlot == null || ItemComparaisonHelper.shouldGoBefore(slot, mainWeaponSlot, true);
            if(isInDeepDark && this.mainWeaponSlot != null) {
                boolean isInsertingHoe = slot.stack().isIn(ItemTags.HOES);
                if(isInsertingHoe && !mainWeaponSlot.stack().isIn(ItemTags.HOES)) isBetterWeapon = true;
                if(!isInsertingHoe && mainWeaponSlot.stack().isIn(ItemTags.HOES)) isBetterWeapon = false;
            }
            if(isBetterWeapon) {
                ItemSlot itemSlotToReturn = this.mainWeaponSlot;
                this.mainWeaponSlot = slot;
                if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + (itemSlotToReturn == null ? " as main weapon" : ", replacing " + itemSlotToReturn.stack().getName().getString()));
                return itemSlotToReturn;
            }
        }
        else if(slot.isWeaponOrShield()) {
            if(this.extraWeaponSlot == null || ItemComparaisonHelper.shouldGoBefore(slot, extraWeaponSlot, true)) {
                ItemSlot itemSlotToReturn = this.extraWeaponSlot;
                this.extraWeaponSlot = slot;
                if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + (itemSlotToReturn == null ? " as weapon" : ", replacing " + itemSlotToReturn.stack().getName().getString()));
                return itemSlotToReturn;
            }
        }
        else if(slot.isFood()) {
            if(this.foodSlot == null || ItemComparaisonHelper.shouldGoBefore(slot, foodSlot)) {
                ItemSlot itemSlotToReturn = this.foodSlot;
                this.foodSlot = slot;
                if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + (itemSlotToReturn == null ? " as food" : ", replacing " + itemSlotToReturn.stack().getName().getString()));
                return itemSlotToReturn;
            }
        }
        else if(!this.hasExcellentPotion && slot.stack().contains(DataComponentTypes.POTION_CONTENTS)) {
            Optional<RegistryEntry<Potion>> potionID = slot.stack().getComponents().get(DataComponentTypes.POTION_CONTENTS).potion();
            if(potionID.isEmpty()) return slot;
            boolean isExcellentPotion = (potionID.get() == Potions.LONG_FIRE_RESISTANCE || potionID.get() == Potions.STRONG_STRENGTH || potionID.get() == Potions.STRONG_SWIFTNESS);
            boolean isGoodPotion = isExcellentPotion || potionID.get() == Potions.FIRE_RESISTANCE || potionID.get() == Potions.STRENGTH || potionID.get() == Potions.LONG_STRENGTH || potionID.get() == Potions.SWIFTNESS || potionID.get() == Potions.LONG_SWIFTNESS || potionID.get() == Potions.HEALING || potionID.get() == Potions.STRONG_HEALING || potionID.get() == Potions.REGENERATION || potionID.get() == Potions.STRONG_REGENERATION || potionID.get() == Potions.LONG_REGENERATION || potionID.get() == Potions.STRONG_TURTLE_MASTER || potionID.get() == Potions.LONG_TURTLE_MASTER || potionID.get() == CustomPotions.HASTE_STRONG;

            // Situational potions:
            if(isInNether) isExcellentPotion = (potionID.get() == Potions.FIRE_RESISTANCE || potionID.get() == CustomPotions.FIRE_RESISTANCE_STRONG);
            if(isInWater) isExcellentPotion = (potionID.get() == Potions.WATER_BREATHING || potionID.get() == Potions.LONG_WATER_BREATHING);

            if(this.potionSlot == null || (isExcellentPotion && !this.hasExcellentPotion) || (isGoodPotion && !this.hasGoodPotion)) {
                ItemSlot itemSlotToReturn = this.potionSlot;
                this.potionSlot = slot;
                this.hasGoodPotion = isGoodPotion || isExcellentPotion;
                this.hasExcellentPotion = isExcellentPotion;
                if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + (itemSlotToReturn == null ? " as potion." : ", replacing " + itemSlotToReturn.stack().getName().getString()));
                return itemSlotToReturn;
            }
        }
        else if(slot.stack().isOf(Items.TORCH) || slot.stack().isOf(Items.SOUL_TORCH) || slot.stack().isOf(Items.LANTERN) || slot.stack().isOf(Items.SOUL_LANTERN)) {
            if(this.lightSlot == null || this.lightSlot.stack().getCount() < slot.stack().getCount()) {
                ItemSlot itemSlotToReturn = this.lightSlot;
                this.lightSlot = slot;
                if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + (itemSlotToReturn == null ? " as light source." : ", replacing " + itemSlotToReturn.stack().getName().getString()));
                return itemSlotToReturn;
            }
        }
        else if(slot.itemType() == ItemType.TOTEM && (this.clutchItem == null || this.clutchItem.itemType() != ItemType.TOTEM)) {
            ItemSlot itemSlotToReturn = this.clutchItem;
            this.clutchItem = slot;
            if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + (itemSlotToReturn == null ? " as totem, for clutch item." : ", replacing " + itemSlotToReturn.stack().getName().getString()));
            return itemSlotToReturn;
        }
        else if(isInDeepDark) {
            if(slot.itemType() == ItemType.SHEARS) {
                ItemSlot itemSlotToReturn = this.clutchItem;
                this.clutchItem = slot;
                if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + (itemSlotToReturn == null ? " as deep dark clutch item." : ", replacing " + itemSlotToReturn.stack().getName().getString()));
                return itemSlotToReturn;
            }
            else if(slot.stack().getItem() instanceof BlockItem blockItem && blockItem.getBlock().getDefaultState().isIn(BlockTags.OCCLUDES_VIBRATION_SIGNALS)) {
                if(blockSlot == null || blockSlot.stack().getCount() <= slot.stack().getCount()) {
                    ItemSlot itemSlotToReturn = this.blockSlot;
                    this.blockSlot = slot;
                    this.isBlockSlotLocked = true;
                    if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + (itemSlotToReturn == null ? " as deep dark block." : ", replacing " + itemSlotToReturn.stack().getName().getString()));
                    return itemSlotToReturn;
                }
            }
            else if(slot.isUsedToClutch() && clutchItem.itemType() != ItemType.SHEARS && (clutchItem == null || ItemComparaisonHelper.shouldGoBefore(slot, clutchItem))) {
                ItemSlot itemSlotToReturn = this.clutchItem;
                this.clutchItem = slot;
                if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + (itemSlotToReturn == null ? " as deep dark clutch item." : ", replacing " + itemSlotToReturn.stack().getName().getString()));
                return itemSlotToReturn;
            }
        }
        else if(isInNether) {
            if(slot.stack().isOf(Items.POWDER_SNOW_BUCKET) || slot.stack().isOf(Items.WARPED_FUNGUS_ON_A_STICK)) {
                ItemSlot itemSlotToReturn = this.clutchItem;
                this.clutchItem = slot;
                if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + (itemSlotToReturn == null ? " as nether clutch item." : ", replacing " + itemSlotToReturn.stack().getName().getString()));
                return itemSlotToReturn;
            }
            else if(slot.isUsedToClutch() && !clutchItem.stack().isOf(Items.POWDER_SNOW_BUCKET) && !clutchItem.stack().isOf(Items.WARPED_FUNGUS_ON_A_STICK) && (clutchItem == null || ItemComparaisonHelper.shouldGoBefore(slot, clutchItem))) {
                ItemSlot itemSlotToReturn = this.clutchItem;
                this.clutchItem = slot;
                if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + (itemSlotToReturn == null ? " as nether clutch item." : ", replacing " + itemSlotToReturn.stack().getName().getString()));
                return itemSlotToReturn;
            }
        }
        else if(isInWater) {
            if(slot.stack().isOf(Items.FILLED_MAP) || slot.stack().isOf(Items.MAGMA_BLOCK)) {
                ItemSlot itemSlotToReturn = this.clutchItem;
                this.clutchItem = slot;
                if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + (itemSlotToReturn == null ? " as ocean clutch item." : ", replacing " + itemSlotToReturn.stack().getName().getString()));
                return itemSlotToReturn;
            }
            else if(slot.isUsedToClutch() && !clutchItem.stack().isOf(Items.FILLED_MAP) && !clutchItem.stack().isOf(Items.MAGMA_BLOCK) && (clutchItem == null || ItemComparaisonHelper.shouldGoBefore(slot, clutchItem))) {
                ItemSlot itemSlotToReturn = this.clutchItem;
                this.clutchItem = slot;
                if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + (itemSlotToReturn == null ? " as ocean clutch item." : ", replacing " + itemSlotToReturn.stack().getName().getString()));
                return itemSlotToReturn;
            }
        }
        else if(slot.isUsedToClutch() && (this.clutchItem == null || ItemComparaisonHelper.shouldGoBefore(slot, this.clutchItem))) {
            ItemSlot itemSlotToReturn = this.clutchItem;
            this.clutchItem = slot;
            if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + (itemSlotToReturn == null ? " as ocean clutch item." : ", replacing " + itemSlotToReturn.stack().getName().getString()));
            return itemSlotToReturn;
        }
        return slot;
    }

    private ItemSlot tryAddingSlot(ItemSlot slot) {
        slot = this.tryInsertingSlot(slot);
        if(slot != null) {
            if(slot.itemType() == ItemType.PICKAXE) {
                if(this.pickaxeSlot == null || ItemComparaisonHelper.shouldGoBefore(slot, pickaxeSlot)) {
                    if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + " as pickaxe");
                    ItemSlot slotToReplace = this.pickaxeSlot;
                    this.pickaxeSlot = slot;
                    return slotToReplace;
                }
            }
            else if(slot.itemType() == ItemType.AXE) {
                if(this.axeSlot == null || ItemComparaisonHelper.shouldGoBefore(slot, axeSlot)) {
                    if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + " as axe");
                    ItemSlot slotToReplace = this.axeSlot;
                    this.axeSlot = slot;
                    return slotToReplace;
                }
                else if(this.extraWeaponSlot == null || (extraWeaponSlot.itemType() == ItemType.AXE && ItemComparaisonHelper.shouldGoBefore(slot, extraWeaponSlot))) {
                    if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + " as special weapon");
                    ItemSlot slotToReplace = this.extraWeaponSlot;
                    this.extraWeaponSlot = slot;
                    return slotToReplace;
                }
            }
            else if(slot.isToolOrWeapon()) {
                boolean mustBeWeapon = this.extraWeaponSlot == null || (slot.isWeapon() && !extraWeaponSlot.isWeapon());
                boolean cannotBeWeapon = !mustBeWeapon && extraWeaponSlot != null && extraWeaponSlot.isWeapon() && !slot.isWeapon();
                if(!cannotBeWeapon && (mustBeWeapon || ItemComparaisonHelper.shouldGoBefore(slot, extraWeaponSlot))) {
                    if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + " as extra weapon");
                    ItemSlot slotToReplace = this.extraWeaponSlot;
                    this.extraWeaponSlot = slot;
                    return slotToReplace;
                }
            }
            else if(slot.itemType() == ItemType.BLOCK_FULL || slot.itemType() == ItemType.BLOCK_WORKSTATION) {
                if(this.blockSlot == null) {
                    if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + " as block");
                    this.blockSlot = slot;
                    return null;
                }
                else if(!isBlockSlotLocked && !blockSlot.stack().isIn(ItemTags.WOOL) && !ItemComparaisonHelper.shouldGoBefore(blockSlot, slot)) {
                    if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting block " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + " as block (replacing " + blockSlot.stack().getName().getString() + ")");
                    ItemSlot oldBlockSlot = blockSlot;
                    this.blockSlot = slot;
                    return oldBlockSlot;
                }
            }
        }
        return slot;
    }


    @Override
    protected void addSlotsToMisc(LinkedList<ItemSlot> newSlots) {
        for (ItemSlot slot : newSlots) {
            int pos = miscItems.add(slot);
            if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + "'s misc items at pos " + pos + ": " + miscItems);
        }
    }


    private LinkedList<ItemSlot> takeImportantItems(int numSlotsToTake) {
        LinkedList<ItemSlot> slotsTaken = new LinkedList<>();
        if(this.mainWeaponSlot != null && numSlotsToTake > 0) {
            slotsTaken.add(mainWeaponSlot);
            mainWeaponSlot = null;
            numSlotsToTake--;
        }
        if(this.pickaxeSlot != null && numSlotsToTake > 0) {
            slotsTaken.add(pickaxeSlot);
            pickaxeSlot = null;
            numSlotsToTake--;
        }
        if(this.axeSlot != null && numSlotsToTake > 0) {
            slotsTaken.add(axeSlot);
            axeSlot = null;
            numSlotsToTake--;
        }
        if(this.extraWeaponSlot != null && numSlotsToTake > 0) {
            slotsTaken.add(extraWeaponSlot);
            extraWeaponSlot = null;
            numSlotsToTake--;
        }
        if(this.potionSlot != null && numSlotsToTake > 0) {
            slotsTaken.add(potionSlot);
            potionSlot = null;
            numSlotsToTake--;
        }
        if(this.foodSlot != null && numSlotsToTake > 0) {
            slotsTaken.add(foodSlot);
            foodSlot = null;
            numSlotsToTake--;
        }
        if(this.clutchItem != null && numSlotsToTake > 0) {
            slotsTaken.add(clutchItem);
            clutchItem = null;
            numSlotsToTake--;
        }
        if(this.lightSlot != null && numSlotsToTake > 0) {
            slotsTaken.add(lightSlot);
            lightSlot = null;
            numSlotsToTake--;
        }
        if(this.blockSlot != null && numSlotsToTake > 0) {
            slotsTaken.add(blockSlot);
            blockSlot = null;
        }
        return slotsTaken;
    }

    @Override
    public LinkedList<ItemSlot> takeAllItems() {
        if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("                 " + this.GROUP_NAME + " - takeAllItems()");
        for (ItemSlot slot:this.takeImportantItems(Integer.MAX_VALUE)) this.miscItems.addBetween(slot, 0, miscItems.size());
        return miscItems.takeAll();
    }

    @Override
    public int getNextListSize() {
        return this.size();
    }

    @Override
    public LinkedList<ItemSlot> takeNextList() {
        return this.takeAllItems();
    }

    @Override
    public void tryFormingRows() {} // Do nothing!

    @Override
    public LinkedList<ItemSlot> takeFirstSlots(int numSlotsToTake, boolean splitUpSubgroups) {
        if(numSlotsToTake == this.size()) return this.takeAllItems();
        if(numSlotsToTake == this.size() - this.miscItems.size()) return this.takeImportantItems(numSlotsToTake);
        if(numSlotsToTake == this.miscItems.size()) return this.miscItems.takeAll();
        if(!splitUpSubgroups) return new LinkedList<>();

        LinkedList<ItemSlot> slotsTaken = this.takeImportantItems(numSlotsToTake);
        int numImportantItemsTaken = slotsTaken.size();
        slotsTaken.addAll(this.miscItems.take(numSlotsToTake - slotsTaken.size()));
        if(DEBUG_SORTING_GROUPS)  VersusMod.MOD_LOGGER.warn("                 " + this.GROUP_NAME + " - takeFirstSlots(" + numSlotsToTake + ", " + splitUpSubgroups + ") - Took " + numImportantItemsTaken + " important items and " + (slotsTaken.size() - numImportantItemsTaken) + " misc (" + (numSlotsToTake - slotsTaken.size()) + " left to take, and size of misc is " + this.miscItems.size() + ")");
        return slotsTaken;
    }

    @Override
    public LinkedList<ItemSlot> tryTakingExactNumSlots(int numSlotsToTake, boolean withTools, boolean startFromEnd) {
        if(numSlotsToTake == this.size() || numSlotsToTake == 9) return this.takeAllItems();
        if(numSlotsToTake == this.size() - this.miscItems.size()) return this.takeImportantItems(numSlotsToTake);
        if(numSlotsToTake == this.miscItems.size()) return this.miscItems.takeAll();
        if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("                 " + this.GROUP_NAME + " - tryTakingExactNumSlots(" + numSlotsToTake + ", " + withTools + ", " + startFromEnd + ") - Returned nothing!");
        return new LinkedList<>();
    }

    public LinkedList<ItemSlot> keepOnlyEssentials() {
        LinkedList<ItemSlot> itemsToRemove = new LinkedList<>();
        boolean hasEnoughCombatItems = this.hasCombatItems();
        if(!hasEnoughCombatItems) {
            if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Hotbar: Not enough combat items!");
            if(this.mainWeaponSlot != null) itemsToRemove.add(this.mainWeaponSlot);
            if(this.extraWeaponSlot != null) itemsToRemove.add(this.extraWeaponSlot);
            if(this.potionSlot != null) itemsToRemove.add(this.potionSlot);
            if(this.clutchItem != null) itemsToRemove.add(this.clutchItem);
            this.mainWeaponSlot = null;
            this.extraWeaponSlot = null;
            this.potionSlot = null;
            this.clutchItem = null;
        }
        boolean hasEnoughBuildingItems = this.hasBuildingItems();
        if(!hasEnoughBuildingItems && !isBlockSlotLocked) {
            if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Hotbar: Not enough building items!");
            if(this.blockSlot != null) itemsToRemove.add(this.blockSlot);
            this.blockSlot = null;
        }
        if(!hasEnoughCombatItems && !hasEnoughBuildingItems) { // No valid hotbar goup
            if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Hotbar: Not enough items tto make a good hotbar, sorting extras back into groups");
            if(this.clutchItem != null) itemsToRemove.add(this.clutchItem);
            if(this.lightSlot != null) itemsToRemove.add(this.lightSlot);
            this.clutchItem = null;
            this.lightSlot = null;
            if(this.miscItems.size() > 0) {
                itemsToRemove.addAll(miscItems.takeAll());
            }
        }
        if(this.miscItems.size() < 3 && this.size() != 9) itemsToRemove.addAll(miscItems.takeAll());
        return itemsToRemove;
    }

    @Override
    public int size() {
        int currentSize = 0;
        if(this.mainWeaponSlot != null) currentSize++;
        if(this.pickaxeSlot != null) currentSize++;
        if(this.axeSlot != null) currentSize++;
        if(this.extraWeaponSlot != null) currentSize++;
        if(this.foodSlot != null) currentSize++;
        if(this.potionSlot != null) currentSize++;
        if(this.clutchItem != null) currentSize++;
        if(this.lightSlot != null) currentSize++;
        if(this.blockSlot != null) currentSize++;
        return currentSize + this.miscItems.size();
    }

    @Override
    public void clear() {
        if(this.miscItems.size() != 0) VersusMod.MOD_LOGGER.error("[ ITEM SORTING ] Hotbar " + this.GROUP_NAME + " - Still had " + this.miscItems.size() + " items left in its list when clear() was run.");
        this.miscItems.clear();
        this.mainWeaponSlot = null;
        this.pickaxeSlot = null;
        this.axeSlot = null;
        this.extraWeaponSlot = null;
        this.foodSlot = null;
        this.potionSlot = null;
        this.clutchItem = null;
        this.lightSlot = null;
        this.blockSlot = null;
    }

    @Override
    public String toString() {
        if( this.size() == 0 ) return "";
        String output = "\n              [ SORTED " + this.GROUP_NAME + " ] (Size: " + this.size() + ")\n";
        String hotBarOutput = "";

        if(this.mainWeaponSlot != null) {
            String name = this.mainWeaponSlot.stack().getName().getString();
            hotBarOutput += name + " (Weapon), ";
        }
        if(this.pickaxeSlot != null) {
            String name = this.pickaxeSlot.stack().getName().getString();
            hotBarOutput += name + " (Pick), ";
        }
        if(this.axeSlot != null) {
            String name = this.axeSlot.stack().getName().getString();
            hotBarOutput += name + " (Axe), ";
        }
        if(this.extraWeaponSlot != null) {
            String name = this.extraWeaponSlot.stack().getName().getString();
            hotBarOutput += name + " (Special), ";
        }
        if(this.foodSlot != null) {
            String name = this.foodSlot.stack().getName().getString();
            hotBarOutput += name + " (Food), ";
        }
        if(this.potionSlot != null) {
            String name = this.potionSlot.stack().getName().getString();
            hotBarOutput += name + " (Potion), ";
        }
        if(this.clutchItem != null) {
            String name = this.clutchItem.stack().getName().getString();
            hotBarOutput += name + " (Clutch), ";
        }
        if(this.lightSlot != null) {
            String name = this.lightSlot.stack().getName().getString();
            hotBarOutput += name + " (Light), ";
        }
        if(this.blockSlot != null) {
            String name = this.blockSlot.stack().getName().getString();
            hotBarOutput += name + " (Block), ";
        }
        if(hotBarOutput.length() > 0) output += "              Hotbar List : " + hotBarOutput + "\n";
        if(this.miscItems.size() > 0) output += "              Other Items List: " + this.miscItems.toString() + "\n";
        return output;
    }
}
