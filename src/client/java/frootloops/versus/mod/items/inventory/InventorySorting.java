package frootloops.versus.mod.items.inventory;

import frootloops.versus.VersusMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.*;

import static frootloops.versus.mod.items.inventory.ItemSortingGroups.*;

public class InventorySorting {

    public static void swapItemsFromSlots(MinecraftClient client, PlayerInventory inventory, int slotOne, int slotTwo) {
        if(slotOne == slotTwo) return;
        ItemStack stackOne = inventory.getStack(slotOne);
        ItemStack stackTwo = inventory.getStack(slotTwo);
        if(stackOne.isEmpty() && stackTwo.isEmpty()) return;
        client.interactionManager.clickSlot(0, slotTwo, slotOne, SlotActionType.SWAP, client.player);
    }

    public static void doHotbarSwap(MinecraftClient client, PlayerInventory inventory) {
        for (int i = 0; i < 9; i++) {
            swapItemsFromSlots(client, inventory, i, i + 9);
            swapItemsFromSlots(client, inventory, i, i + 18);
            swapItemsFromSlots(client, inventory, i, i + 27);
        }
    }


    public static void sortInventory(ScreenHandler handler, MinecraftClient client, PlayerInventory inventory) {
        placeOrDropCursorStack(handler, client, inventory);
        mergeStacksTogether(handler, client, inventory);
        sortStacksIntoRows(handler, client, inventory);
    }

    private static void placeOrDropCursorStack(ScreenHandler handler, MinecraftClient client, PlayerInventory inventory) {
        ItemStack cursorStack = handler.getCursorStack();
        if(cursorStack.isEmpty()) return;

        // Find another stack to merge the cursor stack with:
        if(cursorStack.isStackable() && cursorStack.getCount() < cursorStack.getMaxCount()) {
            for (int i = 9; i < 44; i++) {
                ItemStack otherStack = inventory.getStack(i);
                if(!otherStack.isEmpty() && ItemStack.areItemsAndComponentsEqual(cursorStack, otherStack)) {
                    client.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, client.player);
                    if(cursorStack.getCount() == 0 || cursorStack.isEmpty()) return;
                }
            }
        }

