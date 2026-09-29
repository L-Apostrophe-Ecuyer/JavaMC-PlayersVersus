package frootloops.versus.mod.items_and_effects.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public class InventoryManagementHelper {

    protected static void placeOrDropCursorStack(AbstractContainerMenu handler, Minecraft client, Container inventory) {
        ItemStack cursorStack = handler.getCarried();
        if(cursorStack.isEmpty()) return;

        // Find another stack to merge the cursor stack with:
        if(cursorStack.isStackable() && cursorStack.getCount() < cursorStack.getMaxStackSize()) {
            for (int i = 9; i < 44; i++) {
                ItemStack otherStack = inventory.getItem(i);
                if(!otherStack.isEmpty() && ItemStack.isSameItemSameComponents(cursorStack, otherStack)) {
                    client.gameMode.handleInventoryMouseClick(handler.containerId, i, 0, ClickType.PICKUP, client.player);
                    if(cursorStack.getCount() == 0 || cursorStack.isEmpty()) return;
                }
            }
        }

        // Find an empty slot to insert the cursor stack in:
        for (int i = 9; i < 44; i++) {
            if(inventory.getItem(i).isEmpty()) {
                client.gameMode.handleInventoryMouseClick(handler.containerId, i, 0, ClickType.PICKUP, client.player);
                return;
            }
        }

        // Drop the cursor stack:
        client.gameMode.handleInventoryMouseClick(handler.containerId, -999, 0, ClickType.THROW, client.player);
    }

    protected static void mergeStacksTogether(AbstractContainerMenu handler, Minecraft client, Container inventory, int startingSlotIndex, int totalNumSlots) {
        ItemStack stackOne, stackTwo;
        int maxSlotIndex = startingSlotIndex + totalNumSlots; // + 9 + 1;
        for (int i = startingSlotIndex; i < maxSlotIndex; i++) {
            stackOne = inventory.getItem(i);
            if(stackOne.isEmpty() || !stackOne.isStackable() || stackOne.getCount() == stackOne.getMaxStackSize()) continue;
            for (int j = i + 1; j < maxSlotIndex; j++) {
                stackTwo = inventory.getItem(j);
                if(stackTwo.isEmpty() || !stackTwo.is(stackOne.getItem()) || stackTwo.getCount() == stackTwo.getMaxStackSize() || !ItemStack.isSameItemSameComponents(stackOne, stackTwo)) continue;
                client.gameMode.handleInventoryMouseClick(handler.containerId, j, 0, ClickType.PICKUP, client.player); // Grab the stack
                client.gameMode.handleInventoryMouseClick(handler.containerId, i, 0, ClickType.PICKUP, client.player); // Combine with other
                if(!handler.getCarried().isEmpty()) client.gameMode.handleInventoryMouseClick(handler.containerId, j, 0, ClickType.PICKUP, client.player); // Place leftovers back down
                if(inventory.getItem(i).getCount() == stackOne.getMaxStackSize()) break;
            }
        }
    }
}
