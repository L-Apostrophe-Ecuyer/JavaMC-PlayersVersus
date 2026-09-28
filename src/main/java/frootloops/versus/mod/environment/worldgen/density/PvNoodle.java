package frootloops.versus.mod.environment.worldgen.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;

import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.BASIN_MIN_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.CORRIDOR_BIAS;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.CORRIDOR_ENTRANCES;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.CORRIDOR_FLARE_BIAS;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.CORRIDOR_FLARE_FROM;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.CORRIDOR_FLARE_SCALE;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.CORRIDOR_MAX_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.CORRIDOR_ZONE_SCALE;
import static frootloops.versus.mod.environment.worldgen.density.DensityOps.gradient;

/**
 * Density-function type {@code players-versus:noodle}: noodle caves, negative where they're open (the old
 * {@code caves/noodle}). Its four inputs are vanilla's {@code interpolated} nodes, registered on their own
 * ({@code caves/noodle_*}) so that the final density ({@link PvFinalDensity}, which computes the noodle inline) and
 * the aquifer's basins ({@code caves/noodle}, read by {@code AquiferSpread}) share them.
 *
 * @param toggle    where noodles exist ({@code caves/noodle_toggle})
 * @param thickness {@code caves/noodle_thickness}
 * @param ridgeA    {@code caves/noodle_ridge_a}
 * @param ridgeB    {@code caves/noodle_ridge_b}
 * @param minValue  derived bounds
 * @param maxValue  derived bounds
 */
