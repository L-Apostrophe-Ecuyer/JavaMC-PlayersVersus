package frootloops.versus.mod.items_and_effects.containers;

/**
 * The bundle's tooltip grid, 5 wide and 4 high (vanilla's is 4 by 3): every item up to twenty; past twenty, nineteen and
 * a "+N" cell for the rest. The game asks this for the count it shows and lets be selected (on both sides).
 */
public final class BundleGrid {

    public static final int COLUMNS = 5, ROWS = 4, CELLS = COLUMNS * ROWS;

    private BundleGrid() {
    }

    /** How many of a bundle's stacks the grid shows (the rest are counted in the "+N" cell). */
    public static int itemsToShow(int size) {
        return size > CELLS ? CELLS - 1 : size;
    }
}
