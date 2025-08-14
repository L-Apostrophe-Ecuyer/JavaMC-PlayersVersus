package frootloops.versus.mod.items.inventory.sorting;

import frootloops.versus.VersusMod;

import java.util.LinkedList;

import static frootloops.versus.mod.items.inventory.sorting.InventorySortingHelper.DEBUG_SORTING_OUTPUT;

public class SortedInventoryOutput {

    private final ItemSlot[] invSlots;
    private LinkedList<ItemSlot> slotsToAdd;
    private final int numRows, numEmptySlots;
    private final boolean isPlayerInventory;
    private int numSlotsSkipped = 0, currentCol = 0, currentRow = 0;

    public SortedInventoryOutput(int numRows, int numEmptySlots, boolean isPlayerInventory) {
        this.numRows = numRows;
        this.numEmptySlots = numEmptySlots;
        this.invSlots = new ItemSlot[numRows * 9];
        this.isPlayerInventory = isPlayerInventory;
    }

    public ItemSlot[] getInvSlots(){
        return this.invSlots;
    }

    public boolean addAll(LinkedList<ItemSlot> slots) { return this.addAll(slots, false);}
    public boolean addAll(LinkedList<ItemSlot> slots, boolean isNewGroup) {
        slotsToAdd = slots;
        if(slotsToAdd == null || slotsToAdd.size() == 0) return true;

        if(DEBUG_SORTING_OUTPUT) {
            String debugMsg = isNewGroup ? "[ ITEM SORTING ] - Inventory, at (" + this.currentRow + ", " + this.currentCol + ") - Placing new group in inventory: " : "                 - Inventory, at (" + this.currentRow + ", " + this.currentCol + ") - Placing subgroup of size " + slots.size() + ": ";
            for (ItemSlot slot: slots) debugMsg += slot + ",";
            VersusMod.MOD_LOGGER.warn(debugMsg);
        }

        // Try backtracking, if that improves the fit:
        if((this.slotsToAdd.size() + this.currentCol) > 9 && this.currentCol == 0)
            this.tryBacktracking((this.slotsToAdd.size() + this.currentCol) % 9);

        // Try changing rows, if that improves the fit:
        else if(isNewGroup) this.tryMovingToNextRow(isNewGroup);

        // Insert items:
        int numItemsToAdd = slotsToAdd.size();
        for(int i = 0; i < numItemsToAdd; i++) {
            if(!this.moveToNextAvailableSlot()) {
                if(DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.error("                  Returning. Failed to move to next available slot. (" + this.currentRow + ", " + this.currentCol + "). There were still " + slotsToAdd.size() + " num items to add.");
                return false;
            }

            ItemSlot slotToInsert =  this.slotsToAdd.removeFirst();
            if(slotToInsert == null) continue;
            if(!this.set(this.currentRow, this.currentCol, slotToInsert)) {
                if(DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.error("                  Returning. Failed to set slot. (" + this.currentRow + ", " + this.currentCol + ")");
                return false;
            }
        }
        if(DEBUG_SORTING_OUTPUT) {
            if(isNewGroup) VersusMod.MOD_LOGGER.warn(this.toString());
            VersusMod.MOD_LOGGER.warn("");
        }
        return true;
    }

    public int getCurrentRow() {
        return this.currentRow;
    }
    public int getCurrentCol() {
        return this.currentCol;
    }

    public int getNumSlotsForNextBatch(int minNumItems, boolean isNewGroup) {;
        return this.shouldGoToNextRow(minNumItems, isNewGroup) ? 9 : this.getNumEmptySlotsInRow();
    }

    public int getNumEmptySlotsInRow() {
        if(this.currentCol > 8) VersusMod.MOD_LOGGER.error("[ ITEM SORTING CRITICAL ] SORTED INVENTORY OUTPUT - Negative amount of empty slots in row. Current column is " + this.currentCol);
        return 9 - this.currentCol;
    }

    private void tryBacktracking(int numItemsToFit) {
        if(this.currentRow > 0) {
            int i;
            for(i = 8; i >= numItemsToFit; i--)
                if(this.invSlots[(this.currentRow - 1) * 9 + i] != null) return;
            this.currentRow--;
            this.currentCol = i;
        }
    }

    private boolean moveToNextAvailableSlot() {
        if(this.tryMovingToNextRow(false)) return true;
        else if(currentCol < 0 || this.get(currentRow, currentCol) == null) return true;
        else if(this.currentCol < 8){
            //if(DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.warn("                 - Moving to next spot -> Moving to next column, to (" + this.currentRow + ", " + this.currentCol + ")");
            this.currentCol++;
            return true;
        }
        if(DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.error("[ ITEM SORTING ] SORTED INVENTORY OUTPUT - Tried to move out of bounds!");
        return false;
    }

    public boolean goToNextAvailableRow() {
        if(this.currentCol == 0) return true; // Avoids stupidity
        if(this.currentRow + 1 >= this.numRows) {
            VersusMod.MOD_LOGGER.error("[ ITEM SORTING ] SORTED INVENTORY OUTPUT - Tried to move to a row that was out of bounds!");
            return false;
        }
        if(DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.warn("                 - Skipping to next row, from (" + this.currentRow + ", " + this.currentCol + ") to (" + (this.currentRow + 1) + ", 0) -------  There are currently " + (numEmptySlots - numSlotsSkipped) + " out of " + numEmptySlots + " empty slots, and so " + ((this.numEmptySlots - this.numSlotsSkipped)/Math.min(1, this.numRows - this.currentRow)) + " empty slots left per row");
        this.numSlotsSkipped += 9 - (this.currentCol + 1);
        this.currentRow++;
        this.currentCol = 0;
        return true;
    }

    private boolean tryMovingToNextRow(boolean isNewGroup) {
        if(this.shouldGoToNextRow(this.slotsToAdd.size(), isNewGroup)) return this.goToNextAvailableRow();
        return false;
    }

    private boolean shouldGoToNextRow(int numItemsNewBatch, boolean lenientCheck) {
        if(this.currentRow >= this.numRows - 1 || this.currentCol == 0) return false;
        if(this.currentCol >= 8) return true;
        if(numItemsNewBatch + this.currentCol < (lenientCheck ? 9 : 6)) return false;
        if(numItemsNewBatch > 0) return false;

        // Skip to next row logic:
        int numEmptySlotsLeftPerRow = (this.numEmptySlots - this.numSlotsSkipped)/Math.min(1, this.numRows - this.currentRow);
        return numEmptySlotsLeftPerRow >= (9 - this.currentCol);
    }

    private boolean set(int row, int column, ItemSlot slot){
        if(row < 0 || row > numRows - 1 || column < 0 || column > 8) {
            VersusMod.MOD_LOGGER.error("[ ITEM SORTING CRITICAL ] SORTED INVENTORY OUTPUT - Tried to insert " + slot + " in pos [" + row + "] [" + column + "]");
            return false;
        }
        if(invSlots[row * 9 + column] != null) {
            VersusMod.MOD_LOGGER.error("[ ITEM SORTING CRITICAL ] SORTED INVENTORY OUTPUT - Tried to insert " + slot + ", but already item " + invSlots[row * 9 + column] + " in  pos [" + row + "] [" + column + "]");
            return false;
        }

        // Try sorting each individual row as well:
        if(column > 0 && slot.itemType() != ItemType.MISC && slot.itemType() != ItemType.TRASH) {
            if(DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.warn("                   List item " + this.slotsToAdd.size() + " - Attempting to find a better slot than (" + this.currentRow + ", " + this.currentCol + ") for this tool: " + slot);
            for(int i = 0; i < column; i++) {
                ItemSlot other = this.get(row, i);
                if((isPlayerInventory || !slot.hasSameType(other, false)) && ItemComparaisonHelper.shouldGoBefore(slot, other)) { // If true, swap positions
                    invSlots[row * 9 + i] = slot;
                    slot = other;
                }
            }
        }

        if(DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.warn("                   List item " + this.slotsToAdd.size() + " - Setting slot (" + this.currentRow + ", " + this.currentCol + ") as " + slot);
        invSlots[row * 9 + column] = slot;
        return true;
    }

    public ItemSlot get(int row, int column){
        return invSlots[row * 9 + column];
    }

    @Override
    public String toString() {
        String output = "\n              [ SORTED INVENTORY ]\n";
        for(int i = 0; i < this.numRows; i++) {
            output += "              Row " + i + ": [";
            for(int j = 0; j < 9; j++) {
                if(this.invSlots[i * 9 + j] == null) output += "           |";
                else {
                    String name = this.invSlots[i * 9 + j].toString();
                    if(name.length() > 9) name = name.substring(0, 9);
                    output += String.format("%" + 10 + "s", name) + " |";
                }
            }
            output += "\n";
        }
        return output;
    }
}
