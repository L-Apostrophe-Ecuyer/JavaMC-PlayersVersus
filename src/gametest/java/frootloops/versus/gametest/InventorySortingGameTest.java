package frootloops.versus.gametest;

import frootloops.versus.VersusMod;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

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

    @GameTest
    public void containersSortTogether(GameTestHelper helper) {
        List<ItemStack> stacks = List.of(
                new ItemStack(Items.SHULKER_BOX), new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.BUNDLE),
                new ItemStack(Items.STRING, 13), new ItemStack(Items.DYED_SHULKER_BOX.red()), new ItemStack(Items.STONE, 64),
                new ItemStack(Items.FEATHER, 9), new ItemStack(Items.SHULKER_BOX), new ItemStack(Items.BONE, 17),
                new ItemStack(Items.BUNDLE), new ItemStack(Items.DIRT, 64));

        int[] sorted = sort(helper, stacks, 3, false, false, false, false, "a chest");
        assertRowsHoldOnly(helper, sorted, List.of(0, 2, 4, 7, 9), stacks, "the shulker boxes and bundles");
        helper.succeed();
    }

    @GameTest
    public void minecartsSortWithTheRails(GameTestHelper helper) {
        List<ItemStack> stacks = List.of(new ItemStack(Items.MINECART), new ItemStack(Items.REPEATER, 4), new ItemStack(Items.RAIL, 32));

        int[] sorted = sort(helper, stacks, 3, false, false, false, false, "a chest");
        assertConsecutive(helper, sorted, List.of(0, 2), stacks, "the minecart and the rails");
        helper.succeed();
    }

    @GameTest
    public void blocksInAListSortByMapColour(GameTestHelper helper) {
        // Pickaxe blocks no sorting map ranks, so they share the pickaxe group's last list. Map colours: end stone sand
        // (0xF7E9A3), purpur magenta (0xB24CD8), smooth stone stone (0x707070).
        List<ItemStack> stacks = List.of(new ItemStack(Items.END_STONE, 64), new ItemStack(Items.PURPUR_BLOCK, 64), new ItemStack(Items.SMOOTH_STONE, 64));

        int[] sorted = sort(helper, stacks, 3, false, false, false, false, "a chest");
        int[] firstRow = Arrays.copyOf(sorted, 3);
        if (!Arrays.equals(firstRow, new int[]{2, 1, 0})) {
            helper.fail("Expected smooth stone, purpur then end stone, got " + Arrays.toString(sorted));
        }
        helper.succeed();
    }

    @GameTest
    public void wholeToolRowsStayWhole(GameTestHelper helper) {
        List<ItemStack> stacks = new ArrayList<>(List.of(new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.BREAD, 16), new ItemStack(Items.TORCH, 32), new ItemStack(Items.IRON_AXE)));
        addCopies(stacks, Items.OAK_PLANKS, 8);
        stacks.add(new ItemStack(Items.IRON_PICKAXE));
        addCopies(stacks, Items.COBBLESTONE, 8);

        int[] sorted = sort(helper, stacks, 4, true, false, false, false, "the player's inventory");
        assertInRow(helper, sorted, 0, List.of(0, 1, 2), stacks, "the sword, bread and torches");
        assertWholeRow(helper, sorted, range(12, 21), stacks, "the pickaxe and its cobblestone");
        assertWholeRow(helper, sorted, range(3, 12), stacks, "the axe and its planks");
        helper.succeed();
    }

    @GameTest
    public void aWeakHotbarTakesTheBestWholeToolRow(GameTestHelper helper) {
        List<ItemStack> stacks = new ArrayList<>(List.of(new ItemStack(Items.BREAD, 16), new ItemStack(Items.TORCH, 32), new ItemStack(Items.IRON_PICKAXE)));
        addCopies(stacks, Items.COBBLESTONE, 8);
        stacks.add(new ItemStack(Items.IRON_AXE));
        addCopies(stacks, Items.OAK_PLANKS, 8);

        int[] sorted = sort(helper, stacks, 4, true, false, false, false, "the player's inventory");
        assertRowIs(helper, sorted, 0, range(2, 11), stacks, "the pickaxe and its cobblestone");
        assertWholeRow(helper, sorted, range(11, 20), stacks, "the axe and its planks");
        helper.succeed();
    }

    @GameTest
    public void aLonePickaxeJoinsTheHotbar(GameTestHelper helper) {
        List<ItemStack> stacks = new ArrayList<>(List.of(new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.BREAD, 16), new ItemStack(Items.IRON_PICKAXE)));
        addCopies(stacks, Items.COBBLESTONE, 12);

        int[] sorted = sort(helper, stacks, 4, true, false, false, false, "the player's inventory");
        assertInRow(helper, sorted, 0, List.of(0, 1, 2), stacks, "the sword, bread and pickaxe");
        helper.succeed();
    }

    @GameTest
    public void theSwordStaysWithoutFood(GameTestHelper helper) {
        // The pickaxe, torches and cobblestone keep the hotbar busy, so a sword dropped from it would sort further down.
        List<ItemStack> stacks = new ArrayList<>(List.of(new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.IRON_PICKAXE), new ItemStack(Items.TORCH, 32)));
        addCopies(stacks, Items.COBBLESTONE, 6);
        addCopies(stacks, Items.DIRT, 5);
        addCopies(stacks, Items.OAK_PLANKS, 4);

        int[] sorted = sort(helper, stacks, 4, true, false, false, false, "the player's inventory");
        assertInRow(helper, sorted, 0, List.of(0), stacks, "the sword");
        helper.succeed();
    }

    @GameTest
    public void aShovelAvoidsAWeakHotbar(GameTestHelper helper) {
        // Too many dirt stacks for the shovel's group to fit in the hotbar along with them.
        List<ItemStack> stacks = new ArrayList<>(List.of(new ItemStack(Items.BREAD, 16), new ItemStack(Items.TORCH, 32), new ItemStack(Items.IRON_SHOVEL)));
        addCopies(stacks, Items.DIRT, 10);

        int[] sorted = sort(helper, stacks, 4, true, false, false, false, "the player's inventory");
        assertInRow(helper, sorted, 0, List.of(2), stacks, "the shovel");
        helper.succeed();
    }

    @GameTest
    public void sortingTwiceChangesNothing(GameTestHelper helper) {
        List<ItemStack> loadout = List.of(
                new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.BREAD, 16), new ItemStack(Items.WATER_BUCKET),
                new ItemStack(Items.ENDER_PEARL, 16), new ItemStack(Items.COOKED_BEEF, 32), new ItemStack(Items.COOKED_PORKCHOP, 32),
                new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.TORCH, 32));
        assertSortingTwiceChangesNothing(helper, loadout, 4, true, false, false, false, "a loadout with tied stacks");

        RandomSource random = RandomSource.create(20261011L);
        for (int test = 0; test < 100; test++) {
            boolean playerInventory = test % 3 == 0;
            int numRows = playerInventory ? 4 : test % 3 == 1 ? 3 : 6;
            int situation = playerInventory ? random.nextInt(4) : 0;
            List<ItemStack> stacks = TestInventories.random(random, numRows * 9);
            assertSortingTwiceChangesNothing(helper, stacks, numRows, playerInventory, situation == 1, situation == 2, situation == 3, "inventory " + test);
        }
        helper.succeed();
    }

    @GameTest
    public void aFullHotbarTakesNoMoreGroups(GameTestHelper helper) {
        List<ItemStack> stacks = new ArrayList<>(List.of(
                new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.BOW), new ItemStack(Items.BREAD, 16),
                PotionContents.createItemStack(Items.POTION, Potions.HEALING), new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.TORCH, 32),
                new ItemStack(Items.IRON_AXE)));
        addCopies(stacks, Items.OAK_LOG, 3);
        stacks.add(new ItemStack(Items.IRON_PICKAXE));
        addCopies(stacks, Items.COBBLESTONE, 12);
        stacks.add(new ItemStack(Items.IRON_SHOVEL));
        stacks.add(new ItemStack(Items.IRON_SHOVEL));
        addCopies(stacks, Items.DIRT, 8);

        int[] sorted = sort(helper, stacks, 4, true, false, false, false, "the player's inventory");
        assertInRow(helper, sorted, 0, List.of(0, 6, 10), stacks, "the sword, axe and pickaxe");
        helper.succeed();
    }

    /** Sorts the stacks, lays them out as sorted, sorts again: every position must hold an equal stack. */
    private static void assertSortingTwiceChangesNothing(GameTestHelper helper, List<ItemStack> stacks, int numRows, boolean playerInventory, boolean inDeepDark, boolean inNether, boolean inWater, String label) {
        int[] first = sort(helper, stacks, numRows, playerInventory, inDeepDark, inNether, inWater, label);
        List<ItemStack> laidOut = new ArrayList<>();
        for (int id : first) if (id >= 0) laidOut.add(stacks.get(id));
        int[] second = sort(helper, laidOut, numRows, playerInventory, inDeepDark, inNether, inWater, label + " sorted again");
        for (int position = 0; position < first.length; position++) {
            ItemStack before = first[position] < 0 ? ItemStack.EMPTY : stacks.get(first[position]);
            ItemStack after = second[position] < 0 ? ItemStack.EMPTY : laidOut.get(second[position]);
            if (!ItemStack.matches(before, after)) {
                helper.fail(label + ": sorting again moved " + before.getHoverName().getString() + " at " + position + " for " + after.getHoverName().getString() + ", in " + TestInventories.describe(stacks));
            }
        }
    }

    private static void addCopies(List<ItemStack> stacks, Item item, int numStacks) {
        for (int i = 0; i < numStacks; i++) stacks.add(new ItemStack(item, 64));
    }

    private static List<Integer> range(int from, int to) {
        return IntStream.range(from, to).boxed().toList();
    }

    /** Each of the given stacks is in that row. */
    private static void assertInRow(GameTestHelper helper, int[] sorted, int row, List<Integer> ids, List<ItemStack> stacks, String what) {
        for (int i = 0; i < sorted.length; i++) {
            if (ids.contains(sorted[i]) && i / 9 != row) helper.fail(what + " should be in row " + row + ": " + Arrays.toString(sorted) + " for " + TestInventories.describe(stacks));
        }
    }

    /** That row holds exactly the given stacks. */
    private static void assertRowIs(GameTestHelper helper, int[] sorted, int row, List<Integer> ids, List<ItemStack> stacks, String what) {
        Set<Integer> inRow = new HashSet<>();
        for (int column = 0; column < 9; column++) if (sorted[row * 9 + column] >= 0) inRow.add(sorted[row * 9 + column]);
        if (!inRow.equals(new HashSet<>(ids))) helper.fail("Row " + row + " should hold just " + what + ": " + Arrays.toString(sorted) + " for " + TestInventories.describe(stacks));
    }

    /** Some row below the hotbar holds exactly the given stacks. */
    private static void assertWholeRow(GameTestHelper helper, int[] sorted, List<Integer> ids, List<ItemStack> stacks, String what) {
        for (int row = 1; row < sorted.length / 9; row++) {
            Set<Integer> inRow = new HashSet<>();
            for (int column = 0; column < 9; column++) if (sorted[row * 9 + column] >= 0) inRow.add(sorted[row * 9 + column]);
            if (inRow.equals(new HashSet<>(ids))) return;
        }
        helper.fail("No row holds just " + what + ": " + Arrays.toString(sorted) + " for " + TestInventories.describe(stacks));
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

    /** The rows that hold any of the given stacks hold nothing else. */
    private static void assertRowsHoldOnly(GameTestHelper helper, int[] sorted, List<Integer> ids, List<ItemStack> stacks, String what) {
        for (int row = 0; row < sorted.length / 9; row++) {
            boolean holdsThem = false, holdsOthers = false;
            for (int column = 0; column < 9; column++) {
                int id = sorted[row * 9 + column];
                if (ids.contains(id)) holdsThem = true;
                else if (id >= 0) holdsOthers = true;
            }
            if (holdsThem && holdsOthers) helper.fail(what + " share row " + row + " with other stacks: " + Arrays.toString(sorted) + " for " + TestInventories.describe(stacks));
        }
    }

    /** The given stacks sit next to each other, in one row, in any order. */
    private static void assertConsecutive(GameTestHelper helper, int[] sorted, List<Integer> ids, List<ItemStack> stacks, String what) {
        int first = Integer.MAX_VALUE, last = -1;
        for (int i = 0; i < sorted.length; i++) {
            if (ids.contains(sorted[i])) {
                first = Math.min(first, i);
                last = Math.max(last, i);
            }
        }
        if (last - first + 1 != ids.size() || first / 9 != last / 9) {
            helper.fail(what + " aren't side by side in one row: " + Arrays.toString(sorted) + " for " + TestInventories.describe(stacks));
        }
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
