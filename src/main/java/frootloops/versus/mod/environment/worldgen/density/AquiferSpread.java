package frootloops.versus.mod.environment.worldgen.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import frootloops.versus.mod.environment.worldgen.aquifer.AquiferFormulas;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Density-function type {@code players-versus:aquifer_spread}: the cave-basin floodedness S of the Players Versus
 * aquifer ({@code fluid_level_spread} slot), computed by {@link AquiferFormulas} from the input functions the JSON
 * names. {@link #compute} is exact at any position; the aquifer reads S's smooth part from a per-chunk lattice.
 *
 * @param entrances {@code players-versus:overworld/caves/entrances} ({@link PvEntrances})
 * @param noodle    {@code players-versus:overworld/caves/noodle} ({@link PvNoodle})
 * @param surface   {@code noise(minecraft:surface, xz 4, y 2)}
 */
public record AquiferSpread(DensityFunction entrances, DensityFunction noodle, DensityFunction surface) implements DensityFunction {

    public static final MapCodec<AquiferSpread> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("entrances").forGetter(AquiferSpread::entrances),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("noodle").forGetter(AquiferSpread::noodle),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("surface").forGetter(AquiferSpread::surface)
    ).apply(instance, AquiferSpread::new));
    private static final KeyDispatchDataCodec<AquiferSpread> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    @Override
    public double compute(FunctionContext pos) {
        int y = pos.blockY();
        return AquiferFormulas.spread(y, AquiferFormulas.basinInner(y, this.entrances.compute(pos), this.noodle.compute(pos), this.surface.compute(pos)));
    }

    @Override
    public void fillArray(double[] densities, ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new AquiferSpread(this.entrances.mapAll(visitor), this.noodle.mapAll(visitor), this.surface.mapAll(visitor)));
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
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
