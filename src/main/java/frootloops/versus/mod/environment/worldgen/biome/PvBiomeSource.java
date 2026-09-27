package frootloops.versus.mod.environment.worldgen.biome;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Biome source type {@code players-versus:overworld}: multi-noise biome selection over {@link PvBiomeLayout}. The
 * Players Versus world preset uses it, so its layout never reaches other world types.
 */
public final class PvBiomeSource extends BiomeSource {

    public static final MapCodec<PvBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryOps.getEntryLookupCodec(RegistryKeys.BIOME)
    ).apply(instance, instance.stable(PvBiomeSource::new)));

    private final MultiNoiseUtil.Entries<Placed> entries;

    private PvBiomeSource(RegistryEntryLookup<Biome> biomes) {
        List<Pair<MultiNoiseUtil.NoiseHypercube, Placed>> list = new ArrayList<>();
        for (PvBiomeLayout.Entry entry : PvBiomeLayout.build()) {
            list.add(Pair.of(entry.parameters(), new Placed(biomes.getOrThrow(entry.biome()), entry.rule())));
        }
        this.entries = new MultiNoiseUtil.Entries<>(list);
    }

    @Override
    protected MapCodec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    protected Stream<RegistryEntry<Biome>> biomeStream() {
        return this.entries.getEntries().stream().map(pair -> pair.getSecond().biome()).distinct();
    }

    @Override
    public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
        return this.entries.get(noise.sample(x, y, z)).biome();
    }

    /** The layout rule that picked the biome at this climate point ("vanilla" when no transition applied). */
    public String ruleAt(MultiNoiseUtil.NoiseValuePoint point) {
        return this.entries.get(point).rule();
    }

    @Override
    public void addDebugInfo(List<String> info, BlockPos pos, MultiNoiseUtil.MultiNoiseSampler noise) {
        MultiNoiseUtil.NoiseValuePoint point = noise.sample(
                BiomeCoords.fromBlock(pos.getX()), BiomeCoords.fromBlock(pos.getY()), BiomeCoords.fromBlock(pos.getZ()));
        info.add(String.format(Locale.ROOT, "PV biome rule: %s  T %.3f H %.3f C %.3f E %.3f D %.3f W %.3f",
                ruleAt(point),
                MultiNoiseUtil.toFloat(point.temperatureNoise()),
                MultiNoiseUtil.toFloat(point.humidityNoise()),
                MultiNoiseUtil.toFloat(point.continentalnessNoise()),
                MultiNoiseUtil.toFloat(point.erosionNoise()),
                MultiNoiseUtil.toFloat(point.depth()),
                MultiNoiseUtil.toFloat(point.weirdnessNoise())));
    }

    private record Placed(RegistryEntry<Biome> biome, String rule) {
    }
}
