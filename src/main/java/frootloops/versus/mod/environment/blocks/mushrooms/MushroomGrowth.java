package frootloops.versus.mod.environment.blocks.mushrooms;

/**
 * How a small mushroom grows, by stages: one mushroom (vanilla's block), two, then three, giving off light 2 and 4 as
 * they grow. A mushroom grows a stage on a random tick, by chance, where it's dark (light 5 or less, or on mycelium), on
 * stone, tuff, logs, mushroom blocks or mycelium, and beside another of its kind; to the third stage, beside one of its
 * kind grown to two or more. Above y 0 only: below, brown and red mushrooms grow into pale shrooms instead, which grow
 * only there. Grown, a mushroom that can't grow any more shrinks a stage; at three, where it can, it spreads a mushroom
 * of its kind beside it.
 */
public final class MushroomGrowth {
    private MushroomGrowth() {}

    public enum Kind { BROWN, RED, PALE }

    public enum Action { NONE, GROW, SHRINK, SPREAD, TURN_PALE }

    /**
     * What's around a mushroom.
     *
     * @param dark          light 5 or less, or on mycelium
     * @param ground        on stone, tuff, logs, a mushroom block or mycelium
     * @param neighbour     another of its kind beside it (the blocks round it, diagonals and a level up or down too)
     * @param grownNeighbour another of its kind beside it grown to two or more
     * @param belowZero     below y 0
     */
    public record Conditions(boolean dark, boolean ground, boolean neighbour, boolean grownNeighbour, boolean belowZero) {}

    /** What a mushroom of this kind and stage (1 to 3) does on a random tick. */
    public static Action next(Kind kind, int stage, Conditions around) {
        boolean height = kind == Kind.PALE ? around.belowZero : !around.belowZero;
        boolean place = around.dark && around.ground && around.neighbour;
        boolean canGrow = place && height;
        return switch (stage) {
            case 1 -> canGrow ? Action.GROW : place && kind != Kind.PALE && around.belowZero ? Action.TURN_PALE : Action.NONE;
            case 2 -> !canGrow ? Action.SHRINK : around.grownNeighbour ? Action.GROW : Action.NONE;
            default -> canGrow ? Action.SPREAD : Action.SHRINK;
        };
    }

    /** The light a grown mushroom gives off: 2 for two, 4 for three. */
    public static int light(int stage) {
        return stage >= 3 ? 4 : stage == 2 ? 2 : 1;
    }
}
