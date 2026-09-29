package frootloops.versus.mod.environment.worldgen.density;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.CORRIDOR_BIAS;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.CORRIDOR_FLARE_BIAS;

/**
 * Density-function type {@code players-versus:corridor_bias}: the noodle's height bias with the flooded corridors
 * ({@link PvNoodle#corridorBias}), from their entrance value. Only {@link PvNoodle#corridorBiasFunction} builds it, for
 * C2ME's density-function compiler: the compiler runs a type it doesn't know through vanilla's interface, so the entrance
 * value's {@code interpolated} and the {@code cache_once} under it stay out of the compiled code. Compiled, they made the
 * terrain pass come out different at y 32..95, where the entrance value isn't even read (the refactor plan, Section 9).
 *
 * @param entrances the corridors' entrance value, as {@link PvFinalDensity#entrances()}
 */
public record PvCorridorBias(DensityFunction entrances) implements DensityFunction {

    public static final MapCodec<PvCorridorBias> CODEC = DensityFunction.FUNCTION_CODEC.fieldOf("entrances")
            .xmap(PvCorridorBias::new, PvCorridorBias::entrances);
    private static final CodecHolder<PvCorridorBias> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        int y = pos.blockY();
        return PvNoodle.inCorridorLayers(y) ? PvNoodle.corridorBias(y, this.entrances.sample(pos)) : PvNoodle.bias(y);
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new PvCorridorBias(this.entrances.apply(visitor)));
    }

    /** The bias moves from {@link PvNoodle#bias} towards the corridors' values, so it stays within all of them. */
    @Override
    public double minValue() {
        return Math.min(PvNoodle.BIAS_MIN, Math.min(CORRIDOR_BIAS, CORRIDOR_FLARE_BIAS));
    }

    @Override
    public double maxValue() {
        return Math.max(PvNoodle.BIAS_MAX, Math.max(CORRIDOR_BIAS, CORRIDOR_FLARE_BIAS));
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
