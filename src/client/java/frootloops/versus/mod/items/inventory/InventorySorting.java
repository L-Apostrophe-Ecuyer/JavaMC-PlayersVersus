package frootloops.versus.mod.items.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;

public class InventorySorting {

    public static void swapItemsFromSlots(MinecraftClient client, PlayerInventory inventory, int slotOne, int slotTwo) {
        ItemStack stackOne = inventory.getStack(slotOne);
        ItemStack stackTwo = inventory.getStack(slotTwo);
        if(stackOne.isEmpty() && stackTwo.isEmpty()) return;
        client.interactionManager.clickSlot(0, slotTwo, slotOne, SlotActionType.SWAP, client.player);
    }

    public static void doHotbarSwap(MinecraftClient client, PlayerInventory inventory) {
        PlayerInventory playerInventory = client.player.getInventory();
        for (int i = 0; i < 9; i++) {
            swapItemsFromSlots(client, inventory, i, i + 9);
            swapItemsFromSlots(client, inventory, i, i + 18);
            swapItemsFromSlots(client, inventory, i, i + 27);
        }
    }


    private record SwappableSlot(String name, int originalSlotID, int newSlotID) {}
    public static void sortInventory(PlayerInventory inventory) {

        List<SwappableSlot> listTools = new ArrayList<>();
        List<SwappableSlot> listCombat = new ArrayList<>();
        List<SwappableSlot> listConsumables = new ArrayList<>();
        List<SwappableSlot> listPickaxeMineable = new ArrayList<>();
        List<SwappableSlot> listAxeMineable = new ArrayList<>();
        List<SwappableSlot> listShovelMineable = new ArrayList<>();
        List<SwappableSlot> listRedstone = new ArrayList<>();
        List<SwappableSlot> listNature = new ArrayList<>();
        List<SwappableSlot> listMisc = new ArrayList<>();

        for(int i = 0; i < 36; i++) {
            ItemStack slotStack = inventory.getStack(i);
            if(!slotStack.isEmpty()) {

            }
        }

    }

}
