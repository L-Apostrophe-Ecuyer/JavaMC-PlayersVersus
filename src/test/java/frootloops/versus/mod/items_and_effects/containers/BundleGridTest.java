package frootloops.versus.mod.items_and_effects.containers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BundleGridTest {

    @Test
    void theGridShowsEverythingUpToTwentyAndNineteenPast() {
        assertEquals(5, BundleGrid.COLUMNS);
        assertEquals(4, BundleGrid.ROWS);
        for (int size = 0; size <= 20; size++) assertEquals(size, BundleGrid.itemsToShow(size), "a bundle of " + size);
        // Past twenty, the last cell is the "+N" of the rest
        for (int size : new int[]{21, 25, 64}) assertEquals(19, BundleGrid.itemsToShow(size), "a bundle of " + size);
    }
}
