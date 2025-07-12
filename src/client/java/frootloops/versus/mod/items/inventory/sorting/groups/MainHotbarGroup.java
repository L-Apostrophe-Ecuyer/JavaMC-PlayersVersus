package frootloops.versus.mod.items.inventory.sorting.groups;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.items.inventory.sorting.ItemComparaisonHelper;
import frootloops.versus.mod.items.inventory.sorting.ItemSlot;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.Potions;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;

import java.util.LinkedList;
import java.util.Optional;

import static frootloops.versus.mod.items.inventory.sorting.InventorySortingHelper.DEBUG_SORTING_GROUPS;


public class MainHotbarGroup extends SortingGroup {

    ItemSlot swordSlot = null, pickaxeSlot = null, axeSlot = null, extraWeaponSlot = null, foodSlot = null, potionSlot = null, totemSlot = null, lightSlot = null, blockSlot = null;
    private boolean hasGoodPotion = false;
    private boolean hasExcellentPotion = false;

    public MainHotbarGroup() {
        super("GROUP: MAIN_HOTBAR");
    }

    public boolean hasAtLeastOneConsumable(){
        return (this.foodSlot != null || this.potionSlot != null || this.totemSlot != null);
    }

    public boolean hasCombatItems(){
        boolean hasAtLeastOneWeapon = (this.swordSlot != null || this.extraWeaponSlot != null || this.axeSlot != null);
        if(!hasAtLeastOneWeapon) return false;

        boolean hasAtLeastTwoWeapons = (this.swordSlot != null && this.extraWeaponSlot != null) || (this.axeSlot != null && this.extraWeaponSlot != null) || (this.swordSlot != null && this.axeSlot != null);
        return this.hasAtLeastOneConsumable() || hasAtLeastTwoWeapons;
    }

    public boolean hasBuildingItems(){
        return (this.pickaxeSlot != null && this.blockSlot != null) || (this.axeSlot != null && this.blockSlot != null);
    }

    public boolean hasPickaxe(){
        return (this.pickaxeSlot != null);
    }

    public boolean hasAxe(){
        return (this.pickaxeSlot != null);
    }

    @Override
    public void addSlot(ItemSlot slot) {
        slot = tryInsertingSlot(slot);
        if(slot != null) {
            if(slot.stack().isIn(ItemTags.PICKAXES)) {
                if(this.pickaxeSlot == null) this.pickaxeSlot = slot;
                else this.miscItems.add(slot);
            }
            else if(slot.stack().isIn(ItemTags.AXES)) {
                if(this.axeSlot == null) this.axeSlot = slot;
                else this.miscItems.add(slot);
            }
            else  this.miscItems.add(slot);
        }
    }

