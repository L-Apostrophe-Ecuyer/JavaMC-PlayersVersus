package frootloops.versus.mod.environment.worldgen.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import frootloops.versus.mod.environment.worldgen.aquifer.AquiferFormulas;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.world.gen.densityfunction.DensityFunction;

/**
 * Density-function type {@code players-versus:aquifer_floodedness}: the sea/river floodedness F of the Players Versus
 * aquifer, computed by {@link AquiferFormulas} from the input functions the JSON names.
 *
 * <p>Putting this type in a noise router's {@code fluid_level_floodedness} slot is what turns on the Players Versus
 * aquifer and ore veins for that generator ({@code PvWorldgen.isPvGenerator}); no magic numbers involved.
 *
 * <p>{@link #sample} is exact at any position. The aquifer itself reads the smooth inputs (depth, continentalness,
 * entrances) from a per-chunk lattice and the rest per block ({@code PvAquifer}).
 *
 * @param depth           the router's {@code depth} ({@link PvDepth})
 * @param continentalness {@code shifted_noise(minecraft:continentalness, xz 0.25, y 0.1)}
 * @param ridge           vanilla's {@code minecraft:overworld/ridges} (the shifted ridge noise, xz 0.25, y 0; the terrain
 *                        reads the same function)
 * @param entrances       {@code players-versus:overworld/caves/entrances} ({@link PvEntrances})
 * @param surface         {@code noise(minecraft:surface, xz 2, y 1)}
 * @param ramen           {@code noise(minecraft:noodle, xz 3, y 3)}
 */
public record AquiferFloodedness(DensityFunction depth, DensityFunction continentalness, DensityFunction ridge,
                                 DensityFunction entrances, DensityFunction surface, DensityFunction ramen) implements DensityFunction {

    public static final MapCodec<AquiferFloodedness> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.FUNCTION_CODEC.fieldOf("depth").forGetter(AquiferFloodedness::depth),
            DensityFunction.FUNCTION_CODEC.fieldOf("continentalness").forGetter(AquiferFloodedness::continentalness),
            DensityFunction.FUNCTION_CODEC.fieldOf("ridge").forGetter(AquiferFloodedness::ridge),
            DensityFunction.FUNCTION_CODEC.fieldOf("entrances").forGetter(AquiferFloodedness::entrances),
            DensityFunction.FUNCTION_CODEC.fieldOf("surface").forGetter(AquiferFloodedness::surface),
            DensityFunction.FUNCTION_CODEC.fieldOf("ramen").forGetter(AquiferFloodedness::ramen)
    ).apply(instance, AquiferFloodedness::new));
    private static final CodecHolder<AquiferFloodedness> CODEC_HOLDER = CodecHolder.of(CODEC);

    @Override
    public double sample(NoisePos pos) {
        int y = pos.blockY();
        double entrances = this.entrances.sample(pos);
        double seaFloodedness = AquiferFormulas.seaFloodedness(y, this.depth.sample(pos), this.continentalness.sample(pos),
                entrances, entrances, this.ridge.sample(pos), this.surface.sample(pos));
        return AquiferFormulas.floodedness(y, seaFloodedness, this.ramen, pos);
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new AquiferFloodedness(this.depth.apply(visitor), this.continentalness.apply(visitor),
                this.ridge.apply(visitor), this.entrances.apply(visitor), this.surface.apply(visitor), this.ramen.apply(visitor)));
    }

    /** F is at least 0: its ocean and river terms are, and it's their maximum with the coast term, plus the ramen term (also at least 0). */
    @Override
    public double minValue() {
        return 0.0;
    }

    @Override
    public double maxValue() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
