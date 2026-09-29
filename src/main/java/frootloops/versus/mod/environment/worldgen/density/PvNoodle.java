package frootloops.versus.mod.environment.worldgen.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

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
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("toggle").forGetter(PvNoodle::toggle),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("thickness").forGetter(PvNoodle::thickness),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("ridge_a").forGetter(PvNoodle::ridgeA),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("ridge_b").forGetter(PvNoodle::ridgeB)
    ).apply(instance, PvNoodle::new));
    private static final KeyDispatchDataCodec<PvNoodle> CODEC_HOLDER = KeyDispatchDataCodec.of(CODEC);

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
        return DensityFunctions.add(DensityFunctions.yClampedGradient(96, 56, -0.05, 0.08),
                DensityFunctions.add(DensityFunctions.yClampedGradient(56, 40, 0.0, -0.1),
                        DensityFunctions.add(DensityFunctions.yClampedGradient(32, 20, 0.0, 0.1),
                                DensityFunctions.add(DensityFunctions.yClampedGradient(-8, -32, 0.0, -0.3),
                                        DensityFunctions.yClampedGradient(-52, -64, 0.0, 0.35)))));
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
     * corridors the terrain opens. {@link #corridorBiasFunction} gives the same doubles for C2ME's compiler.
     */
    public static double corridorBias(int y, double entrances) {
        double now = bias(y);
        if (!inCorridorLayers(y)) return now;
        double share = Mth.clamp((CORRIDOR_ENTRANCES - entrances) * CORRIDOR_ZONE_SCALE, 0.0, 1.0);
        double flare = Mth.clamp((CORRIDOR_FLARE_FROM - entrances) * CORRIDOR_FLARE_SCALE, 0.0, 1.0);
        double target = CORRIDOR_BIAS + (CORRIDOR_FLARE_BIAS - CORRIDOR_BIAS) * flare;
        return now + (target - now) * share;
    }

    /**
     * {@link #corridorBias} for C2ME's compiler: vanilla's {@link #biasFunction} outside the corridors' layers, chosen
     * by a {@code range_choice} on vanilla's {@code y} (whose value at a block is within a hair of its height), and
     * {@link PvCorridorBias} in them. That type is Java, which the compiler calls through vanilla's interface, one block at
     * a time: built from vanilla types instead, the corridors put the entrance value's {@code interpolated} into the
     * compiled code, and the terrain pass came out different where they aren't (the refactor plan, Section 9).
     */
    static DensityFunction corridorBiasFunction(DensityFunction entrances) {
        DensityFunction y = DensityFunctions.yClampedGradient(-4064, 4062, -4064.0, 4062.0);
        return DensityFunctions.rangeChoice(y, BASIN_MIN_Y + 0.5, CORRIDOR_MAX_Y - 0.5, new PvCorridorBias(entrances), biasFunction());
    }

    /** The noodle without its bias: 64 (solid) where the toggle is off, else the thickness plus the larger ridge. */
    public static double tunnel(FunctionContext pos, DensityFunction toggle, DensityFunction thickness, DensityFunction ridgeA,
                                DensityFunction ridgeB) {
        double on = toggle.compute(pos);
        if (on >= -1000000.0 && on < -0.2) return 64.0;
        return thickness.compute(pos) + Math.max(Math.abs(ridgeA.compute(pos)), Math.abs(ridgeB.compute(pos))) * 1.5;
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
    public double compute(FunctionContext pos) {
        return bias(pos.blockY()) + tunnel(pos, this.toggle, this.thickness, this.ridgeA, this.ridgeB);
    }

    @Override
    public void fillArray(double[] densities, ContextProvider applier) {
        applier.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new PvNoodle(this.toggle.mapAll(visitor), this.thickness.mapAll(visitor), this.ridgeA.mapAll(visitor),
                this.ridgeB.mapAll(visitor)));
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
