package frootloops.versus.mod.items_and_effects.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public abstract class ContainerDumping {

    private static final boolean DEBUG_MODE = false;

    public static final WidgetSprites TEXTURE_DUMP_TO_PLAYER_BUTTON = new WidgetSprites(Identifier.fromNamespaceAndPath("players-versus", "container/dump_to_player"), Identifier.fromNamespaceAndPath("players-versus", "container/dump_to_player_highlighted"));
    public static final WidgetSprites TEXTURE_DUMP_TO_STORAGE_BUTTON = new WidgetSprites(Identifier.fromNamespaceAndPath("players-versus", "container/dump_to_storage"), Identifier.fromNamespaceAndPath("players-versus", "container/dump_to_storage_highlighted"));

    private record InventorySlot(int slodId, ItemStack stack) {}


    public static void quickDumpIntoContainer(AbstractContainerMenu handler, Minecraft client, Container playerInventory, Container containerInventory) {
        int numSlotsInContainer = containerInventory.getContainerSize();
        InventoryManagementHelper.placeOrDropCursorStack(handler, client, playerInventory);
        InventoryManagementHelper.mergeStacksTogether(handler, client, containerInventory, 0, numSlotsInContainer);
        InventoryManagementHelper.mergeStacksTogether(handler, client, playerInventory, numSlotsInContainer, 36);

        doQuickDump(handler, client, playerInventory, containerInventory, 36, numSlotsInContainer, numSlotsInContainer);
        //InventorySorting.sortInventory(handler, client, containerInventory, InventorySorting.InventoryToSort.INVENTORY_WHILE_CHEST_OPEN, 0, numSlotsInContainer);
    }

    public static void quickDumpIntoPlayerInventory(AbstractContainerMenu handler, Minecraft client, Container playerInventory, Container containerInventory) {
        int numSlotsInContainer = containerInventory.getContainerSize();
        InventoryManagementHelper.placeOrDropCursorStack(handler, client, playerInventory);
        InventoryManagementHelper.mergeStacksTogether(handler, client, containerInventory, 0, numSlotsInContainer);
        InventoryManagementHelper.mergeStacksTogether(handler, client, playerInventory, numSlotsInContainer, 36);

        doQuickDump(handler, client, containerInventory, playerInventory, numSlotsInContainer, 36, 0);
        //InventorySorting.sortInventory(handler, client, playerInventory, InventorySorting.InventoryToSort.INVENTORY_WHILE_CHEST_OPEN, numSlotsInContainer, 36);
    }

    private static void doQuickDump(AbstractContainerMenu handler, Minecraft client, Container invOrigin, Container invDestination, int numSlotsOrigin, int numSlotsDestination, int startingIndexOrigin) {

        ItemStack stack;
        int numEmptySlots = 0;
        Map<Item,Integer> destinationItems = new HashMap<>();
        //List<Integer> destinationUniqueStacks = new ArrayList<>(numSlotsDestination/2);

        // First step: for each item in invDestination, store how many of that item there are. This will be used to know which items from invOrigin to send to invDestination
        for (int i = 0; i < numSlotsDestination; i++) {
            stack = invDestination.getItem(i);
            if(stack.isEmpty()) numEmptySlots++;

            else if(ItemStack.isSameItemSameComponents(stack, stack.getItem().getDefaultInstance())) {
                destinationItems.put(stack.getItem(), stack.getCount() + destinationItems.getOrDefault(stack.getItem(), 0));
            }
            //else destinationUniqueStacks.add(i);
        }

        // Second step: for each stack in invOrigin, if there is a matching item in invDestination, we should move it:
        //List<Integer> slotsToMove = new ArrayList<>(numSlotsOrigin);
        boolean isPlayerInventory = (startingIndexOrigin > 0 && numSlotsOrigin == 36);
        for (int i = 0; i < numSlotsOrigin; i++) {
            stack = invOrigin.getItem(i);
            if(!stack.isEmpty() && destinationItems.containsKey(stack.getItem()) && ItemStack.isSameItemSameComponents(stack, stack.getItem().getDefaultInstance())) {

                if(isPlayerInventory) {
                    int actualSlotOrigin = (i < 9) ? (i + startingIndexOrigin + 27) : i + startingIndexOrigin - 9;
                    client.gameMode.handleInventoryMouseClick(handler.containerId, actualSlotOrigin, 0, ClickType.QUICK_MOVE, client.player);
                }
                else client.gameMode.handleInventoryMouseClick(handler.containerId, i + startingIndexOrigin, 0, ClickType.QUICK_MOVE, client.player);
            }
        }
    }
}
