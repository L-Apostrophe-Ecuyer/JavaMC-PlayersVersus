package frootloops.versus.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;

/**
 * Sorts inventories on the game test server, with the game's real items and tags.
 */
public class InventorySortingGameTest {

    @GameTest
    public void aMixedInventoryKeepsEveryItem(GameTestHelper helper) {
        List<ItemStack> stacks = List.of(
                new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.IRON_AXE),
                new ItemStack(Items.IRON_SHOVEL), new ItemStack(Items.BOW), new ItemStack(Items.ARROW, 64),
                new ItemStack(Items.BREAD, 16), new ItemStack(Items.COOKED_BEEF, 32), new ItemStack(Items.TORCH, 32),
                new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.COBBLESTONE, 64),
                new ItemStack(Items.OAK_PLANKS, 64), new ItemStack(Items.OAK_PLANKS, 64), new ItemStack(Items.DIRT, 64),
                new ItemStack(Items.IRON_INGOT, 16), new ItemStack(Items.DIAMOND, 8), new ItemStack(Items.REDSTONE, 12),
                new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.SHIELD));

        Optional<int[]> sorted = TestInventories.sortIds(stacks, 4, true, false, false, false);
        helper.assertTrue(sorted.isPresent(), "the sort gave up");
        assertEveryStackOnce(helper, sorted.get(), stacks.size());
        helper.succeed();
    }

    private static void assertEveryStackOnce(GameTestHelper helper, int[] sorted, int numStacks) {
        int[] timesLaid = new int[numStacks];
        for (int id : sorted) if (id >= 0) timesLaid[id]++;
        for (int i = 0; i < numStacks; i++) helper.assertTrue(timesLaid[i] == 1, "stack " + i + " was laid " + timesLaid[i] + " times");
    }
}
