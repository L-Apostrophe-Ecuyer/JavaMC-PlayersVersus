package frootloops.versus.mod.environment.worldgen.density;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

/**
 * Density-function type {@code players-versus:aquifer_floodedness}: the sea/river floodedness input of the Players
 * Versus aquifer.
 *
 * <p>Putting this type in a noise router's {@code fluid_level_floodedness} slot is what turns on the Players Versus
 * aquifer and ore veins for that generator ({@code PvWorldgen.isPvGenerator}); no magic numbers involved.
 *
 * <p>For now it only wraps the existing JSON function ({@code argument}) and returns its values unchanged. Plan phase 2
 * replaces the JSON graph with Java code under this same type id.
 */
public record AquiferFloodedness(DensityFunction argument) implements DensityFunction {

    public static final MapCodec<AquiferFloodedness> CODEC = DensityFunction.FUNCTION_CODEC
            .fieldOf("argument")
            .xmap(AquiferFloodedness::new, AquiferFloodedness::argument);
    private static final CodecHolder<AquiferFloodedness> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        return this.argument.sample(pos);
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        this.argument.fill(densities, applier);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new AquiferFloodedness(this.argument.apply(visitor)));
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
