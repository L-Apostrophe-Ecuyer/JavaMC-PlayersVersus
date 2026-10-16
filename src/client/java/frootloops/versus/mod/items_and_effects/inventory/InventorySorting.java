package frootloops.versus.mod.items_and_effects.inventory;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.items_and_effects.inventory.sorting.InventorySorter;
import frootloops.versus.mod.items_and_effects.inventory.sorting.ItemSlot;
import frootloops.versus.mod.items_and_effects.inventory.sorting.MovePlanner;
import frootloops.versus.mod.items_and_effects.inventory.sorting.SortingDebug;
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
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * The sort buttons: sorts the stacks of an inventory with {@link InventorySorter}, then lays them out with the fewest
 * clicks, each swapping one slot with a spare hotbar slot ({@link MovePlanner}).
 */
@Environment(EnvType.CLIENT)
public class InventorySorting {

    public static final WidgetSprites TEXTURE_HOTBAR_SWAP_BUTTON = new WidgetSprites(Identifier.fromNamespaceAndPath("players-versus", "container/hotbar_swap_down"), Identifier.fromNamespaceAndPath("players-versus", "container/hotbar_swap_down_highlighted"));
    public static final WidgetSprites TEXTURE_INVENTORY_SORT_BUTTON = new WidgetSprites(Identifier.fromNamespaceAndPath("players-versus", "container/sort_inventory"), Identifier.fromNamespaceAndPath("players-versus", "container/sort_inventory_highlighted"));
    public static final WidgetSprites TEXTURE_SMALL_INVENTORY_SORT_BUTTON = new WidgetSprites(Identifier.fromNamespaceAndPath("players-versus", "container/sort_inventory_small"), Identifier.fromNamespaceAndPath("players-versus", "container/sort_inventory_small_highlighted"));
    public static final WidgetSprites TEXTURE_CHEST_SORT_BUTTON = new WidgetSprites(Identifier.fromNamespaceAndPath("players-versus", "container/sort_chest"), Identifier.fromNamespaceAndPath("players-versus", "container/sort_chest_highlighted"));
    public static final WidgetSprites TEXTURE_SHULKER_SORT_BUTTON = new WidgetSprites(Identifier.fromNamespaceAndPath("players-versus", "container/sort_shulker"), Identifier.fromNamespaceAndPath("players-versus", "container/sort_shulker_highlighted"));

    public enum InventoryToSort {
        PLAYER_INVENTORY,
        CREATIVE_INVENTORY,
        CONTAINER_INVENTORY,
    }

    /**
     * Sorts the player's 36 inventory slots, or all of a container's slots, as the menu shows them.
     */
    public static void sortInventory(AbstractContainerMenu menu, Minecraft client, Container inventory, InventoryToSort inventoryType) {
        if (client.player == null || client.gameMode == null) return;
        boolean isPlayerInventory = inventoryType != InventoryToSort.CONTAINER_INVENTORY;
        int numSlots = isPlayerInventory ? 36 : inventory.getContainerSize();
        if (numSlots < 2) return;

        InventoryManagementHelper.placeOrDropCursorStack(menu, client, inventory, numSlots);
        InventoryManagementHelper.mergeStacksTogether(menu, client, inventory, numSlots);

        List<ItemSlot> slots = new ArrayList<>();
        for (int i = 0; i < numSlots; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) slots.add(ItemSlot.of(i, stack));
        }
        Optional<ItemSlot[]> sorted = InventorySorter.sort(slots, numSlots / 9, situationOf(client, isPlayerInventory));
        if (sorted.isEmpty()) {
            VersusMod.MOD_LOGGER.warn("[ INVENTORY SORTING ] Couldn't lay out these {} stacks, leaving the inventory as it is", slots.size());
            return;
        }

