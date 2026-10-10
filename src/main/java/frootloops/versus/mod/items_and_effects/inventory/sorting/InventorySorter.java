package frootloops.versus.mod.items_and_effects.inventory.sorting;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.items_and_effects.inventory.sorting.groups.MainHotbarGroup;
import frootloops.versus.mod.items_and_effects.inventory.sorting.groups.SortingGroup;
import frootloops.versus.mod.items_and_effects.inventory.sorting.groups.SortingGroups;
import frootloops.versus.mod.items_and_effects.inventory.sorting.groups.ToolSortingGroup;
import frootloops.versus.mod.items_and_effects.inventory.sorting.lists.SortingLists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Sorts one inventory: puts each stack in a group, curates the player's hotbar, cleans up and merges the groups, orders
 * them, then lays them out in rows. Each sort works on its own lists and groups, so nothing carries over to the next.
 */
public final class InventorySorter {

    /**
     * Where the sort happens: a player's inventory, with the biome they're in, or a container.
     */
    public record Situation(boolean playerInventory, boolean inDeepDark, boolean inNether, boolean inWater) {
        public static final Situation CONTAINER = new Situation(false, false, false, false);
    }

    private final SortingLists lists = new SortingLists();
    private final SortingGroups groups = new SortingGroups(lists);
    private final MainHotbarGroup hotbar = groups.hotbar;
    private final Situation situation;

    private InventorySorter(Situation situation) {
        this.situation = situation;
    }

    /**
     * Lays the slots out in {@code numRows} rows: index {@code row * 9 + column}, {@code null} for an empty slot, row 0
     * being a player's hotbar. Empty when the layout gives up.
     */
    public static Optional<ItemSlot[]> sort(List<ItemSlot> slots, int numRows, Situation situation) {
        return Optional.ofNullable(new InventorySorter(situation).layOut(slots, numRows));
    }

    private void printGroups(String debugMsg) {
        if(!SortingDebug.ENABLED) return;
        StringBuilder output = new StringBuilder(debugMsg);
        if(hotbar.size() > 0) output.append(hotbar);
        for (SortingGroup group : groups.all()) if(group.size() > 0) output.append(group);
        SortingDebug.log(output::toString);
    }

    private void printGroups(String debugMsg, List<SortingGroup> orderedGroups) {
        if(!SortingDebug.ENABLED) return;
        StringBuilder output = new StringBuilder(debugMsg);
        for (SortingGroup group : orderedGroups) if(group.size() > 0) output.append(group);
        SortingDebug.log(output::toString);
    }

