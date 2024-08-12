package frootloops.versus.mod.items.inventory;

import frootloops.versus.VersusMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ContainerDumping {

    private static final boolean DEBUG_MODE = false;

    public static final ButtonTextures TEXTURE_DUMP_TO_PLAYER_BUTTON = new ButtonTextures(Identifier.of("players-versus", "container/dump_to_player"), Identifier.of("players-versus", "container/dump_to_player_highlighted"));
    public static final ButtonTextures TEXTURE_DUMP_TO_STORAGE_BUTTON = new ButtonTextures(Identifier.of("players-versus", "container/dump_to_storage"), Identifier.of("players-versus", "container/dump_to_storage_highlighted"));

    private record InventorySlot(int slodId, ItemStack stack) {}


    public static void quickDumpIntoContainer(ScreenHandler handler, MinecraftClient client, Inventory playerInventory, Inventory containerInventory) {
        int numSlotsInContainer = containerInventory.size();
        InventoryManagementHelper.placeOrDropCursorStack(handler, client, playerInventory);
        InventoryManagementHelper.mergeStacksTogether(handler, client, containerInventory, 0, numSlotsInContainer);
        InventoryManagementHelper.mergeStacksTogether(handler, client, playerInventory, numSlotsInContainer, 36);

        doQuickDump(handler, client, playerInventory, containerInventory, 36, numSlotsInContainer, numSlotsInContainer);
        //InventorySorting.sortInventory(handler, client, containerInventory, InventorySorting.InventoryToSort.INVENTORY_WHILE_CHEST_OPEN, 0, numSlotsInContainer);
    }

    public static void quickDumpIntoPlayerInventory(ScreenHandler handler, MinecraftClient client, Inventory playerInventory, Inventory containerInventory) {
        int numSlotsInContainer = containerInventory.size();
        InventoryManagementHelper.placeOrDropCursorStack(handler, client, playerInventory);
        InventoryManagementHelper.mergeStacksTogether(handler, client, containerInventory, 0, numSlotsInContainer);
        InventoryManagementHelper.mergeStacksTogether(handler, client, playerInventory, numSlotsInContainer, 36);

        doQuickDump(handler, client, containerInventory, playerInventory, numSlotsInContainer, 36, 0);
        //InventorySorting.sortInventory(handler, client, playerInventory, InventorySorting.InventoryToSort.INVENTORY_WHILE_CHEST_OPEN, numSlotsInContainer, 36);
    }

    private static void doQuickDump(ScreenHandler handler, MinecraftClient client, Inventory invOrigin, Inventory invDestination, int numSlotsOrigin, int numSlotsDestination, int startingIndexOrigin) {

        ItemStack stack;
        int numEmptySlots = 0;
        Map<Item,Integer> destinationItems = new HashMap<>();
        //List<Integer> destinationUniqueStacks = new ArrayList<>(numSlotsDestination/2);

        // First step: for each item in invDestination, store how many of that item there are. This will be used to know which items from invOrigin to send to invDestination
        for (int i = 0; i < numSlotsDestination; i++) {
            stack = invDestination.getStack(i);
            if(stack.isEmpty()) numEmptySlots++;

            else if(ItemStack.areItemsAndComponentsEqual(stack, stack.getItem().getDefaultStack())) {
                destinationItems.put(stack.getItem(), stack.getCount() + destinationItems.getOrDefault(stack.getItem(), 0));
            }
            //else destinationUniqueStacks.add(i);
        }

        // Second step: for each stack in invOrigin, if there is a matching item in invDestination, we should move it:
        //List<Integer> slotsToMove = new ArrayList<>(numSlotsOrigin);
        boolean isPlayerInventory = (startingIndexOrigin > 0 && numSlotsOrigin == 36);
        for (int i = 0; i < numSlotsOrigin; i++) {
            stack = invOrigin.getStack(i);
            if(!stack.isEmpty() && destinationItems.containsKey(stack.getItem()) && ItemStack.areItemsAndComponentsEqual(stack, stack.getItem().getDefaultStack())) {

                if(isPlayerInventory) {
                    int actualSlotOrigin = (i < 9) ? (i + startingIndexOrigin + 27) : i + startingIndexOrigin - 9;
                    client.interactionManager.clickSlot(handler.syncId, actualSlotOrigin, 0, SlotActionType.QUICK_MOVE, client.player);
                }
                else client.interactionManager.clickSlot(handler.syncId, i + startingIndexOrigin, 0, SlotActionType.QUICK_MOVE, client.player);
            }
        }
    }
}
