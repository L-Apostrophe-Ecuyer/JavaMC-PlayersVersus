package frootloops.versus.mod.items_and_effects.recipes;

import org.jetbrains.annotations.Nullable;

/**
 * When a recipe group's popup opens and closes under the mouse. A group opens once the mouse has stayed on its button for
 * the dwell (so sweeping across the page opens nothing), and its popup closes when the mouse is outside both the button
 * and the popup. A group that was clicked stays closed until the mouse has left it, so placing a recipe doesn't open the
 * popup again under the cursor. A pinned popup (one clicked open) stays open wherever the mouse goes, until it closes
 * by other means (a click outside, Esc). Called once a frame with what's under the mouse.
 *
 * @param <B> what stands for a group's button (compared by identity, as the page reuses its buttons)
 */
public final class RecipeGroupHover<B> {
    /** Three ticks: long enough that a mouse passing over doesn't open anything, short enough to feel immediate. */
    public static final long DWELL_MILLIS = 150;

    public enum Action { NONE, OPEN, CLOSE }

    @Nullable private B candidate;
    @Nullable private B suppressed;
    private long since;
    private boolean pinned;

    /**
     * @param hovered the button of a group of several recipes under the mouse, or null
     * @param open whether a popup is open
     * @param overOpenGroup whether the mouse is over the open popup's button or the popup (only read when one is open)
     * @param now the time, in milliseconds
     * @return OPEN: open the hovered group's popup; CLOSE: close the open popup; NONE: nothing
     */
    public Action update(@Nullable B hovered, boolean open, boolean overOpenGroup, long now) {
        if (!open) pinned = false;
        if (open) {
            candidate = null;
            return overOpenGroup || pinned ? Action.NONE : Action.CLOSE;
        }
        if (suppressed != null) {
            if (hovered == suppressed) return Action.NONE;
            suppressed = null;
        }
        if (hovered != candidate) {
            candidate = hovered;
            since = now;
            return Action.NONE;
        }
        if (hovered != null && now - since >= DWELL_MILLIS) {
            candidate = null;
            return Action.OPEN;
        }
        return Action.NONE;
    }

    /** The popup now open stays open wherever the mouse goes, until it closes. */
    public void pin() {
        pinned = true;
        candidate = null;
    }

    public boolean isPinned() {
        return pinned;
    }

    /** The group was clicked (or closed under the mouse): its popup stays closed until the mouse leaves it. */
    public void suppress(@Nullable B clicked) {
        suppressed = clicked;
        candidate = null;
    }
}
