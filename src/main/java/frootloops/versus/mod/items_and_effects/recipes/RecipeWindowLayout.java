package frootloops.versus.mod.items_and_effects.recipes;

/**
 * Where things are in a recipe book collection's popup and window (FamilyWindow): a title bar with a close box at its
 * right end, then rows of cells, six at most to a row, each holding an item's icon at three quarters of its size, so
 * that the window takes less room than the inventory's slots would.
 *
 * @param x          its left edge
 * @param y          its top edge
 * @param count      how many items it shows
 * @param titleWidth how wide its title is, in pixels
 */
public record RecipeWindowLayout(int x, int y, int count, int titleWidth) {
    /** A cell's size, an icon's (an item's 16 pixels at three quarters), the title bar's height, the edge around the cells. */
    public static final int CELL = 14, ICON = 12, TITLE = 12, PADDING = 4;
    /** The close box's width, at the title bar's right end. */
    public static final int CLOSE = 10;
    public static final int MAX_COLUMNS = 6;

    public int columns() {
        return Math.max(1, Math.min(count, MAX_COLUMNS));
    }

    public int rows() {
        return Math.max(1, (count + columns() - 1) / columns());
    }

    public int width() {
        return Math.max(columns() * CELL + 2 * PADDING, PADDING + titleWidth + 2 + CLOSE);
    }

    public int height() {
        return TITLE + rows() * CELL + 2 * PADDING;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width() && mouseY >= y && mouseY < y + height();
    }

    public boolean inTitleBar(double mouseX, double mouseY) {
        return contains(mouseX, mouseY) && mouseY < y + TITLE;
    }

    public boolean inCloseBox(double mouseX, double mouseY) {
        return inTitleBar(mouseX, mouseY) && mouseX >= x + width() - CLOSE;
    }

    /** The cell under the point, or -1. */
    public int cellAt(double mouseX, double mouseY) {
        double left = mouseX - (x + PADDING), top = mouseY - (y + TITLE + PADDING);
        if (left < 0 || top < 0) return -1;
        int column = (int) (left / CELL), row = (int) (top / CELL);
        if (column >= columns() || row >= rows()) return -1;
        int index = row * columns() + column;
        return index < count ? index : -1;
    }

    /** The left edge of the cell. */
    public int cellX(int index) {
        return x + PADDING + (index % columns()) * CELL;
    }

    /** The top edge of the cell. */
    public int cellY(int index) {
        return y + TITLE + PADDING + (index / columns()) * CELL;
    }

    /** The same window moved to this corner. */
    public RecipeWindowLayout at(int newX, int newY) {
        return new RecipeWindowLayout(newX, newY, count, titleWidth);
    }
}
