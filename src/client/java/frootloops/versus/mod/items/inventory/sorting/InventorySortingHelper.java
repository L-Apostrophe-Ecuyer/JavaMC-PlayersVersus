package frootloops.versus.mod.items.inventory.sorting;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.items.inventory.InventorySorting;
import frootloops.versus.mod.items.inventory.sorting.groups.SortingGroup;
import frootloops.versus.mod.items.inventory.sorting.groups.ToolSortingGroup;
import frootloops.versus.mod.items.inventory.sorting.lists.SortedItemLists;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static frootloops.versus.mod.items.inventory.sorting.groups.SortingGroups.*;

public class InventorySortingHelper {
    public static final boolean DEBUG_SORTING_GROUPS = true;
    public static final boolean DEBUG_SORTING_MERGE = true;
    public static final boolean DEBUG_SORTING_OUTPUT = true;

    private static void printGroups(String debugMsg) {
        if(MAIN_HOTBAR.size() > 0) debugMsg += MAIN_HOTBAR.toString();
        for (SortingGroup group : SORTING_GROUPS) if(group.size() > 0) debugMsg += group.toString();
        VersusMod.MOD_LOGGER.warn(debugMsg);
    }

    private static void printGroups(String debugMsg, List<SortingGroup> orderedGroups) {
        for (SortingGroup g : orderedGroups) if(g.size() > 0) debugMsg += g.toString();
        VersusMod.MOD_LOGGER.warn(debugMsg);
    }


