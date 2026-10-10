package frootloops.versus.gametest;

import frootloops.versus.VersusMod;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import java.util.Arrays;
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

        int[] sorted = sort(helper, stacks, 4, true, false, false, false, "a mixed inventory");
        assertEveryStackOnce(helper, sorted, stacks, "a mixed inventory");
        helper.succeed();
    }

    @GameTest
    public void everySlotEndsUpExactlyOnce(GameTestHelper helper) {
        RandomSource random = RandomSource.create(20261010L);
        for (int test = 0; test < 300; test++) {
            boolean playerInventory = test % 3 == 0;
            int numRows = playerInventory ? 4 : test % 3 == 1 ? 3 : 6;
            int situation = playerInventory ? random.nextInt(4) : 0;
            List<ItemStack> stacks = TestInventories.random(random, numRows * 9);
            String label = "inventory " + test + " (" + numRows + " rows, situation " + situation + ", " + stacks.size() + " stacks)";
            int[] sorted = sort(helper, stacks, numRows, playerInventory, situation == 1, situation == 2, situation == 3, label);
            assertEveryStackOnce(helper, sorted, stacks, label);
        }
        helper.succeed();
    }

    @GameTest
    public void aSortDoesNotChangeTheNext(GameTestHelper helper) {
        List<ItemStack> inventory = List.of(
                new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.BOW),
                new ItemStack(Items.ARROW, 32), new ItemStack(Items.BREAD, 16), new ItemStack(Items.TORCH, 32),
                PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS), new ItemStack(Items.COBBLESTONE, 64),
                new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.DIRT, 64), new ItemStack(Items.OAK_PLANKS, 20),
                new ItemStack(Items.IRON_INGOT, 8));
        List<ItemStack> deepDarkInventory = List.of(
                new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.WOOL.white(), 64),
                PotionContents.createItemStack(Items.POTION, Potions.LONG_FIRE_RESISTANCE), new ItemStack(Items.BREAD, 16));

        int[] first = sort(helper, inventory, 4, true, false, false, false, "the first sort");
        sort(helper, deepDarkInventory, 4, true, true, false, false, "the Deep Dark sort");
        int[] again = sort(helper, inventory, 4, true, false, false, false, "the same sort again");
        if (!Arrays.equals(first, again)) {
            helper.fail("The same inventory sorted differently after a Deep Dark sort: " + Arrays.toString(first) + " then " + Arrays.toString(again));
        }
        helper.succeed();
    }

    @GameTest
    public void inWaterWithAPearlAndNoTotem(GameTestHelper helper) {
        List<ItemStack> stacks = List.of(
                new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.ENDER_PEARL, 16), new ItemStack(Items.BREAD, 16),
                new ItemStack(Items.COBBLESTONE, 64));

        int[] sorted = sort(helper, stacks, 4, true, false, false, true, "in water");
        assertEveryStackOnce(helper, sorted, stacks, "in water");
        helper.succeed();
    }

    /** Sorts the stacks, failing the test if the sort throws or gives up. */
    private static int[] sort(GameTestHelper helper, List<ItemStack> stacks, int numRows, boolean playerInventory, boolean inDeepDark, boolean inNether, boolean inWater, String label) {
        Optional<int[]> sorted;
        try {
            sorted = TestInventories.sortIds(stacks, numRows, playerInventory, inDeepDark, inNether, inWater);
        } catch (RuntimeException e) {
            VersusMod.MOD_LOGGER.error("Sorting {} threw: {}", label, TestInventories.describe(stacks), e);
            helper.fail(label + " threw " + e);
            throw e;
        }
        if (sorted.isEmpty()) helper.fail(label + ": the sort gave up on " + TestInventories.describe(stacks));
        return sorted.get();
    }

    private static void assertEveryStackOnce(GameTestHelper helper, int[] sorted, List<ItemStack> stacks, String label) {
        int[] timesLaid = new int[stacks.size()];
        int numEmpty = 0;
        for (int id : sorted) {
            if (id < 0) numEmpty++;
            else timesLaid[id]++;
        }
        for (int i = 0; i < stacks.size(); i++) {
            if (timesLaid[i] != 1) {
                helper.fail(label + ": stack " + i + " (" + stacks.get(i).getHoverName().getString() + ") was laid " + timesLaid[i]
                        + " times in " + Arrays.toString(sorted) + " for " + TestInventories.describe(stacks));
            }
        }
        if (numEmpty != sorted.length - stacks.size()) helper.fail(label + ": " + numEmpty + " empty slots for " + stacks.size() + " stacks in " + sorted.length);
    }
}
