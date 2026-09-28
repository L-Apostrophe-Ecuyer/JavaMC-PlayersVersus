package frootloops.versus.mod.environment.worldgen.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import static frootloops.versus.mod.environment.worldgen.density.DensityOps.gradient;

/**
 * Density-function type {@code players-versus:entrances}: cave entrances and spaghetti tunnels, plus the ramen caves
 * in y 0..43 (the old {@code caves/entrances} and {@code ramen_cave_carver}), negative where they're open. The terrain
 * reads it at every cell corner and the aquifer on its lattice, so the JSON wraps it in {@code cache_once}.
 *
 * <p>The height terms put the most caves at y 28..38 (the low of {@code (48 -> 38, 0 -> -0.155)}, which
 * {@code (28 -> 18, 0 -> 0.265)} lifts back under y 28). Until revision 6 of the refactor plan, the low was -0.165 and
 * reached from y 36, and the lift 0.275: the same caves under y 18, a few more at y 36..47, fewer at y 16..35.
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
            DensityFunction.FUNCTION_CODEC.fieldOf("continents").forGetter(PvEntrances::continents),
            DensityFunction.FUNCTION_CODEC.fieldOf("spaghetti_roughness").forGetter(PvEntrances::spaghettiRoughness),
            DensityFunction.FUNCTION_CODEC.fieldOf("ramen_ridge").forGetter(PvEntrances::ramenRidge),
            DensityFunction.FUNCTION_CODEC.fieldOf("ramen_noodle").forGetter(PvEntrances::ramenNoodle),
            DensityFunction.FUNCTION_CODEC.fieldOf("cave_entrance").forGetter(PvEntrances::caveEntrance),
            DensityFunction.FUNCTION_CODEC.fieldOf("spaghetti_1").forGetter(PvEntrances::spaghetti1),
            DensityFunction.FUNCTION_CODEC.fieldOf("spaghetti_2").forGetter(PvEntrances::spaghetti2),
            DensityFunction.FUNCTION_CODEC.fieldOf("spaghetti_thickness").forGetter(PvEntrances::spaghettiThickness)
    ).apply(instance, PvEntrances::new));
    private static final CodecHolder<PvEntrances> CODEC_HOLDER = CodecHolder.of(CODEC);

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
    public double sample(NoisePos pos) {
        int y = pos.blockY();
        double yValue = DensityOps.yValue(y);
        double ramen = yValue >= 0.0 && yValue < 44.0 ? this.ramen(pos, y) : 0.0;
        double base = Math.min(this.continents.sample(pos), 0.1) * -0.1
                + ((gradient(y, 96, 72, 0.15, 0.0) + gradient(y, 66, 56, -0.1, 0.025))
                + (gradient(y, 48, 38, 0.0, -0.155) + (gradient(y, 28, 18, 0.0, 0.265)
                + (gradient(y, -16, -40, 0.0, -0.2) + gradient(y, -40, -60, 0.0, 0.215)))));
        double entrance = (this.caveEntrance.sample(pos) + 0.37) + gradient(y, -10, 30, 0.3, 0.0);
        double caves = entrance < this.tunnelsMin ? entrance : Math.min(entrance, this.tunnels(pos));
        return ramen + (base + caves);
    }

    /** Ramen caves: a band of noodle-noise tunnels that fades in from y -4 and out towards y 44. */
    private double ramen(NoisePos pos, int y) {
        double shape = DensityOps.mul(DensityOps.mul(gradient(y, -4, 8, 0.0, 1.0), gradient(y, 8, 16, 1.4, 1.0)), gradient(y, 16, 44, 1.2, 0.0));
        double carved = shape == 0.0 ? 0.0
                : shape * ((this.ramenRidge.sample(pos) * 0.08 + -0.2) + Math.abs(this.ramenNoodle.sample(pos)));
        return Math.min(0.0, (carved + 0.1) * 2.0);
    }

    /** Spaghetti tunnels. */
    private double tunnels(NoisePos pos) {
        double spaghetti = Math.max(this.spaghetti1.sample(pos), this.spaghetti2.sample(pos))
                + (this.spaghettiThickness.sample(pos) * -0.011499999999999996 + -0.0765);
        return this.spaghettiRoughness.sample(pos) + MathHelper.clamp(spaghetti, -1.0, 1.0);
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new PvEntrances(this.continents.apply(visitor), this.spaghettiRoughness.apply(visitor),
                this.ramenRidge.apply(visitor), this.ramenNoodle.apply(visitor), this.caveEntrance.apply(visitor),
                this.spaghetti1.apply(visitor), this.spaghetti2.apply(visitor), this.spaghettiThickness.apply(visitor)));
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
