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
 * Density-function type {@code players-versus:aquifer_spread}: the cave-basin floodedness S of the Players Versus
 * aquifer ({@code aquifers.fluid_level_spread} in the noise settings), computed by {@link AquiferFormulas} from the
 * input functions the settings name. Its sampler is exact at any block; the aquifer reads S's smooth part from a
 * per-chunk lattice ({@code PvAquifer}).
 *
 * @param entrances {@code players-versus:overworld/caves/entrances}
 * @param noodle    {@code players-versus:overworld/caves/noodle}: the noodle at the block, without interpolation
 * @param surface   {@code noise(minecraft:surface, xz 4, y 2)}
 * @param corridors the final density's noodle ({@code players-versus:overworld/caves/corridor_noodle}), with the flooded
 *                  corridors' bias: the aquifer floods the basin layers where it's at most 0. Not part of S.
 */
public record AquiferSpread(DensityFunction entrances, DensityFunction noodle, DensityFunction surface,
                            DensityFunction corridors) implements DensityFunction {

    public static final MapCodec<AquiferSpread> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.CODEC.fieldOf("entrances").forGetter(AquiferSpread::entrances),
            DensityFunction.CODEC.fieldOf("noodle").forGetter(AquiferSpread::noodle),
            DensityFunction.CODEC.fieldOf("surface").forGetter(AquiferSpread::surface),
            DensityFunction.CODEC.fieldOf("corridors").forGetter(AquiferSpread::corridors)
    ).apply(instance, AquiferSpread::new));

    @Override
    public MapCodec<AquiferSpread> codec() {
        return CODEC;
    }

    /** S is a factor in -0.2..1 times a value in 0..0.9. */
    @Override
    public Interval range() {
        return Interval.of(-0.2F, 1.0F);
    }

    @Override
    public int domainAxes() {
        return ALL_AXES;
    }

    @Override
    public DensityFunction rewriteChildren(DfRewriteRule rule) {
        DensityFunction entrances = rule.rewrite(this.entrances), noodle = rule.rewrite(this.noodle);
        DensityFunction surface = rule.rewrite(this.surface), corridors = rule.rewrite(this.corridors);
        if (entrances == this.entrances && noodle == this.noodle && surface == this.surface && corridors == this.corridors) {
            return this;
        }
        return new AquiferSpread(entrances, noodle, surface, corridors);
    }

    @Override
    public DensitySampler compileSampler(CompileContext context) {
        return new Sampler(this.entrances.compileSampler(context), this.noodle.compileSampler(context), this.surface.compileSampler(context));
    }

    /** S at a block, from its inputs there. */
    record Sampler(DensitySampler entrances, DensitySampler noodle, DensitySampler surface) implements DensitySampler {

        @Override
        public float sampleValue(SamplerContext context, int x, int y, int z) {
            return (float) AquiferFormulas.spread(y, AquiferFormulas.basinInner(y, this.entrances.sampleValue(context, x, y, z),
                    this.noodle.sampleValue(context, x, y, z), this.surface.sampleValue(context, x, y, z)));
        }

        @Override
        public void sampleVolume(SamplerContext context, DensityBuffer buffer, DensityVolume volume) {
            DensitySampler.sampleVolumeNaive(context, buffer, volume, this);
        }
    }
}
