package frootloops.versus.mod.environment.worldgen.rocks;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntBinaryOperator;

/**
 * A canvas for tests: remembers what was put where. Its ground rises with what is stamped, as the world's height map
 * does, so a stamper that read a column's ground after writing near it would be caught.
 */
final class RockTestCanvas implements RockCanvas {

    static final int NO_WATER = Integer.MIN_VALUE;

    /** What was put at each position: {@code rock:BASE}, {@code ground}, {@code plant}. */
    final Map<Long, String> cells = new HashMap<>();
    private final Map<Long, Integer> tops = new HashMap<>();
    private final IntBinaryOperator ground;
    private final int waterLevel;

    RockTestCanvas(IntBinaryOperator ground, int waterLevel) {
        this.ground = ground;
        this.waterLevel = waterLevel;
    }

    static long key(int x, int y, int z) {
        return ((long) (x + (1 << 20)) << 42) | ((long) (z + (1 << 20)) << 21) | (y + (1 << 12));
    }

    static int x(long key) {
        return (int) (key >>> 42) - (1 << 20);
    }

    static int z(long key) {
        return (int) ((key >>> 21) & ((1 << 21) - 1)) - (1 << 20);
    }

    static int y(long key) {
        return (int) (key & ((1 << 21) - 1)) - (1 << 12);
    }

    /** The ground before anything was stamped. */
    int original(int x, int z) {
        return this.ground.applyAsInt(x, z);
    }

    String at(int x, int y, int z) {
        return this.cells.get(key(x, y, z));
    }

    boolean isRock(int x, int y, int z) {
        String cell = this.at(x, y, z);
        return cell != null && cell.startsWith("rock:");
    }

    boolean isFullRock(int x, int y, int z) {
        String cell = this.at(x, y, z);
        return cell != null && cell.startsWith("rock:");
    }

    @Override
    public int ground(int x, int z) {
        return this.tops.getOrDefault(key(x, 0, z), this.original(x, z));
    }

    @Override
    public boolean water(int x, int y, int z) {
        return y <= this.waterLevel && y >= this.original(x, z) && !this.cells.containsKey(key(x, y, z));
    }

    @Override
    public boolean replaceable(int x, int y, int z) {
        return true;
    }

    @Override
    public void rock(int x, int y, int z, RockMaterial material) {
        this.cells.put(key(x, y, z), "rock:" + material);
        this.tops.merge(key(x, 0, z), y + 1, Math::max);
    }

    @Override
    public void raise(int x, int z, int from, int to) {
        for (int y = from; y < to; y++) this.cells.put(key(x, y, z), "ground");
        this.tops.merge(key(x, 0, z), to, Math::max);
    }

    @Override
    public void plant(int x, int y, int z, int data) {
        this.cells.put(key(x, y, z), "plant");
    }
}
