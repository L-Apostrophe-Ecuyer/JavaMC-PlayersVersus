package frootloops.versus.mod.environment.worldgen.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_BED;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_FADE;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_HALF_WIDTH;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_TOP;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_VALLEY_MAX_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_WIDENING;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_Y;

/**
 * Density-function type {@code players-versus:high_river}: the high river's valley, which the final density
 * ({@link PvFinalDensity}) takes the minimum with (the refactor plan, Section 10, question 7). A river on its own noise,
 * its water at {@code HIGH_RIVER_Y}, cut into ground that rises above that:
 * <ul>
 *   <li>above the surface the valley opens every block where the noise is within a half width that grows with height,
 *   so no ground is left over the water: it's cut back into banks;</li>
 *   <li>at the surface and in a shallow bed under it, it opens blocks only where the terrain at the surface was solid,
 *   so the river runs on to the ground's edge, where the aquifer's water there spills ({@code aquifer/PvAquifer});</li>
 *   <li>it keeps out of ground that rises far above the surface ({@link #activity}).</li>
 * </ul>
 * Negative where it opens a block, infinite elsewhere, so the final density is unchanged away from the river.
 *
 * <p>Its three inputs are 2D, {@code interpolated} on the terrain pass's cells, and the aquifer reads the same functions
 * on lattices of the same points ({@code aquifer/PvAquifer}); for 2D values both interpolations give the same doubles,
 * so the aquifer's water ({@link #waterAt}) fills exactly what the valley opens at and under the surface.
 *
 * @param channel the river noise: the river runs along its zeros
 * @param depth   the depth at the surface's height: how far the ground rises above it
 * @param terrain the terrain ({@link PvTerrain}) at the surface's height: solid where above 0
 */
public record PvHighRiver(DensityFunction channel, DensityFunction depth, DensityFunction terrain) implements DensityFunction {

    public static final MapCodec<PvHighRiver> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("channel").forGetter(PvHighRiver::channel),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("depth").forGetter(PvHighRiver::depth),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("terrain").forGetter(PvHighRiver::terrain)
    ).apply(instance, PvHighRiver::new));
    private static final KeyDispatchDataCodec<PvHighRiver> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    /** The bed's bottom: the lowest block the river opens. */
    public static final int MIN_Y = HIGH_RIVER_Y - HIGH_RIVER_BED;
    /** Above the valley's top (exclusive), the river opens nothing. */
    public static final int MAX_Y = HIGH_RIVER_VALLEY_MAX_Y;

    /**
     * How much of the river there is where the ground rises this far above its surface: all of it up to
     * {@code HIGH_RIVER_TOP}, none from {@code HIGH_RIVER_FADE} further.
     */
    public static double activity(double depth) {
        return Mth.clamp((HIGH_RIVER_TOP + HIGH_RIVER_FADE - depth) / HIGH_RIVER_FADE, 0.0, 1.0);
    }

    /**
     * The river's half width at full activity, in noise units: the surface's, wider by {@code HIGH_RIVER_WIDENING} per
     * block above it, narrower in the bed down to nothing below its bottom; 0 outside the valley's heights.
     */
    public static double fullHalfWidth(int y) {
        if (y >= MAX_Y || y < MIN_Y) return 0.0;
        if (y >= HIGH_RIVER_Y) return HIGH_RIVER_HALF_WIDTH + (y - HIGH_RIVER_Y) * HIGH_RIVER_WIDENING;
        return HIGH_RIVER_HALF_WIDTH * (1.0 - (double) (HIGH_RIVER_Y - y) / (HIGH_RIVER_BED + 1));
    }

    /**
     * Whether the aquifer puts the river's water at a block: at or under the surface, where the valley opens it (the
     * same comparisons as {@link #compute}).
     */
    public static boolean waterAt(int y, double channel, double depth, double terrain) {
        if (y < MIN_Y || y > HIGH_RIVER_Y) return false;
        return Math.abs(channel) < activity(depth) * fullHalfWidth(y) && terrain > 0.0;
    }

    @Override
    public double compute(FunctionContext pos) {
        int y = pos.blockY();
        double full = fullHalfWidth(y);
        if (full == 0.0) return Double.POSITIVE_INFINITY;
        double across = Math.abs(this.channel.compute(pos));
        if (across >= full) return Double.POSITIVE_INFINITY;
        double halfWidth = activity(this.depth.compute(pos)) * full;
        if (across >= halfWidth) return Double.POSITIVE_INFINITY;
        if (y <= HIGH_RIVER_Y && !(this.terrain.compute(pos) > 0.0)) return Double.POSITIVE_INFINITY;
        return across - halfWidth;
    }

    @Override
    public void fillArray(double[] densities, ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new PvHighRiver(this.channel.mapAll(visitor), this.depth.mapAll(visitor), this.terrain.mapAll(visitor)));
    }

    /** The widest the valley gets, at its top, where the channel's value is 0. */
    @Override
    public double minValue() {
        return -fullHalfWidth(MAX_Y - 1);
    }

    @Override
    public double maxValue() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
