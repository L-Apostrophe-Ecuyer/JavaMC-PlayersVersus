package frootloops.versus.mod.environment.worldgen.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

import static frootloops.versus.mod.environment.worldgen.density.DensityOps.gradient;

/**
 * Density-function type {@code players-versus:entrances}: cave entrances and spaghetti tunnels, plus the ramen caves
 * in y 0..43 (the old {@code caves/entrances} and {@code ramen_cave_carver}), negative where they're open. The terrain
 * reads it at every cell corner and the aquifer on its lattice, so the JSON wraps it in {@code cache_once}.
 *
 * <p>The height terms put the most caves at y 28..38 (the low of {@code (48 -> 38, 0 -> -0.155)}, which
 * {@code (28 -> 18, 0 -> 0.265)} lifts back under y 28). Until revision 6 of the refactor plan the low was -0.165,
 * reached at y 36, and the lift 0.275; now there are as many caves under y 18, a few more at y 36..47 and fewer at
 * y 16..35.
 *
 * <p>The tunnels (four noises and the roughness) are skipped where the entrance term is already below anything they
 * could reach, as vanilla's {@code min} does.
 *
 * @param continents         vanilla's {@code minecraft:overworld/continents}
 * @param spaghettiRoughness vanilla's {@code minecraft:overworld/caves/spaghetti_roughness_function} (shared with
 *                           {@link PvTerrain}; {@code cache_once} in vanilla's data)
 * @param ramenRidge         {@code noise(minecraft:noodle_ridge_a, xz 4, y 2)}
 * @param ramenNoodle        {@code noise(minecraft:noodle, xz 3, y 3)}
 * @param caveEntrance       {@code noise(minecraft:cave_entrance, xz 0.8, y 0.75)}
 * @param spaghetti1         vanilla's {@code weird_scaled_sampler} over {@code spaghetti_3d_1}
 * @param spaghetti2         the same over {@code spaghetti_3d_2}
 * @param spaghettiThickness {@code noise(minecraft:spaghetti_3d_thickness)}
 * @param tunnelsMin         the lowest the tunnel term can go (derived from the inputs' bounds)
 * @param minValue           derived bounds
 * @param maxValue           derived bounds
 */
