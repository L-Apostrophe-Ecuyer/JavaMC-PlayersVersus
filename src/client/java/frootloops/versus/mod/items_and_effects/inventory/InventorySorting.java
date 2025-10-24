package frootloops.versus.mod.items_and_effects.inventory;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemComparaisonHelper;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items_and_effects.inventory.sorting.SortingHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;


import java.util.Arrays;
import java.util.LinkedList;

@Environment(EnvType.CLIENT)
public class InventorySorting {

    public static final boolean DEBUG_SORTING_GROUPS = true;
    public static final boolean DEBUG_SORTING_MERGE = true;
    public static final boolean DEBUG_SORTING_OUTPUT = true;
    private static final boolean DEBUG_ITEM_SWITICHING = true;

    public static final ButtonTextures TEXTURE_HOTBAR_SWAP_BUTTON = new ButtonTextures(Identifier.of("players-versus", "container/hotbar_swap_down"), Identifier.of("players-versus", "container/hotbar_swap_down_highlighted"));
    public static final ButtonTextures TEXTURE_INVENTORY_SORT_BUTTON = new ButtonTextures(Identifier.of("players-versus", "container/sort_inventory"), Identifier.of("players-versus", "container/sort_inventory_highlighted"));
    public static final ButtonTextures TEXTURE_SMALL_INVENTORY_SORT_BUTTON = new ButtonTextures(Identifier.of("players-versus", "container/sort_inventory_small"), Identifier.of("players-versus", "container/sort_inventory_small_highlighted"));
    public static final ButtonTextures TEXTURE_CHEST_SORT_BUTTON = new ButtonTextures(Identifier.of("players-versus", "container/sort_chest"), Identifier.of("players-versus", "container/sort_chest_highlighted"));
    public static final ButtonTextures TEXTURE_SHULKER_SORT_BUTTON = new ButtonTextures(Identifier.of("players-versus", "container/sort_shulker"), Identifier.of("players-versus", "container/sort_shulker_highlighted"));

    public enum InventoryToSort {
        SURVIVAL_INVENTORY,
        CREATIVE_INVENTORY,
        INVENTORY_WHITH_SLOTS_ABOVE,
        CONTAINER_INVENTORY,
    }


    public static void sortInventory(ScreenHandler handler, MinecraftClient client, Inventory inventory, InventoryToSort inventoryType) {
        sortInventory(handler, client, inventory, inventoryType, 0, (inventoryType == InventoryToSort.CONTAINER_INVENTORY ? inventory.size() : 36));
    }

    public static void sortInventory(ScreenHandler handler, MinecraftClient client, Inventory inventory, InventoryToSort inventoryType, int startingSlotIndex, int numSlots) {
        InventoryManagementHelper.placeOrDropCursorStack(handler, client, inventory);
        InventoryManagementHelper.mergeStacksTogether(handler, client, inventory, (inventoryType == InventoryToSort.SURVIVAL_INVENTORY || inventoryType == InventoryToSort.CREATIVE_INVENTORY) ? 9 : 0, numSlots);
        sortInventory(handler, client, inventory, startingSlotIndex, numSlots, inventoryType);
    }

    private static int[] getRemappedSlotIndices(Inventory inventory, int numRows, int totalNumSlots, boolean isPlayerInventory) {
        int[] remappedSlotIndices = new int[totalNumSlots];
        LinkedList<ItemSlot> slots = new LinkedList<>();
        for (int i = 0; i < totalNumSlots; i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) slots.add(new ItemSlot(i, stack, ItemComparaisonHelper.getItemTypeOf(stack)));
        }

        // For situational hotbar item priority:
        boolean isInDeepDark = false, isInNether = false, isInWater = false;
        if(isPlayerInventory) {
            RegistryEntry<Biome> playerBiome = MinecraftClient.getInstance().world.getBiome(MinecraftClient.getInstance().player.getBlockPos()); 
            isInDeepDark = playerBiome.matchesKey(BiomeKeys.DEEP_DARK);
            isInNether = !isInDeepDark && playerBiome.isIn(BiomeTags.IS_NETHER);
            isInWater = !isInDeepDark && (playerBiome.isIn(BiomeTags.IS_OCEAN) || MinecraftClient.getInstance().player.isSubmergedInWater());
        }

