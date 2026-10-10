package frootloops.versus.mod.items_and_effects.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

import java.util.OptionalInt;

@Environment(EnvType.CLIENT)
public class InventoryManagementHelper {

    /**
     * Puts the carried stack back in the inventory's first {@code numSlots} slots, onto equal stacks first. Without room,
     * it stays on the cursor.
     */
    protected static void placeOrDropCursorStack(AbstractContainerMenu menu, Minecraft client, Container inventory, int numSlots) {
        ItemStack cursorStack = menu.getCarried();
        if(cursorStack.isEmpty()) return;

        // Find another stack to merge the cursor stack with:
        if(cursorStack.isStackable() && cursorStack.getCount() < cursorStack.getMaxStackSize()) {
            for (int i = 0; i < numSlots; i++) {
                ItemStack otherStack = inventory.getItem(i);
                OptionalInt menuSlot = menu.findSlot(inventory, i);
                if(menuSlot.isPresent() && !otherStack.isEmpty() && ItemStack.isSameItemSameComponents(menu.getCarried(), otherStack)) {
                    client.gameMode.handleContainerInput(menu.containerId, menuSlot.getAsInt(), 0, ContainerInput.PICKUP, client.player);
                    if(menu.getCarried().isEmpty()) return;
                }
            }
        }

        // Find an empty slot to insert the cursor stack in:
        for (int i = 0; i < numSlots; i++) {
            OptionalInt menuSlot = menu.findSlot(inventory, i);
            if(menuSlot.isPresent() && inventory.getItem(i).isEmpty()) {
                client.gameMode.handleContainerInput(menu.containerId, menuSlot.getAsInt(), 0, ContainerInput.PICKUP, client.player);
                return;
            }
        }
    }

    /**
     * Fills up partial stacks with equal stacks further on, among the inventory's first {@code numSlots} slots.
     */
    protected static void mergeStacksTogether(AbstractContainerMenu menu, Minecraft client, Container inventory, int numSlots) {
        for (int i = 0; i < numSlots; i++) {
            ItemStack stackOne = inventory.getItem(i);
            OptionalInt menuSlotOne = menu.findSlot(inventory, i);
            if(menuSlotOne.isEmpty() || stackOne.isEmpty() || !stackOne.isStackable() || stackOne.getCount() == stackOne.getMaxStackSize()) continue;
            for (int j = i + 1; j < numSlots; j++) {
                ItemStack stackTwo = inventory.getItem(j);
                OptionalInt menuSlotTwo = menu.findSlot(inventory, j);
                if(menuSlotTwo.isEmpty() || stackTwo.isEmpty() || stackTwo.getCount() == stackTwo.getMaxStackSize() || !ItemStack.isSameItemSameComponents(stackOne, stackTwo)) continue;
                client.gameMode.handleContainerInput(menu.containerId, menuSlotTwo.getAsInt(), 0, ContainerInput.PICKUP, client.player); // Grab the stack
                client.gameMode.handleContainerInput(menu.containerId, menuSlotOne.getAsInt(), 0, ContainerInput.PICKUP, client.player); // Combine with other
                if(!menu.getCarried().isEmpty()) client.gameMode.handleContainerInput(menu.containerId, menuSlotTwo.getAsInt(), 0, ContainerInput.PICKUP, client.player); // Place leftovers back down
                if(inventory.getItem(i).getCount() == inventory.getItem(i).getMaxStackSize()) break;
            }
        }
    }
}
