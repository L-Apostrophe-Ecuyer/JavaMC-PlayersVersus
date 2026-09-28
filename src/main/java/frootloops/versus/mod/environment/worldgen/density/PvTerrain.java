package frootloops.versus.mod.environment.worldgen.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.dynamic.CodecHolder;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import static frootloops.versus.mod.environment.worldgen.density.DensityOps.gradient;

/**
 * Density-function type {@code players-versus:terrain}: the Players Versus terrain density at a cell corner, before
 * vanilla blends and interpolates it ({@code final_density} wraps it in {@code blend_density} and {@code interpolated}).
 * It's the old {@code final_density} inner tree, {@code sloped_cheese}, {@code river_carver}, {@code depth},
 * {@code caves/pillars} and {@code caves/spaghetti_2d} in one pass, giving the same doubles
 * ({@code TerrainPortTest}) with less work per corner:
 * <ul>
 *   <li>the sloped cheese (the terrain surface, with the 3D base noise) is computed once; the JSON computed it twice
 *   at every corner, for the range test and again inside the chosen branch;</li>
 *   <li>the river ridge noise is read once, from vanilla's {@code minecraft:overworld/ridges}, which the chunk keeps
 *   per column; the JSON sampled it again in the river carver and in the depth, twice each where rivers run;</li>
 *   <li>{@code jagged} (2D) comes through a per-column cache ({@code cache_2d} in the JSON), and only where the
 *   jaggedness isn't 0, as vanilla's {@code mul} does;</li>
 *   <li>entrances, spaghetti and pillars are skipped where the value can't reach them, as vanilla's {@code min} and
 *   {@code max} do.</li>
 * </ul>
 *
 * @param offset             vanilla's {@code minecraft:overworld/offset}
 * @param factor             vanilla's {@code minecraft:overworld/factor}
 * @param jaggedness         vanilla's {@code minecraft:overworld/jaggedness}
 * @param jagged             {@code noise(minecraft:jagged, xz 1500, y 0)}, in {@code cache_2d}
 * @param ridges             vanilla's {@code minecraft:overworld/ridges} (R)
 * @param base3d             vanilla's {@code minecraft:overworld/base_3d_noise}
 * @param entrances          {@code players-versus:overworld/caves/entrances} ({@link PvEntrances} in {@code cache_once})
 * @param spaghettiRoughness vanilla's {@code minecraft:overworld/caves/spaghetti_roughness_function}
 * @param caveLayer          {@code noise(minecraft:cave_layer, xz 1, y 8)}
 * @param caveCheese         {@code noise(minecraft:cave_cheese, xz 1, y 2/3)}
 * @param pillar             {@code noise(minecraft:pillar, xz 20, y 0.5)}
 * @param entrancesMin       the lowest the entrances can go (from their bounds)
 * @param spaghettiMin       the lowest the spaghetti term can go (from the roughness's bounds)
 */
