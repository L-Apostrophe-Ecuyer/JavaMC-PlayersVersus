package frootloops.versus.mod.environment.worldgen.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import frootloops.versus.mod.environment.worldgen.aquifer.AquiferFormulas;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

/**
 * Density-function type {@code players-versus:aquifer_spread}: the cave-basin floodedness S of the Players Versus
 * aquifer ({@code fluid_level_spread} slot), computed by {@link AquiferFormulas} from the input functions the JSON
 * names. {@link #sample} is exact at any position; the aquifer reads S's smooth part from a per-chunk lattice.
 *
 * @param entrances {@code players-versus:overworld/caves/entrances}
 * @param noodle    {@code players-versus:overworld/caves/noodle}
 * @param surface   {@code noise(minecraft:surface, xz 4, y 2)}
 */
public record AquiferSpread(DensityFunction entrances, DensityFunction noodle, DensityFunction surface) implements DensityFunction {

    public static final MapCodec<AquiferSpread> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.FUNCTION_CODEC.fieldOf("entrances").forGetter(AquiferSpread::entrances),
            DensityFunction.FUNCTION_CODEC.fieldOf("noodle").forGetter(AquiferSpread::noodle),
            DensityFunction.FUNCTION_CODEC.fieldOf("surface").forGetter(AquiferSpread::surface)
    ).apply(instance, AquiferSpread::new));
    private static final CodecHolder<AquiferSpread> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        int y = pos.blockY();
        return AquiferFormulas.spread(y, AquiferFormulas.basinInner(y, this.entrances.sample(pos), this.noodle.sample(pos), this.surface.sample(pos)));
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new AquiferSpread(this.entrances.apply(visitor), this.noodle.apply(visitor), this.surface.apply(visitor)));
    }

    /** S is a factor in -0.2..1 times a value in 0..0.9. */
    @Override
    public double minValue() {
        return -0.2;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
