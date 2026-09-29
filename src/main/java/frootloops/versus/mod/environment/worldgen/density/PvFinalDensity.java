package frootloops.versus.mod.environment.worldgen.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;

/**
 * Density-function type {@code players-versus:final_density}: the router's {@code final_density}, sampled for every
 * block. The interpolated terrain ({@link PvTerrain} inside vanilla's {@code interpolated} and {@code blend_density}),
 * scaled and squeezed, then cut by noodle caves ({@link PvNoodle}) and by the high river's valley ({@link PvHighRiver}),
 * giving the old JSON's doubles ({@code TerrainPortTest}) except in the flooded corridors
 * ({@link PvNoodle#corridorBias}), where in the basin layers, near the flooded caves, the noodle's height bias is lower,
 * and in the high river's valley, which the JSON never had. The aquifer floods what both open.
 *
 * <p>The JSON asked for the noodle at nearly every block: vanilla's {@code min} only skips it below the noodle's
 * lowest possible value, about -0.55, which a squeezed value (never below -0.46) doesn't reach. Here the bound
 * includes the noodle's height bias at the block's y, so the noodle is skipped for most air blocks, where the terrain
 * is already lower than any noodle there. The river is only asked about in its valley's heights.
 *
 * <p>With C2ME's density-function compiler ({@link DensityCompilerCompat}), {@link #apply} returns
 * {@link #asVanillaTypes} instead, which the compiler can compile.
 *
 * @param terrain         {@code interpolated(blend_density(players-versus:overworld/terrain))}
 * @param noodleToggle    {@code caves/noodle_toggle}
 * @param noodleThickness {@code caves/noodle_thickness}
 * @param noodleRidgeA    {@code caves/noodle_ridge_a}
 * @param noodleRidgeB    {@code caves/noodle_ridge_b}
 * @param entrances       the entrance value for the corridors, interpolated like the aquifer's lattice of it; only read
 *                        in the corridors' layers, so the JSON only fills it in around them
 * @param highRiver       the high river's valley ({@link PvHighRiver}), infinite away from it
 * @param noodleFloor     the lowest the noodle can be above its height bias, less a margin for rounding
 * @param minValue        derived bounds
 * @param maxValue        derived bounds
 */
