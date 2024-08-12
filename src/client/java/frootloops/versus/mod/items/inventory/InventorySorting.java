package frootloops.versus.mod.items.inventory;
import frootloops.versus.VersusMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;


import static frootloops.versus.mod.items.inventory.ItemSortingGroups.*;

public class InventorySorting {

    private static final boolean DEBUG_MODE = false;

    public static final ButtonTextures TEXTURE_HOTBAR_SWAP_BUTTON = new ButtonTextures(Identifier.of("players-versus", "container/hotbar_swap_down"), Identifier.of("players-versus", "container/hotbar_swap_down_highlighted"));
    public static final ButtonTextures TEXTURE_INVENTORY_SORT_BUTTON = new ButtonTextures(Identifier.of("players-versus", "container/sort_inventory"), Identifier.of("players-versus", "container/sort_inventory_highlighted"));
    public static final ButtonTextures TEXTURE_SMALL_INVENTORY_SORT_BUTTON = new ButtonTextures(Identifier.of("players-versus", "container/sort_inventory_small"), Identifier.of("players-versus", "container/sort_inventory_small_highlighted"));
    public static final ButtonTextures TEXTURE_CHEST_SORT_BUTTON = new ButtonTextures(Identifier.of("players-versus", "container/sort_chest"), Identifier.of("players-versus", "container/sort_chest_highlighted"));
    public static final ButtonTextures TEXTURE_SHULKER_SORT_BUTTON = new ButtonTextures(Identifier.of("players-versus", "container/sort_shulker"), Identifier.of("players-versus", "container/sort_shulker_highlighted"));

    public static enum InventoryToSort {
        SURVIVAL_INVENTORY,
        CREATIVE_INVENTORY,
        INVENTORY_WHILE_CHEST_OPEN,
        CONTAINER_INVENTORY,
    }


    public static void sortInventory(ScreenHandler handler, MinecraftClient client, Inventory inventory, InventoryToSort inventoryType) {
        sortInventory(handler, client, inventory, inventoryType, 0, (inventoryType == InventoryToSort.CONTAINER_INVENTORY ? inventory.size() : 36));
    }

    public static void sortInventory(ScreenHandler handler, MinecraftClient client, Inventory inventory,  InventoryToSort inventoryType, int startingSlotIndex, int numSlots) {
        InventoryManagementHelper.placeOrDropCursorStack(handler, client, inventory);
        InventoryManagementHelper.mergeStacksTogether(handler, client, inventory, startingSlotIndex, numSlots);
        sortStacksIntoRows(handler, client, inventory, startingSlotIndex, numSlots, inventoryType);
    }

