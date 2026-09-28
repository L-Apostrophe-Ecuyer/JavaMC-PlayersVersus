package frootloops.versus.mod.environment.worldgen.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;

/**
 * Density-function type {@code players-versus:final_density}: the router's {@code final_density}, sampled for every
 * block. The interpolated terrain ({@link PvTerrain} inside vanilla's {@code interpolated} and {@code blend_density}),
 * scaled and squeezed, then cut by noodle caves ({@link PvNoodle}), giving the old JSON's doubles
 * ({@code TerrainPortTest}) except in the flooded corridors ({@link PvNoodle#corridorBias}): in the basin layers, near the
 * flooded caves, the noodle's height bias is lower, and the aquifer floods what it opens there.
 *
 * <p>The JSON asked for the noodle at nearly every block: vanilla's {@code min} only skips it below the noodle's
 * lowest possible value, about -0.55, which a squeezed value (never below -0.46) doesn't reach. Here the bound
 * includes the noodle's height bias at the block's y, so the noodle is skipped for most air blocks, where the terrain
 * is already lower than any noodle there.
 *
 * <p>With C2ME's density-function compiler ({@link DensityCompilerCompat}), {@link #apply} returns
 * {@link #asVanillaTypes} instead, which the compiler can compile.
 *
 * @param terrain         {@code interpolated(blend_density(players-versus:terrain))}
 * @param noodleToggle    {@code caves/noodle_toggle}
 * @param noodleThickness {@code caves/noodle_thickness}
 * @param noodleRidgeA    {@code caves/noodle_ridge_a}
 * @param noodleRidgeB    {@code caves/noodle_ridge_b}
 * @param entrances       the entrance value for the corridors, interpolated like the aquifer's lattice of it; only read
 *                        in the corridors' layers, so the JSON only fills it in around them
 * @param noodleFloor     the lowest the noodle can be above its height bias, less a margin for rounding
 * @param minValue        derived bounds
 * @param maxValue        derived bounds
 */
public record PvFinalDensity(DensityFunction terrain, DensityFunction noodleToggle, DensityFunction noodleThickness,
                             DensityFunction noodleRidgeA, DensityFunction noodleRidgeB, DensityFunction entrances, double noodleFloor,
                             double minValue, double maxValue) implements DensityFunction {

    public static final MapCodec<PvFinalDensity> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.FUNCTION_CODEC.fieldOf("terrain").forGetter(PvFinalDensity::terrain),
            DensityFunction.FUNCTION_CODEC.fieldOf("noodle_toggle").forGetter(PvFinalDensity::noodleToggle),
            DensityFunction.FUNCTION_CODEC.fieldOf("noodle_thickness").forGetter(PvFinalDensity::noodleThickness),
            DensityFunction.FUNCTION_CODEC.fieldOf("noodle_ridge_a").forGetter(PvFinalDensity::noodleRidgeA),
            DensityFunction.FUNCTION_CODEC.fieldOf("noodle_ridge_b").forGetter(PvFinalDensity::noodleRidgeB),
            DensityFunction.FUNCTION_CODEC.fieldOf("corridor_entrances").forGetter(PvFinalDensity::entrances)
    ).apply(instance, PvFinalDensity::new));
    private static final CodecHolder<PvFinalDensity> CODEC_HOLDER = CodecHolder.of(CODEC);

    /**
     * Interpolation can land a hair outside its corners' range; this margin keeps {@link #noodleFloor} below any
     * value the noodle can take, so skipping it never changes the result.
     */
    private static final double ROUNDING_MARGIN = 1.0e-9;

    public PvFinalDensity(DensityFunction terrain, DensityFunction noodleToggle, DensityFunction noodleThickness,
                          DensityFunction noodleRidgeA, DensityFunction noodleRidgeB, DensityFunction entrances) {
        this(terrain, noodleToggle, noodleThickness, noodleRidgeA, noodleRidgeB, entrances,
                PvNoodle.tunnelMin(noodleThickness) - ROUNDING_MARGIN,
                Math.min(-DensityOps.SQUEEZE_MAX, PvNoodle.BIAS_MIN + PvNoodle.tunnelMin(noodleThickness)),
                Math.min(DensityOps.SQUEEZE_MAX, PvNoodle.BIAS_MAX + PvNoodle.tunnelMax(noodleThickness, noodleRidgeA, noodleRidgeB)));
    }

    @Override
    public double sample(NoisePos pos) {
        double terrain = DensityOps.squeeze(this.terrain.sample(pos) * 0.64);
        int y = pos.blockY();
        double bias = PvNoodle.inCorridorLayers(y) ? PvNoodle.corridorBias(y, this.entrances.sample(pos)) : PvNoodle.bias(y);
        if (terrain < bias + this.noodleFloor) return terrain;
        return Math.min(terrain, bias + PvNoodle.tunnel(pos, this.noodleToggle, this.noodleThickness, this.noodleRidgeA, this.noodleRidgeB));
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    /**
     * This function from vanilla types, for C2ME's compiler, which runs a type it doesn't know through vanilla's
     * interface with a new position object for every block ({@link DensityCompilerCompat}). The same doubles as
     * {@link #sample} ({@code TerrainPortTest}), and the same skip: a {@code range_choice} on the terrain minus the
     * noodle's height bias. Built from this function's inputs as they are, so call it on a function whose references are
     * bound, as {@link #apply} does; before that, the noodle's bounds aren't known and nothing would be skipped.
     */
    public DensityFunction asVanillaTypes() {
        DensityFunction terrain = DensityFunctionTypes.mul(DensityFunctionTypes.constant(0.64), this.terrain).squeeze();
        DensityFunction bias = PvNoodle.corridorBiasFunction(this.entrances);
        DensityFunction tunnel = DensityFunctionTypes.rangeChoice(this.noodleToggle, -1000000.0, -0.2, DensityFunctionTypes.constant(64.0),
                DensityFunctionTypes.add(this.noodleThickness, DensityFunctionTypes.mul(DensityFunctionTypes.constant(1.5),
                        DensityFunctionTypes.max(this.noodleRidgeA.abs(), this.noodleRidgeB.abs()))));
        DensityFunction aboveBias = DensityFunctionTypes.add(terrain, DensityFunctionTypes.mul(DensityFunctionTypes.constant(-1.0), bias));
        return DensityFunctionTypes.rangeChoice(aboveBias, -1000000.0, this.noodleFloor, terrain,
                DensityFunctionTypes.min(terrain, DensityFunctionTypes.add(bias, tunnel)));
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        PvFinalDensity applied = new PvFinalDensity(this.terrain.apply(visitor), this.noodleToggle.apply(visitor),
                this.noodleThickness.apply(visitor), this.noodleRidgeA.apply(visitor), this.noodleRidgeB.apply(visitor),
                this.entrances.apply(visitor));
        return visitor.apply(DensityCompilerCompat.ACTIVE ? applied.asVanillaTypes() : applied);
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
