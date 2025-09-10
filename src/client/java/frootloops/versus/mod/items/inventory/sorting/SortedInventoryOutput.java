package frootloops.versus.mod.items.inventory.sorting;

import frootloops.versus.VersusMod;

import java.util.LinkedList;

import static frootloops.versus.mod.items.inventory.sorting.InventorySortingHelper.DEBUG_SORTING_OUTPUT;

public class SortedInventoryOutput {

    private final ItemSlot[] invSlots;
    private LinkedList<ItemSlot> slotsToAdd;
    private final int numRows, numEmptySlots;
    public int numGroupsToPlace;
    private final boolean isPlayerInventory;
    private int numSlotsSkipped = 0, currentCol = 0, currentRow = 0, currentGroupColStart = 0, currentSlotsColStart;

    public SortedInventoryOutput(int numRows, int numEmptySlots, int numGroups, boolean isPlayerInventory) {
        this.numRows = numRows;
        this.numEmptySlots = numEmptySlots;
        this.numGroupsToPlace = numGroups;
        this.invSlots = new ItemSlot[numRows * 9];
        this.isPlayerInventory = isPlayerInventory;
    }

    public ItemSlot[] getInvSlots(){
        return this.invSlots;
    }

    public void markGroupAsDone() {
        boolean wasGroupMultirow = this.currentCol <= this.currentGroupColStart && this.currentCol % 8 != 0 && !(currentGroupColStart == currentCol && currentCol == 7);
        boolean shouldMoveLastRowToRight = wasGroupMultirow && (this.numGroupsToPlace > 1 || this.currentGroupColStart > 0);
        if(shouldMoveLastRowToRight) {
            int indexEnd, indexStart = 0;
            for(indexEnd = 8; indexEnd > this.currentCol; indexEnd--) if(this.get(this.currentRow, indexEnd) == null) break;
            if(indexEnd >= currentCol) { // If enough empty columns at end of row to fit the item slots. This should always be true... but better safe than sorry
                if(DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.warn("================= MOVING ITEMS OF GROUP TO END OF ROW! Current group's col start was " + currentGroupColStart + " and current col is " + currentCol);
                for (int i = 0; i <= this.currentCol; i++) {
                    ItemSlot slotToMove = this.get(this.currentRow, i);
                    if (!slotToMove.isToolOrWeapon()) {
                        this.invSlots[this.currentRow * 9 + i] = this.get(this.currentRow, indexEnd);
                        this.invSlots[this.currentRow * 9 + indexEnd] = slotToMove;
                        indexEnd--;
                    }
                    else {
                        this.invSlots[this.currentRow * 9 + i] = this.get(this.currentRow, indexStart);
                        this.invSlots[this.currentRow * 9 + indexStart] = slotToMove;
                        indexStart++;
                    }
                }
                this.currentCol = 0;
            } else if(DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.warn("----------------- End of group. Unable to move last items to the right, IndexEnd is " + indexEnd);
        } else if(DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.warn("----------------- End of group. Group's col value started at " + currentGroupColStart + " and ended at " + currentCol);
        this.currentSlotsColStart = 0;
        this.numGroupsToPlace--;

        if(DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.warn(this.toString());
    }

    public boolean addAll(LinkedList<ItemSlot> slots) { return this.addAll(slots, false, true);}
    public boolean addAll(LinkedList<ItemSlot> slots, boolean isNewGroup, boolean allowBacktracking) {
        slotsToAdd = slots;
        if(slotsToAdd == null || slotsToAdd.size() == 0) return true;

        if(DEBUG_SORTING_OUTPUT) {
            String debugMsg = isNewGroup ? "[ ITEM SORTING ] - Inventory, at (" + this.currentRow + ", " + this.currentCol + ") - Placing new group in inventory: " : "                 - Inventory, at (" + this.currentRow + ", " + this.currentCol + ") - Placing subgroup of size " + slots.size() + ": ";
            for (ItemSlot slot: slots) debugMsg += slot + ",";
            VersusMod.MOD_LOGGER.warn(debugMsg);
        }

        // Try backtracking, if that improves the fit:
        if(allowBacktracking && (this.slotsToAdd.size() + this.currentCol) > 9 && this.currentCol == 0)
            this.tryBacktracking((this.slotsToAdd.size() + this.currentCol) % 9);

        // Try changing rows, if that improves the fit:
        else if(isNewGroup) this.tryMovingToNextRow(true);

        // Update counters. These help keep groups and lists together:
        if(isNewGroup) this.currentGroupColStart = this.currentCol;
        currentSlotsColStart = currentCol;

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

    public int getNumEmptyRowsLeft() {
        int numFilledRows = this.currentRow + (currentCol > 0 ? -1 : 0);
        return this.numRows - numFilledRows;
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
        this.currentSlotsColStart = 0;
        return true;
    }

    private boolean tryMovingToNextRow(boolean isNewGroup) {
        if(isNewGroup && this.numRows - this.currentRow > numGroupsToPlace + 1) return this.goToNextAvailableRow();
        if(this.shouldGoToNextRow(this.slotsToAdd.size(), isNewGroup)) return this.goToNextAvailableRow();
        return false;
    }

    private boolean shouldGoToNextRow(int numItemsNewBatch, boolean lenientCheck) {
        if(this.currentRow >= this.numRows - 1 || this.currentCol == 0) return false;
        if(this.currentCol >= 8) return true;

        // Check how many empty slots in row are actually left:
        int numEmptySlotsInRow = 9 - currentCol;
        if(this.get(this.currentRow,8) != null) {
            numEmptySlotsInRow = 0;
            for(int i = 7; i > this.currentCol; i--) if(this.get(this.currentRow,i) == null) numEmptySlotsInRow++;
            if(DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.warn("                                 Checking if needing to go to next row: There are " + numEmptySlotsInRow + " empty slots left in row " + this.currentRow);
            if(numEmptySlotsInRow == 0) return true;
        }

        // Check if we can fit the items of the current batch into the same row:
        if(numItemsNewBatch + this.currentCol < (lenientCheck ? 9 : 6)) return false;
        if(numItemsNewBatch > 0) return false;

        // Skip to next row logic:
        int numEmptySlotsLeftPerRow = (this.numEmptySlots - this.numSlotsSkipped)/Math.min(1, this.numRows - this.currentRow);
        return numEmptySlotsLeftPerRow >= (9 - numEmptySlotsInRow);
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
                boolean isSameGroup = i >= this.currentSlotsColStart;
                boolean isHotbar = this.isPlayerInventory && row == 0;
                boolean mustGoBefore = slot != null && ((isSameGroup && other == null) || slot.shouldAlwaysGoBefore(other, !isHotbar)) && (other != null && !other.shouldAlwaysGoBefore(slot, !isHotbar));
                if(mustGoBefore && DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.warn("                               - Will swap at column " + i + "! Should " + slot + " always go before " + other + "? " + slot.shouldAlwaysGoBefore(other, !isHotbar));
                if(mustGoBefore) {

                    // Move back to not separate similar items:
                    if(!slot.isVerySimilarTo(other, isSameGroup)) {
                        if (i > 0 && this.get(row, i - 1).isVerySimilarTo(other, isSameGroup)) {
                            int j;
                            for (j = i - 1; j >= 0; j--)
                                if (!this.get(row, j).isVerySimilarTo(other, isSameGroup)) break;
                            i = Math.max(0, j);
                            other = this.get(row, i);
                        }
                    }

                    // Place item:
                    if(DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.warn("                               - Setting slot (" + this.currentRow + ", " + i + ") as " + slot + ", replacing " + other + " (Must: " + mustGoBefore + ")");
                    invSlots[row * 9 + i] = slot;
                    slot = other;

                    // Update group index start, if moved item to index before current group start:
                    if(!isSameGroup) this.currentSlotsColStart++;

                    // Move items to the right:
                    for(int k = i + 1; k < column + 1; k++) {
                        other = invSlots[row * 9 + k];
                        if(DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.warn("                               - Moving down slot (" + this.currentRow + ", " + k + ") as " + slot + ", replacing " + other);
                        invSlots[row * 9 + k] = slot;
                        slot = other;
                    }
                    return true; // Exit: we successfully placed the item
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
        String output = "\n              [ SORTED INVENTORY ] Current position is row " + currentRow + " and col " + currentCol + "\n";
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
