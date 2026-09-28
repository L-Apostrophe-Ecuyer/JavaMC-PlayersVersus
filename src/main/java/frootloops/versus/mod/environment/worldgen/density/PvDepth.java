package frootloops.versus.mod.environment.worldgen.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

/**
 * Density-function type {@code players-versus:depth}: the router's {@code depth} (cave-biome bands, the aquifer),
 * vanilla's depth lowered along rivers ({@link TerrainFormulas#depth}). {@link PvTerrain} computes the same value
 * inline for its sloped cheese, from the ridge value it reads anyway.
 *
 * @param offset vanilla's {@code minecraft:overworld/offset}
 * @param ridges vanilla's {@code minecraft:overworld/ridges} (R, cached per column in a chunk)
 */
public record PvDepth(DensityFunction offset, DensityFunction ridges) implements DensityFunction {

    public static final MapCodec<PvDepth> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.FUNCTION_CODEC.fieldOf("offset").forGetter(PvDepth::offset),
            DensityFunction.FUNCTION_CODEC.fieldOf("ridges").forGetter(PvDepth::ridges)
    ).apply(instance, PvDepth::new));
    private static final CodecHolder<PvDepth> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        int y = pos.blockY();
        double yValue = DensityOps.yValue(y);
        double ridge = TerrainFormulas.inRiverBand(yValue) ? this.ridges.sample(pos) : 0.0;
        return TerrainFormulas.depth(y, this.offset.sample(pos), TerrainFormulas.riverDepth(y, yValue, ridge));
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new PvDepth(this.offset.apply(visitor), this.ridges.apply(visitor)));
    }

    /** The height gradient spans -1.5..1.5 and the river term -1..0. */
    @Override
    public double minValue() {
        return -1.5 + this.offset.minValue() + TerrainFormulas.RIVER_DEPTH_MIN;
    }

    @Override
    public double maxValue() {
        return 1.5 + this.offset.maxValue();
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
