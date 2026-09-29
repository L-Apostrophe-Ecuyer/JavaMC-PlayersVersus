package frootloops.versus.mod.items_and_effects.inventory;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public abstract class HotbarCycling {

    public static void doHotbarSwap(Inventory inventory, int numTypesToSwap) {
        if(numTypesToSwap > 0) {
            for (int i = 0; i < 9; i++) {
                swapItemsFromSlots(inventory, i, i + numTypesToSwap * 9);
                swapItemsFromSlots(inventory, i, i + (numTypesToSwap == 2 ? 27 : 18));
                swapItemsFromSlots(inventory, i, i + (numTypesToSwap == 1 ? 27 : 9));
            }
        }
    }

    public static void doHotbarSwap(Inventory inventory) {
        for (int i = 0; i < 9; i++) {
            swapItemsFromSlots(inventory, i, i + 9);
            swapItemsFromSlots(inventory, i, i + 18);
            swapItemsFromSlots(inventory, i, i + 27);
        }
    }

    public static void doInverseHotbarSwap(Inventory inventory) {
        for (int i = 0; i < 9; i++) {
            swapItemsFromSlots(inventory, i + 9, i);
            swapItemsFromSlots(inventory,i + 18, i);
            swapItemsFromSlots(inventory, i + 27, i);
        }
    }

    private static void swapItemsFromSlots(Inventory inventory, int slotOne, int slotTwo) {
        if(slotOne == slotTwo) return;
        ItemStack stackOne = inventory.getItem(slotOne);
        ItemStack stackTwo = inventory.getItem(slotTwo);
        if(stackOne.isEmpty() && stackTwo.isEmpty()) return;
        Minecraft.getInstance().gameMode.handleInventoryMouseClick(0, slotTwo, slotOne, ClickType.SWAP, Minecraft.getInstance().player);
    }
}
