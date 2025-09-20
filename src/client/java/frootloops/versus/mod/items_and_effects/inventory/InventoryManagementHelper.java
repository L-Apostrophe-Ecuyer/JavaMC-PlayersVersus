package frootloops.versus.mod.items_and_effects.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

@Environment(EnvType.CLIENT)
public class InventoryManagementHelper {

    protected static void placeOrDropCursorStack(ScreenHandler handler, MinecraftClient client, Inventory inventory) {
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

    protected static void mergeStacksTogether(ScreenHandler handler, MinecraftClient client, Inventory inventory, int startingSlotIndex, int totalNumSlots) {
        ItemStack stackOne, stackTwo;
        int maxSlotIndex = startingSlotIndex + totalNumSlots; // + 9 + 1;
        for (int i = startingSlotIndex; i < maxSlotIndex; i++) {
            stackOne = inventory.getStack(i);
            if(stackOne.isEmpty() || !stackOne.isStackable() || stackOne.getCount() == stackOne.getMaxCount()) continue;
            for (int j = i + 1; j < maxSlotIndex; j++) {
                stackTwo = inventory.getStack(j);
                if(stackTwo.isEmpty() || !stackTwo.isOf(stackOne.getItem()) || stackTwo.getCount() == stackTwo.getMaxCount() || !ItemStack.areItemsAndComponentsEqual(stackOne, stackTwo)) continue;
                client.interactionManager.clickSlot(handler.syncId, j, 0, SlotActionType.PICKUP, client.player); // Grab the stack
                client.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, client.player); // Combine with other
                if(!handler.getCursorStack().isEmpty()) client.interactionManager.clickSlot(handler.syncId, j, 0, SlotActionType.PICKUP, client.player); // Place leftovers back down
                if(inventory.getStack(i).getCount() == stackOne.getMaxCount()) break;
            }
        }
    }
}
