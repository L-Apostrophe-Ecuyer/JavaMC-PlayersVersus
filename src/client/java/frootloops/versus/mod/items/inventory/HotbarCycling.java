package frootloops.versus.mod.items.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

public class HotbarCycling {

    public static void doHotbarSwap(MinecraftClient client, PlayerInventory inventory) {
        for (int i = 0; i < 9; i++) {
            swapItemsFromSlots(client, inventory, i, i + 9);
            swapItemsFromSlots(client, inventory, i, i + 18);
            swapItemsFromSlots(client, inventory, i, i + 27);
        }
    }

    private static void swapItemsFromSlots(MinecraftClient client, PlayerInventory inventory, int slotOne, int slotTwo) {
        if(slotOne == slotTwo) return;
        ItemStack stackOne = inventory.getStack(slotOne);
        ItemStack stackTwo = inventory.getStack(slotTwo);
        if(stackOne.isEmpty() && stackTwo.isEmpty()) return;
        client.interactionManager.clickSlot(0, slotTwo, slotOne, SlotActionType.SWAP, client.player);
    }
}
