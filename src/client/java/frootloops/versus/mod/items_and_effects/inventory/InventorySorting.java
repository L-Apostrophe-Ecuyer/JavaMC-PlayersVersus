package frootloops.versus.mod.items_and_effects.inventory;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.items_and_effects.inventory.sorting.InventorySorter;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemSlot;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class InventorySorting {

    private static final boolean DEBUG_ITEM_SWITICHING = true;

    public static final WidgetSprites TEXTURE_HOTBAR_SWAP_BUTTON = new WidgetSprites(Identifier.fromNamespaceAndPath("players-versus", "container/hotbar_swap_down"), Identifier.fromNamespaceAndPath("players-versus", "container/hotbar_swap_down_highlighted"));
    public static final WidgetSprites TEXTURE_INVENTORY_SORT_BUTTON = new WidgetSprites(Identifier.fromNamespaceAndPath("players-versus", "container/sort_inventory"), Identifier.fromNamespaceAndPath("players-versus", "container/sort_inventory_highlighted"));
    public static final WidgetSprites TEXTURE_SMALL_INVENTORY_SORT_BUTTON = new WidgetSprites(Identifier.fromNamespaceAndPath("players-versus", "container/sort_inventory_small"), Identifier.fromNamespaceAndPath("players-versus", "container/sort_inventory_small_highlighted"));
    public static final WidgetSprites TEXTURE_CHEST_SORT_BUTTON = new WidgetSprites(Identifier.fromNamespaceAndPath("players-versus", "container/sort_chest"), Identifier.fromNamespaceAndPath("players-versus", "container/sort_chest_highlighted"));
    public static final WidgetSprites TEXTURE_SHULKER_SORT_BUTTON = new WidgetSprites(Identifier.fromNamespaceAndPath("players-versus", "container/sort_shulker"), Identifier.fromNamespaceAndPath("players-versus", "container/sort_shulker_highlighted"));

    public enum InventoryToSort {
        SURVIVAL_INVENTORY,
        CREATIVE_INVENTORY,
        INVENTORY_WHITH_SLOTS_ABOVE,
        CONTAINER_INVENTORY,
    }


    public static void sortInventory(AbstractContainerMenu handler, Minecraft client, Container inventory, InventoryToSort inventoryType) {
        sortInventory(handler, client, inventory, inventoryType, 0, (inventoryType == InventoryToSort.CONTAINER_INVENTORY ? inventory.getContainerSize() : 36));
    }

    public static void sortInventory(AbstractContainerMenu handler, Minecraft client, Container inventory, InventoryToSort inventoryType, int startingSlotIndex, int numSlots) {
        InventoryManagementHelper.placeOrDropCursorStack(handler, client, inventory);
        InventoryManagementHelper.mergeStacksTogether(handler, client, inventory, (inventoryType == InventoryToSort.SURVIVAL_INVENTORY || inventoryType == InventoryToSort.CREATIVE_INVENTORY) ? 9 : 0, numSlots);
        sortInventory(handler, client, inventory, startingSlotIndex, numSlots, inventoryType);
    }

    private static int[] getRemappedSlotIndices(Container inventory, int numRows, int totalNumSlots, boolean isPlayerInventory) {
        int[] remappedSlotIndices = new int[totalNumSlots];
        List<ItemSlot> slots = new ArrayList<>();
        for (int i = 0; i < totalNumSlots; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) slots.add(ItemSlot.of(i, stack));
        }

        // For situational hotbar item priority:
        boolean isInDeepDark = false, isInNether = false, isInWater = false;
        if(isPlayerInventory) {
            Holder<Biome> playerBiome = Minecraft.getInstance().level.getBiome(Minecraft.getInstance().player.blockPosition()); 
            isInDeepDark = playerBiome.is(Biomes.DEEP_DARK);
            isInNether = !isInDeepDark && playerBiome.is(BiomeTags.IS_NETHER);
            isInWater = !isInDeepDark && (playerBiome.is(BiomeTags.IS_OCEAN) || Minecraft.getInstance().player.isUnderWater());
        }

        Optional<ItemSlot[]> sorted = InventorySorter.sort(slots, numRows, new InventorySorter.Situation(isPlayerInventory, isInDeepDark, isInNether, isInWater));
        if (sorted.isEmpty()) return null;
        ItemSlot[] newSlots = sorted.get();
        for (int i = 0; i < totalNumSlots; i++) {
            if (newSlots[i] != null) {
                remappedSlotIndices[i] = newSlots[i].slotId() + 9;
            }
        }
        return remappedSlotIndices;
    }

    private static void sortInventory(AbstractContainerMenu handler, Minecraft client, Container inventory, int startingSlotIndex, int totalNumSlots, InventoryToSort inventoryToSort) {

        if (DEBUG_ITEM_SWITICHING)
            VersusMod.MOD_LOGGER.warn("Started sorting! 8==============================================================================================D");
        int numRows = totalNumSlots / 9;
        if (totalNumSlots < 2) return;
        int[] remappedSlots = getRemappedSlotIndices(inventory, numRows, totalNumSlots, inventoryToSort != InventoryToSort.CONTAINER_INVENTORY);
        if (remappedSlots == null) return; // The sort gave up: leave the inventory as it is.

        if (DEBUG_ITEM_SWITICHING) {
            VersusMod.MOD_LOGGER.warn("");
            VersusMod.MOD_LOGGER.warn("     Planned: ");

            String remappedSlotsStr = "";
            String remappedSlotsAndItemsStr = "";
            for (int i = 0; i < totalNumSlots; i++) {
                remappedSlotsStr += (remappedSlots[i] == 0 ? "  -," : (remappedSlots[i] - 9 < 10 ? "  " + (remappedSlots[i] - 9) + "," : " " + (remappedSlots[i] - 9) + ","));
                remappedSlotsAndItemsStr += (remappedSlots[i] < 9 ? " -," : " " + (inventory.getItem(remappedSlots[i] - 9).getHoverName().getString() + ","));
                if ((i + 1) % 9 == 0) {
                    VersusMod.MOD_LOGGER.warn("     -> [" + (remappedSlotsStr.substring(0, remappedSlotsStr.length() - 1)) + " ]   -->   {" + (remappedSlotsAndItemsStr.substring(0, remappedSlotsAndItemsStr.length() - 1)) + " }");
                    remappedSlotsStr = "";
                    remappedSlotsAndItemsStr = "";
                }
            }

            VersusMod.MOD_LOGGER.warn("");
            VersusMod.MOD_LOGGER.warn("     Current: ");

            for (int i = 0; i < totalNumSlots; i++) {
                remappedSlotsStr += inventory.getItem(i).isEmpty() ? "  -," : (i < 10 ? "  " + i + "," : " " + i + ",");
                remappedSlotsAndItemsStr += (inventory.getItem(i).isEmpty() ? " -," : " " + (inventory.getItem(i).getHoverName().getString() + ","));
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
                if (inventory.getItem(slotDestination).isEmpty())
                    continue; // If the slot is meant to be empty, and already is, then no need to go through any more work
                slotOrigin = slotDestination; // This is to make sure that, if for some reason there are no empty slots ahead (normally, impossible), then at least nothing breaks
                prevOrigin = -1;
                for (int slotId = totalNumSlots - 1; slotId > slotDestination; slotId--) {
                    if (inventory.getItem(slotId).isEmpty()) {
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
                        VersusMod.MOD_LOGGER.error("           -> Couldn't find the index of slot " + prevOrigin + ": Currently looking at displacedSlots[" + slotOrigin + "]: " + (displacedSlots[slotOrigin] - 9) + " + 9, which is " + inventory.getItem(displacedSlots[slotOrigin] - 9).getHoverName().getString());
                    if (iter == 20)
                        VersusMod.MOD_LOGGER.error("           -> Couldn't find the index of slot " + prevOrigin + ": Currently looking at displacedSlots[" + slotOrigin + "]: ...");
                    if (iter > 100 && iter < 106)
                        VersusMod.MOD_LOGGER.error("           -> Couldn't find the index of slot " + prevOrigin + ": Currently looking at displacedSlots[" + slotOrigin + "]: " + (displacedSlots[slotOrigin] - 9) + " + 9, which is " + inventory.getItem(displacedSlots[slotOrigin] - 9).getHoverName().getString());
                    if (iter == 135) VersusMod.MOD_LOGGER.error("              " + Arrays.toString(displacedSlots));
                }
            }

            boolean isMovingItemsAround = !inventory.getItem(slotOrigin).isEmpty() || !inventory.getItem(slotDestination).isEmpty();
            if (slotDestination != slotOrigin && isMovingItemsAround) {
                displacedSlots[slotDestination] = slotOrigin + 9;

                if (DEBUG_ITEM_SWITICHING) {
                    if (prevOrigin == -1)
                        VersusMod.MOD_LOGGER.warn("            -> Now populating slot " + slotDestination + " with empty slot " + slotOrigin + ((prevOrigin != slotOrigin) ? " (in array, slot was " + (remappedSlots[slotDestination] - 9) + ")" : "") + (!inventory.getItem(slotDestination).isEmpty() ? " -  (displacedSlots[" + slotDestination + "] = " + (slotOrigin) + " + 9)" : ""));
                    else if (prevOrigin != slotOrigin)
                        VersusMod.MOD_LOGGER.warn("            -> Now populating slot " + slotDestination + " with slot " + prevOrigin + " (which got moved to " + slotOrigin + (!inventory.getItem(slotDestination).isEmpty() ? " - displacedSlots[" + slotDestination + "] = " + (slotOrigin) + " + 9)" : ")"));
                    else
                        VersusMod.MOD_LOGGER.warn("            -> Now populating slot " + slotDestination + " with slot " + slotOrigin + (!inventory.getItem(slotDestination).isEmpty() ? " (displacedSlots[" + slotDestination + "] = " + (slotOrigin) + " + 9)" : ""));
                    VersusMod.MOD_LOGGER.warn("                   Origin: " + slotOrigin + " (" + inventory.getItem(slotOrigin).getHoverName().getString() + ")");
                    VersusMod.MOD_LOGGER.warn("                   Dest. : " + slotDestination + " (" + inventory.getItem(slotDestination).getHoverName().getString() + ")");
                }

                if (inventoryToSort == InventoryToSort.CREATIVE_INVENTORY) {
                    ItemStack stackOrigin = inventory.getItem(slotOrigin).copy();
                    inventory.setItem(slotOrigin, inventory.getItem(slotDestination).copy());
                    inventory.setItem(slotDestination, stackOrigin);
                } else if (inventoryToSort == InventoryToSort.CONTAINER_INVENTORY) {
                    client.gameMode.handleContainerInput(handler.containerId, slotOrigin, 8, ContainerInput.SWAP, client.player);
                    client.gameMode.handleContainerInput(handler.containerId, slotDestination, 8, ContainerInput.SWAP, client.player);
                    client.gameMode.handleContainerInput(handler.containerId, slotOrigin, 8, ContainerInput.SWAP, client.player);
                } else if (inventoryToSort == InventoryToSort.INVENTORY_WHITH_SLOTS_ABOVE) {

                    // Inversed row: in containers and chests, the hotbar slot indices go AFTER regular inventory slots rather than before.
                    int actualSlotOrigin = (slotOrigin < 9) ? (slotOrigin + startingSlotIndex + 27) : slotOrigin + startingSlotIndex - 9;
                    int actualSlotDest = (slotDestination < 9) ? (slotDestination + startingSlotIndex + 27) : slotDestination + startingSlotIndex - 9;
                    int rowStartingIndex = startingSlotIndex + totalNumSlots - 9;

                    if (actualSlotDest >= rowStartingIndex && actualSlotOrigin >= rowStartingIndex) {
                        client.gameMode.handleContainerInput(handler.containerId, actualSlotDest, (actualSlotOrigin - rowStartingIndex) % 9, ContainerInput.SWAP, client.player);
                    } else if (actualSlotDest >= rowStartingIndex) {
                        client.gameMode.handleContainerInput(handler.containerId, actualSlotOrigin, (actualSlotDest - rowStartingIndex) % 9, ContainerInput.SWAP, client.player);
                    } else if (slotOrigin >= rowStartingIndex) {
                        client.gameMode.handleContainerInput(handler.containerId, actualSlotDest, (actualSlotOrigin - rowStartingIndex) % 9, ContainerInput.SWAP, client.player);
                    } else {
                        client.gameMode.handleContainerInput(handler.containerId, actualSlotOrigin, 8, ContainerInput.SWAP, client.player);
                        client.gameMode.handleContainerInput(handler.containerId, actualSlotDest, 8, ContainerInput.SWAP, client.player);
                        client.gameMode.handleContainerInput(handler.containerId, actualSlotOrigin, 8, ContainerInput.SWAP, client.player);
                    }
                } else if (inventoryToSort == InventoryToSort.SURVIVAL_INVENTORY) {
                    if (slotDestination < 9 && slotOrigin < 9) {
                        client.gameMode.handleContainerInput(handler.containerId, slotDestination + totalNumSlots, slotOrigin, ContainerInput.SWAP, client.player);
                    } else if (slotDestination < 9) {
                        client.gameMode.handleContainerInput(handler.containerId, slotOrigin, slotDestination, ContainerInput.SWAP, client.player);
                    } else if (slotOrigin < 9) {
                        client.gameMode.handleContainerInput(handler.containerId, slotDestination, slotOrigin, ContainerInput.SWAP, client.player);
                    } else {
                        client.gameMode.handleContainerInput(handler.containerId, slotOrigin, 8, ContainerInput.SWAP, client.player);
                        client.gameMode.handleContainerInput(handler.containerId, slotDestination, 8, ContainerInput.SWAP, client.player);
                        client.gameMode.handleContainerInput(handler.containerId, slotOrigin, 8, ContainerInput.SWAP, client.player);
                    }
                }
            }

        }
        if (inventoryToSort == InventoryToSort.CREATIVE_INVENTORY)
            client.player.inventoryMenu.broadcastChanges();
        ;
    }
}