        if (inventoryType == InventoryToSort.CREATIVE_INVENTORY) setSortedStacks(client, inventory, sorted.get());
        else swapIntoPlace(menu, client, inventory, sorted.get(), isPlayerInventory);
    }

    private static InventorySorter.Situation situationOf(Minecraft client, boolean isPlayerInventory) {
        if (!isPlayerInventory || client.level == null) return InventorySorter.Situation.CONTAINER;
        Holder<Biome> playerBiome = client.level.getBiome(client.player.blockPosition());
        boolean isInDeepDark = playerBiome.is(Biomes.DEEP_DARK);
        boolean isInNether = !isInDeepDark && playerBiome.is(BiomeTags.IS_NETHER);
        boolean isInWater = !isInDeepDark && (playerBiome.is(BiomeTags.IS_OCEAN) || client.player.isUnderWater());
        return new InventorySorter.Situation(true, isInDeepDark, isInNether, isInWater);
    }

    /** Creative mode sets the stacks directly. */
    private static void setSortedStacks(Minecraft client, Container inventory, ItemSlot[] sorted) {
        ItemStack[] stacks = new ItemStack[sorted.length];
        for (int i = 0; i < sorted.length; i++) stacks[i] = sorted[i] == null ? ItemStack.EMPTY : inventory.getItem(sorted[i].slotId()).copy();
        for (int i = 0; i < sorted.length; i++) inventory.setItem(i, stacks[i]);
        client.player.inventoryMenu.broadcastChanges();
    }

    /**
     * Lays the sorted stacks out with SWAP clicks through a spare hotbar slot: one of the sorted hotbar slots for the
     * player's inventory, else the first empty hotbar slot (or one whose stack the container's slots accept).
     */
    private static void swapIntoPlace(AbstractContainerMenu menu, Minecraft client, Container inventory, ItemSlot[] sorted, boolean isPlayerInventory) {
        int numSlots = sorted.length;
        int[] menuSlots = new int[numSlots];
        for (int i = 0; i < numSlots; i++) {
            OptionalInt menuSlot = menu.findSlot(inventory, i);
            if (menuSlot.isEmpty()) {
                VersusMod.MOD_LOGGER.warn("[ INVENTORY SORTING ] Slot {} of the inventory isn't in this menu, not sorting", i);
                return;
            }
            menuSlots[i] = menuSlot.getAsInt();
        }

        // Equal stacks share a class, so stacks already in place stay put. Class 0 is an empty slot.
        int[] currentClass = new int[numSlots];
        List<ItemStack> classStacks = new ArrayList<>();
        for (int i = 0; i < numSlots; i++) currentClass[i] = classOf(inventory.getItem(i), classStacks);
        int[] targetClass = new int[numSlots];
        for (int i = 0; i < numSlots; i++) targetClass[i] = sorted[i] == null ? 0 : currentClass[sorted[i].slotId()];

        int spare, spareInside;
        if (isPlayerInventory) {
            spare = 8;
            for (int h = 0; h < 9; h++) {
                if (targetClass[h] == 0) {
                    spare = h;
                    break;
                }
            }
            spareInside = spare;
        }
        else {
            boolean[] emptyHotbar = new boolean[9], placeableHotbar = new boolean[9];
            for (int h = 0; h < 9; h++) {
                ItemStack stack = client.player.getInventory().getItem(h);
                emptyHotbar[h] = stack.isEmpty();
                placeableHotbar[h] = menu.getSlot(menuSlots[0]).mayPlace(stack);
            }
            spare = MovePlanner.chooseSpare(emptyHotbar, placeableHotbar);
            spareInside = -1;
            if (spare < 0) {
                VersusMod.MOD_LOGGER.warn("[ INVENTORY SORTING ] No hotbar slot can hold the container's stacks while sorting, not sorting");
                return;
            }
        }

        int spareSlot = spare;
        int[] swaps = MovePlanner.swapsWithSpare(currentClass, targetClass, spareInside);
        SortingDebug.log(() -> "[ INVENTORY SORTING ] Sorting with " + swaps.length + " swaps through hotbar slot " + spareSlot);
        for (int position : swaps) {
            client.gameMode.handleContainerInput(menu.containerId, menuSlots[position], spareSlot, ContainerInput.SWAP, client.player);
        }
    }

    /** The class of the stack: 0 when empty, else the same number as any equal stack before it. */
    private static int classOf(ItemStack stack, List<ItemStack> classStacks) {
        if (stack.isEmpty()) return 0;
        for (int i = 0; i < classStacks.size(); i++) {
            if (ItemStack.matches(stack, classStacks.get(i))) return i + 1;
        }
        classStacks.add(stack);
        return classStacks.size();
    }
}
