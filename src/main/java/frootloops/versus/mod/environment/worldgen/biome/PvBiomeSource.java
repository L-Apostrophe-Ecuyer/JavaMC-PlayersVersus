package frootloops.versus.mod.environment.worldgen.biome;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import java.util.function.BiConsumer;
import net.minecraft.world.level.biome.NoiseBiomeResolver;

/**
 * Biome source type {@code players-versus:overworld}: multi-noise biome selection over {@link PvBiomeLayout}. The
 * Players Versus world preset uses it, so its layout never reaches other world types.
 *
 * <p>The biomes come from a vanilla {@link MultiNoiseBiomeSource} over the layout's list, which since 26.3 samples a
 * chunk's climate as whole volumes; this source keeps the layout rule of each entry, in the same order, for
 * {@code /pvwg probe} and the debug screen.
 */
public final class PvBiomeSource extends BiomeSource {

    public static final MapCodec<PvBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryOps.retrieveGetter(Registries.BIOME)
    ).apply(instance, instance.stable(PvBiomeSource::new)));

    private final Climate.ParameterList<Placed> entries;
    private final MultiNoiseBiomeSource biomes;

    private PvBiomeSource(HolderGetter<Biome> biomes) {
        List<Pair<Climate.ParameterPoint, Placed>> list = new ArrayList<>();
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> biomeList = new ArrayList<>();
        for (PvBiomeLayout.Entry entry : PvBiomeLayout.build()) {
            Holder<Biome> biome = biomes.getOrThrow(entry.biome());
            list.add(Pair.of(entry.parameters(), new Placed(biome, entry.rule())));
            biomeList.add(Pair.of(entry.parameters(), biome));
        }
        this.entries = new Climate.ParameterList<>(list);
        this.biomes = MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(biomeList));
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return this.entries.values().stream().map(pair -> pair.getSecond().biome()).distinct();
    }

    @Override
    public NoiseBiomeResolver createResolver(Climate.Sampler sampler) {
        return this.biomes.createResolver(sampler);
    }

    @Override
    public NoiseBiomeResolver createResolverForChunk(Climate.Sampler sampler, int a, int b, int c, int d, int e, int f) {
        return this.biomes.createResolverForChunk(sampler, a, b, c, d, e, f);
    }

    /** The layout rule that picked the biome at this climate point ("vanilla" when no transition applied). */
    public String ruleAt(Climate.TargetPoint point) {
        return this.entries.findValue(point).rule();
    }

    @Override
    public void addDebugInfo(BiConsumer<String, String> info, BlockPos pos, Climate.Sampler noise) {
        Climate.TargetPoint point = noise.sample(
                QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()));
        info.accept("PV biome rule", String.format(Locale.ROOT, "%s  T %.3f H %.3f C %.3f E %.3f D %.3f W %.3f",
                ruleAt(point),
                Climate.unquantizeCoord(point.temperature()),
                Climate.unquantizeCoord(point.humidity()),
                Climate.unquantizeCoord(point.continentalness()),
                Climate.unquantizeCoord(point.erosion()),
                Climate.unquantizeCoord(point.depth()),
                Climate.unquantizeCoord(point.weirdness())));
    }

    private record Placed(Holder<Biome> biome, String rule) {
    }
}