    /**
     * Makes a list of rows.
     */
    private ItemSlot[] layOut(List<ItemSlot> inventorySlots, int numRows) {
        boolean isPlayerInventory = situation.playerInventory();
        SortingDebug.log(() -> "[ INVENTORY SORTING ] Started sorting! Is Player Inventory? " + isPlayerInventory);
        int numItems = inventorySlots.size();
        int numEmptySlots = numRows * 9 - numItems;

        // Step 1: Populate ItemSortingGroups
        for(ItemSlot slot: inventorySlots) this.insertItemIntoGroup(slot, isPlayerInventory);
        this.printGroups("[ INVENTORY SORTING ] ---- AFTER INSERTING -----\n");

        // Step 2: Try forming rows withing a group, and combining similar groups
        if(isPlayerInventory) {
            this.cleanUpHotbar();
            for (ItemSlot slot : hotbar.keepOnlyEssentials()) this.insertItemIntoGroup(slot);
            this.printGroups("[ INVENTORY SORTING ] ---- AFTER HOTBAR CLEAN UP -----\n");
        }
        this.cleanUpGroups();
        this.printGroups("[ INVENTORY SORTING ] ---- AFTER GROUPS CLEAN UP & MERGE -----\n");

        // Step 3: Order the resulting non-empty groups such that they combine into rows
        List<SortingGroup> orderedGroups = this.getOrderedListOfGroups(numEmptySlots, numRows);
        this.printGroups("[ INVENTORY SORTING ] ---- AFTER ORDERING -----\n", orderedGroups);

        // Step 4: Finally, place sorted item groups into an inventory output
        SortingDebug.log(() -> "[ INVENTORY SORTING ] ---- STARTING TO INPUT INTO INVENTORY -----");
        SortedInventoryOutput inventoryOutput = new SortedInventoryOutput(numRows, numEmptySlots, orderedGroups.size(), isPlayerInventory);
        List<ItemSlot> slotsTaken = new ArrayList<>(), slotsToAdd = new ArrayList<>();

        // Case 4.1 - One Row Per List
        // If each individual sublist can have their own row, then do the work to split them:
        if(!isPlayerInventory) {
            int numRowsForEachList = 0;
            for(SortingGroup group : orderedGroups) numRowsForEachList += group.getMaxNumRows();
            if(numRowsForEachList <= numRows) {
                SortingDebug.log(() -> "              -> TRIVIAL PLACEMENT!! Placing each list into a row");
                for (SortingGroup group : orderedGroups) {
                    while(group.size() > 0) {
                        inventoryOutput.goToNextAvailableRow();
                        inventoryOutput.addAll(group.takeNextList(), true, true);
                    }
                }
                return inventoryOutput.getInvSlots();
            }
        }

        // Case 4.2 - One Row Per Group
        // If each group can have their own row, then best case scenario:
        if(orderedGroups.size() <= numRows) {
            int actualNumRowsInGroups = orderedGroups.size() + orderedGroups.stream().mapToInt(g -> (g.size() - 1)/9).sum();
            SortingDebug.log(() -> "              -> Actual number of rows in groups is " + actualNumRowsInGroups + ", the inventory has " + numRows);
            if(actualNumRowsInGroups <= numRows) {
                SortingDebug.log(() -> "              -> TRIVIAL PLACEMENT!! Placing each group into a row :D");
                for (SortingGroup group : orderedGroups) {
                    inventoryOutput.goToNextAvailableRow();
                    inventoryOutput.addAll(group.takeAllItems(), true, false);
                }
                return inventoryOutput.getInvSlots();
            }
        }

        // This is the tough part.
        // Otherwise, some groups may start at one row and end at another. We need to be smarter of how to place groups in each row:
        for(SortingGroup group : orderedGroups) {
            if(group.size() == 0) continue;
            SortingDebug.log(() -> "\n[ INVENTORY SORTING ] Inserting items of " + group.GROUP_NAME + " (Size " + group.size() + ")...");

            // Try to break down the rest of the rows into sorted lists:
            int numIterations = 0;
            boolean wasSuccessful;
            boolean hasAddedItems = false;
            while(group.size() > 0) {

                int numItemsToPlace = group.getNextListSize();
                int numSlotsInRow = inventoryOutput.getNumSlotsForNextBatch(numItemsToPlace, !hasAddedItems);
                slotsTaken = group.takeFirstSlots(numSlotsInRow, false);
                if(SortingDebug.ENABLED) VersusMod.MOD_LOGGER.info("              -> Next list size is " + numItemsToPlace + ", numSlotsInRow is "+ numSlotsInRow + ". Current group size is " + group.size());

                if(!slotsTaken.isEmpty()) {
                    if(SortingDebug.ENABLED) VersusMod.MOD_LOGGER.info("              -> " + group.GROUP_NAME + " - Took exactly " + slotsTaken.size() + " slots, with splitUpGroups = false");
                    slotsToAdd.addAll(slotsTaken);
                }
                else if(!isPlayerInventory && inventoryOutput.numGroupsToPlace == 1 && group.getMaxNumRows() < inventoryOutput.getNumEmptyRowsLeft()) {
                    SortingDebug.log(() -> "              -> " + group.GROUP_NAME + " - Taking each sublist of group to split into rows");
                    while(group.size() > 0) {
                        slotsToAdd.addAll(group.takeNextList());
                        wasSuccessful = inventoryOutput.addAll(slotsToAdd, !hasAddedItems, true);
                        slotsToAdd.clear();
                        hasAddedItems = true;
                        if(!wasSuccessful) return null;
                    }
                }
                else {
                    slotsTaken = group.tryTakingExactNumSlots(numSlotsInRow, !hasAddedItems, false);
                    if(!slotsTaken.isEmpty()) {
                        slotsToAdd.addAll(slotsTaken);
                        if(SortingDebug.ENABLED) VersusMod.MOD_LOGGER.info("              -> " + group.GROUP_NAME + " - Took exactly " + slotsTaken.size() + " slots, with splitUpGroups = false");
                    }
                    else {
                        if(numSlotsInRow > 1) slotsTaken = group.takeFirstSlots(numSlotsInRow, false);
                        if(!slotsTaken.isEmpty()) {
                            slotsToAdd.addAll(slotsTaken);
                            if(SortingDebug.ENABLED) VersusMod.MOD_LOGGER.info("              -> " + group.GROUP_NAME + " - Took a bit less, " + slotsTaken.size() + " slots, with splitUpGroups = false");
                        }
                        else if(group.size() > 0){
                            slotsTaken = group.takeFirstSlots(numSlotsInRow, true);
                            if(slotsTaken.isEmpty()) VersusMod.MOD_LOGGER.error("[ ITEM SORTING ERROR ] " + group.GROUP_NAME + " - Tried to take " + numSlotsInRow + " slots, with splitUpGroups = true, but received nothing. Group: " + group);
                            else if(SortingDebug.ENABLED) VersusMod.MOD_LOGGER.info("                 " + group.GROUP_NAME + " - Took " + slotsTaken.size() + " slots, with splitUpGroups = true");
                            slotsToAdd.addAll(slotsTaken);
                        }
                    }
                }
                wasSuccessful = inventoryOutput.addAll(slotsToAdd, !hasAddedItems, true);
                slotsToAdd.clear();
                hasAddedItems = true;
                if(!wasSuccessful) return null;

                numIterations++;
                if(numIterations > 30) {
                    VersusMod.MOD_LOGGER.error("[ ITEM SORTING ERROR ] " + group.GROUP_NAME + " - Unable to extract all items, still has " + group.size() + ":\n" + group + "\n           SlotsTaken size: " + slotsTaken.size() + "\n           Row/Column Inventory: (" + inventoryOutput.getCurrentRow() + ", " + inventoryOutput.getCurrentCol() + ")");
                    return null;
                }
            }
            inventoryOutput.markGroupAsDone();
        }
        return inventoryOutput.getInvSlots();
    }