public record PvNoodle(DensityFunction toggle, DensityFunction thickness, DensityFunction ridgeA, DensityFunction ridgeB,
                       double minValue, double maxValue) implements DensityFunction {

    public static final MapCodec<PvNoodle> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.FUNCTION_CODEC.fieldOf("toggle").forGetter(PvNoodle::toggle),
            DensityFunction.FUNCTION_CODEC.fieldOf("thickness").forGetter(PvNoodle::thickness),
            DensityFunction.FUNCTION_CODEC.fieldOf("ridge_a").forGetter(PvNoodle::ridgeA),
            DensityFunction.FUNCTION_CODEC.fieldOf("ridge_b").forGetter(PvNoodle::ridgeB)
    ).apply(instance, PvNoodle::new));
    private static final CodecHolder<PvNoodle> CODEC_HOLDER = CodecHolder.of(CODEC);

    /** The lowest and highest {@link #bias}: the sums of its five gradients' extremes. */
    static final double BIAS_MIN = -0.05 + -0.1 + 0.0 + -0.3 + 0.0;
    static final double BIAS_MAX = 0.08 + 0.0 + 0.1 + 0.0 + 0.35;

    public PvNoodle(DensityFunction toggle, DensityFunction thickness, DensityFunction ridgeA, DensityFunction ridgeB) {
        this(toggle, thickness, ridgeA, ridgeB, BIAS_MIN + tunnelMin(thickness), BIAS_MAX + tunnelMax(thickness, ridgeA, ridgeB));
    }

    /** The noodle's height bias: fewer noodles near the surface and in the deep layers. */
    public static double bias(int y) {
        return gradient(y, 96, 56, -0.05, 0.08) + (gradient(y, 56, 40, 0.0, -0.1) + (gradient(y, 32, 20, 0.0, 0.1)
                + (gradient(y, -8, -32, 0.0, -0.3) + gradient(y, -52, -64, 0.0, 0.35))));
    }

    /** {@link #bias} from vanilla types: the same gradients, added in the same order. */
    static DensityFunction biasFunction() {
        return DensityFunctionTypes.add(DensityFunctionTypes.yClampedGradient(96, 56, -0.05, 0.08),
                DensityFunctionTypes.add(DensityFunctionTypes.yClampedGradient(56, 40, 0.0, -0.1),
                        DensityFunctionTypes.add(DensityFunctionTypes.yClampedGradient(32, 20, 0.0, 0.1),
                                DensityFunctionTypes.add(DensityFunctionTypes.yClampedGradient(-8, -32, 0.0, -0.3),
                                        DensityFunctionTypes.yClampedGradient(-52, -64, 0.0, 0.35)))));
    }

    /** Whether {@code y} is in the flooded corridors' layers, the basin water's: y -3..23. */
    public static boolean inCorridorLayers(int y) {
        return y > BASIN_MIN_Y && y < CORRIDOR_MAX_Y;
    }

    /**
     * The noodle's height bias with the flooded corridors (the refactor plan, Section 10, question 7): in their layers,
     * where the entrance value says a flooded cave is near, it moves from {@link #bias} towards
     * {@code PvWorldgenConstants.CORRIDOR_BIAS}, and on to {@code CORRIDOR_FLARE_BIAS} nearer the caves. The final
     * density and the aquifer both use it, with the same interpolated entrance value, so the aquifer floods exactly the
     * corridors the terrain opens. {@link #corridorBiasFunction} gives the same doubles from vanilla types.
     */
    public static double corridorBias(int y, double entrances) {
        double now = bias(y);
        if (!inCorridorLayers(y)) return now;
        double share = MathHelper.clamp((CORRIDOR_ENTRANCES - entrances) * CORRIDOR_ZONE_SCALE, 0.0, 1.0);
        double flare = MathHelper.clamp((CORRIDOR_FLARE_FROM - entrances) * CORRIDOR_FLARE_SCALE, 0.0, 1.0);
        double target = CORRIDOR_BIAS + (CORRIDOR_FLARE_BIAS - CORRIDOR_BIAS) * flare;
        return now + (target - now) * share;
    }

    /**
     * {@link #corridorBias} from vanilla types, for C2ME's compiler: the same operations in the same order (a
     * subtraction as the addition of a negated value, which IEEE defines as the same), and the layers as a
     * {@code range_choice} on vanilla's {@code y}, whose value at a block is within a hair of its height.
     */
    static DensityFunction corridorBiasFunction(DensityFunction entrances) {
        DensityFunction now = biasFunction();
        DensityFunction negated = DensityFunctionTypes.mul(DensityFunctionTypes.constant(-1.0), entrances);
        DensityFunction share = DensityFunctionTypes.mul(DensityFunctionTypes.add(DensityFunctionTypes.constant(CORRIDOR_ENTRANCES), negated),
                DensityFunctionTypes.constant(CORRIDOR_ZONE_SCALE)).clamp(0.0, 1.0);
        DensityFunction flare = DensityFunctionTypes.mul(DensityFunctionTypes.add(DensityFunctionTypes.constant(CORRIDOR_FLARE_FROM), negated),
                DensityFunctionTypes.constant(CORRIDOR_FLARE_SCALE)).clamp(0.0, 1.0);
        DensityFunction target = DensityFunctionTypes.add(DensityFunctionTypes.constant(CORRIDOR_BIAS),
                DensityFunctionTypes.mul(DensityFunctionTypes.constant(CORRIDOR_FLARE_BIAS - CORRIDOR_BIAS), flare));
        DensityFunction corridor = DensityFunctionTypes.add(now, DensityFunctionTypes.mul(
                DensityFunctionTypes.add(target, DensityFunctionTypes.mul(DensityFunctionTypes.constant(-1.0), now)), share));
        DensityFunction y = DensityFunctionTypes.yClampedGradient(-4064, 4062, -4064.0, 4062.0);
        return DensityFunctionTypes.rangeChoice(y, BASIN_MIN_Y + 0.5, CORRIDOR_MAX_Y - 0.5, corridor, now);
    }

    /** The noodle without its bias: 64 (solid) where the toggle is off, else the thickness plus the larger ridge. */
    public static double tunnel(NoisePos pos, DensityFunction toggle, DensityFunction thickness, DensityFunction ridgeA,
                                DensityFunction ridgeB) {
        double on = toggle.sample(pos);
        if (on >= -1000000.0 && on < -0.2) return 64.0;
        return thickness.sample(pos) + Math.max(Math.abs(ridgeA.sample(pos)), Math.abs(ridgeB.sample(pos))) * 1.5;
    }

    /** {@link #tunnel} is at least the thickness: the ridge term is at least 0. */
    static double tunnelMin(DensityFunction thickness) {
        return Math.min(64.0, thickness.minValue());
    }

    static double tunnelMax(DensityFunction thickness, DensityFunction ridgeA, DensityFunction ridgeB) {
        double ridge = Math.max(Math.max(Math.abs(ridgeA.minValue()), Math.abs(ridgeA.maxValue())),
                Math.max(Math.abs(ridgeB.minValue()), Math.abs(ridgeB.maxValue())));
        return Math.max(64.0, thickness.maxValue() + ridge * 1.5);
    }

    @Override
    public double sample(NoisePos pos) {
        return bias(pos.blockY()) + tunnel(pos, this.toggle, this.thickness, this.ridgeA, this.ridgeB);
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new PvNoodle(this.toggle.apply(visitor), this.thickness.apply(visitor), this.ridgeA.apply(visitor),
                this.ridgeB.apply(visitor)));
    }

    @Override
    public CodecHolder<? extends DensityFunction> getCodecHolder() {
        return CODEC_HOLDER;
    }
}
