package frootloops.versus.mod.items_and_effects.recipes;

import org.junit.jupiter.api.Test;

import static frootloops.versus.mod.items_and_effects.recipes.RecipeWindowLayout.CELL;
import static frootloops.versus.mod.items_and_effects.recipes.RecipeWindowLayout.PADDING;
import static frootloops.versus.mod.items_and_effects.recipes.RecipeWindowLayout.TITLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A collection's window: rows of small cells under a title bar with a close box. */
class RecipeWindowLayoutTest {

    @Test
    void theCellsAreSmallerThanTheInventorysSlots() {
        assertTrue(CELL < 18, "a cell is " + CELL + " pixels, an inventory slot 18");
        assertEquals(12, RecipeWindowLayout.ICON, "icons are three quarters of an item's 16 pixels");
    }

    @Test
    void itemsFillRowsOfSixAtMost() {
        RecipeWindowLayout three = new RecipeWindowLayout(100, 50, 3, 20);
        assertEquals(3, three.columns());
        assertEquals(1, three.rows());
        RecipeWindowLayout ten = new RecipeWindowLayout(100, 50, 10, 20);
        assertEquals(6, ten.columns());
        assertEquals(2, ten.rows());
        assertEquals(TITLE + 2 * CELL + 2 * PADDING, ten.height());
        assertEquals(6 * CELL + 2 * PADDING, ten.width());
    }

    @Test
    void aLongTitleWidensTheWindow() {
        RecipeWindowLayout one = new RecipeWindowLayout(0, 0, 1, 80);
        assertTrue(one.width() >= 80 + RecipeWindowLayout.CLOSE, "a window " + one.width() + " wide for a title 80 wide");
    }

    @Test
    void theMouseFindsTheCellTitleAndCloseBox() {
        RecipeWindowLayout layout = new RecipeWindowLayout(100, 50, 8, 20);
        int firstX = 100 + PADDING, firstY = 50 + TITLE + PADDING;
        assertEquals(0, layout.cellAt(firstX + 1, firstY + 1));
        assertEquals(1, layout.cellAt(firstX + CELL + 1, firstY + 1));
        assertEquals(6, layout.cellAt(firstX + 1, firstY + CELL + 1));
        // Row two holds two of six: past them is nothing
        assertEquals(-1, layout.cellAt(firstX + 3 * CELL + 1, firstY + CELL + 1));
        assertEquals(-1, layout.cellAt(99, firstY + 1));
        assertTrue(layout.inTitleBar(101, 51));
        assertFalse(layout.inTitleBar(101, firstY + 1));
        assertTrue(layout.inCloseBox(100 + layout.width() - 3, 53));
        assertFalse(layout.inCloseBox(101, 53));
        assertTrue(layout.contains(100, 50));
        assertFalse(layout.contains(100 + layout.width(), 50));
    }

    @Test
    void aCellsCornerIsWhereItsIconGoes() {
        RecipeWindowLayout layout = new RecipeWindowLayout(10, 20, 7, 20);
        assertEquals(10 + PADDING + CELL, layout.cellX(1));
        assertEquals(20 + TITLE + PADDING + CELL, layout.cellY(6));
    }
}