    /** GROUP INSERTION ----------------------------------------------------------------------- */

    /**
     * Inserts and sorts the ItemSlot into the most appropriate group.
     * @param includeHotbar Whether or not to include the main hotbar
     */
    private void insertItemIntoGroup(ItemSlot newSlot, boolean includeHotbar) {
        if(includeHotbar) {
            newSlot = hotbar.tryInsertingSlot(newSlot, situation.inDeepDark(), situation.inNether(), situation.inWater());
            if(newSlot == null) return;
        }

        // Special case for trash, which should go in the back:
        if(newSlot.itemType() == ItemType.TRASH) {
            groups.random.addSlot(newSlot);
            return;
        }

        this.insertItemIntoGroup(newSlot);
    }

    private void insertItemIntoGroup(ItemSlot newSlot) {

        // Containers sort together, before the pickaxe group takes shulker boxes as blocks:
        if(newSlot.itemType() == ItemType.SHULKER_BOX || newSlot.itemType() == ItemType.BUNDLE || newSlot.itemType() == ItemType.ITEM_CONTAINER) {
            groups.containers.addSlot(newSlot);
            return;
        }

        // Insert in the regular groups:
        for (SortingGroup group : groups.all()) {
            newSlot = group.tryInsertingSlot(newSlot);
            if(newSlot == null) return;
        }

        // Fallback: Item didn't belong to any group.
        groups.random.addSlot(newSlot);
    }



    /** INVENTORY PLACEMENT ------------------------------------------------------------------- */

