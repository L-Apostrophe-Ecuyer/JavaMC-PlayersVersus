package frootloops.versus.mod.environment.worldgen.rocks;

/** Numbers drawn from a seed, one after another: the same seed draws the same numbers. */
final class RockDice {

    private final long seed;
    private int rolls;

    RockDice(long seed) {
        this.seed = seed;
    }

    long next() {
        return RockNoise.hash(this.seed, ++this.rolls);
    }

    /** From 0 (included) to 1 (not). */
    double unit() {
        return RockNoise.unit(this.next());
    }

    double between(double from, double to) {
        return from + (to - from) * this.unit();
    }

    /** A whole number from {@code from} to {@code to}, both included. */
    int whole(int from, int to) {
        return from + (int) ((to - from + 1) * this.unit());
    }

    double angle() {
        return this.between(0, 2 * Math.PI);
    }

    boolean chance(double chance) {
        return this.unit() < chance;
    }
}
