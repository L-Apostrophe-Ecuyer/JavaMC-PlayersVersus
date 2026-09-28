package frootloops.versus.mod.environment.worldgen.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

/**
 * Density-function type {@code players-versus:final_density}: the router's {@code final_density}, sampled for every
 * block. The interpolated terrain ({@link PvTerrain} inside vanilla's {@code interpolated} and {@code blend_density}),
 * scaled and squeezed, then cut by noodle caves ({@link PvNoodle}), giving the old JSON's doubles
 * ({@code TerrainPortTest}).
 *
 * <p>The JSON asked for the noodle at nearly every block: vanilla's {@code min} only skips it below the noodle's
 * lowest possible value, about -0.55, which a squeezed value (never below -0.46) doesn't reach. Here the bound
 * includes the noodle's height bias at the block's y, so the noodle is skipped for most air blocks, where the terrain
 * is already lower than any noodle there.
 *
 * @param terrain         {@code interpolated(blend_density(players-versus:terrain))}
 * @param noodleToggle    {@code caves/noodle_toggle}
 * @param noodleThickness {@code caves/noodle_thickness}
 * @param noodleRidgeA    {@code caves/noodle_ridge_a}
 * @param noodleRidgeB    {@code caves/noodle_ridge_b}
 * @param noodleFloor     the lowest the noodle can be above its height bias, less a margin for rounding
 * @param minValue        derived bounds
 * @param maxValue        derived bounds
 */
public record PvFinalDensity(DensityFunction terrain, DensityFunction noodleToggle, DensityFunction noodleThickness,
                             DensityFunction noodleRidgeA, DensityFunction noodleRidgeB, double noodleFloor,
                             double minValue, double maxValue) implements DensityFunction {

    public static final MapCodec<PvFinalDensity> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.FUNCTION_CODEC.fieldOf("terrain").forGetter(PvFinalDensity::terrain),
            DensityFunction.FUNCTION_CODEC.fieldOf("noodle_toggle").forGetter(PvFinalDensity::noodleToggle),
            DensityFunction.FUNCTION_CODEC.fieldOf("noodle_thickness").forGetter(PvFinalDensity::noodleThickness),
            DensityFunction.FUNCTION_CODEC.fieldOf("noodle_ridge_a").forGetter(PvFinalDensity::noodleRidgeA),
            DensityFunction.FUNCTION_CODEC.fieldOf("noodle_ridge_b").forGetter(PvFinalDensity::noodleRidgeB)
    ).apply(instance, PvFinalDensity::new));
    private static final CodecHolder<PvFinalDensity> CODEC_HOLDER = CodecHolder.of(CODEC);

    /**
     * Interpolation can land a hair outside its corners' range; this margin keeps {@link #noodleFloor} below any
     * value the noodle can take, so skipping it never changes the result.
     */
    private static final double ROUNDING_MARGIN = 1.0e-9;

    public PvFinalDensity(DensityFunction terrain, DensityFunction noodleToggle, DensityFunction noodleThickness,
                          DensityFunction noodleRidgeA, DensityFunction noodleRidgeB) {
        this(terrain, noodleToggle, noodleThickness, noodleRidgeA, noodleRidgeB,
                PvNoodle.tunnelMin(noodleThickness) - ROUNDING_MARGIN,
                Math.min(-DensityOps.SQUEEZE_MAX, PvNoodle.BIAS_MIN + PvNoodle.tunnelMin(noodleThickness)),
                Math.min(DensityOps.SQUEEZE_MAX, PvNoodle.BIAS_MAX + PvNoodle.tunnelMax(noodleThickness, noodleRidgeA, noodleRidgeB)));
    }

    @Override
    public double sample(NoisePos pos) {
        double terrain = DensityOps.squeeze(this.terrain.sample(pos) * 0.64);
        double bias = PvNoodle.bias(pos.blockY());
        if (terrain < bias + this.noodleFloor) return terrain;
        return Math.min(terrain, bias + PvNoodle.tunnel(pos, this.noodleToggle, this.noodleThickness, this.noodleRidgeA, this.noodleRidgeB));
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new PvFinalDensity(this.terrain.apply(visitor), this.noodleToggle.apply(visitor),
                this.noodleThickness.apply(visitor), this.noodleRidgeA.apply(visitor), this.noodleRidgeB.apply(visitor)));
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
