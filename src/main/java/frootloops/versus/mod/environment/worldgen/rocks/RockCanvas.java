package frootloops.versus.mod.environment.worldgen.rocks;

/**
 * What a rock is stamped into: the world while it generates, a grid in a test or a preview. Positions are the canvas's
 * own (the world's). A {@link RockStamper} asks for a column's ground once, before it writes anything in that column.
 */
public interface RockCanvas {

    /** The first position above the ground of this column: what stands on it (plants) and water don't count. */
    int ground(int x, int z);

    boolean water(int x, int y, int z);

    /** Whether rock may take this position: air, water, plants, earth and stone, but not what someone built. */
    boolean replaceable(int x, int y, int z);

    void rock(int x, int y, int z, RockMaterial material);

    /** Raises this column's ground: fills {@code from} up to {@code to} (exclusive), the ground's own top block on top. */
    void raise(int x, int z, int from, int to);

    /** A plant in this position, standing on the block below; {@code data} picks which. */
    void plant(int x, int y, int z, int data);
}
