package frootloops.versus.mod.environment.worldgen.density;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

/**
 * Density-function type {@code players-versus:at_height}: its argument in the same column at a fixed height, which
 * makes a 2D function of a 3D one (the high river's depth and terrain at its surface, {@link PvHighRiver}). The argument
 * is sampled at a plain position ({@link DensityFunction.UnblendedNoisePos}), which a chunk's caches answer like any
 * other position that isn't the chunk's own: {@code flat_cache} and {@code cache_2d} by column, {@code cache_once} by
 * block, and no blending.
 *
 * @param argument the function
 * @param y        the height it's sampled at
 */
public record PvAtHeight(DensityFunction argument, int y) implements DensityFunction {

    public static final MapCodec<PvAtHeight> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.FUNCTION_CODEC.fieldOf("argument").forGetter(PvAtHeight::argument),
            Codec.INT.fieldOf("y").forGetter(PvAtHeight::y)
    ).apply(instance, PvAtHeight::new));
    private static final CodecHolder<PvAtHeight> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        return this.argument.sample(new UnblendedNoisePos(pos.blockX(), this.y, pos.blockZ()));
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new PvAtHeight(this.argument.apply(visitor), this.y));
    }

    @Override
    public double minValue() {
        return this.argument.minValue();
    }

    @Override
    public double maxValue() {
        return this.argument.maxValue();
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
