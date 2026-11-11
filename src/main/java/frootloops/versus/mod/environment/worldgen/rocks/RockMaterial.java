package frootloops.versus.mod.environment.worldgen.rocks;

/**
 * What a cell of a rock is made of; a {@link RockPalette} says which block each is. The first three are the rock
 * itself, as a builder would shade it: its shade in cracks and under overhangs, its light on the faces and edges that
 * catch it and in streaks for texture, and the plain rock everywhere else.
 */
public enum RockMaterial {
    /** The rock in shade: its undersides, inside corners and cracks, and the least lit of it (tuff). */
    SHADE,
    /** The rock (stone): most of it. */
    BASE,
    /** The rock in the light: highlights and the edges that stand out (andesite). */
    LIGHT,
    /** The rock in the brightest light, rarely (clay). */
    BRIGHT,
    /** Earth under the grass on top. */
    SOIL,
    /** The plant cover of level tops, where the sky is open. */
    GRASS,
    /** Moss: among the grass, on ledges, and hanging a little down the face from an edge. */
    MOSS;

    private static final RockMaterial[] VALUES = values();

    public static RockMaterial of(int ordinal) {
        return VALUES[ordinal];
    }

    /** Whether this is bare rock rather than plants or earth. */
    public boolean isRock() {
        return this.ordinal() <= BRIGHT.ordinal();
    }
}
