package frootloops.versus.mod.environment.worldgen.density;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import frootloops.versus.mod.environment.worldgen.aquifer.AquiferFormulas;
import net.minecraft.util.Interval;
import net.minecraft.world.level.levelgen.densityfunction.DensityBuffer;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.DfRewriteRule;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

/**
 * Density-function type {@code players-versus:aquifer_floodedness}: the sea/river floodedness F of the Players Versus
 * aquifer, computed by {@link AquiferFormulas} from the input functions the settings name.
 *
 * <p>Putting this type in the noise settings' aquifer config ({@code aquifers.fluid_level_floodedness}) is what turns
 * on the Players Versus aquifer for that generator ({@code PvWorldgen.isPvGenerator}); no magic numbers involved.
 *
 * <p>Its sampler is exact at any block, for {@code /pvwg probe}. The aquifer reads the smooth inputs (depth,
 * continentalness, entrances) from per-chunk lattices and the rest per block ({@code PvAquifer}), and the high river's
 * valley, which it carries along for the aquifer, from the chunk's own sampler of it.
 *
 * @param depth           the router's {@code depth} ({@code players-versus:overworld/depth})
 * @param continentalness {@code noise(minecraft:continentalness, xz 0.25, y 0.1)}, shifted like the climate noises
 * @param ridge           vanilla's {@code minecraft:overworld/ridges} (the shifted ridge noise, xz 0.25, y 0; the
 *                        terrain reads the same function)
 * @param entrances       {@code players-versus:overworld/caves/entrances}
 * @param surface         {@code noise(minecraft:surface, xz 2, y 1)}
 * @param ramen           {@code noise(minecraft:noodle, xz 3, y 3)}
 * @param highRiver       the high river's valley ({@code players-versus:overworld/high_river/valley}), which the final
 *                        density takes the minimum with: the aquifer puts the river's water where it's negative at or
 *                        under its surface. Not part of F.
 */
public record AquiferFloodedness(DensityFunction depth, DensityFunction continentalness, DensityFunction ridge,
                                 DensityFunction entrances, DensityFunction surface, DensityFunction ramen,
                                 DensityFunction highRiver) implements DensityFunction {

    public static final MapCodec<AquiferFloodedness> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.CODEC.fieldOf("depth").forGetter(AquiferFloodedness::depth),
            DensityFunction.CODEC.fieldOf("continentalness").forGetter(AquiferFloodedness::continentalness),
            DensityFunction.CODEC.fieldOf("ridge").forGetter(AquiferFloodedness::ridge),
            DensityFunction.CODEC.fieldOf("entrances").forGetter(AquiferFloodedness::entrances),
            DensityFunction.CODEC.fieldOf("surface").forGetter(AquiferFloodedness::surface),
            DensityFunction.CODEC.fieldOf("ramen").forGetter(AquiferFloodedness::ramen),
            DensityFunction.CODEC.fieldOf("high_river").forGetter(AquiferFloodedness::highRiver)
    ).apply(instance, AquiferFloodedness::new));

    @Override
    public MapCodec<AquiferFloodedness> codec() {
        return CODEC;
    }

    /** F is at least 0: its ocean and river terms are, and it's their maximum with the coast term, plus the ramen term (also at least 0). */
    @Override
    public Interval range() {
        return Interval.of(0.0F, Float.POSITIVE_INFINITY);
    }

    @Override
    public int domainAxes() {
        return ALL_AXES;
    }

    @Override
    public DensityFunction rewriteChildren(DfRewriteRule rule) {
        DensityFunction depth = rule.rewrite(this.depth), continentalness = rule.rewrite(this.continentalness);
        DensityFunction ridge = rule.rewrite(this.ridge), entrances = rule.rewrite(this.entrances);
        DensityFunction surface = rule.rewrite(this.surface), ramen = rule.rewrite(this.ramen);
        DensityFunction highRiver = rule.rewrite(this.highRiver);
        if (depth == this.depth && continentalness == this.continentalness && ridge == this.ridge && entrances == this.entrances
                && surface == this.surface && ramen == this.ramen && highRiver == this.highRiver) {
            return this;
        }
        return new AquiferFloodedness(depth, continentalness, ridge, entrances, surface, ramen, highRiver);
    }

    @Override
    public DensitySampler compileSampler(CompileContext context) {
        return new Sampler(this.depth.compileSampler(context), this.continentalness.compileSampler(context),
                this.ridge.compileSampler(context), this.entrances.compileSampler(context), this.surface.compileSampler(context),
                this.ramen.compileSampler(context));
    }

    /** F at a block, from its inputs there. */
    record Sampler(DensitySampler depth, DensitySampler continentalness, DensitySampler ridge, DensitySampler entrances,
                   DensitySampler surface, DensitySampler ramen) implements DensitySampler {

        @Override
        public float sampleValue(SamplerContext context, int x, int y, int z) {
            double entrances = this.entrances.sampleValue(context, x, y, z);
            double seaFloodedness = AquiferFormulas.seaFloodedness(y, this.depth.sampleValue(context, x, y, z),
                    this.continentalness.sampleValue(context, x, y, z), entrances, entrances, this.ridge.sampleValue(context, x, y, z),
                    this.surface.sampleValue(context, x, y, z));
            double ramen = AquiferFormulas.addsRamen(y, seaFloodedness) ? this.ramen.sampleValue(context, x, y, z) : 0.0;
            return (float) AquiferFormulas.floodedness(y, seaFloodedness, ramen);
        }

        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer buffer, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, buffer, volume, this);
        }
    }
}
