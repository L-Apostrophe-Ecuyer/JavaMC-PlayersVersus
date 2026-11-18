package frootloops.versus.mod.items_and_effects.containers;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The peek's selection: through the items and past them to nothing, remembering where a take left it. */
class PeekCursorTest {

    private static final int SIZE = 27, DOWN = -1, UP = 1;

    private static Set<Integer> box(int... filled) {
        Set<Integer> slots = new HashSet<>();
        IntStream.of(filled).forEach(slots::add);
        return slots;
    }

    private static IntPredicate holds(Set<Integer> box) {
        return box::contains;
    }

    @Test
    void fromNothingDownIsTheFirstItemAndUpTheLast() {
        PeekCursor cursor = new PeekCursor();
        cursor.scroll(SIZE, holds(box(3, 8, 20)), DOWN);
        assertEquals(3, cursor.selected());
        cursor = new PeekCursor();
        cursor.scroll(SIZE, holds(box(3, 8, 20)), UP);
        assertEquals(20, cursor.selected());
    }

    @Test
    void theWheelPassesThroughNothingBetweenTheLastItemAndTheFirst() {
        PeekCursor cursor = new PeekCursor();
        IntPredicate box = holds(box(3, 8, 20));
        cursor.scroll(SIZE, box, DOWN);
        cursor.scroll(SIZE, box, DOWN);
        cursor.scroll(SIZE, box, DOWN);
        assertEquals(20, cursor.selected());
        cursor.scroll(SIZE, box, DOWN);
        assertEquals(-1, cursor.selected(), "past the last item: nothing");
        cursor.scroll(SIZE, box, DOWN);
        assertEquals(3, cursor.selected(), "then the first again");
        cursor.scroll(SIZE, box, UP);
        assertEquals(-1, cursor.selected(), "back before the first: nothing");
        cursor.scroll(SIZE, box, UP);
        assertEquals(20, cursor.selected(), "then the last");
    }

    @Test
    void oneItemTogglesWithNothing() {
        PeekCursor cursor = new PeekCursor();
        IntPredicate box = holds(box(5));
        cursor.scroll(SIZE, box, DOWN);
        assertEquals(5, cursor.selected());
        cursor.scroll(SIZE, box, DOWN);
        assertEquals(-1, cursor.selected());
        cursor.scroll(SIZE, box, DOWN);
        assertEquals(5, cursor.selected());
    }

    @Test
    void afterATakeNothingIsSelectedButTheWheelGoesOnFromThere() {
        Set<Integer> box = box(3, 8, 20);
        PeekCursor cursor = new PeekCursor();
        cursor.scroll(SIZE, holds(box), DOWN);
        cursor.scroll(SIZE, holds(box), DOWN);
        assertEquals(8, cursor.selected());
        cursor.taken();
        box.remove(8);
        assertEquals(-1, cursor.selected(), "a second take needs a turn of the wheel");
        cursor.scroll(SIZE, holds(box), DOWN);
        assertEquals(20, cursor.selected(), "down goes on after the slot just emptied");
        cursor.taken();
        box.remove(20);
        cursor.scroll(SIZE, holds(box), UP);
        assertEquals(3, cursor.selected(), "up goes back before it");
    }

    @Test
    void aSlotEmptiedOrChangedUnselectsAndKeepsThePlace() {
        Set<Integer> box = box(3, 8, 20);
        PeekCursor cursor = new PeekCursor();
        cursor.scroll(SIZE, holds(box), DOWN);
        cursor.scroll(SIZE, holds(box), DOWN);
        // Someone else took the selected stack
        box.remove(8);
        cursor.refresh(holds(box), slot -> true);
        assertEquals(-1, cursor.selected());
        cursor.scroll(SIZE, holds(box), DOWN);
        assertEquals(20, cursor.selected(), "on from where it was");
        // Something else came to be in the selected slot
        cursor.refresh(holds(box), slot -> slot != 20);
        assertEquals(-1, cursor.selected(), "a changed slot unselects");
        cursor.refresh(holds(box), slot -> true);
        assertEquals(-1, cursor.selected(), "and stays unselected");
    }

    @Test
    void anUnchangedSlotStaysSelected() {
        PeekCursor cursor = new PeekCursor();
        cursor.scroll(SIZE, holds(box(3, 8)), DOWN);
        cursor.refresh(holds(box(3, 8)), slot -> true);
        assertEquals(3, cursor.selected());
    }

    @Test
    void aResetForgetsThePlace() {
        Set<Integer> box = box(3, 8, 20);
        PeekCursor cursor = new PeekCursor();
        cursor.scroll(SIZE, holds(box), DOWN);
        cursor.scroll(SIZE, holds(box), DOWN);
        cursor.taken();
        cursor.reset();
        cursor.scroll(SIZE, holds(box), DOWN);
        assertEquals(3, cursor.selected(), "a new look starts from the first item");
    }

    @Test
    void anEmptyBoxSelectsNothing() {
        PeekCursor cursor = new PeekCursor();
        cursor.scroll(SIZE, holds(box()), DOWN);
        assertEquals(-1, cursor.selected());
    }
}