public record PvFinalDensity(DensityFunction terrain, DensityFunction noodleToggle, DensityFunction noodleThickness,
                             DensityFunction noodleRidgeA, DensityFunction noodleRidgeB, DensityFunction entrances,
                             DensityFunction highRiver, double noodleFloor, double minValue, double maxValue) implements DensityFunction {

    public static final MapCodec<PvFinalDensity> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.FUNCTION_CODEC.fieldOf("terrain").forGetter(PvFinalDensity::terrain),
            DensityFunction.FUNCTION_CODEC.fieldOf("noodle_toggle").forGetter(PvFinalDensity::noodleToggle),
            DensityFunction.FUNCTION_CODEC.fieldOf("noodle_thickness").forGetter(PvFinalDensity::noodleThickness),
            DensityFunction.FUNCTION_CODEC.fieldOf("noodle_ridge_a").forGetter(PvFinalDensity::noodleRidgeA),
            DensityFunction.FUNCTION_CODEC.fieldOf("noodle_ridge_b").forGetter(PvFinalDensity::noodleRidgeB),
            DensityFunction.FUNCTION_CODEC.fieldOf("corridor_entrances").forGetter(PvFinalDensity::entrances),
            DensityFunction.FUNCTION_CODEC.fieldOf("high_river").forGetter(PvFinalDensity::highRiver)
    ).apply(instance, PvFinalDensity::new));
    private static final CodecHolder<PvFinalDensity> CODEC_HOLDER = CodecHolder.of(CODEC);

    /**
     * Interpolation can land a hair outside its corners' range; this margin keeps {@link #noodleFloor} below any
     * value the noodle can take, so skipping it never changes the result.
     */
    private static final double ROUNDING_MARGIN = 1.0e-9;

    public PvFinalDensity(DensityFunction terrain, DensityFunction noodleToggle, DensityFunction noodleThickness,
                          DensityFunction noodleRidgeA, DensityFunction noodleRidgeB, DensityFunction entrances,
                          DensityFunction highRiver) {
        this(terrain, noodleToggle, noodleThickness, noodleRidgeA, noodleRidgeB, entrances, highRiver,
                PvNoodle.tunnelMin(noodleThickness) - ROUNDING_MARGIN,
                Math.min(Math.min(-DensityOps.SQUEEZE_MAX, PvNoodle.BIAS_MIN + PvNoodle.tunnelMin(noodleThickness)), highRiver.minValue()),
                Math.min(DensityOps.SQUEEZE_MAX, PvNoodle.BIAS_MAX + PvNoodle.tunnelMax(noodleThickness, noodleRidgeA, noodleRidgeB)));
    }

    @Override
    public double sample(NoisePos pos) {
        double terrain = DensityOps.squeeze(this.terrain.sample(pos) * 0.64);
        int y = pos.blockY();
        double bias = PvNoodle.inCorridorLayers(y) ? PvNoodle.corridorBias(y, this.entrances.sample(pos)) : PvNoodle.bias(y);
        double carved = terrain < bias + this.noodleFloor ? terrain
                : Math.min(terrain, bias + PvNoodle.tunnel(pos, this.noodleToggle, this.noodleThickness, this.noodleRidgeA, this.noodleRidgeB));
        return y >= PvHighRiver.MIN_Y && y < PvHighRiver.MAX_Y ? Math.min(carved, this.highRiver.sample(pos)) : carved;
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    /**
     * This function from vanilla types, for C2ME's compiler, which runs a type it doesn't know through vanilla's
     * interface with a new position object for every block ({@link DensityCompilerCompat}). All of it but two parts that
     * stay Java on purpose: the height bias in the corridors' layers ({@link PvNoodle#corridorBiasFunction}) and the high
     * river's valley, which a {@code range_choice} on y only asks for in its heights. The same doubles as {@link #sample}
     * ({@code TerrainPortTest}), and the same skip: a {@code range_choice} on the terrain minus the noodle's height bias.
     * Built from this function's inputs as they are, so call it on a function whose references are bound, as
     * {@link #apply} does; before that, the noodle's bounds aren't known and nothing would be skipped.
     */
    public DensityFunction asVanillaTypes() {
        DensityFunction terrain = DensityFunctionTypes.mul(DensityFunctionTypes.constant(0.64), this.terrain).squeeze();
        DensityFunction bias = PvNoodle.corridorBiasFunction(this.entrances);
        DensityFunction tunnel = DensityFunctionTypes.rangeChoice(this.noodleToggle, -1000000.0, -0.2, DensityFunctionTypes.constant(64.0),
                DensityFunctionTypes.add(this.noodleThickness, DensityFunctionTypes.mul(DensityFunctionTypes.constant(1.5),
                        DensityFunctionTypes.max(this.noodleRidgeA.abs(), this.noodleRidgeB.abs()))));
        DensityFunction aboveBias = DensityFunctionTypes.add(terrain, DensityFunctionTypes.mul(DensityFunctionTypes.constant(-1.0), bias));
        DensityFunction carved = DensityFunctionTypes.rangeChoice(aboveBias, -1000000.0, this.noodleFloor, terrain,
                DensityFunctionTypes.min(terrain, DensityFunctionTypes.add(bias, tunnel)));
        DensityFunction y = DensityFunctionTypes.yClampedGradient(-4064, 4062, -4064.0, 4062.0);
        DensityFunction river = DensityFunctionTypes.rangeChoice(y, PvHighRiver.MIN_Y - 0.5, PvHighRiver.MAX_Y - 0.5, this.highRiver,
                DensityFunctionTypes.constant(Double.POSITIVE_INFINITY));
        return DensityFunctionTypes.min(carved, river);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        PvFinalDensity applied = new PvFinalDensity(this.terrain.apply(visitor), this.noodleToggle.apply(visitor),
                this.noodleThickness.apply(visitor), this.noodleRidgeA.apply(visitor), this.noodleRidgeB.apply(visitor),
                this.entrances.apply(visitor), this.highRiver.apply(visitor));
        return visitor.apply(DensityCompilerCompat.ACTIVE ? applied.asVanillaTypes() : applied);
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
