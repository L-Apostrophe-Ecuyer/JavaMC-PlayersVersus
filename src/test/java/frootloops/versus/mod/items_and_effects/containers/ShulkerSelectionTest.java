package frootloops.versus.mod.items_and_effects.containers;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The wheel over a shulker box: from item to item, the empty slots skipped, round and round. */
class ShulkerSelectionTest {

    // The wheel as the game gives it: up is positive, and moves back through the slots (as the hotbar does)
    private static final int DOWN = -1, UP = 1;

    /** A box's 27 slots, items in these. */
    private static IntPredicate box(int... filled) {
        Set<Integer> slots = IntStream.of(filled).boxed().collect(Collectors.toSet());
        return slots::contains;
    }

    private static int next(IntPredicate box, int selected, int wheel) {
        return ShulkerSelection.next(27, box, selected, wheel);
    }

    @Test
    void scrollingDownFromNothingSelectsTheFirstItemAndUpTheLast() {
        assertEquals(3, next(box(3, 8, 20), -1, DOWN));
        assertEquals(20, next(box(3, 8, 20), -1, UP));
    }

    @Test
    void theEmptySlotsAreSkipped() {
        assertEquals(8, next(box(3, 8, 20), 3, DOWN));
        assertEquals(20, next(box(3, 8, 20), 8, DOWN));
        assertEquals(3, next(box(3, 8, 20), 8, UP));
    }

    @Test
    void theSelectionWrapsBothWays() {
        assertEquals(3, next(box(3, 8, 20), 20, DOWN));
        assertEquals(20, next(box(3, 8, 20), 3, UP));
        assertEquals(26, next(box(0, 26), 0, UP));
        // One item: it stays selected
        assertEquals(5, next(box(5), 5, DOWN));
    }

    @Test
    void anEmptyBoxHasNothingToSelect() {
        assertEquals(-1, next(box(), -1, DOWN));
        assertEquals(-1, next(box(), 4, UP));
    }

    @Test
    void aSelectedSlotEmptiedSinceStepsOnFromWhereItWas() {
        assertEquals(8, next(box(3, 8, 20), 5, DOWN));
        assertEquals(3, next(box(3, 8, 20), 5, UP));
    }

    @Test
    void noTurnOfTheWheelChangesNothing() {
        assertEquals(8, next(box(3, 8, 20), 8, 0));
    }
}
