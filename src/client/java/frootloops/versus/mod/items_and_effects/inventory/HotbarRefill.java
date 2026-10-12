package frootloops.versus.mod.items_and_effects.inventory;

import frootloops.versus.VersusSettings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/**
 * When the stack in your hand runs out (the last block placed, the last torch, the last bite), the biggest matching stack
 * in the inventory takes its place: one ordinary swap click, the one the number keys send, so the server sees nothing
 * unusual. Only stackable items (a broken tool isn't replaced), only from the main inventory, and not when you dropped
 * the stack or swapped hands, or have a screen open. The offhand is refilled too.
 */
@Environment(EnvType.CLIENT)
public final class HotbarRefill {
    /** The swap click's button for the offhand, as the swap-hands key sends it. */
    private static final int OFFHAND_BUTTON = 40;
    private static final int MAIN_INVENTORY_START = 9, MAIN_INVENTORY_SIZE = 27;

    private static ItemStack previousMainHand = ItemStack.EMPTY, previousOffhand = ItemStack.EMPTY;
    private static int previousSlot = -1;
    private static boolean previousKeyMovedItems;

    private HotbarRefill() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(HotbarRefill::tick);
    }

    private static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (!VersusSettings.QOL.DO_HOTBAR_REFILL || player == null || minecraft.gameMode == null || minecraft.screen != null
                || player.isSpectator() || player.getAbilities().instabuild) {
            forget();
            return;
        }
        Inventory inventory = player.getInventory();
        int slot = inventory.getSelectedSlot();
        ItemStack mainHand = inventory.getItem(slot), offhand = player.getOffhandItem();
        boolean keyMovedItems = previousKeyMovedItems || minecraft.options.keyDrop.isDown() || minecraft.options.keySwapOffhand.isDown();

        if (HotbarRefillRules.usedUp(isStackable(previousMainHand), mainHand.isEmpty(), slot == previousSlot, keyMovedItems)) {
            refill(minecraft, player, previousMainHand, slot);
        }
        if (HotbarRefillRules.usedUp(isStackable(previousOffhand), offhand.isEmpty(), true, keyMovedItems)) {
            refill(minecraft, player, previousOffhand, OFFHAND_BUTTON);
        }

        previousSlot = slot;
        previousMainHand = mainHand.copy();
        previousOffhand = offhand.copy();
        previousKeyMovedItems = minecraft.options.keyDrop.isDown() || minecraft.options.keySwapOffhand.isDown();
    }

    private static boolean isStackable(ItemStack stack) {
        return !stack.isEmpty() && stack.getMaxStackSize() > 1;
    }

    /** Swaps the biggest stack of the same item in the main inventory into the hand (the hotbar slot, or the offhand's 40). */
    private static void refill(Minecraft minecraft, LocalPlayer player, ItemStack used, int button) {
        Inventory inventory = player.getInventory();
        int[] counts = new int[MAIN_INVENTORY_SIZE];
        for (int i = 0; i < MAIN_INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getItem(MAIN_INVENTORY_START + i);
            counts[i] = ItemStack.isSameItemSameComponents(stack, used) ? stack.getCount() : 0;
        }
        int best = HotbarRefillRules.largest(counts);
        if (best < 0) return;
        // The player's inventory menu numbers the main inventory like the inventory does, 9 to 35
        minecraft.gameMode.handleContainerInput(player.inventoryMenu.containerId, MAIN_INVENTORY_START + best, button, ContainerInput.SWAP, player);
    }

    private static void forget() {
        previousMainHand = ItemStack.EMPTY;
        previousOffhand = ItemStack.EMPTY;
        previousSlot = -1;
        previousKeyMovedItems = false;
    }
}