    /**
     * Merge groups together to fill hotbars as much as possible, and order the group by
     * @return Ordered list of non-empty groups, to add to the inventory
     */
    private List<SortingGroup> getOrderedListOfGroups(int numEmptySlots, int numRows) {

        // Step 1: Get the list of non-empty groups in order of size (of leftovers)
        List<SortingGroup> nonEmptyGroups = Arrays.stream(groups.all()).filter(g -> g.size() > 0).sorted().collect(Collectors.toList());
        numRows -= nonEmptyGroups.stream().mapToInt(g -> (g.size() - 1)/9).sum(); // Minus groups with two or more rows
        numRows -= hotbar.getMaxNumRows();

        // Step 2: Get the number of rows to aim for, i.e. trying to fit everything together
        int numRowsIdeal = Math.min(numRows, 1 + ((numRows * 9) - numEmptySlots)/9);
        if(SortingDebug.ENABLED) VersusMod.MOD_LOGGER.info("[ INVENTORY SORTING ] Ideal number of rows is: " + numRowsIdeal + ". Non-empty groups: " + nonEmptyGroups.stream().map(g -> g.GROUP_NAME + " (Size: " + g.size() + ")").collect(Collectors.joining(", ")));

        // Step 3: Optional. If no simple fit, Loop over each group. Try to find combinations of group sizes such that they both fit together
        List<SortingGroup> orderedGroups = new ArrayList<>();
        if(nonEmptyGroups.size() <= numRowsIdeal) {
            while (!nonEmptyGroups.isEmpty() && (nonEmptyGroups.size() >= numRows || nonEmptyGroups.size() > numRowsIdeal)) {

                // Pop the next group:
                SortingGroup groupToPlace = nonEmptyGroups.removeFirst();
                orderedGroups.add(groupToPlace);
                if (groupToPlace.size() > 9) numRows--;

                // Look for complimentary group. If the next one fits, combine!  (Note: nonEmptyGroups is sorted in asc order)
                while (!nonEmptyGroups.isEmpty() && groupToPlace.size() < 9) {
                    SortingGroup otherGroup = nonEmptyGroups.getFirst();
                    if ((groupToPlace.size() % 9) + otherGroup.size() <= 9) {
                        nonEmptyGroups.removeFirst();
                        groupToPlace.mergeWithOtherGroup(otherGroup);
                    } else {
                        break;
                    }
                }

                // Consider this being a full row:
                numEmptySlots -= 9 - groupToPlace.size();
                numRows--;
            }
        }
        orderedGroups.addAll(0, nonEmptyGroups.reversed());

        // Sort by order of item type:
        orderedGroups = new ArrayList<>(orderedGroups.stream().sorted(Comparator.comparing(SortingGroup::getItemType)).toList());

        // Make sure Hotbar is first:
        if (hotbar.size() > 0) orderedGroups.addFirst(hotbar);
        if(SortingDebug.ENABLED) VersusMod.MOD_LOGGER.info("[ INVENTORY SORTING ] Final ordered list of non-empty groups: " + orderedGroups.stream().map(g -> g.GROUP_NAME + " (Size: " + g.size() + ")").collect(Collectors.joining(", ")));
        return orderedGroups;
    }



    /** GROUP MERGING LOGIC ------------------------------------------------------------------- */