public record PvEntrances(DensityFunction continents, DensityFunction spaghettiRoughness, DensityFunction ramenRidge,
                          DensityFunction ramenNoodle, DensityFunction caveEntrance, DensityFunction spaghetti1,
                          DensityFunction spaghetti2, DensityFunction spaghettiThickness,
                          double tunnelsMin, double minValue, double maxValue) implements DensityFunction {

    public static final MapCodec<PvEntrances> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("continents").forGetter(PvEntrances::continents),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("spaghetti_roughness").forGetter(PvEntrances::spaghettiRoughness),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("ramen_ridge").forGetter(PvEntrances::ramenRidge),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("ramen_noodle").forGetter(PvEntrances::ramenNoodle),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("cave_entrance").forGetter(PvEntrances::caveEntrance),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("spaghetti_1").forGetter(PvEntrances::spaghetti1),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("spaghetti_2").forGetter(PvEntrances::spaghetti2),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("spaghetti_thickness").forGetter(PvEntrances::spaghettiThickness)
    ).apply(instance, PvEntrances::new));
    private static final KeyDispatchDataCodec<PvEntrances> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

    /** The ramen shape's largest value: its three gradients peak at 1, 1.4 and 1.2. */
    private static final double RAMEN_SHAPE_MAX = 1.0 * 1.4 * 1.2;
    /** The sum of the height terms' lowest and highest values. */
    private static final double HEIGHT_TERMS_MIN = 0.0 + -0.1 + -0.155 + 0.0 + -0.2 + 0.0;
    private static final double HEIGHT_TERMS_MAX = 0.15 + 0.025 + 0.0 + 0.265 + 0.0 + 0.215;

    public PvEntrances(DensityFunction continents, DensityFunction spaghettiRoughness, DensityFunction ramenRidge,
                       DensityFunction ramenNoodle, DensityFunction caveEntrance, DensityFunction spaghetti1,
                       DensityFunction spaghetti2, DensityFunction spaghettiThickness) {
        this(continents, spaghettiRoughness, ramenRidge, ramenNoodle, caveEntrance, spaghetti1, spaghetti2, spaghettiThickness,
                spaghettiRoughness.minValue() + -1.0,
                ramenMin(ramenRidge) + (-0.1 * 0.1 + HEIGHT_TERMS_MIN
                        + Math.min(caveEntrance.minValue() + 0.37, spaghettiRoughness.minValue() + -1.0)),
                0.0 + (-0.1 * Math.min(continents.minValue(), 0.1) + HEIGHT_TERMS_MAX
                        + Math.min(caveEntrance.maxValue() + 0.37 + 0.3, spaghettiRoughness.maxValue() + 1.0)));
    }

    private static double ramenMin(DensityFunction ramenRidge) {
        double termMin = Math.min(ramenRidge.minValue() * 0.08, ramenRidge.maxValue() * 0.08) + -0.2;
        return Math.min(0.0, (Math.min(0.0, RAMEN_SHAPE_MAX * termMin) + 0.1) * 2.0);
    }

    @Override
    public double compute(FunctionContext pos) {
        int y = pos.blockY();
        double yValue = DensityOps.yValue(y);
        double ramen = yValue >= 0.0 && yValue < 44.0 ? this.ramen(pos, y) : 0.0;
        double base = Math.min(this.continents.compute(pos), 0.1) * -0.1
                + ((gradient(y, 96, 72, 0.15, 0.0) + gradient(y, 66, 56, -0.1, 0.025))
                + (gradient(y, 48, 38, 0.0, -0.155) + (gradient(y, 28, 18, 0.0, 0.265)
                + (gradient(y, -16, -40, 0.0, -0.2) + gradient(y, -40, -60, 0.0, 0.215)))));
        double entrance = (this.caveEntrance.compute(pos) + 0.37) + gradient(y, -10, 30, 0.3, 0.0);
        double caves = entrance < this.tunnelsMin ? entrance : Math.min(entrance, this.tunnels(pos));
        return ramen + (base + caves);
    }

    /** Ramen caves: a band of noodle-noise tunnels that fades in from y -4 and out towards y 44. */
    private double ramen(FunctionContext pos, int y) {
        double shape = DensityOps.mul(DensityOps.mul(gradient(y, -4, 8, 0.0, 1.0), gradient(y, 8, 16, 1.4, 1.0)), gradient(y, 16, 44, 1.2, 0.0));
        double carved = shape == 0.0 ? 0.0
                : shape * ((this.ramenRidge.compute(pos) * 0.08 + -0.2) + Math.abs(this.ramenNoodle.compute(pos)));
        return Math.min(0.0, (carved + 0.1) * 2.0);
    }

    /** Spaghetti tunnels. */
    private double tunnels(FunctionContext pos) {
        double spaghetti = Math.max(this.spaghetti1.compute(pos), this.spaghetti2.compute(pos))
                + (this.spaghettiThickness.compute(pos) * -0.011499999999999996 + -0.0765);
        return this.spaghettiRoughness.compute(pos) + Mth.clamp(spaghetti, -1.0, 1.0);
    }

    @Override
    public void fillArray(double[] densities, ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new PvEntrances(this.continents.mapAll(visitor), this.spaghettiRoughness.mapAll(visitor),
                this.ramenRidge.mapAll(visitor), this.ramenNoodle.mapAll(visitor), this.caveEntrance.mapAll(visitor),
                this.spaghetti1.mapAll(visitor), this.spaghetti2.mapAll(visitor), this.spaghettiThickness.mapAll(visitor)));
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
