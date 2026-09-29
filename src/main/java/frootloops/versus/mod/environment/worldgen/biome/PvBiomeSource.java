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

/**
 * Biome source type {@code players-versus:overworld}: multi-noise biome selection over {@link PvBiomeLayout}. The
 * Players Versus world preset uses it, so its layout never reaches other world types.
 */
public final class PvBiomeSource extends BiomeSource {

    public static final MapCodec<PvBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryOps.retrieveGetter(Registries.BIOME)
    ).apply(instance, instance.stable(PvBiomeSource::new)));

    private final Climate.ParameterList<Placed> entries;

    private PvBiomeSource(HolderGetter<Biome> biomes) {
        List<Pair<Climate.ParameterPoint, Placed>> list = new ArrayList<>();
        for (PvBiomeLayout.Entry entry : PvBiomeLayout.build()) {
            list.add(Pair.of(entry.parameters(), new Placed(biomes.getOrThrow(entry.biome()), entry.rule())));
        }
        this.entries = new Climate.ParameterList<>(list);
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
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler noise) {
        return this.entries.findValue(noise.sample(x, y, z)).biome();
    }

    /** The layout rule that picked the biome at this climate point ("vanilla" when no transition applied). */
    public String ruleAt(Climate.TargetPoint point) {
        return this.entries.findValue(point).rule();
    }

    @Override
    public void addDebugInfo(List<String> info, BlockPos pos, Climate.Sampler noise) {
        Climate.TargetPoint point = noise.sample(
                QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()));
        info.add(String.format(Locale.ROOT, "PV biome rule: %s  T %.3f H %.3f C %.3f E %.3f D %.3f W %.3f",
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