    /**
     * Merges similar groups depending on the inventory's composition, cleans up the tool groups, then tries to form
     * rows within each group.
     */
    private void cleanUpGroups() {

        // Step 1: Merge similar groups depending on inventory composition:
        if (groups.redstone.size() > 0 && lists.REDSTONE_RAW.size() > 0) {
            groups.redstone.addSlots(lists.REDSTONE_RAW.takeAll());
        }
        else if(groups.rareMinerals.size() > 0 && groups.rareMinerals.size() == lists.REDSTONE_RAW.size() && groups.brewing.size() > 0) {
            groups.brewing.addSlots(lists.REDSTONE_RAW.takeAll());
        }
        if (groups.goodies.size() > 0 && groups.rareMinerals.size() > 0) tryCombiningTwoGroups(groups.goodies, groups.rareMinerals);
        if (groups.rareMinerals.size() > 0 && groups.commonMinerals.size() > 0) {
            if(!tryCombiningTwoGroups(groups.rareMinerals, groups.commonMinerals) && groups.rareMinerals.size() + groups.commonMinerals.size() <= 9) groups.rareMinerals.mergeWithOtherGroup(groups.commonMinerals);
        }
        if (groups.brewing.size() > 0 && groups.consumables.size() > 0) {
            if(!tryCombiningTwoGroups(groups.consumables, groups.brewing)) {
                int numPotions = lists.POTION_ITEMS.size();
                int numConcentrates = lists.BREWING_INGREDIENTS.size();
                if(numPotions > 0 && numConcentrates > 0 && numPotions + numConcentrates > 4) {
                    for (ItemSlot slot : lists.BREWING_INGREDIENTS.takeAll()) lists.POTION_ITEMS.addWithoutSorting(slot);
                    groups.consumables.mergeWithOtherGroup(groups.brewing);
                }
            }
        }
        if (groups.brewing.size() > 0 && groups.world.size() > 0) {
            if(!tryCombiningTwoGroups(groups.brewing, groups.world) && groups.brewing.size() <= 2 && groups.world.size() <= 7) groups.world.mergeWithOtherGroup(groups.brewing);
        }

        // Step 2: Clean up tool groups and merge them:
        if (situation.playerInventory()) {
            this.cleanUpToolGroup(groups.shears, groups.hoes, groups.world);
            this.cleanUpToolGroup(groups.hoes, groups.shears, groups.shovels);
            this.cleanUpToolGroup(groups.shovels, groups.hoes, groups.shears);
            this.cleanUpToolGroup(groups.pickaxes, groups.axes, groups.shovels, groups.consumables);
            this.cleanUpToolGroup(groups.axes, groups.pickaxes, groups.combat, groups.consumables);
            if(lists.REDSTONE_COMPONENTS.size() > 2) this.giveExtraToolsFromAndTo(groups.pickaxes, groups.redstone);
            else if(lists.REDSTONE_COMPONENTS.size() > 0) tryCombiningTwoGroups(groups.pickaxes, groups.redstone);
            if(lists.SHULKER_BOXES.size() > 1 && !tryCombiningTwoGroups(groups.pickaxes, groups.containers)) this.giveExtraToolsFromAndTo(groups.pickaxes, groups.containers);
            if(lists.ORE_BLOCKS.size() > 2  && !tryCombiningTwoGroups(groups.pickaxes, groups.rareMinerals)) this.giveExtraToolsFromAndTo(groups.pickaxes, groups.rareMinerals);
            this.giveExtraToolsFromAndTo(groups.combat, groups.consumables);
            this.giveExtraToolsFromAndTo(groups.axes, groups.combat, groups.consumables);
            this.giveExtraToolsFromAndTo(groups.shovels, groups.pickaxes);
        }
        else {
            this.cleanUpToolGroup(groups.shears, groups.hoes, groups.world, groups.shovels);
            this.cleanUpToolGroup(groups.hoes, groups.shears, groups.shovels, groups.world);
            this.cleanUpToolGroup(groups.shovels, groups.hoes, groups.shears);
            this.cleanUpToolGroup(groups.pickaxes, groups.shovels, groups.axes);
            this.cleanUpToolGroup(groups.axes, groups.combat, groups.pickaxes);
            if(groups.shovels.hasOnlyTools()) this.cleanUpToolGroup(groups.shovels, groups.pickaxes, groups.axes);
            if(groups.hoes.hasOnlyTools()) this.cleanUpToolGroup(groups.hoes, groups.pickaxes, groups.axes);
            if(groups.axes.hasOnlyTools()) this.cleanUpToolGroup(groups.axes, groups.pickaxes);
        }
        this.cleanUpMisc();

        // Step 3: Try merging groups' lists into rows, if possible
        for (SortingGroup group : groups.all()) group.tryFormingRows();
    }

    private static boolean tryCombiningTwoGroups(SortingGroup groupThatReceives, SortingGroup groupThatGives) {
        int sizeBottom = groupThatReceives.size() % 9;
        int sizeTop = groupThatGives.size();
        if(sizeTop + sizeBottom == 9) {
            SortingDebug.log(() -> "               -> Merging " + groupThatReceives.GROUP_NAME + " with " + groupThatGives.GROUP_NAME + " -> Together they'll fit neatly in a row");
            groupThatReceives.mergeWithOtherGroup(groupThatGives);
            return true;
        }
        return false;
    }

