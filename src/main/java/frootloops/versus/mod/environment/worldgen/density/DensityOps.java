package frootloops.versus.mod.environment.worldgen.density;

import net.minecraft.util.math.MathHelper;

/**
 * Vanilla's density-function operations as plain static methods, written to give the same doubles as the nodes they
 * stand for, so a Java kernel can fuse a tree of nodes without changing any value. The tests compare every kernel with
 * the JSON it replaced, to the bit.
 *
 * <p>Two of vanilla's rules matter for exactness: {@code mul} of two functions returns 0 when the first is 0, without
 * looking at the second (so a kernel skips that work too), and {@code range_choice} on {@code minecraft:y} tests
 * {@link #yValue}, not the integer.
 */
public final class DensityOps {

    private DensityOps() {
    }

    /**
     * Vanilla's {@code minecraft:y} at a block: a {@code y_clamped_gradient} from -4064 to 4062, which rounds a few
     * heights (y 32 among them) to just below the integer. Bands that test {@code minecraft:y} test this value.
     */
    public static double yValue(int y) {
        return MathHelper.clampedMap((double) y, -4064.0, 4062.0, -4064.0, 4062.0);
    }

    /** {@code y_clamped_gradient}. */
    public static double gradient(int y, int fromY, int toY, double fromValue, double toValue) {
        return MathHelper.clampedMap((double) y, (double) fromY, (double) toY, fromValue, toValue);
    }

    /**
     * {@code mul} of two functions: 0 when the first is 0. Both values are already computed here; where the second one
     * is expensive, kernels test the first themselves and skip it.
     */
    public static double mul(double first, double second) {
        return first == 0.0 ? 0.0 : first * second;
    }

    /** {@code half_negative}. */
    public static double halfNegative(double value) {
        return value > 0.0 ? value : value * 0.5;
    }

    /** {@code quarter_negative}. */
    public static double quarterNegative(double value) {
        return value > 0.0 ? value : value * 0.25;
    }

    /** {@code squeeze}: clamped to -1..1, then {@code x / 2 - x³ / 24}. */
    public static double squeeze(double value) {
        double clamped = MathHelper.clamp(value, -1.0, 1.0);
        return clamped / 2.0 - clamped * clamped * clamped / 24.0;
    }

    /** The range of {@link #squeeze}: it rises monotonically over -1..1. */
    public static final double SQUEEZE_MAX = 0.5 - 1.0 / 24.0;
}
