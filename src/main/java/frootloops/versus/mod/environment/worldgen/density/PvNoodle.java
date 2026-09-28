package frootloops.versus.mod.environment.worldgen.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import static frootloops.versus.mod.environment.worldgen.density.DensityOps.gradient;

/**
 * Density-function type {@code players-versus:noodle}: noodle caves, negative where they're open (the old
 * {@code caves/noodle}). Its four inputs are vanilla's {@code interpolated} nodes, registered on their own
 * ({@code caves/noodle_*}) so that the final density ({@link PvFinalDensity}, which computes the noodle inline) and
 * the aquifer's basins ({@code caves/noodle}, read by {@code AquiferSpread}) share them.
 *
 * @param toggle    where noodles exist ({@code caves/noodle_toggle})
 * @param thickness {@code caves/noodle_thickness}
 * @param ridgeA    {@code caves/noodle_ridge_a}
 * @param ridgeB    {@code caves/noodle_ridge_b}
 * @param minValue  derived bounds
 * @param maxValue  derived bounds
 */
public record PvNoodle(DensityFunction toggle, DensityFunction thickness, DensityFunction ridgeA, DensityFunction ridgeB,
                       double minValue, double maxValue) implements DensityFunction {

    public static final MapCodec<PvNoodle> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.FUNCTION_CODEC.fieldOf("toggle").forGetter(PvNoodle::toggle),
            DensityFunction.FUNCTION_CODEC.fieldOf("thickness").forGetter(PvNoodle::thickness),
            DensityFunction.FUNCTION_CODEC.fieldOf("ridge_a").forGetter(PvNoodle::ridgeA),
            DensityFunction.FUNCTION_CODEC.fieldOf("ridge_b").forGetter(PvNoodle::ridgeB)
    ).apply(instance, PvNoodle::new));
    private static final CodecHolder<PvNoodle> CODEC_HOLDER = CodecHolder.of(CODEC);

    /** The lowest and highest {@link #bias}: the sums of its five gradients' extremes. */
    static final double BIAS_MIN = -0.05 + -0.1 + 0.0 + -0.3 + 0.0;
    static final double BIAS_MAX = 0.08 + 0.0 + 0.1 + 0.0 + 0.35;

    public PvNoodle(DensityFunction toggle, DensityFunction thickness, DensityFunction ridgeA, DensityFunction ridgeB) {
        this(toggle, thickness, ridgeA, ridgeB, BIAS_MIN + tunnelMin(thickness), BIAS_MAX + tunnelMax(thickness, ridgeA, ridgeB));
    }

    /** The noodle's height bias: fewer noodles near the surface and in the deep layers. */
    public static double bias(int y) {
        return gradient(y, 96, 56, -0.05, 0.08) + (gradient(y, 56, 40, 0.0, -0.1) + (gradient(y, 32, 20, 0.0, 0.1)
                + (gradient(y, -8, -32, 0.0, -0.3) + gradient(y, -52, -64, 0.0, 0.35))));
    }

    /** The noodle without its bias: 64 (solid) where the toggle is off, else the thickness plus the larger ridge. */
    public static double tunnel(NoisePos pos, DensityFunction toggle, DensityFunction thickness, DensityFunction ridgeA,
                                DensityFunction ridgeB) {
        double on = toggle.sample(pos);
        if (on >= -1000000.0 && on < -0.2) return 64.0;
        return thickness.sample(pos) + Math.max(Math.abs(ridgeA.sample(pos)), Math.abs(ridgeB.sample(pos))) * 1.5;
    }

    /** {@link #tunnel} is at least the thickness: the ridge term is at least 0. */
    static double tunnelMin(DensityFunction thickness) {
        return Math.min(64.0, thickness.minValue());
    }

    static double tunnelMax(DensityFunction thickness, DensityFunction ridgeA, DensityFunction ridgeB) {
        double ridge = Math.max(Math.max(Math.abs(ridgeA.minValue()), Math.abs(ridgeA.maxValue())),
                Math.max(Math.abs(ridgeB.minValue()), Math.abs(ridgeB.maxValue())));
        return Math.max(64.0, thickness.maxValue() + ridge * 1.5);
    }

    @Override
    public double sample(NoisePos pos) {
        return bias(pos.blockY()) + tunnel(pos, this.toggle, this.thickness, this.ridgeA, this.ridgeB);
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new PvNoodle(this.toggle.apply(visitor), this.thickness.apply(visitor), this.ridgeA.apply(visitor),
                this.ridgeB.apply(visitor)));
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