    private void cleanUpHotbar() {
        int size = hotbar.size();
        if(size == 9) return;
        SortingDebug.log(() -> "[ INVENTORY SORTING ] ---- CLEANING HOTBAR -----");

        // If no weapon, try adding an axe:
        if(!hotbar.hasAxe() && groups.axes.getNumTools() > 0) {
            if(groups.axes.hasOnlyTools()) {
                hotbar.addSlot(groups.axes.takeBestTool());
                if(tryCombiningTwoGroups(hotbar, groups.axes)) return;
            }
            else if(groups.axes.canGiveawayTools()) hotbar.addSlot(groups.axes.takeBestTool());
        }
        if(hotbar.size() >= 9) return;

        // If still some space, and still no pickaxe, try adding one, and try combining groups:
        if(!hotbar.hasPickaxe() && groups.pickaxes.getNumTools() > 0) {
            if(tryCombiningTwoGroups(hotbar, groups.pickaxes)) return;
            else if(groups.pickaxes.hasOnlyTools()) {
                hotbar.addSlot(groups.pickaxes.takeBestTool());
                if(tryCombiningTwoGroups(hotbar, groups.pickaxes)) return;
            }
            else if(groups.pickaxes.canGiveawayTools()) hotbar.addSlot(groups.pickaxes.takeBestTool());
            else if(groups.pickaxes.size() + hotbar.size() < 9 && (hotbar.hasCombatItems() || groups.combat.size() == 0)) hotbar.mergeWithOtherGroup(groups.pickaxes);
        }
        if(hotbar.size() >= 9) return;

        // If still some space, and still no axe, try adding one:
        if(!hotbar.hasAxe() && groups.axes.canGiveawayTools()) hotbar.addSlot(groups.axes.takeBestTool());
        if(hotbar.size() >= 9) return;
        if(hotbar.hasAxe() && tryCombiningTwoGroups(hotbar, groups.axes)) return;

        // If in deep dark:
        if(situation.inDeepDark()) {
            this.giveExtraToolsFromAndTo(groups.hoes, hotbar);
            if(hotbar.size() >= 9) return;
            this.giveExtraToolsFromAndTo(groups.shears, hotbar);
            if(hotbar.size() >= 9) return;
        }

        // If still space, try smartly adding tools and blocks to hotbar:
        if(!hotbar.hasBuildingItems()) {
            if(groups.axes.size() > 0 && hotbar.size() + groups.axes.size() <= 9) hotbar.mergeWithOtherGroup(groups.axes);
            else if(groups.axes.hasOnlyTools() || (!hotbar.hasAxe() && groups.axes.canGiveawayTools())) {
                this.giveExtraToolsFromAndTo(groups.axes, hotbar);
                if(tryCombiningTwoGroups(hotbar, groups.axes)) return;
            }
            if(hotbar.size() >= 9) return;

            if(hotbar.size() + groups.pickaxes.size() <= 9) hotbar.mergeWithOtherGroup(groups.pickaxes);
            else if(groups.pickaxes.hasOnlyTools() || (!hotbar.hasPickaxe() && groups.pickaxes.canGiveawayTools())) {
                this.giveExtraToolsFromAndTo(groups.pickaxes, hotbar);
                if(tryCombiningTwoGroups(hotbar, groups.pickaxes)) return;
            }
            if(hotbar.size() >= 9) return;

            if(hotbar.size() + groups.shovels.size() <= 9) hotbar.mergeWithOtherGroup(groups.shovels);
            else if(groups.shovels.getNumTools() > 0) {
                this.giveExtraToolsFromAndTo(groups.shovels, hotbar);
                if(tryCombiningTwoGroups(hotbar, groups.shovels)) return;
            }
            if(hotbar.size() >= 9) return;

            if(groups.shears.getNumTools() > 0) {
                this.giveExtraToolsFromAndTo(groups.shears, hotbar);
                if(tryCombiningTwoGroups(hotbar, groups.shears)) return;
            }
            if(hotbar.size() >= 9) return;

            if(groups.hoes.getNumTools() > 0) {
                this.giveExtraToolsFromAndTo(groups.hoes, hotbar);
                if(tryCombiningTwoGroups(hotbar, groups.hoes)) return;
            }
            if(hotbar.size() >= 9) return;
            if(!hotbar.hasBuildingItems()) {
                if(hotbar.hasPickaxe() && groups.pickaxes.size() - groups.pickaxes.getNumTools() > 0) {
                    SortingDebug.log(() -> "  Will add pickaxe blocks to hotbar!");
                    if(!hotbar.addSlots(groups.pickaxes.tryTakingExactNumSlots(9 - hotbar.size(), groups.pickaxes.size() > 9, false)))
                        hotbar.addSlots(groups.pickaxes.takeFirstSlots(9 - hotbar.size(), true, true));
                }

                if(hotbar.hasAxe() && hotbar.size() < 9 && groups.axes.size() - groups.axes.getNumTools() > 0) {
                    SortingDebug.log(() -> "  Will add wood blocks to hotbar!");
                    if(!hotbar.addSlots(groups.axes.tryTakingExactNumSlots(9 - hotbar.size(), groups.axes.size() > 9, false)))
                        hotbar.addSlots(groups.axes.takeFirstSlots(9 - hotbar.size(), true, true));
                }
            }
        }
        else {
            if(hotbar.hasPickaxe() && tryCombiningTwoGroups(hotbar, groups.pickaxes)) return;
            if(hotbar.hasAxe() && tryCombiningTwoGroups(hotbar, groups.axes)) return;
            if(hotbar.size() < 9) this.giveExtraToolsFromAndTo(groups.pickaxes, hotbar);
            if(hotbar.size() < 9) this.giveExtraToolsFromAndTo(groups.axes, hotbar);
            if(hotbar.size() < 9) this.giveExtraToolsFromAndTo(groups.shovels, hotbar);
            if(hotbar.size() < 9) this.giveExtraToolsFromAndTo(groups.hoes, hotbar);
        }

        // Try adding combat items to hotbar:
        this.giveExtraToolsFromAndTo(groups.combat, hotbar);
        if(hotbar.size() >= 9) return;
        if(tryCombiningTwoGroups(hotbar, groups.consumables)) if(hotbar.size() >= 9) return;
        if(tryCombiningTwoGroups(hotbar, groups.combat)) if(hotbar.size() >= 9) return;
        if(hotbar.size() < 9 && hotbar.hasCombatItems()) {
            if(groups.combat.getNumTools() > 0 && hotbar.size() + groups.combat.getNumTools() <= 9) hotbar.addSlots(groups.combat.takeAllTools());
            if(groups.consumables.size() > 0 && hotbar.size() + groups.consumables.size() <= 9) hotbar.mergeWithOtherGroup(groups.consumables);
        }
        if(hotbar.size() >= 9) return;

        // If hotbar has items, but no food, try to add some:
        if(size > 2 && size < 8 && (groups.consumables.size() == 1 || groups.consumables.size() > 3) && !hotbar.hasAtLeastOneConsumable()) {
            int numConsumablesToGive = Math.min(9 - size, Math.max(1, groups.consumables.size()/4));
            hotbar.addSlots(groups.consumables.takeFirstSlots(numConsumablesToGive, true));
        }
        SortingDebug.log(hotbar::toString);
    }

