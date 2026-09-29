package frootloops.versus.mod.environment.worldgen.density;

import net.minecraft.util.Mth;

/**
 * The density-function operations the aquifer's formulas ({@code aquifer/AquiferFormulas}) are written with, as they
 * were in 1.21.10's JSON they come from, so the aquifer keeps its bands and values.
 *
 * <p>Two of 1.21.10's rules matter: {@code mul} of two functions returns 0 when the first is 0, and bands on
 * {@code minecraft:y} test {@link #yValue}, not the integer.
 */
public final class DensityOps {

    private DensityOps() {
    }

    /**
     * 1.21.10's {@code minecraft:y} at a block: a {@code y_clamped_gradient} from -4064 to 4062, which rounds a few
     * heights (y 32 among them) to just below the integer. The aquifer's bands test this value. (26.3's is exact; the
     * terrain's bands in the data are written to hold the same heights, {@code tools/port-26.3/pv_density.py}.)
     */
    public static double yValue(int y) {
        return Mth.clampedMap((double) y, -4064.0, 4062.0, -4064.0, 4062.0);
    }

    /** {@code y_clamped_gradient}. */
    public static double gradient(int y, int fromY, int toY, double fromValue, double toValue) {
        return Mth.clampedMap((double) y, (double) fromY, (double) toY, fromValue, toValue);
    }

    /** {@code mul} of two functions: 0 when the first is 0. */
    public static double mul(double first, double second) {
        return first == 0.0 ? 0.0 : first * second;
    }
}