public record PvTerrain(DensityFunction offset, DensityFunction factor, DensityFunction jaggedness, DensityFunction jagged,
                        DensityFunction ridges, DensityFunction base3d, DensityFunction entrances,
                        DensityFunction spaghettiRoughness, DensityFunction caveLayer, DensityFunction caveCheese,
                        DensityFunction pillar, double entrancesMin, double spaghettiMin) implements DensityFunction {

    public static final MapCodec<PvTerrain> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.FUNCTION_CODEC.fieldOf("offset").forGetter(PvTerrain::offset),
            DensityFunction.FUNCTION_CODEC.fieldOf("factor").forGetter(PvTerrain::factor),
            DensityFunction.FUNCTION_CODEC.fieldOf("jaggedness").forGetter(PvTerrain::jaggedness),
            DensityFunction.FUNCTION_CODEC.fieldOf("jagged").forGetter(PvTerrain::jagged),
            DensityFunction.FUNCTION_CODEC.fieldOf("ridges").forGetter(PvTerrain::ridges),
            DensityFunction.FUNCTION_CODEC.fieldOf("base_3d_noise").forGetter(PvTerrain::base3d),
            DensityFunction.FUNCTION_CODEC.fieldOf("entrances").forGetter(PvTerrain::entrances),
            DensityFunction.FUNCTION_CODEC.fieldOf("spaghetti_roughness").forGetter(PvTerrain::spaghettiRoughness),
            DensityFunction.FUNCTION_CODEC.fieldOf("cave_layer").forGetter(PvTerrain::caveLayer),
            DensityFunction.FUNCTION_CODEC.fieldOf("cave_cheese").forGetter(PvTerrain::caveCheese),
            DensityFunction.FUNCTION_CODEC.fieldOf("pillar").forGetter(PvTerrain::pillar)
    ).apply(instance, PvTerrain::new));
    private static final CodecHolder<PvTerrain> CODEC_HOLDER = CodecHolder.of(CODEC);

    /** Pillars are {@code min(0.3, …)}, so a value above 0.3 can't be raised by them. */
    private static final double PILLARS_MAX = 0.3;

    public PvTerrain(DensityFunction offset, DensityFunction factor, DensityFunction jaggedness, DensityFunction jagged,
                     DensityFunction ridges, DensityFunction base3d, DensityFunction entrances, DensityFunction spaghettiRoughness,
                     DensityFunction caveLayer, DensityFunction caveCheese, DensityFunction pillar) {
        this(offset, factor, jaggedness, jagged, ridges, base3d, entrances, spaghettiRoughness, caveLayer, caveCheese, pillar,
                entrances.minValue(), 1.0 + spaghettiRoughness.minValue());
    }

    /** The terrain at a corner, faded to its fixed values at the bottom (y -64..-40) and top (y 240..256) of the world. */
    @Override
    public double sample(NoisePos pos) {
        int y = pos.blockY();
        double bottom = gradient(y, -64, -40, 0.0, 1.0);
        if (bottom == 0.0) return 0.1171875;
        double top = gradient(y, 240, 256, 1.0, 0.0);
        double shaped = top == 0.0 ? 0.0 : top * (this.caves(pos, y) + 0.078125);
        return bottom * ((shaped + -0.078125) + -0.1171875) + 0.1171875;
    }

    /**
     * Near and above the surface (sloped cheese below 1.5625): the surface, cut by cave entrances. Below: cheese caves
     * and the cave layer, cut by entrances and spaghetti, with pillars standing in them.
     */
    private double caves(NoisePos pos, int y) {
        double slopedCheese = this.slopedCheese(pos, y);
        if (slopedCheese >= -1000000.0 && slopedCheese < 1.5625) {
            return slopedCheese < this.entrancesMin * 5.0 ? slopedCheese : Math.min(slopedCheese, this.entrances.sample(pos) * 5.0);
        }
        double layer = this.caveLayer.sample(pos);
        double cheese = layer * layer * 4.0 + (MathHelper.clamp(this.caveCheese.sample(pos) + 0.27, -1.0, 1.0)
                + MathHelper.clamp(slopedCheese * -0.64 + 1.5, 0.0, 0.5));
        double withEntrances = cheese < this.entrancesMin ? cheese : Math.min(cheese, this.entrances.sample(pos));
        double withSpaghetti = withEntrances < this.spaghettiMin ? withEntrances
                : Math.min(withEntrances, 1.0 + this.spaghettiRoughness.sample(pos));
        if (withSpaghetti > PILLARS_MAX) return withSpaghetti;
        double pillars = Math.min(0.3, Math.max(-0.02, Math.max(0.0, this.pillar.sample(pos) + -0.15) * 0.7 + -0.2));
        return Math.max(withSpaghetti, pillars >= -1000000.0 && pillars < 0.03 ? -1000000.0 : pillars);
    }

    /** The terrain surface: depth with jagged peaks, scaled by the factor, plus the 3D base noise; river valleys cut in. */
    private double slopedCheese(NoisePos pos, int y) {
        double yValue = DensityOps.yValue(y);
        double ridge = TerrainFormulas.inRiverBand(yValue) ? this.ridges.sample(pos) : 0.0;
        double riverCarver = TerrainFormulas.riverCarver(y, yValue, ridge);
        double depth = TerrainFormulas.depth(y, this.offset.sample(pos), TerrainFormulas.riverDepth(y, yValue, ridge));
        double jaggedness = this.jaggedness.sample(pos);
        double withPeaks = depth + (jaggedness == 0.0 ? 0.0 : jaggedness * DensityOps.halfNegative(this.jagged.sample(pos)));
        double shaped = withPeaks == 0.0 ? 0.0 : withPeaks * this.factor.sample(pos);
        return Math.min(riverCarver, DensityOps.quarterNegative(shaped) * 4.0 + this.base3d.sample(pos));
    }

    @Override
    public void fill(double[] densities, EachApplier applier) {
        applier.fill(densities, this);
    }

    @Override
    public DensityFunction apply(DensityFunctionVisitor visitor) {
        return visitor.apply(new PvTerrain(this.offset.apply(visitor), this.factor.apply(visitor), this.jaggedness.apply(visitor),
                this.jagged.apply(visitor), this.ridges.apply(visitor), this.base3d.apply(visitor), this.entrances.apply(visitor),
                this.spaghettiRoughness.apply(visitor), this.caveLayer.apply(visitor), this.caveCheese.apply(visitor),
                this.pillar.apply(visitor)));
    }

    /**
     * Unbounded, which is always valid. Nothing needs tighter bounds: the terrain only feeds {@code blend_density} and
     * {@code interpolated}, and {@link PvFinalDensity} bounds its own result.
     */
    @Override
    public double minValue() {
        return Double.NEGATIVE_INFINITY;
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