    private void cleanUpMisc() {
        tryCombiningTwoGroups(groups.goodies, groups.containers);
        tryCombiningTwoGroups(groups.rareMinerals, groups.commonMinerals);
        tryCombiningTwoGroups(groups.world, groups.commonMinerals);
        tryCombiningTwoGroups(groups.commonMinerals, groups.random);
        if(groups.commonMinerals.size() > 0) {
            if(groups.rareMinerals.size() > 0 && groups.commonMinerals.size() + groups.rareMinerals.size() < 9)
                groups.rareMinerals.mergeWithOtherGroup(groups.commonMinerals);
            else if(groups.world.size() > 0 && groups.commonMinerals.size() + groups.world.size() < 9)
                groups.world.mergeWithOtherGroup(groups.commonMinerals);
        }

        tryCombiningTwoGroups(groups.world, groups.shears);
        tryCombiningTwoGroups(groups.world, groups.brewing);
        int size = groups.random.size();
        if(size % 9 != 0) {
            if(tryCombiningTwoGroups(groups.world, groups.random)) return;
            if(tryCombiningTwoGroups(groups.hoes, groups.random)) return;
            if(tryCombiningTwoGroups(groups.shears, groups.random)) return;
            if(tryCombiningTwoGroups(groups.goodies, groups.random)) return;
            if(tryCombiningTwoGroups(groups.redstone, groups.random)) return;
            if(tryCombiningTwoGroups(groups.shovels, groups.random)) return;

            if(groups.world.size() + size <= 9)
                groups.world.mergeWithOtherGroup(groups.random);

            else if(groups.shears.size() + size <= 9)
                groups.shears.mergeWithOtherGroup(groups.random);

            else if(groups.goodies.size() + groups.random.size() <= 9)
                groups.goodies.mergeWithOtherGroup(groups.random);

            else if(groups.containers.size() + groups.random.size() <= 9)
                groups.containers.mergeWithOtherGroup(groups.random);
        }
    }

    private void cleanUpToolGroup(ToolSortingGroup groupToCleanUp, ToolSortingGroup firstChoice, SortingGroup secondChoice) {
        this.cleanUpToolGroup(groupToCleanUp, firstChoice, secondChoice, null);
    }

    private void cleanUpToolGroup(ToolSortingGroup groupToCleanUp, ToolSortingGroup firstChoice) {
        this.cleanUpToolGroup(groupToCleanUp, firstChoice, null, null);
    }