    /**
     * Makes a list of rows.
     */
    public static ItemSlot[] getOptimalInventoryRows(LinkedList<ItemSlot> inventorySlots, int numRows, boolean isPlayerInventory, boolean isInDeepDark, boolean isInNether, boolean isInWater) {

        int numItems = inventorySlots.size();
        int numEmptySlots = numRows * 9 - numItems;

        // Step 0: If less than 9 items, trivial, just add everything and sort the single row
        if(inventorySlots.size() <= 9) {
            SortedInventoryOutput inventoryOutput = new SortedInventoryOutput(numRows, numEmptySlots, true);
            inventoryOutput.addAll(inventorySlots);
            return inventoryOutput.getInvSlots();
        }

        // Step 1: Populate ItemSortingGroups
        for(ItemSlot slot: inventorySlots) InventorySortingHelper.insertItemIntoGroup(slot, isPlayerInventory, isInDeepDark, isInNether, isInWater);
        if(isPlayerInventory) {
            InventorySortingHelper.cleanUpHotbar();
            LinkedList<ItemSlot> slotsRemovedFromHotbar = MAIN_HOTBAR.keepOnlyEssentials();
            for (ItemSlot slot:slotsRemovedFromHotbar) insertItemIntoGroup(slot);
        }
        if(DEBUG_SORTING_GROUPS) printGroups("[ INVENTORY SORTING ] ---- AFTER INSERTING -----\n");

        // Step 2: Try forming rows withing a group, and combining similar groups
        InventorySortingHelper.cleanUpGroups(isPlayerInventory);

        // Step 3: Order the resulting non-empty groups such that they combine into rows
        List<SortingGroup> orderedGroups = InventorySortingHelper.getOrderedListOfGroups(numEmptySlots, numRows);
        if(DEBUG_SORTING_MERGE) printGroups("[ INVENTORY SORTING ] ---- AFTER CLEAN UP & MERGING -----\n", orderedGroups);

        // Step 4: Finally, place sorted item groups into an inventory output
        if(DEBUG_SORTING_MERGE) VersusMod.MOD_LOGGER.warn("[ INVENTORY SORTING ] ---- STARTING TO INPUT INTO INVENTORY -----");
        SortedInventoryOutput inventoryOutput = new SortedInventoryOutput(numRows, numEmptySlots, isPlayerInventory);
        LinkedList<ItemSlot> slotsTaken, slotsToAdd = new LinkedList<>();

        // If each group can have their own row, then best case scenario:
        if(orderedGroups.size() <= numRows) {
            int actualNumRowsInGroups = orderedGroups.size() + orderedGroups.stream().mapToInt(g -> g.size()/9).sum();
            if(actualNumRowsInGroups <= numRows) {
                for (SortingGroup group : orderedGroups) {
                    inventoryOutput.goToNextAvailableRow();
                    inventoryOutput.addAll(group.takeAllItems(), true);
                }
                return inventoryOutput.getInvSlots();
            }
        }

        // This is the tough part.
        // Otherwise, some groups may start at one row and end at another. We need to be smarter of how to place groups in each row:
        for(SortingGroup group : orderedGroups) {
            if(group.size() == 0) continue;
            if(DEBUG_SORTING_MERGE) {
                VersusMod.MOD_LOGGER.warn("");
                VersusMod.MOD_LOGGER.warn("[ INVENTORY SORTING ] Inserting items of " + group.GROUP_NAME + " (Size " + group.size() + ")...");
            }

            // Try to break down the rest of the rows into sorted lists:
            int numIterations = 0;
            boolean wasSuccessful = false;
            boolean hasAddedItems = false;
            while(group.size() > 0) {

                int numItemsToPlace = group.getNextListSize();
                int numSlotsInRow = inventoryOutput.getNumSlotsForNextBatch(numItemsToPlace, !hasAddedItems);
                slotsTaken = group.takeFirstSlots(numSlotsInRow, false);
                if(DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.warn("              -> Next list size is " + numItemsToPlace + ", numSlotsInRow is "+ numSlotsInRow + ". Current group size is " + group.size());

                if(slotsTaken != null && slotsTaken.size() > 0) {
                    if(DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.warn("              -> " + group.GROUP_NAME + " - Took exactly " + slotsTaken.size() + " slots, with splitUpGroups = false");
                    slotsToAdd.addAll(slotsTaken);
                }
                else {
                    slotsTaken = group.tryTakingExactNumSlots(numSlotsInRow, !hasAddedItems, false);
                    if(slotsTaken != null && slotsTaken.size() > 0) {
                        slotsToAdd.addAll(slotsTaken);;
                        if(DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.warn("              -> " + group.GROUP_NAME + " - Took exactly " + slotsTaken.size() + " slots, with splitUpGroups = false");
                    }
                    else {
                        if(numSlotsInRow > 1) slotsTaken = group.takeFirstSlots(numSlotsInRow, false);
                        if(slotsTaken != null && slotsTaken.size() > 0) {
                            slotsToAdd.addAll(slotsTaken);
                            if(DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.warn("              -> " + group.GROUP_NAME + " - Took a bit less, " + slotsTaken.size() + " slots, with splitUpGroups = false");
                        }
                        else if(group.size() > 0){
                            slotsTaken = group.takeFirstSlots(numSlotsInRow, true);
                            if(slotsTaken == null || slotsTaken.size() == 0) VersusMod.MOD_LOGGER.error("[ ITEM SORTING ERROR ] " + group.GROUP_NAME + " - Tried to take " + numSlotsInRow + " slots, with splitUpGroups = true, but received nothing");
                            else if (DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.warn("                 " + group.GROUP_NAME + " - Took " + slotsTaken.size() + " slots, with splitUpGroups = true");
                            slotsToAdd.addAll(slotsTaken);
                        }
                        else if(DEBUG_SORTING_OUTPUT) VersusMod.MOD_LOGGER.warn("                 " + group.GROUP_NAME + " - Didn't take any slots, its size is zero.");
                    }
                }
                wasSuccessful = inventoryOutput.addAll(slotsToAdd, !hasAddedItems);
                slotsToAdd.clear();
                hasAddedItems = true;
                if(!wasSuccessful) return null;

                numIterations++;
                if(numIterations > 100) {
                    VersusMod.MOD_LOGGER.error("[ ITEM SORTING ERROR ] " + group.GROUP_NAME + " - Unable to extract all items, still has " + group.size() + ":\n" + group.toString() + "\n           SlotsTaken size: " + slotsTaken.size() + "\n           SlotsToAdd size: " + slotsToAdd + "\n           Row/Column Inventory: (" + inventoryOutput.getCurrentRow() + ", " + inventoryOutput.getCurrentCol() + ")");
                    return null;
                }
            }
        }

        // Finally, clear all and return
        if(isPlayerInventory) MAIN_HOTBAR.clear();
        for (SortingGroup group : SORTING_GROUPS) group.clear();
        return inventoryOutput.getInvSlots();
    }



    /** PRIVATE FUNCTIONS -  GROUP MERGING LOGIC  ----------------------------------------------- */

    /**
     * Inserts and sorts the ItemSlot into the most appropriate group.
     * @param includeHotbar Whether or not to include the main hotbar
     */
    public static void insertItemIntoGroup(ItemSlot newSlot, boolean includeHotbar, boolean isInDeepDark, boolean isInNether, boolean isInWater) {
        if(includeHotbar) {
            newSlot = MAIN_HOTBAR.tryInsertingSlot(newSlot, isInDeepDark, isInNether, isInWater);
            if(newSlot == null) return;
        }

        // Insert in the regular groups:
        for (SortingGroup group : SORTING_GROUPS) {
            newSlot = group.tryInsertingSlot(newSlot);
            if(newSlot == null) return;
        }

        // Fallback: Item didn't belong to any group.
        if(newSlot != null) RANDOM_GROUP.addSlot(newSlot);
    }
    public static void insertItemIntoGroup(ItemSlot newSlot) {

        // Insert in the regular groups:
        for (SortingGroup group : SORTING_GROUPS) {
            newSlot = group.tryInsertingSlot(newSlot);
            if(newSlot == null) return;
        }

        // Fallback: Item didn't belong to any group.
        if(newSlot != null) RANDOM_GROUP.addSlot(newSlot);
    }



    /** PRIVATE FUNCTIONS - INVENTORY PLACEMENT ------------------------------------------------ */
    /**
     * Merge groups together to fill hotbars as much as possible, and order the group by
     * @return Ordered list of non-empty groups, to add to the inventory
     */
    public static List<SortingGroup> getOrderedListOfGroups(int numEmptySlots, int numRows) {

        // Step 1: Get the list of non-empty groups in order of size (of leftovers)
        List<SortingGroup> nonEmptyGroups = Arrays.stream(SORTING_GROUPS).filter(g -> g.size() > 0).sorted().collect(Collectors.toList());
        numRows -=  nonEmptyGroups.stream().mapToInt(g -> g.size()/9).sum(); // Minus groups with two or more rows
        if(MAIN_HOTBAR.size() > 0) nonEmptyGroups.addLast(MAIN_HOTBAR);

        // Step 2: Get the number of rows to aim for, i.e. trying to fit everything together
        int numRowsIdeal = Math.min(numRows, 1 + ((numRows * 9) - numEmptySlots)/9);

        // Check trivial case:
        if(nonEmptyGroups.size() <= numRowsIdeal) return nonEmptyGroups.reversed();
        if(DEBUG_SORTING_MERGE) {
            String output = nonEmptyGroups.stream().map(g -> g.GROUP_NAME + " (Size: " + g.size() + ")").collect(Collectors.joining(", "));
            VersusMod.MOD_LOGGER.warn("[ INVENTORY SORTING ] Ordered list of groups: " + output);
        }

        // Step 3: Loop over each group. Try to find combinations of group sizes such that they both fit together
        LinkedList<SortingGroup> orderedGroups = new LinkedList<>();
        while(nonEmptyGroups.size() > 0 && (nonEmptyGroups.size() >= numRows || nonEmptyGroups.size() > numRowsIdeal)) {

            // Pop the next group:
            SortingGroup groupToPlace = nonEmptyGroups.removeFirst();
            orderedGroups.add(0, groupToPlace);
            if(groupToPlace.size() > 9) numRows--;
            if(DEBUG_SORTING_MERGE) VersusMod.MOD_LOGGER.warn("                   -> Next group to place: " + groupToPlace.GROUP_NAME + " of size " + groupToPlace.size() + " - (" + numRows + " rows and " + numEmptySlots + " left)");

            // Look for complimentary group. If the next one fits, combine!  (Note: nonEmptyGroups is sorted in asc order)
            while(nonEmptyGroups.size() > 0 && groupToPlace.size() < 9) {
                SortingGroup otherGroup = nonEmptyGroups.getFirst();
                if(groupToPlace.size() % 9 + otherGroup.size() <= 9) {
                    nonEmptyGroups.removeFirst();
                    groupToPlace.addSlots(otherGroup.takeAllItems());
                }
                else {
                    break;
                }
            }

            // Consider this being a full row:
            if(DEBUG_SORTING_MERGE) VersusMod.MOD_LOGGER.warn("                      Skipping next row for " + groupToPlace.GROUP_NAME + " of size " + groupToPlace.size() + " - (" + numRows + " rows and " + numEmptySlots + " left)");
            numEmptySlots -= 9 - groupToPlace.size();
            numRows--;
        }
        orderedGroups.addAll(0, nonEmptyGroups);
        return orderedGroups;
    }


    /** PRIVATE FUNCTIONS -  GROUP MERGING LOGIC  ----------------------------------------------- */

    /**
     * Merge groups together to fill hotbars as much as possible, and order the group by
     * @return Ordered list of non-empty groups, to add to the inventory
     */
    public static void cleanUpGroups(boolean isPlayerInventory) {

        // Step 1: Merge similar groups depending on inventory composition:
        if (REDSTONE_GROUP.size() > 0 && SortedItemLists.REDSTONE_RAW.size() > 0) {
            REDSTONE_GROUP.addSlots(SortedItemLists.REDSTONE_RAW.takeAll());
            MINERALS_GROUP.recalculateActualSize();
        }
        if (GOODIES_GROUP.size() > 0 && MINERALS_GROUP.size() > 0) tryCombiningTwoGroups(GOODIES_GROUP, MINERALS_GROUP);

        // Step 2: Clean up tool groups and merge them:
        if (isPlayerInventory) {
            if (MAIN_HOTBAR.size() > 0) cleanUpHotbar();
            cleanUpToolGroup(SHEARS_GROUP, HOE_GROUP);
            cleanUpToolGroup(HOE_GROUP, SHEARS_GROUP, SHOVEL_GROUP);
            cleanUpToolGroup(SHOVEL_GROUP, HOE_GROUP, SHEARS_GROUP);
            cleanUpToolGroup(PICKAXE_GROUP, AXE_GROUP, SHOVEL_GROUP, CONSUMABLES_GROUP);
            cleanUpToolGroup(AXE_GROUP, PICKAXE_GROUP, COMBAT_GROUP, CONSUMABLES_GROUP);
            giveExtraToolsFromAndTo(PICKAXE_GROUP, REDSTONE_GROUP);
            giveExtraToolsFromAndTo(PICKAXE_GROUP, MINERALS_GROUP);
            giveExtraToolsFromAndTo(COMBAT_GROUP, CONSUMABLES_GROUP);
            giveExtraToolsFromAndTo(AXE_GROUP, COMBAT_GROUP, CONSUMABLES_GROUP);
            giveExtraToolsFromAndTo(SHOVEL_GROUP, PICKAXE_GROUP);
        }
        cleanUpMisc();

        // Step 2: Try merging groups' lists into rows, if possible"
        for (SortingGroup group : SORTING_GROUPS) group.tryFormingRows();
    }

    private static boolean tryCombiningTwoGroups(SortingGroup groupThatReceives, SortingGroup groupThatGives) {
        int sizeBottom = groupThatReceives.size() % 9;
        int sizeTop = groupThatGives.size();
        if(sizeTop + sizeBottom <= 9 && sizeTop < 9) {
            groupThatReceives.addSlots(groupThatGives.takeAllItems());
            return true;
        }
        else if(sizeTop > 4 && sizeBottom > 4 && sizeBottom + sizeTop > 9) return false;

        int numItemsToSwap = 9 - sizeBottom;
        if(sizeBottom <= 0 || sizeTop <= 0 || sizeTop < numItemsToSwap) return false;

        LinkedList<ItemSlot> itemsTaken = groupThatGives.tryTakingExactNumSlots(numItemsToSwap, true, true);
        if(itemsTaken != null && itemsTaken.size() > 0) {

            // Success!! These two groups should go one after the other.
            groupThatReceives.addSlots(itemsTaken); // Complete first group to make a full row
            groupThatReceives.addSlots(groupThatGives.takeAllItems()); // Append second group entirely
            return true;
        }
        return false;
    }

    public static void cleanUpHotbar() {
        int size = MAIN_HOTBAR.size();
        if(size == 9) return;
        if(DEBUG_SORTING_MERGE) VersusMod.MOD_LOGGER.warn("[ INVENTORY SORTING ] ---- CLEANING HOTBAR -----");

        // Try adding combat items to hotbar:
        if(MAIN_HOTBAR.size() < 9) giveExtraToolsFromAndTo(COMBAT_GROUP, MAIN_HOTBAR);
        if(tryCombiningTwoGroups(MAIN_HOTBAR, CONSUMABLES_GROUP)) if(MAIN_HOTBAR.size() >= 9) return;
        if(tryCombiningTwoGroups(MAIN_HOTBAR, COMBAT_GROUP)) if(MAIN_HOTBAR.size() >= 9) return;
        if(MAIN_HOTBAR.size() < 9 && MAIN_HOTBAR.hasCombatItems()) {
            if(MAIN_HOTBAR.size() + COMBAT_GROUP.getNumTools() <= 9) MAIN_HOTBAR.addSlots(COMBAT_GROUP.takeAllTools());
            if(MAIN_HOTBAR.size() + CONSUMABLES_GROUP.size() <= 9) MAIN_HOTBAR.addSlots(CONSUMABLES_GROUP.takeAllItems());
        }
        if(MAIN_HOTBAR.size() >= 9) return;

        // If still space, try smartly adding tools and blocks to hotbar:
        if(!MAIN_HOTBAR.hasBuildingItems()) {
            if(MAIN_HOTBAR.size() + AXE_GROUP.size() <= 9) MAIN_HOTBAR.addSlots(AXE_GROUP.takeAllItems());
            else if(!MAIN_HOTBAR.hasAxe() && AXE_GROUP.canGiveawayTools()) {
                giveExtraToolsFromAndTo(AXE_GROUP, MAIN_HOTBAR);
                if(tryCombiningTwoGroups(MAIN_HOTBAR, AXE_GROUP)) return;
                if(MAIN_HOTBAR.size() >= 9) return;
            }
            if(MAIN_HOTBAR.size() + PICKAXE_GROUP.size() <= 9) MAIN_HOTBAR.addSlots(PICKAXE_GROUP.takeAllItems());
            else if(!MAIN_HOTBAR.hasPickaxe() && PICKAXE_GROUP.canGiveawayTools()) {
                giveExtraToolsFromAndTo(PICKAXE_GROUP, MAIN_HOTBAR);
                if(tryCombiningTwoGroups(MAIN_HOTBAR, PICKAXE_GROUP)) return;
                if(MAIN_HOTBAR.size() >= 9) return;
            }
            if(!MAIN_HOTBAR.hasBuildingItems() && SHOVEL_GROUP.getNumTools() > 0) {
                giveExtraToolsFromAndTo(SHOVEL_GROUP, MAIN_HOTBAR);
                if(tryCombiningTwoGroups(MAIN_HOTBAR, SHOVEL_GROUP)) return;
                if(MAIN_HOTBAR.size() >= 9) return;
            }
        }
        else {
            if(MAIN_HOTBAR.hasPickaxe() && tryCombiningTwoGroups(MAIN_HOTBAR, PICKAXE_GROUP)) return;
            if(MAIN_HOTBAR.hasAxe() && tryCombiningTwoGroups(MAIN_HOTBAR, AXE_GROUP)) return;
            if(MAIN_HOTBAR.size() < 9) giveExtraToolsFromAndTo(PICKAXE_GROUP, MAIN_HOTBAR);
            if(MAIN_HOTBAR.size() < 9) giveExtraToolsFromAndTo(AXE_GROUP, MAIN_HOTBAR);
            if(MAIN_HOTBAR.size() < 9) giveExtraToolsFromAndTo(SHOVEL_GROUP, MAIN_HOTBAR);
            if(MAIN_HOTBAR.size() < 9) giveExtraToolsFromAndTo(HOE_GROUP, MAIN_HOTBAR);
        }
        size = MAIN_HOTBAR.size();
        if(size < 9 && SHOVEL_GROUP.getNumTools() > 0 && tryCombiningTwoGroups(MAIN_HOTBAR, SHOVEL_GROUP)) return;
        if(size < 9 && HOE_GROUP.getNumTools() > 0 && tryCombiningTwoGroups(MAIN_HOTBAR, HOE_GROUP)) return;
        if(size < 9 && SHEARS_GROUP.getNumTools() > 0 && tryCombiningTwoGroups(MAIN_HOTBAR, SHEARS_GROUP)) return;

        // If hotbar has items, but no food, try to add some:
        if(size > 2 && size < 8 && (CONSUMABLES_GROUP.size() == 1 || CONSUMABLES_GROUP.size() > 3) && !MAIN_HOTBAR.hasAtLeastOneConsumable()) {
            int numConsumablesToGive = Math.min(9 - size, Math.max(1, CONSUMABLES_GROUP.size()/4));
            MAIN_HOTBAR.addSlots(CONSUMABLES_GROUP.takeFirstSlots(numConsumablesToGive, true));
        }
        if(DEBUG_SORTING_MERGE) VersusMod.MOD_LOGGER.warn(MAIN_HOTBAR.toString());
    }


    public static void cleanUpMisc() {
        int size = RANDOM_GROUP.size();
        if(size % 9 != 0) {
            if(tryCombiningTwoGroups(SHEARS_GROUP, RANDOM_GROUP)) return;
            if(tryCombiningTwoGroups(HOE_GROUP, RANDOM_GROUP)) return;
            if(tryCombiningTwoGroups(GOODIES_GROUP, RANDOM_GROUP)) return;
            if(tryCombiningTwoGroups(REDSTONE_GROUP, RANDOM_GROUP)) return;
            if(tryCombiningTwoGroups(SHOVEL_GROUP, RANDOM_GROUP)) return;

            if(SHEARS_GROUP.size() + size <= 9)
                SHEARS_GROUP.addSlots(RANDOM_GROUP.takeAllItems());

            if(GOODIES_GROUP.size() + RANDOM_GROUP.size() <= 9)
                GOODIES_GROUP.addSlots(RANDOM_GROUP.takeAllItems());

            if(GOODIES_GROUP.size() + RANDOM_GROUP.size() <= 9)
                GOODIES_GROUP.addSlots(RANDOM_GROUP.takeAllItems());

            if(SHEARS_GROUP.size() + size <= 9)
                SHEARS_GROUP.addSlots(RANDOM_GROUP.takeAllItems());
        }
    }

    public static void cleanUpToolGroup(ToolSortingGroup groupToCleanUp, ToolSortingGroup firstChoice) {
        cleanUpToolGroup(groupToCleanUp, firstChoice, null, null);
    }
    public static void cleanUpToolGroup(ToolSortingGroup groupToCleanUp, ToolSortingGroup firstChoice, SortingGroup secondChoice) {
        cleanUpToolGroup(groupToCleanUp, firstChoice, secondChoice, null);
    }
    public static void cleanUpToolGroup(ToolSortingGroup groupToCleanUp, ToolSortingGroup firstChoice, SortingGroup secondChoice, SortingGroup lastChoice) {
        if(groupToCleanUp.getNumTools() == 0 && (groupToCleanUp.size() < 9 || groupToCleanUp.size() % 9 <= 2)) {
            VersusMod.MOD_LOGGER.warn("                   -> " + groupToCleanUp.GROUP_NAME + " - Cleaning up tool group!");

            if(firstChoice != null && firstChoice.size() > 0 && firstChoice.getNumTools() > 0)
                if(groupToCleanUp != null && tryCombiningTwoGroups(firstChoice, groupToCleanUp)) return;

            if(secondChoice != null && groupToCleanUp.size() > 0 && secondChoice.size() > 0 && (!(secondChoice instanceof ToolSortingGroup) || ((ToolSortingGroup)secondChoice).getNumTools() > 0))
                if(secondChoice != null && tryCombiningTwoGroups(secondChoice, groupToCleanUp)) return;

            if(lastChoice != null && groupToCleanUp.size() > 0 && lastChoice.size() > 0)
                if(lastChoice != null && tryCombiningTwoGroups(secondChoice, groupToCleanUp)) return;

            VersusMod.MOD_LOGGER.warn("                      Adding the items of " + groupToCleanUp.GROUP_NAME + " to lastchoice");
            if(lastChoice != null && groupToCleanUp.size() > 0 && lastChoice.size() > 0) lastChoice.addSlots(groupToCleanUp.takeAllItems());
            else RANDOM_GROUP.addSlots(groupToCleanUp.takeAllItems());
        }
    }

    public static void giveExtraToolsFromAndTo(ToolSortingGroup groupToCleanUp, SortingGroup firstChoice) {
        giveExtraToolsFromAndTo(groupToCleanUp, firstChoice, null);
    }
    public static void giveExtraToolsFromAndTo(ToolSortingGroup groupToCleanUp, SortingGroup firstChoice, SortingGroup secondChoice) {
        if(groupToCleanUp.canGiveawayTools()) {
            if(firstChoice != null && firstChoice.size() > 0)
                firstChoice.addSlot(groupToCleanUp.takeWorstTool());

            if(secondChoice != null && groupToCleanUp.getNumTools() > 1 && secondChoice.size() > 0)
                secondChoice.addSlot(groupToCleanUp.takeWorstTool());
        }
    }
}