    @Override
    public ItemSlot tryInsertingSlot(ItemSlot slot) {
        if(slot.stack().isIn(ItemTags.SWORDS)) {
            if(this.swordSlot == null || ItemComparaisonHelper.shouldGoBefore(slot, swordSlot, true)) {
                ItemSlot itemSlotToReturn = this.swordSlot;
                this.swordSlot = slot;
                if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + (itemSlotToReturn == null ? " as sword" : ", replacing " + itemSlotToReturn.stack().getName().getString()));
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
            boolean isGoodPotion = !isExcellentPotion && (potionID.get() == Potions.FIRE_RESISTANCE || potionID.get() == Potions.STRENGTH || potionID.get() == Potions.LONG_STRENGTH || potionID.get() == Potions.SWIFTNESS || potionID.get() == Potions.LONG_SWIFTNESS || potionID.get() == Potions.HEALING || potionID.get() == Potions.STRONG_HEALING || potionID.get() == Potions.REGENERATION || potionID.get() == Potions.STRONG_REGENERATION || potionID.get() == Potions.LONG_REGENERATION);
            if(this.potionSlot == null || (isExcellentPotion && !this.hasExcellentPotion) || (isGoodPotion && !this.hasGoodPotion)) {
                ItemSlot itemSlotToReturn = this.potionSlot;
                this.potionSlot = slot;
                this.hasGoodPotion = isGoodPotion || isExcellentPotion;
                this.hasExcellentPotion = isExcellentPotion;
                if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + (itemSlotToReturn == null ? " as potion." : ", replacing " + itemSlotToReturn.stack().getName().getString()));
                return itemSlotToReturn;
            }
        }
        else if(this.totemSlot == null && slot.stack().getComponents().contains(DataComponentTypes.DEATH_PROTECTION)) {
            ItemSlot itemSlotToReturn = this.totemSlot;
            this.totemSlot = slot;
            return itemSlotToReturn;
        }
        else if(slot.stack().isOf(Items.TORCH) || slot.stack().isOf(Items.SOUL_TORCH) || slot.stack().isOf(Items.LANTERN) || slot.stack().isOf(Items.SOUL_LANTERN)) {
            if(this.lightSlot == null || this.lightSlot.stack().getCount() < slot.stack().getCount()) {
                ItemSlot itemSlotToReturn = this.lightSlot;
                this.lightSlot = slot;
                if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Inserting " + slot.stack().getName().getString() + " into " + this.GROUP_NAME + (itemSlotToReturn == null ? " as light source." : ", replacing " + itemSlotToReturn.stack().getName().getString()));
                return itemSlotToReturn;
            }
        }
        return slot;
    }


    private LinkedList<ItemSlot> takeImportantItems(int numSlotsToTake) {
        LinkedList<ItemSlot> slotsTaken = new LinkedList<>();
        if(this.swordSlot != null && numSlotsToTake > 0) {
            slotsTaken.add(swordSlot);
            swordSlot = null;
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
        if(this.totemSlot != null && numSlotsToTake > 0) {
            slotsTaken.add(totemSlot);
            totemSlot = null;
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
        for (ItemSlot slot:this.takeImportantItems(9)) this.miscItems.add(slot);
        return miscItems.takeAll();
    }

    @Override
    public int getNextListSize() {
        return this.size();
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
            if(this.swordSlot != null) itemsToRemove.add(this.swordSlot);
            if(this.extraWeaponSlot != null) itemsToRemove.add(this.extraWeaponSlot);
            if(this.potionSlot != null) itemsToRemove.add(this.potionSlot);
            if(this.totemSlot != null) itemsToRemove.add(this.totemSlot);
            this.swordSlot = null;
            this.extraWeaponSlot = null;
            this.potionSlot = null;
            this.totemSlot = null;
        }
        boolean hasEnoughBuildingItems = this.hasBuildingItems();
        if(!hasEnoughBuildingItems) {
            if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Hotbar: Not enough building items!");
            if(this.pickaxeSlot != null) itemsToRemove.add(this.pickaxeSlot);
            if(this.axeSlot != null) itemsToRemove.add(this.axeSlot);
            if(this.blockSlot != null) itemsToRemove.add(this.blockSlot);
            this.pickaxeSlot = null;
            this.axeSlot = null;
            this.blockSlot = null;
        }
        if(!hasEnoughCombatItems && !hasEnoughBuildingItems) { // No valid hotbar goup
            if(DEBUG_SORTING_GROUPS) VersusMod.MOD_LOGGER.warn("              -> Hotbar: Not enough items tto make a good hotbar, sorting everything back into groups");
            if(this.foodSlot != null) itemsToRemove.add(this.foodSlot);
            if(this.lightSlot != null) itemsToRemove.add(this.lightSlot);
            this.foodSlot = null;
            this.lightSlot = null;
            if(this.miscItems.size() > 0) {
                itemsToRemove.addAll(miscItems.takeAll());
            }
        }
        return itemsToRemove;
    }

    @Override
    public int size() {
        int currentSize = 0;
        if(this.swordSlot != null) currentSize++;
        if(this.pickaxeSlot != null) currentSize++;
        if(this.axeSlot != null) currentSize++;
        if(this.extraWeaponSlot != null) currentSize++;
        if(this.foodSlot != null) currentSize++;
        if(this.potionSlot != null) currentSize++;
        if(this.totemSlot != null) currentSize++;
        if(this.lightSlot != null) currentSize++;
        if(this.blockSlot != null) currentSize++;
        return currentSize + this.miscItems.size();
    }

    @Override
    public void clear() {
        if(this.miscItems.size() != 0) VersusMod.MOD_LOGGER.error("[ ITEM SORTING ] Hotbar " + this.GROUP_NAME + " - Still had " + this.miscItems.size() + " items left in its list when clear() was run.");
        this.miscItems.clear();
        this.swordSlot = null;
        this.pickaxeSlot = null;
        this.axeSlot = null;
        this.extraWeaponSlot = null;
        this.foodSlot = null;
        this.potionSlot = null;
        this.totemSlot = null;
        this.lightSlot = null;
        this.blockSlot = null;
    }

    @Override
    public String toString() {
        if( this.size() == 0 ) return "";
        String output = "\n              [ SORTED " + this.GROUP_NAME + " ] (Size: " + this.size() + ")\n";
        String hotBarOutput = "";

        if(this.swordSlot != null) {
            String name = this.swordSlot.stack().getName().getString();
            hotBarOutput += name + ", ";
        }
        if(this.pickaxeSlot != null) {
            String name = this.pickaxeSlot.stack().getName().getString();
            hotBarOutput += name + ", ";
        }
        if(this.axeSlot != null) {
            String name = this.axeSlot.stack().getName().getString();
            hotBarOutput += name + ", ";
        }
        if(this.extraWeaponSlot != null) {
            String name = this.extraWeaponSlot.stack().getName().getString();
            hotBarOutput += name + ", ";
        }
        if(this.foodSlot != null) {
            String name = this.foodSlot.stack().getName().getString();
            hotBarOutput += name + ", ";
        }
        if(this.potionSlot != null) {
            String name = this.potionSlot.stack().getName().getString();
            hotBarOutput += name + ", ";
        }
        if(this.totemSlot != null) {
            String name = this.totemSlot.stack().getName().getString();
            hotBarOutput += name + ", ";
        }
        if(this.lightSlot != null) {
            String name = this.lightSlot.stack().getName().getString();
            hotBarOutput += name + ", ";
        }
        if(this.blockSlot != null) {
            String name = this.blockSlot.stack().getName().getString();
            hotBarOutput += name + ", ";
        }
        if(hotBarOutput.length() > 0) output += "              Hotbar List : " + hotBarOutput + "\n";
        if(this.miscItems.size() > 0) output += "              Other Items List: " + this.miscItems.toString() + "\n";
        return output;
    }
}
