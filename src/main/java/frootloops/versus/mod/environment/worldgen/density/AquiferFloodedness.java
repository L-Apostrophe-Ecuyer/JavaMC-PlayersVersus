package frootloops.versus.mod.environment.worldgen.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import frootloops.versus.mod.environment.worldgen.aquifer.AquiferFormulas;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Density-function type {@code players-versus:aquifer_floodedness}: the sea/river floodedness F of the Players Versus
 * aquifer, computed by {@link AquiferFormulas} from the input functions the JSON names.
 *
 * <p>Putting this type in a noise router's {@code fluid_level_floodedness} slot is what turns on the Players Versus
 * aquifer and ore veins for that generator ({@code PvWorldgen.isPvGenerator}); no magic numbers involved.
 *
 * <p>{@link #compute} is exact at any position. The aquifer itself reads the smooth inputs (depth, continentalness,
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
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("depth").forGetter(AquiferFloodedness::depth),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("continentalness").forGetter(AquiferFloodedness::continentalness),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("ridge").forGetter(AquiferFloodedness::ridge),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("entrances").forGetter(AquiferFloodedness::entrances),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("surface").forGetter(AquiferFloodedness::surface),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("ramen").forGetter(AquiferFloodedness::ramen)
    ).apply(instance, AquiferFloodedness::new));
    private static final KeyDispatchDataCodec<AquiferFloodedness> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    @Override
    public double compute(FunctionContext pos) {
        int y = pos.blockY();
        double entrances = this.entrances.compute(pos);
        double seaFloodedness = AquiferFormulas.seaFloodedness(y, this.depth.compute(pos), this.continentalness.compute(pos),
                entrances, entrances, this.ridge.compute(pos), this.surface.compute(pos));
        return AquiferFormulas.floodedness(y, seaFloodedness, this.ramen, pos);
    }

    @Override
    public void fillArray(double[] densities, ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new AquiferFloodedness(this.depth.mapAll(visitor), this.continentalness.mapAll(visitor),
                this.ridge.mapAll(visitor), this.entrances.mapAll(visitor), this.surface.mapAll(visitor), this.ramen.mapAll(visitor)));
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
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