        ItemSlot[] newSlots = SortingHelper.getOptimalInventoryRows(slots, numRows, isPlayerInventory, isInDeepDark, isInNether, isInWater);
        for (int i = 0; i < totalNumSlots; i++) {
            if (newSlots[i] != null) {
                remappedSlotIndices[i] = newSlots[i].slodId() + 9;
            }
        }
        return remappedSlotIndices;
    }

    private static void sortInventory(ScreenHandler handler, MinecraftClient client, Inventory inventory, int startingSlotIndex, int totalNumSlots, InventoryToSort inventoryToSort) {

        if (DEBUG_ITEM_SWITICHING)
            VersusMod.MOD_LOGGER.warn("Started sorting! 8==============================================================================================D");
        int numRows = totalNumSlots / 9;
        if (totalNumSlots < 2) return;
        int[] remappedSlots = getRemappedSlotIndices(inventory, numRows, totalNumSlots, inventoryToSort != InventoryToSort.CONTAINER_INVENTORY);

        if (DEBUG_ITEM_SWITICHING) {
            VersusMod.MOD_LOGGER.warn("");
            VersusMod.MOD_LOGGER.warn("     Planned: ");

            String remappedSlotsStr = "";
            String remappedSlotsAndItemsStr = "";
            for (int i = 0; i < totalNumSlots; i++) {
                remappedSlotsStr += (remappedSlots[i] == 0 ? "  -," : (remappedSlots[i] - 9 < 10 ? "  " + (remappedSlots[i] - 9) + "," : " " + (remappedSlots[i] - 9) + ","));
                remappedSlotsAndItemsStr += (remappedSlots[i] < 9 ? " -," : " " + (inventory.getStack(remappedSlots[i] - 9).getItem().getName().getString() + ","));
                if ((i + 1) % 9 == 0) {
                    VersusMod.MOD_LOGGER.warn("     -> [" + (remappedSlotsStr.substring(0, remappedSlotsStr.length() - 1)) + " ]   -->   {" + (remappedSlotsAndItemsStr.substring(0, remappedSlotsAndItemsStr.length() - 1)) + " }");
                    remappedSlotsStr = "";
                    remappedSlotsAndItemsStr = "";
                }
            }

            VersusMod.MOD_LOGGER.warn("");
            VersusMod.MOD_LOGGER.warn("     Current: ");

            for (int i = 0; i < totalNumSlots; i++) {
                remappedSlotsStr += inventory.getStack(i).isEmpty() ? "  -," : (i < 10 ? "  " + i + "," : " " + i + ",");
                remappedSlotsAndItemsStr += (inventory.getStack(i).isEmpty() ? " -," : " " + (inventory.getStack(i).getItem().getName().getString() + ","));
                if ((i + 1) % 9 == 0) {
                    VersusMod.MOD_LOGGER.warn("     -> [" + (remappedSlotsStr.substring(0, remappedSlotsStr.length() - 1)) + " ]   -->   {" + (remappedSlotsAndItemsStr.substring(0, remappedSlotsAndItemsStr.length() - 1)) + " }");
                    remappedSlotsStr = "";
                    remappedSlotsAndItemsStr = "";
                }
            }
        }

        // Sort the player's inventory accordingly!
        if (DEBUG_ITEM_SWITICHING) VersusMod.MOD_LOGGER.warn("");
        if (DEBUG_ITEM_SWITICHING) VersusMod.MOD_LOGGER.warn("Now actually modifying the player's inventory:");
        int[] displacedSlots = new int[totalNumSlots];
        int slotOrigin = -1, prevOrigin = -1, slotDestination = -1;
        for (int i = 0; i < totalNumSlots; i++) {

            slotDestination = i;
            slotOrigin = remappedSlots[slotDestination] - 9;
            boolean isPlacingEmptySlot = slotOrigin < 0;
            if (isPlacingEmptySlot) {
                if (inventory.getStack(slotDestination).isEmpty())
                    continue; // If the slot is meant to be empty, and already is, then no need to go through any more work
                slotOrigin = slotDestination; // This is to make sure that, if for some reason there are no empty slots ahead (normally, impossible), then at least nothing breaks
                prevOrigin = -1;
                for (int slotId = totalNumSlots - 1; slotId > slotDestination; slotId--) {
                    if (inventory.getStack(slotId).isEmpty()) {
                        slotOrigin = slotId;
                        break;
                    }
                }
            } else {
                int iter = 0;
                prevOrigin = slotOrigin;
                while (displacedSlots[slotOrigin] >= 9 && displacedSlots[slotOrigin] - 9 != slotOrigin && displacedSlots[slotOrigin] - 9 != prevOrigin) {
                    slotOrigin = displacedSlots[slotOrigin] - 9;
                    iter++;
                    if (iter == 12) slotOrigin = prevOrigin;
                    if (iter > 12 && iter < 20)
                        VersusMod.MOD_LOGGER.error("           -> Couldn't find the index of slot " + prevOrigin + ": Currently looking at displacedSlots[" + slotOrigin + "]: " + (displacedSlots[slotOrigin] - 9) + " + 9, which is " + inventory.getStack(displacedSlots[slotOrigin] - 9).getName().getString());
                    if (iter == 20)
                        VersusMod.MOD_LOGGER.error("           -> Couldn't find the index of slot " + prevOrigin + ": Currently looking at displacedSlots[" + slotOrigin + "]: ...");
                    if (iter > 100 && iter < 106)
                        VersusMod.MOD_LOGGER.error("           -> Couldn't find the index of slot " + prevOrigin + ": Currently looking at displacedSlots[" + slotOrigin + "]: " + (displacedSlots[slotOrigin] - 9) + " + 9, which is " + inventory.getStack(displacedSlots[slotOrigin] - 9).getName().getString());
                    if (iter == 135) VersusMod.MOD_LOGGER.error("              " + Arrays.toString(displacedSlots));
                }
            }

            boolean isMovingItemsAround = !inventory.getStack(slotOrigin).isEmpty() || !inventory.getStack(slotDestination).isEmpty();
            if (slotDestination != slotOrigin && isMovingItemsAround) {
                displacedSlots[slotDestination] = slotOrigin + 9;

                if (DEBUG_ITEM_SWITICHING) {
                    if (prevOrigin == -1)
                        VersusMod.MOD_LOGGER.warn("            -> Now populating slot " + slotDestination + " with empty slot " + slotOrigin + ((prevOrigin != slotOrigin) ? " (in array, slot was " + (remappedSlots[slotDestination] - 9) + ")" : "") + (!inventory.getStack(slotDestination).isEmpty() ? " -  (displacedSlots[" + slotDestination + "] = " + (slotOrigin) + " + 9)" : ""));
                    else if (prevOrigin != slotOrigin)
                        VersusMod.MOD_LOGGER.warn("            -> Now populating slot " + slotDestination + " with slot " + prevOrigin + " (which got moved to " + slotOrigin + (!inventory.getStack(slotDestination).isEmpty() ? " - displacedSlots[" + slotDestination + "] = " + (slotOrigin) + " + 9)" : ")"));
                    else
                        VersusMod.MOD_LOGGER.warn("            -> Now populating slot " + slotDestination + " with slot " + slotOrigin + (!inventory.getStack(slotDestination).isEmpty() ? " (displacedSlots[" + slotDestination + "] = " + (slotOrigin) + " + 9)" : ""));
                    VersusMod.MOD_LOGGER.warn("                   Origin: " + slotOrigin + " (" + inventory.getStack(slotOrigin).getItem().getName().getString() + ")");
                    VersusMod.MOD_LOGGER.warn("                   Dest. : " + slotDestination + " (" + inventory.getStack(slotDestination).getItem().getName().getString() + ")");
                }

                if (inventoryToSort == InventoryToSort.CREATIVE_INVENTORY) {
                    ItemStack stackOrigin = inventory.getStack(slotOrigin).copy();
                    inventory.setStack(slotOrigin, inventory.getStack(slotDestination).copy());
                    inventory.setStack(slotDestination, stackOrigin);
                } else if (inventoryToSort == InventoryToSort.CONTAINER_INVENTORY) {
                    client.interactionManager.clickSlot(handler.syncId, slotOrigin, 8, SlotActionType.SWAP, client.player);
                    client.interactionManager.clickSlot(handler.syncId, slotDestination, 8, SlotActionType.SWAP, client.player);
                    client.interactionManager.clickSlot(handler.syncId, slotOrigin, 8, SlotActionType.SWAP, client.player);
                } else if (inventoryToSort == InventoryToSort.INVENTORY_WHITH_SLOTS_ABOVE) {

                    // Inversed row: in containers and chests, the hotbar slot indices go AFTER regular inventory slots rather than before.
                    int actualSlotOrigin = (slotOrigin < 9) ? (slotOrigin + startingSlotIndex + 27) : slotOrigin + startingSlotIndex - 9;
                    int actualSlotDest = (slotDestination < 9) ? (slotDestination + startingSlotIndex + 27) : slotDestination + startingSlotIndex - 9;
                    int rowStartingIndex = startingSlotIndex + totalNumSlots - 9;

                    if (actualSlotDest >= rowStartingIndex && actualSlotOrigin >= rowStartingIndex) {
                        client.interactionManager.clickSlot(handler.syncId, actualSlotDest, (actualSlotOrigin - rowStartingIndex) % 9, SlotActionType.SWAP, client.player);
                    } else if (actualSlotDest >= rowStartingIndex) {
                        client.interactionManager.clickSlot(handler.syncId, actualSlotOrigin, (actualSlotDest - rowStartingIndex) % 9, SlotActionType.SWAP, client.player);
                    } else if (slotOrigin >= rowStartingIndex) {
                        client.interactionManager.clickSlot(handler.syncId, actualSlotDest, (actualSlotOrigin - rowStartingIndex) % 9, SlotActionType.SWAP, client.player);
                    } else {
                        client.interactionManager.clickSlot(handler.syncId, actualSlotOrigin, 8, SlotActionType.SWAP, client.player);
                        client.interactionManager.clickSlot(handler.syncId, actualSlotDest, 8, SlotActionType.SWAP, client.player);
                        client.interactionManager.clickSlot(handler.syncId, actualSlotOrigin, 8, SlotActionType.SWAP, client.player);
                    }
                } else if (inventoryToSort == InventoryToSort.SURVIVAL_INVENTORY) {
                    if (slotDestination < 9 && slotOrigin < 9) {
                        client.interactionManager.clickSlot(handler.syncId, slotDestination + totalNumSlots, slotOrigin, SlotActionType.SWAP, client.player);
                    } else if (slotDestination < 9) {
                        client.interactionManager.clickSlot(handler.syncId, slotOrigin, slotDestination, SlotActionType.SWAP, client.player);
                    } else if (slotOrigin < 9) {
                        client.interactionManager.clickSlot(handler.syncId, slotDestination, slotOrigin, SlotActionType.SWAP, client.player);
                    } else {
                        client.interactionManager.clickSlot(handler.syncId, slotOrigin, 8, SlotActionType.SWAP, client.player);
                        client.interactionManager.clickSlot(handler.syncId, slotDestination, 8, SlotActionType.SWAP, client.player);
                        client.interactionManager.clickSlot(handler.syncId, slotOrigin, 8, SlotActionType.SWAP, client.player);
                    }
                }
            }

        }
        if (inventoryToSort == InventoryToSort.CREATIVE_INVENTORY)
            client.player.playerScreenHandler.sendContentUpdates();
        ;
    }
}