package frootloops.versus.mod.items_and_effects.inventory.sorting;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovePlannerTest {

    /** What the spare holds when it's outside the sorted slots. */
    private static final int OUTSIDE_STACK = 99;

    @Test
    void alreadySortedNeedsNoSwaps() {
        int[] contents = {3, 0, 7, 7, 1, 0, 2};
        assertEquals(0, MovePlanner.swapsWithSpare(contents, contents.clone(), -1).length);
        assertEquals(0, MovePlanner.swapsWithSpare(contents, contents.clone(), 1).length);
    }

    @Test
    void swapsRealiseTheTarget() {
        Random random = new Random(20261010);
        int[] sizes = {27, 36, 54};
        for (int test = 0; test < 1000; test++) {
            int n = sizes[test % sizes.length];
            int[] current = new int[n];
            for (int i = 0; i < n; i++) current[i] = random.nextInt(9);
            int[] target = current.clone();
            for (int i = n - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int swapped = target[i];
                target[i] = target[j];
                target[j] = swapped;
            }
            int spare = random.nextBoolean() ? -1 : random.nextInt(9);

            int[] contents = current.clone();
            int outside = OUTSIDE_STACK;
            for (int p : MovePlanner.swapsWithSpare(current, target, spare)) {
                assertNotEquals(spare, p, "test " + test + " swaps the spare with itself");
                int moved = contents[p];
                if (spare < 0) {
                    contents[p] = outside;
                    outside = moved;
                } else {
                    contents[p] = contents[spare];
                    contents[spare] = moved;
                }
            }
            assertArrayEquals(target, contents, "test " + test);
            if (spare < 0) assertEquals(OUTSIDE_STACK, outside, "test " + test + " leaves the spare with another stack");
        }
    }

    @Test
    void twoCycleCostsThreeOutsideOneInside() {
        int[] current = {1, 2};
        int[] target = {2, 1};
        assertEquals(3, MovePlanner.swapsWithSpare(current, target, -1).length);
        assertEquals(1, MovePlanner.swapsWithSpare(current, target, 0).length);
    }

    @Test
    void equalStacksAreInterchangeable() {
        int[] swaps = MovePlanner.swapsWithSpare(new int[]{5, 5, 5, 0}, new int[]{0, 5, 5, 5}, -1);
        assertTrue(swaps.length <= 3, "took " + swaps.length + " swaps: " + Arrays.toString(swaps));
        for (int p : swaps) assertTrue(p == 0 || p == 3, "moved the stack in place at " + p);
    }

    @Test
    void unequalContentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> MovePlanner.swapsWithSpare(new int[]{1, 2}, new int[]{1, 1}, -1));
        assertThrows(IllegalArgumentException.class, () -> MovePlanner.swapsWithSpare(new int[]{1, 1}, new int[]{2, 1}, -1));
        assertThrows(IllegalArgumentException.class, () -> MovePlanner.swapsWithSpare(new int[]{1, 2}, new int[]{2, 1, 0}, -1));
    }

    @Test
    void chooseSparePrefersEmptyThenPlaceable() {
        boolean[] none = new boolean[9];
        boolean[] emptyAt4And8 = new boolean[9];
        emptyAt4And8[4] = true;
        emptyAt4And8[8] = true;
        boolean[] placeableAt2And5 = new boolean[9];
        placeableAt2And5[2] = true;
        placeableAt2And5[5] = true;

        assertEquals(4, MovePlanner.chooseSpare(emptyAt4And8, placeableAt2And5));
        assertEquals(2, MovePlanner.chooseSpare(none, placeableAt2And5));
        assertEquals(-1, MovePlanner.chooseSpare(none, none));
    }
}