    private void cleanUpToolGroup(ToolSortingGroup groupToCleanUp, ToolSortingGroup firstChoice, SortingGroup secondChoice, SortingGroup lastChoice) {
        boolean hasOnlyBlocks = groupToCleanUp.getNumTools() == 0 && groupToCleanUp.size() > 0 && groupToCleanUp.size() % 9 != 0 && (groupToCleanUp.size() < 9 || groupToCleanUp.size() % 9 <= 2);
        boolean hasOnlyTools = groupToCleanUp.hasOnlyTools() && groupToCleanUp.size() % 9 != 0 && (groupToCleanUp.size() < 6 || groupToCleanUp.size() % 9 <= 2);
        if(groupToCleanUp.size() >= ((hasOnlyBlocks || hasOnlyTools) ? 6 : 4)) return;
        SortingDebug.log(() -> "           -> " + groupToCleanUp.GROUP_NAME + " - Cleaning up tool group!");

        if(firstChoice != null && firstChoice.size() > 0 && firstChoice.getNumTools() > 0) {
            if (tryCombiningTwoGroups(firstChoice, groupToCleanUp)) return;
            else if (hasOnlyTools && tryCombiningTwoGroups(groupToCleanUp, firstChoice)) return;
            else if (hasOnlyTools && groupToCleanUp.size() + firstChoice.size() <= 9) {
                firstChoice.mergeWithOtherGroup(groupToCleanUp);
                return;
            }
        }

        if(secondChoice != null && groupToCleanUp.size() > 0 && secondChoice.size() > 0 && (!(secondChoice instanceof ToolSortingGroup secondToolChoice) || secondToolChoice.getNumTools() > 0)){
            if (tryCombiningTwoGroups(secondChoice, groupToCleanUp)) return;
            else if (hasOnlyTools && tryCombiningTwoGroups(groupToCleanUp, secondChoice)) return;
            else if (hasOnlyTools && groupToCleanUp.size() + secondChoice.size() <= 9) {
                secondChoice.mergeWithOtherGroup(groupToCleanUp);
                return;
            }
        }

        if(firstChoice != null && firstChoice.size() > 0 && (firstChoice.size() + groupToCleanUp.size() <= 9 || hasOnlyBlocks)) {
            if (secondChoice != null && tryCombiningTwoGroups(secondChoice, groupToCleanUp)) return;
            else if(groupToCleanUp.getNumTools() > 0) groupToCleanUp.mergeWithOtherGroup(firstChoice);
            else firstChoice.mergeWithOtherGroup(groupToCleanUp);
            return;
        }

        if(lastChoice != null && lastChoice.size() > 0 && (lastChoice.size() + groupToCleanUp.size() <= 9 || hasOnlyBlocks))
            if(tryCombiningTwoGroups(lastChoice, groupToCleanUp)) return;

        if(hasOnlyBlocks) {
            SortingDebug.log(() -> "                      Found no good match, but the building and random items of " + groupToCleanUp.GROUP_NAME + " will be given to last choice (or random)");
            if (lastChoice != null && groupToCleanUp.size() > 0 && lastChoice.size() > 0) lastChoice.mergeWithOtherGroup(groupToCleanUp);
            else groups.random.mergeWithOtherGroup(groupToCleanUp);
        }
    }

    private void giveExtraToolsFromAndTo(ToolSortingGroup groupToCleanUp, SortingGroup firstChoice) {
        this.giveExtraToolsFromAndTo(groupToCleanUp, firstChoice, null);
    }

    private void giveExtraToolsFromAndTo(ToolSortingGroup groupToCleanUp, SortingGroup firstChoice, SortingGroup secondChoice) {
        if(groupToCleanUp.canGiveawayTools()) {
            if(firstChoice != null && firstChoice.size() > 0)
                firstChoice.addSlot(firstChoice == hotbar ? groupToCleanUp.takeBestTool() : groupToCleanUp.takeWorstTool());

            if(secondChoice != null && groupToCleanUp.getNumTools() > 1 && secondChoice.size() > 0)
                secondChoice.addSlot(groupToCleanUp.takeWorstTool());
        }
        else if(groupToCleanUp.hasOnlyTools()) {
            if(firstChoice != null && firstChoice.size() > 0 && firstChoice.size() + groupToCleanUp.size() < 9)
                firstChoice.addSlots(groupToCleanUp.takeAllTools());

            else if(secondChoice != null && groupToCleanUp.getNumTools() > 1 && secondChoice.size() > 0)
                secondChoice.addSlots(groupToCleanUp.takeAllTools());
        }
    }
}