    private static void sortStacksIntoRows(ScreenHandler handler, MinecraftClient client, Inventory inventory, int startingSlotIndex, int totalNumSlots, InventoryToSort inventoryToSort) {

        if(DEBUG_MODE) VersusMod.MOD_LOGGER.warn("Started sorting! 8==============================================================================================D");
        int numHotbars =  totalNumSlots / 9;
        if(totalNumSlots < 2) return;

        // Note: Group order is important here
        ItemSortingGroups.SortedItemGroup[] sortedGroups = new ItemSortingGroups.SortedItemGroup[]{sortedRedstoneGroup, sortedPickaxeGroup, sortedCombatGroup, sortedAxeGroup, sortedShovelGroup, sortedHoesGroup, sortedShearsGroup, sortedRareGroup, sortedMiscGroup};
        for (SortedItemGroup group : sortedGroups) {
            group.clear();
        }
        if(DEBUG_MODE) VersusMod.MOD_LOGGER.warn("Groups: " + sortedGroups);


        // Sort stacks into groups:
        int numFilledSlots = 0, numEmptySlots = 0;
        for(int i = 0; i < totalNumSlots; i++) {
            ItemStack stack = inventory.getStack(i);
            if(stack.isEmpty()) {
                numEmptySlots++;
                continue;
            }

            numFilledSlots++;
            for (SortedItemGroup group : sortedGroups) {
                if(group.tryInsert(new InventorySlot(i, stack))) {
                    if(DEBUG_MODE) VersusMod.MOD_LOGGER.warn("     -> "+numFilledSlots +": Inserted " + stack.getItem().getName().getString()  + " of slot " + i + " into group " + group.getClass().getName().replace("frootloops.versus.mod.items.inventory.ItemSortingGroups$", ""));
                    break;
                }
            }
        }


        if(DEBUG_MODE || true) {
            VersusMod.MOD_LOGGER.warn("Groups before merging:");
            for (SortedItemGroup group : sortedGroups) {
                if(group.size() == 0) continue;
                String groupItems = "     -> Group " + group.getClass().getSimpleName() + " (Size "+ group.size() +"): ";
                for(InventorySlot slot : group.inventorySlots) {
                    groupItems += slot.stack().getItem().getName().getString() + ", ";
                }
                VersusMod.MOD_LOGGER.warn(groupItems.substring(0, groupItems.length() - 2));
            }
        }

        // Merge similar groups together when possible:
        if(DEBUG_MODE)VersusMod.MOD_LOGGER.warn("Merging similar groups when possible.");
        InventorySorting.tryCombiningSortedGroups(numEmptySlots);


        if(DEBUG_MODE) {
            VersusMod.MOD_LOGGER.warn("Groups after merging:");
            for (SortedItemGroup group : sortedGroups) {
                if(group.size() == 0) continue;
                String groupItems = "     -> Group " + group.getClass().getSimpleName() + " (Size "+ group.size() +"): ";
                for(InventorySlot slot : group.inventorySlots) {
                    groupItems += slot.stack().getItem().getName().getString() + ", ";
                }
                VersusMod.MOD_LOGGER.warn(groupItems.substring(0, groupItems.length() - 2));
            }
        }


        // Organize groups into the four hotbars:
        if(DEBUG_MODE) VersusMod.MOD_LOGGER.warn("Organizing items into hotbars!");
        int[] remappedSlots = new int[totalNumSlots];
        int hotbarIndex = 0, hotbarSlotIndex = 0, numSlotsLeft = totalNumSlots;
        for (SortedItemGroup group : sortedGroups) {

            // Check if we should skip to another hotbar:
            boolean canSkip = hotbarIndex < 3 && hotbarSlotIndex > 0 && numEmptySlots > 8 - hotbarSlotIndex && numSlotsLeft > 8 - hotbarSlotIndex;
            if(canSkip && (group.size() + hotbarSlotIndex) > 8 && (group.size() + hotbarSlotIndex - 8) < 3 + numEmptySlots) {
                if(DEBUG_MODE) VersusMod.MOD_LOGGER.warn("     -> Skip to next hotbar!");
                numEmptySlots -= 8 - hotbarSlotIndex;
                numSlotsLeft -= 8 - hotbarSlotIndex;
                hotbarSlotIndex = 0;
                hotbarIndex++;
            }

            // Attribute each ordered item of the group to the best positions in hotbars array:
            while(hotbarIndex < numHotbars && group.size() > 0) {
                remappedSlots[hotbarIndex * 9 + hotbarSlotIndex] = group.popFirst().slodId() + 9;
                if(DEBUG_MODE) VersusMod.MOD_LOGGER.warn("            -> Will move slot " + (remappedSlots[hotbarIndex * 9 + hotbarSlotIndex] - 9) + " to " + (hotbarIndex * 9 + hotbarSlotIndex) + " to populate it with " + (remappedSlots[hotbarIndex * 9 + hotbarSlotIndex] < 9 ? "Air" : (inventory.getStack(remappedSlots[hotbarIndex * 9 + hotbarSlotIndex] - 9).isEmpty()? "Air" : inventory.getStack(remappedSlots[hotbarIndex * 9 + hotbarSlotIndex] - 9).getItem().getName().getString())));
                hotbarSlotIndex++;
                numSlotsLeft--;
                if(hotbarSlotIndex > 8) {
                    hotbarSlotIndex = 0;
                    hotbarIndex++;
                }
            }

            // Check if we organized every item:
            if(numSlotsLeft == numEmptySlots) break;
        }

        if(DEBUG_MODE) {

            VersusMod.MOD_LOGGER.warn("");
            VersusMod.MOD_LOGGER.warn("     Planned: ");

            String remappedSlotsStr = "";
            String remappedSlotsAndItemsStr = "";
            for(int i = 0; i < totalNumSlots; i++){
                remappedSlotsStr += (remappedSlots[i] == 0 ? "  -," : (remappedSlots[i] - 9 < 10 ? "  " + (remappedSlots[i] - 9) + "," : " " + (remappedSlots[i] - 9) + ","));
                remappedSlotsAndItemsStr  += (remappedSlots[i] < 9 ? " -," : " " + (inventory.getStack(remappedSlots[i] - 9).getItem().getName().getString() + ","));
                if((i + 1) % 9 == 0) {
                    VersusMod.MOD_LOGGER.warn("     -> [" + (remappedSlotsStr.substring(0, remappedSlotsStr.length() - 1)) + " ]   -->   {" + (remappedSlotsAndItemsStr.substring(0, remappedSlotsAndItemsStr.length() - 1)) + " }");
                    remappedSlotsStr ="";
                    remappedSlotsAndItemsStr = "";
                }
            }

            VersusMod.MOD_LOGGER.warn("");
            VersusMod.MOD_LOGGER.warn("     Current: ");

            for(int i = 0; i < totalNumSlots; i++){
                remappedSlotsStr += inventory.getStack(i).isEmpty() ? "  -," : (i < 10 ? "  " + i + "," : " " + i + ",");
                remappedSlotsAndItemsStr  += (inventory.getStack(i).isEmpty() ? " -," : " " + (inventory.getStack(i).getItem().getName().getString() + ","));
                if((i + 1) % 9 == 0) {
                    VersusMod.MOD_LOGGER.warn("     -> [" + (remappedSlotsStr.substring(0, remappedSlotsStr.length() - 1)) + " ]   -->   {" + (remappedSlotsAndItemsStr.substring(0, remappedSlotsAndItemsStr.length() - 1)) + " }");
                    remappedSlotsStr ="";
                    remappedSlotsAndItemsStr = "";
                }
            }
        }



        // Sort the player's inventory accordingly!
        if(DEBUG_MODE) VersusMod.MOD_LOGGER.warn("Now actually modifying the player's inventory:");
        int[] displacedSlots = new int[totalNumSlots];
        int slotOrigin = -1, prevOrigin = -1;
        for(int slotDestination = 0; slotDestination < totalNumSlots; slotDestination ++ ) {

                slotOrigin = remappedSlots[slotDestination] - 9;
                if(slotOrigin < 0) {
                    if(inventory.getStack(slotDestination).isEmpty()) continue; // If the slot is meant to be empty, and already is, then no need to go through any more work
                    slotOrigin = slotDestination; // This is to make sure that, if for some reason there are no empty slots ahead (normally, impossible), then at least nothing breaks
                    prevOrigin = -1;
                    for(int slotId = totalNumSlots - 1; slotId > slotDestination; slotId--) {
                        if(inventory.getStack(slotId).isEmpty()) {
                            slotOrigin = slotId;
                            break;
                        }
                    }
                }
                else {
                    prevOrigin = slotOrigin;
                    while (displacedSlots[slotOrigin] != 0 && displacedSlots[slotOrigin] - 9 != slotOrigin && displacedSlots[slotOrigin] - 9 != prevOrigin) {
                        slotOrigin = displacedSlots[slotOrigin] - 9;
                    }
                }


                if(slotDestination != slotOrigin && (!inventory.getStack(slotOrigin).isEmpty() || !inventory.getStack(slotDestination).isEmpty())) {
                    displacedSlots[slotDestination] = slotOrigin + 9;

                    if(DEBUG_MODE) {
                        if (prevOrigin == -1) VersusMod.MOD_LOGGER.warn("            -> Now populating slot " + slotDestination + " with empty slot " + slotOrigin + ((prevOrigin != slotOrigin) ? " (in array, slot was " + (remappedSlots[slotDestination] - 9) +")": ""));
                        else if (prevOrigin != slotOrigin) VersusMod.MOD_LOGGER.warn("            -> Now populating slot " + slotDestination + " with slot " + prevOrigin + " (moved to " + slotOrigin + ")");
                        else VersusMod.MOD_LOGGER.warn("            -> Now populating slot " + slotDestination + " with slot " + slotOrigin);
                        VersusMod.MOD_LOGGER.warn("                   Origin: " + slotOrigin + " (" + inventory.getStack(slotOrigin).getItem().getName().getString() + ")");
                        VersusMod.MOD_LOGGER.warn("                   Dest. : " + slotDestination + " (" + inventory.getStack(slotDestination).getItem().getName().getString() + ")");
                    }

                    if(inventoryToSort == InventoryToSort.CREATIVE_INVENTORY) {
                        ItemStack stackOrigin = inventory.getStack(slotOrigin).copy();
                        inventory.setStack(slotOrigin, inventory.getStack(slotDestination).copy());
                        inventory.setStack(slotDestination, stackOrigin);
                    }
                    else if(inventoryToSort == InventoryToSort.CONTAINER_INVENTORY) {
                        client.interactionManager.clickSlot(handler.syncId, slotOrigin, 8, SlotActionType.SWAP, client.player);
                        client.interactionManager.clickSlot(handler.syncId, slotDestination, 8, SlotActionType.SWAP, client.player);
                        client.interactionManager.clickSlot(handler.syncId, slotOrigin, 8, SlotActionType.SWAP, client.player);
                    }
                    else if(inventoryToSort == InventoryToSort.INVENTORY_WHILE_CHEST_OPEN) {

                        // Inversed hotbar: in containers and chests, the hotbar slot indexes go AFTER regular inventory slots rather than before.
                        int actualSlotOrigin = (slotOrigin < 9) ? (slotOrigin + startingSlotIndex + 27) : slotOrigin + startingSlotIndex - 9;
                        int actualSlotDest = (slotDestination < 9) ? (slotDestination + startingSlotIndex + 27) : slotDestination + startingSlotIndex - 9;
                        int hotBarStartingIndex = startingSlotIndex + totalNumSlots - 9;

                        if(actualSlotDest >= hotBarStartingIndex && actualSlotOrigin >= hotBarStartingIndex) {
                            client.interactionManager.clickSlot(handler.syncId, actualSlotDest, (actualSlotOrigin - hotBarStartingIndex) % 9, SlotActionType.SWAP, client.player);
                        }
                        else if(actualSlotDest >= hotBarStartingIndex) {
                            client.interactionManager.clickSlot(handler.syncId, actualSlotOrigin, (actualSlotDest - hotBarStartingIndex) % 9, SlotActionType.SWAP, client.player);
                        }
                        else if(slotOrigin >= hotBarStartingIndex) {
                            client.interactionManager.clickSlot(handler.syncId, actualSlotDest, (actualSlotOrigin - hotBarStartingIndex) % 9, SlotActionType.SWAP, client.player);
                        }
                        else {
                            client.interactionManager.clickSlot(handler.syncId, actualSlotOrigin, 8, SlotActionType.SWAP, client.player);
                            client.interactionManager.clickSlot(handler.syncId, actualSlotDest, 8, SlotActionType.SWAP, client.player);
                            client.interactionManager.clickSlot(handler.syncId, actualSlotOrigin, 8, SlotActionType.SWAP, client.player);
                        }
                    }
                    else {
                        if(slotDestination < 9 && slotOrigin < 9) {
                            client.interactionManager.clickSlot(handler.syncId, slotDestination + totalNumSlots, slotOrigin, SlotActionType.SWAP, client.player);
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
                }

        }
        if(inventoryToSort == InventoryToSort.CREATIVE_INVENTORY) client.player.playerScreenHandler.sendContentUpdates();

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
