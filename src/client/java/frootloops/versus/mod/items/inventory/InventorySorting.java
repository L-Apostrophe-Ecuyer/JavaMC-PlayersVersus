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

        // Fall back:
        if(inventory.getStack(slotOne).isOf(stackOne.getItem()) && inventory.getStack(slotOne).getCount() == stackOne.getCount()) {
            client.interactionManager.clickSlot(0, slotOne, 0, SlotActionType.PICKUP, client.player); // Grab the stack
            client.interactionManager.clickSlot(0, slotTwo, 0, SlotActionType.PICKUP, client.player); // Put it back
            client.interactionManager.clickSlot(0, slotOne, 0, SlotActionType.PICKUP, client.player); // Grab the stack
        }
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
        ItemSortingGroups.SortedItemGroup[] sortedGroups = new ItemSortingGroups.SortedItemGroup[]{sortedPickaxeGroup, sortedCombatGroup, sortedAxeGroup, sortedShovelGroup, sortedRedstoneGroup, sortedMiscGroup};
        for (SortedItemGroup group : sortedGroups) {
            group.clear();
        }
        VersusMod.MOD_LOGGER.warn("Groups: " + sortedGroups);


        // Sort stacks into groups:
        List<Integer> emptySlotsIndexes = new LinkedList<>();
        for(int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if(stack.isEmpty()) {
                emptySlotsIndexes.add(i);
                continue;
            }
            for (SortedItemGroup group : sortedGroups) {
                //VersusMod.MOD_LOGGER.warn("    Trying to insert " + stack.getItem().getName().getString()  + " of slot " + i + " into group " + group.getClass().getName().replace("frootloops.versus.mod.items.inventory.ItemSortingGroups$", ""));
                if(group.tryInsert(new InventorySlot(i, stack))) {
                    VersusMod.MOD_LOGGER.warn("     -> Inserted " + stack.getItem().getName().getString()  + " of slot " + i + " into group " + group.getClass().getName().replace("frootloops.versus.mod.items.inventory.ItemSortingGroups$", ""));
                    break;
                }
            }
        }

        // Give items to similar groups when possible:
        VersusMod.MOD_LOGGER.warn("Giving items to similar groups when possible.");
        if(sortedPickaxeGroup.size() > 9 && sortedPickaxeGroup.size() - 9 + sortedRedstoneGroup.size() <= 9) {
            sortedPickaxeGroup.giveExtrasTo(sortedRedstoneGroup);
        }
        if(sortedMiscGroup.size() > 9 && sortedMiscGroup.size() - 9 + sortedRedstoneGroup.size() <= 9) {
            sortedMiscGroup.giveExtrasTo(sortedRedstoneGroup);
        }
        if(sortedMiscGroup.size() > 9 && sortedMiscGroup.size() - 9 + sortedCombatGroup.size() <= 9) {
            sortedMiscGroup.giveExtrasTo(sortedCombatGroup);
        }


        // Merge similar groups together when possible:
        VersusMod.MOD_LOGGER.warn("Merging similar groups when possible.");
        if(sortedPickaxeGroup.size() > 0 && sortedRedstoneGroup.size() > 0 && sortedPickaxeGroup.size() + sortedRedstoneGroup.size() <= 9) {
            sortedPickaxeGroup.merge(sortedRedstoneGroup);
        }
        if(sortedCombatGroup.size() > 0 && sortedAxeGroup.size() > 0 && sortedCombatGroup.size() + sortedAxeGroup.size() <= 9) {
            sortedCombatGroup.merge(sortedAxeGroup);
        }
        if(sortedCombatGroup.size() > 0 && sortedMiscGroup.size() > 0 && sortedCombatGroup.size() + sortedMiscGroup.size() <= 9) {
            sortedCombatGroup.merge(sortedMiscGroup);
        }
        if(sortedRedstoneGroup.size() > 0 && sortedMiscGroup.size() > 0 && sortedRedstoneGroup.size() + sortedMiscGroup.size() <= 9) {
            sortedRedstoneGroup.merge(sortedMiscGroup);
        }
        if(sortedCombatGroup.size() > 0 && sortedShovelGroup.size() > 0 && sortedCombatGroup.size() + sortedShovelGroup.size() <= 9) {
            sortedCombatGroup.merge(sortedShovelGroup);
        }
        if(sortedMiscGroup.size() > 0 && sortedShovelGroup.size() > 0 && sortedMiscGroup.size() + sortedShovelGroup.size() <= 9) {
            sortedShovelGroup.merge(sortedMiscGroup);
        }


        VersusMod.MOD_LOGGER.warn("Groups before sorting:");
        int filledSlotNum = 0;
        for (SortedItemGroup group : sortedGroups) {
            if(group.size() == 0) continue;
            String groupItems = "     -> Group " + group.getClass().getSimpleName() + ": ";
            for(InventorySlot slot : group.inventorySlots) {
                groupItems += slot.stack().getItem().getName().getString() + ", ";
                filledSlotNum ++;
            }
            VersusMod.MOD_LOGGER.warn(groupItems.substring(0, groupItems.length() - 2));
        }

        VersusMod.MOD_LOGGER.warn("Empty slots:");
        String emptySlots = "     -> Empty slots: ";
        for(int slotId : emptySlotsIndexes) {
            emptySlots += slotId + ", ";
        }
        emptySlots = emptySlots.substring(0, emptySlots.length() - 2) + (" (There are " + emptySlotsIndexes.size() + " empty slots and " + filledSlotNum + " filled ones, total: " + (emptySlotsIndexes.size() + filledSlotNum)+ ") ");
        VersusMod.MOD_LOGGER.warn(emptySlots);


        // Sort groups into the four hotbars:
        int[][] hotbars = new int[4][9];
        int hotbarIndex = 0, hotbarSlotIndex = 0, numEmptySlots = emptySlotsIndexes.size(), numSlotsLeft = 36;
        for (SortedItemGroup group : sortedGroups) {
            while(group.size() > 0 && hotbarIndex < 4) {
                hotbars[hotbarIndex][hotbarSlotIndex] = group.popFirst().slodId() + 9;
                hotbarSlotIndex++;
                numSlotsLeft--;
                if(hotbarSlotIndex > 8) {
                    hotbarSlotIndex = 0;
                    hotbarIndex++;
                }
            }
            if(hotbarSlotIndex > numEmptySlots/(hotbarIndex + 3) && numEmptySlots > 8 - hotbarSlotIndex && numSlotsLeft > 8 - hotbarSlotIndex) {
                numEmptySlots -= 8 - hotbarSlotIndex;
                numSlotsLeft -= 8 - hotbarSlotIndex;
                hotbarSlotIndex = 0;
                hotbarIndex++;
            }
        }
        VersusMod.MOD_LOGGER.warn("Sorted items into hotbars!");


        // Sort the player's inventory accordingly!
        Map<Integer, Integer> newSlotIds = new HashMap<>();
        for(int i = 0; i < 4; i ++ ) {
            for(int j = 0; j < 9; j++) {
                int slot = ((i + 1) * 9) + j;
                int slotToSwapWith = hotbars[i][j] - 9;
                if(slotToSwapWith < 0) {
                    if(emptySlotsIndexes.size() > 0) slotToSwapWith = emptySlotsIndexes.removeFirst();
                    else {
                        for(int slotId = 0; slotId < 36; slotId++) {
                            if(inventory.getStack(slotId).isEmpty()) {
                                slotToSwapWith = slotId;
                                break;
                            }
                        }
                    }
                }
                if(newSlotIds.containsKey(slotToSwapWith)) {
                    slotToSwapWith = newSlotIds.get(slotToSwapWith);
                }
                newSlotIds.put(slot, slotToSwapWith);
                swapItemsFromSlots(client, inventory, slotToSwapWith, slot);
                if(!handler.getCursorStack().isEmpty()) {
                    if(inventory.getStack(slot).isEmpty()) client.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, client.player);
                    else client.interactionManager.clickSlot(handler.syncId, slotToSwapWith, 0, SlotActionType.PICKUP, client.player);
                }
            }
        }


        // Reset:
        for (SortedItemGroup group : sortedGroups) {
            group.clear();
        }
    }
}
