package frootloops.versus.mod.items_and_effects.containers;

import java.util.function.IntPredicate;

/**
 * The peek's selection in a box: the wheel goes from item to item (the empty slots skipped) and, past the last or before
 * the first, to nothing, so that one more notch unselects and a use opens the box again. After a take, or when the
 * selected slot empties or changes, nothing is selected, but the place is kept: the next turn of the wheel goes on from
 * there. A new look starts afresh.
 */
public final class PeekCursor {

    private int selected = -1, anchor = -1;

    public int selected() {
        return this.selected;
    }

    /** A turn of the wheel (up is positive and goes back), over a box's slots, those that hold an item. */
    public void scroll(int size, IntPredicate holds, int wheel) {
        if (wheel == 0) return;
        if (this.selected < 0) {
            // From nothing: on from the place kept, or else the first item (down) or the last (up)
            this.selected = ShulkerSelection.next(size, holds, this.anchor, wheel);
            this.anchor = -1;
            return;
        }
        int next = ShulkerSelection.next(size, holds, this.selected, wheel);
        boolean wrapped = next < 0 || (wheel < 0 ? next <= this.selected : next >= this.selected);
        this.selected = wrapped ? -1 : next;
    }

    /** The selected stack was taken: nothing selected, the place kept. */
    public void taken() {
        this.anchor = this.selected;
        this.selected = -1;
    }

    /** The box's contents again: a selected slot emptied, or holding something else than when selected, unselects. */
    public void refresh(IntPredicate holds, IntPredicate unchanged) {
        if (this.selected >= 0 && (!holds.test(this.selected) || !unchanged.test(this.selected))) taken();
    }

    /** A new look: nothing selected, no place kept. */
    public void reset() {
        this.selected = -1;
        this.anchor = -1;
    }
}
