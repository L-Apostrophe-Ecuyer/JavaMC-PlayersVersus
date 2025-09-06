package frootloops.versus.mod.items.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

@Environment(EnvType.CLIENT)
public abstract class HotbarCycling {

    public static void doHotbarSwap(PlayerInventory inventory, int numTypesToSwap) {
        if(numTypesToSwap % 4 == 0 || numTypesToSwap < 0) return;
        if(numTypesToSwap % 4 == 1) doHotbarSwap(inventory);
        if(numTypesToSwap % 4 == 3) doInverseHotbarSwap(inventory);
        else {
            for (int i = 0; i < 9; i++) {
                swapItemsFromSlots(inventory, i, i + 9);
                swapItemsFromSlots(inventory, i, i + 18);
                swapItemsFromSlots(inventory, i, i + 27);
            }
        }
    }

    public static void doHotbarSwap(PlayerInventory inventory, boolean skipEmptyRows) {
        for (int i = 0; i < 9; i++) {
            swapItemsFromSlots(inventory, i, i + 9);
            swapItemsFromSlots(inventory, i, i + 18);
            swapItemsFromSlots(inventory, i, i + 27);
        }
    }

    public static void doHotbarSwap(PlayerInventory inventory) {
        for (int i = 0; i < 9; i++) {
            swapItemsFromSlots(inventory, i, i + 9);
            swapItemsFromSlots(inventory, i, i + 18);
            swapItemsFromSlots(inventory, i, i + 27);
        }
    }

    public static void doInverseHotbarSwap(PlayerInventory inventory) {
        for (int i = 0; i < 9; i++) {
            swapItemsFromSlots(inventory, i + 9, i);
            swapItemsFromSlots(inventory,i + 18, i);
            swapItemsFromSlots(inventory, i + 27, i);
        }
    }

    private static void swapItemsFromSlots(PlayerInventory inventory, int slotOne, int slotTwo) {
        if(slotOne == slotTwo) return;
        ItemStack stackOne = inventory.getStack(slotOne);
        ItemStack stackTwo = inventory.getStack(slotTwo);
        if(stackOne.isEmpty() && stackTwo.isEmpty()) return;
        MinecraftClient.getInstance().interactionManager.clickSlot(0, slotTwo, slotOne, SlotActionType.SWAP, MinecraftClient.getInstance().player);
    }
}
