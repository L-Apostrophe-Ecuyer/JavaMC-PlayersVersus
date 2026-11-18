package frootloops.versus.mod.items_and_effects.containers;

import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The shulker grid's colours: the box's own, in three shades. */
class ShulkerColorsTest {

    private static int channel(int rgb, int shift) {
        return rgb >> shift & 0xFF;
    }

    private static int brightness(int rgb) {
        return channel(rgb, 16) + channel(rgb, 8) + channel(rgb, 0);
    }

    @Test
    void everyDyeHasItsOwnColourAndTheUndyedBoxItsOwnToo() {
        Set<Integer> seen = new HashSet<>();
        for (DyeColor dye : DyeColor.VALUES) assertTrue(seen.add(ShulkerColors.of(dye)), dye + " shares its colour");
        assertTrue(seen.add(ShulkerColors.of(null)), "the undyed box shares a dye's colour");
        assertEquals(17, seen.size());
    }

    @Test
    void theBoxesColoursAreTheDyesUsualOnes() {
        assertEquals(0xB02E26, ShulkerColors.of(DyeColor.RED));
        assertEquals(0x3C44AA, ShulkerColors.of(DyeColor.BLUE));
        assertEquals(0x80C71F, ShulkerColors.of(DyeColor.LIME));
    }

    @Test
    void theShadesRunFromTheFrameThroughTheCellsToTheHighlight() {
        for (DyeColor dye : DyeColor.VALUES) {
            int base = ShulkerColors.of(dye);
            int frame = ShulkerColors.frame(base), cell = ShulkerColors.cell(base), highlight = ShulkerColors.highlight(base);
            assertTrue(brightness(frame) < brightness(cell), dye + ": the frame should be darker than the cells");
            assertTrue(brightness(cell) < brightness(highlight), dye + ": the highlight should be lighter than the cells");
            assertNotEquals(frame, cell, dye + " frame and cells");
            for (int shade : new int[]{frame, cell, highlight}) assertEquals(0xFF, shade >>> 24, dye + ": every shade should be opaque");
        }
    }

    @Test
    void theCellsOfADarkBoxAreStillSeenAgainstItsFrame() {
        int black = ShulkerColors.of(DyeColor.BLACK);
        assertTrue(brightness(ShulkerColors.cell(black)) - brightness(ShulkerColors.frame(black)) >= 24, "black's cells should stand out from its frame");
    }

    @Test
    void theHueOfTheBoxIsKept() {
        // A red box's cells are redder than they are green or blue, a blue box's bluer
        int red = ShulkerColors.cell(ShulkerColors.of(DyeColor.RED)), blue = ShulkerColors.cell(ShulkerColors.of(DyeColor.BLUE));
        assertTrue(channel(red, 16) > channel(red, 8) && channel(red, 16) > channel(red, 0));
        assertTrue(channel(blue, 0) > channel(blue, 16) && channel(blue, 0) > channel(blue, 8));
    }
}