        // Find an empty slot to insert the cursor stack in:
        for (int i = 9; i < 44; i++) {
            if(inventory.getStack(i).isEmpty()) {
                client.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, client.player);
                return;
            }
        }

        // Drop the cursor stack:
        client.interactionManager.clickSlot(handler.syncId, -999, 0, SlotActionType.THROW, client.player);
    }

    private static void mergeStacksTogether(ScreenHandler handler, MinecraftClient client, PlayerInventory inventory) {
        ItemStack stackOne, stackTwo;
        for (int i = 9; i < 44; i++) {
            stackOne = inventory.getStack(i);
            if(stackOne.isEmpty() || !stackOne.isStackable() || stackOne.getCount() == stackOne.getMaxCount()) continue;

            for (int j = i + 1; j < 44; j++) {
                stackTwo = inventory.getStack(j);
                if(stackTwo.isEmpty() || !stackTwo.isStackable() || stackTwo.getCount() == stackTwo.getMaxCount() || !ItemStack.areItemsAndComponentsEqual(stackOne, stackTwo)) continue;
                client.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, client.player); // Grab the stack
                client.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP_ALL, client.player); // Grab all matching items
                if(!handler.getCursorStack().isEmpty()) client.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, client.player); // Place them back down
            }
        }
    }

    public static void sortStacksIntoRows(ScreenHandler handler, MinecraftClient client, PlayerInventory inventory) {

        VersusMod.MOD_LOGGER.warn("Started sorting!");

        // Note: Group order is important here
        ItemSortingGroups.SortedItemGroup[] sortedGroups = new ItemSortingGroups.SortedItemGroup[]{sortedRedstoneGroup, sortedPickaxeGroup, sortedCombatGroup, sortedAxeGroup, sortedShovelGroup, sortedHoesGroup, sortedShearsGroup, sortedRareGroup, sortedMiscGroup};
        for (SortedItemGroup group : sortedGroups) {
            group.clear();
        }
        VersusMod.MOD_LOGGER.warn("Groups: " + sortedGroups);


        // Sort stacks into groups:
        int numFilledSlots = 0;
        List<Integer> emptySlotsIndexes = new LinkedList<>();
        for(int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if(stack.isEmpty()) {
                emptySlotsIndexes.add(i);
                continue;
            }
            numFilledSlots++;
            for (SortedItemGroup group : sortedGroups) {
                //VersusMod.MOD_LOGGER.warn("    Trying to insert " + stack.getItem().getName().getString()  + " of slot " + i + " into group " + group.getClass().getName().replace("frootloops.versus.mod.items.inventory.ItemSortingGroups$", ""));
                if(group.tryInsert(new InventorySlot(i, stack))) {
                    VersusMod.MOD_LOGGER.warn("     -> "+numFilledSlots +": Inserted " + stack.getItem().getName().getString()  + " of slot " + i + " into group " + group.getClass().getName().replace("frootloops.versus.mod.items.inventory.ItemSortingGroups$", ""));
                    break;
                }
            }
        }


        VersusMod.MOD_LOGGER.warn("Groups before merging:");
        for (SortedItemGroup group : sortedGroups) {
            if(group.size() == 0) continue;
            String groupItems = "     -> Group " + group.getClass().getSimpleName() + " (Size "+ group.size() +"): ";
            for(InventorySlot slot : group.inventorySlots) {
                groupItems += slot.stack().getItem().getName().getString() + ", ";
            }
            VersusMod.MOD_LOGGER.warn(groupItems.substring(0, groupItems.length() - 2));
        }

        // Merge similar groups together when possible:
        VersusMod.MOD_LOGGER.warn("Merging similar groups when possible.");
        int numEmptySlotsLeft = emptySlotsIndexes.size();
        InventorySorting.tryCombiningSortedGroups(numEmptySlotsLeft);


        VersusMod.MOD_LOGGER.warn("Groups after merging:");
        numFilledSlots = 0;
        for (SortedItemGroup group : sortedGroups) {
            if(group.size() == 0) continue;
            String groupItems = "     -> Group " + group.getClass().getSimpleName() + " (Size "+ group.size() +"): ";
            for(InventorySlot slot : group.inventorySlots) {
                groupItems += slot.stack().getItem().getName().getString() + ", ";
                numFilledSlots++;
            }
            VersusMod.MOD_LOGGER.warn(groupItems.substring(0, groupItems.length() - 2));
        }

        int numFilledSlotsAfterMerge = 0;
        for (SortedItemGroup group : sortedGroups) {
            numFilledSlotsAfterMerge += group.size();
        }
        if(numFilledSlotsAfterMerge != numFilledSlots) VersusMod.MOD_LOGGER.error("ERROR: The number of filled slots has changed! From " + numFilledSlots + " to " + numFilledSlotsAfterMerge);


        VersusMod.MOD_LOGGER.warn("Empty slots:");
        String emptySlots = "     -> Empty slots: ";
        for(int slotId : emptySlotsIndexes) {
            emptySlots += slotId + ", ";
        }
        emptySlots = emptySlots.substring(0, emptySlots.length() - 2) + (" (There are " + emptySlotsIndexes.size() + " empty slots and " + numFilledSlots + " filled ones, total: " + (emptySlotsIndexes.size() + numFilledSlots)+ ") ");
        VersusMod.MOD_LOGGER.warn(emptySlots);


        // Organize groups into the four hotbars:
        VersusMod.MOD_LOGGER.warn("Organizing items into hotbars!");
        int[][] hotbars = new int[4][9];
        int hotbarIndex = 0, hotbarSlotIndex = 0, numSlotsLeft = 36;
        for (SortedItemGroup group : sortedGroups) {

            // Check if we should skip to another hotbar:
            boolean canSkip = hotbarIndex < 3 && hotbarSlotIndex > 0 && numEmptySlotsLeft > 8 - hotbarSlotIndex && numSlotsLeft > 8 - hotbarSlotIndex;
            if(canSkip && (group.size() + hotbarSlotIndex) > 8 && (group.size() + hotbarSlotIndex - 8) < 3 + numEmptySlotsLeft) {
                VersusMod.MOD_LOGGER.warn("     -> Skip to next hotbar!");
                numEmptySlotsLeft -= 8 - hotbarSlotIndex;
                numSlotsLeft -= 8 - hotbarSlotIndex;
                hotbarSlotIndex = 0;
                hotbarIndex++;
            }

            // Attribute each ordered item of the group to the best positions in hotbars array:
            while(hotbarIndex < 4 && group.size() > 0) {
                hotbars[hotbarIndex][hotbarSlotIndex] = group.popFirst().slodId() + 9;
                VersusMod.MOD_LOGGER.warn("            -> Will move slot " + (hotbars[hotbarIndex][hotbarSlotIndex] - 9) + " to " + (hotbarIndex * 9 + hotbarSlotIndex) + " to populate it with " + (hotbars[hotbarIndex][hotbarSlotIndex] < 9 ? "Air" : (inventory.getStack(hotbars[hotbarIndex][hotbarSlotIndex] - 9).isEmpty()? "Air" : inventory.getStack(hotbars[hotbarIndex][hotbarSlotIndex] - 9).getItem().getName().getString())));
                hotbarSlotIndex++;
                numSlotsLeft--;
                if(hotbarSlotIndex > 8) {
                    hotbarSlotIndex = 0;
                    hotbarIndex++;
                }
            }

            // Check if we organized every item:
            if(numSlotsLeft == numEmptySlotsLeft) break;
        }

        VersusMod.MOD_LOGGER.warn("Final setup:");
        for(int i = 0; i < 4; i ++ ) {
            String hotbarItems = "     -> Hotbar " + i + ": ";
            for (int j = 0; j < 9; j++) {
                int slotId = hotbars[i][j] - 9;
                hotbarItems += (slotId < 0 ? "Air" : (inventory.getStack(slotId).isEmpty()? "Air" : inventory.getStack(slotId).getItem().getName().getString())) + ",";
            }
            VersusMod.MOD_LOGGER.warn(hotbarItems);
        }

        VersusMod.MOD_LOGGER.warn("Current inventory:");
        for(int i = 0; i < 4; i ++ ) {
            String hotbarItems = "     -> Hotbar " + i + ": ";
            for (int j = 0; j < 9; j++) {
                int slotId = (i * 9) + j;
                hotbarItems += (inventory.getStack(slotId).isEmpty()? "Air" : inventory.getStack(slotId).getItem().getName().getString()) + ",";
            }
            VersusMod.MOD_LOGGER.warn(hotbarItems);
        }


        // Sort the player's inventory accordingly!
        VersusMod.MOD_LOGGER.warn("Now actually modifying the player's inventory:");
        int[] displacedSlots = new int[36];
        for(int i = 0; i < 4; i ++ ) {

            String hotbarItems = "     -> Hotbar " + i + ": ";

            for(int j = 0; j < 9; j++) {
                int slotDestination = (i * 9) + j;
                int slotOrigin = hotbars[i][j] - 9;
                if(slotOrigin < 0) {
                    if(emptySlotsIndexes.size() > 0) slotOrigin = emptySlotsIndexes.removeFirst();
                    else {
                        for(int slotId = 0; slotId < 36; slotId++) {
                            if(inventory.getStack(slotId).isEmpty()) {
                                slotOrigin = slotId;
                                break;
                            }
                        }
                    }
                }

                int prevOrigin = slotOrigin;

                while(displacedSlots[slotOrigin] != 0 && displacedSlots[slotOrigin] - 9 !=  slotOrigin) {
                    slotOrigin = displacedSlots[slotOrigin] - 9;
                }


                if(prevOrigin != slotOrigin) VersusMod.MOD_LOGGER.warn("            -> Now populating slot " + slotDestination + " with slot " + prevOrigin + " (moved to " + slotOrigin + ")");
                else  VersusMod.MOD_LOGGER.warn("            -> Now populating slot " + slotDestination + " with slot " + slotOrigin);


                if(slotDestination != slotOrigin && (!inventory.getStack(slotOrigin).isEmpty() || !inventory.getStack(slotDestination).isEmpty())) {
                    displacedSlots[slotDestination] = slotOrigin + 9;

                    VersusMod.MOD_LOGGER.warn("               Moving Item! " + inventory.getStack(slotOrigin).getItem().getName().getString() + " moving from " + slotOrigin + " to " + slotDestination);
                    if(slotDestination < 9 && slotOrigin < 9) {
                        client.interactionManager.clickSlot(handler.syncId, slotDestination + 36, slotOrigin, SlotActionType.SWAP, client.player);
                    }
                    else if(slotDestination < 9) {
                        client.interactionManager.clickSlot(handler.syncId, slotOrigin, slotDestination, SlotActionType.SWAP, client.player);
                    }
                    else if(slotOrigin < 9) {
                        client.interactionManager.clickSlot(handler.syncId, slotDestination, slotOrigin, SlotActionType.SWAP, client.player);
                    }
                    else {
                        client.interactionManager.clickSlot(handler.syncId, slotOrigin, 8, SlotActionType.SWAP, client.player);
                        client.interactionManager.clickSlot(handler.syncId, slotDestination, 8, SlotActionType.SWAP, client.player);
                        client.interactionManager.clickSlot(handler.syncId, slotOrigin, 8, SlotActionType.SWAP, client.player);
                    }
                }

                hotbarItems += (slotOrigin < 9) ? " " + slotOrigin + ", " : slotOrigin + ", ";
            }
            VersusMod.MOD_LOGGER.warn(hotbarItems.substring(0, hotbarItems.length() - 2));
        }


        // Reset:
        for (SortedItemGroup group : sortedGroups) {
            group.clear();
        }
    }


    private static void tryCombiningSortedGroups(int numEmptySlots) {

        // Give items to similar groups when possible, when one is overflowing:
        if(sortedPickaxeGroup.size() > 9 && sortedPickaxeGroup.size() - 9 + sortedRedstoneGroup.size() <= 9) {
            sortedPickaxeGroup.giveExtrasTo(sortedRedstoneGroup);
        }
        if(sortedRareGroup.size() > 9 && sortedRareGroup.size() - 9 + sortedRedstoneGroup.size() <= 9) {
            sortedRareGroup.giveExtrasTo(sortedRedstoneGroup);
        }
        if(sortedMiscGroup.size() > 9 && sortedMiscGroup.size() - 9 + sortedRedstoneGroup.size() <= 9) {
            sortedMiscGroup.giveExtrasTo(sortedRedstoneGroup);
        }
        if(sortedMiscGroup.size() > 9 && sortedMiscGroup.size() - 9 + sortedCombatGroup.size() <= 9) {
            sortedMiscGroup.giveExtrasTo(sortedCombatGroup);
        }

        // Merge similar groups together when possible:
        int maxSizeToCombine = 9 - numEmptySlots/10;
        if(sortedPickaxeGroup.size() > 0 && sortedRedstoneGroup.size() > 0 && sortedPickaxeGroup.size() + sortedRedstoneGroup.size() <= maxSizeToCombine) {
            sortedPickaxeGroup.merge(sortedRedstoneGroup);
        }
        if(sortedHoesGroup.size() > 0 && sortedShearsGroup.size() > 0 && sortedHoesGroup.size() + sortedShearsGroup.size() <= maxSizeToCombine) {
            sortedHoesGroup.merge(sortedShearsGroup);
        }
        if(sortedHoesGroup.size() > 0 && sortedShovelGroup.size() > 0 && sortedHoesGroup.size() + sortedShovelGroup.size() <= maxSizeToCombine) {
            sortedHoesGroup.merge(sortedShovelGroup);
        }
        if(sortedCombatGroup.size() > 0 && sortedAxeGroup.size() > 0 && sortedCombatGroup.size() + sortedAxeGroup.size() <= maxSizeToCombine) {
            sortedCombatGroup.merge(sortedAxeGroup);
        }
        if(sortedCombatGroup.size() > 0 && sortedAxeGroup.size() > 0 && sortedCombatGroup.size() + sortedAxeGroup.size() <= maxSizeToCombine) {
            sortedCombatGroup.merge(sortedAxeGroup);
        }
        if(sortedCombatGroup.size() > 0 && sortedMiscGroup.size() > 0 && sortedCombatGroup.size() + sortedMiscGroup.size() <= maxSizeToCombine) {
            sortedCombatGroup.merge(sortedMiscGroup);
        }
        if(sortedHoesGroup.size() > 0 && sortedAxeGroup.size() > 0 && sortedHoesGroup.size() + sortedAxeGroup.size() <= maxSizeToCombine) {
            sortedHoesGroup.merge(sortedAxeGroup);
        }
        if(sortedRedstoneGroup.size() > 0 && sortedRareGroup.size() > 0 && sortedRedstoneGroup.size() + sortedRareGroup.size() <= maxSizeToCombine) {
            sortedRedstoneGroup.merge(sortedRareGroup);
        }
        if(sortedRedstoneGroup.size() > 0 && sortedMiscGroup.size() > 0 && sortedRedstoneGroup.size() + sortedMiscGroup.size() <= maxSizeToCombine) {
            sortedRedstoneGroup.merge(sortedMiscGroup);
        }
        if(sortedCombatGroup.size() > 0 && sortedShovelGroup.size() > 0 && sortedCombatGroup.size() + sortedShovelGroup.size() <= maxSizeToCombine) {
            sortedCombatGroup.merge(sortedShovelGroup);
        }
        if(sortedMiscGroup.size() > 0 && sortedShovelGroup.size() > 0 && sortedMiscGroup.size() + sortedShovelGroup.size() <= maxSizeToCombine) {
            sortedShovelGroup.merge(sortedMiscGroup);
        }
        if(sortedMiscGroup.size() > 0 && sortedHoesGroup.size() > 0 && sortedMiscGroup.size() + sortedHoesGroup.size() <= maxSizeToCombine) {
            sortedShovelGroup.merge(sortedMiscGroup);
        }
        if(sortedMiscGroup.size() > 0 && sortedShearsGroup.size() > 0 && sortedMiscGroup.size() + sortedShearsGroup.size() <= maxSizeToCombine) {
            sortedShovelGroup.merge(sortedMiscGroup);
        }
        if(sortedMiscGroup.size() > 0 && sortedRareGroup.size() > 0 && sortedMiscGroup.size() + sortedRareGroup.size() <= maxSizeToCombine) {
            sortedRareGroup.merge(sortedMiscGroup);
        }
    }
}
