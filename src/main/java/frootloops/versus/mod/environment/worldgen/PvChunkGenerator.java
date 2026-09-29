package frootloops.versus.mod.environment.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.carver.WorldCarver;

/**
 * Generator type {@code players-versus:noise}, the Improved world type's overworld: vanilla's noise generator, whose
 * biomes carve with the Players Versus carvers ({@link PvCarvers}). Everything else is vanilla's
 * {@link NoiseBasedChunkGenerator}; the terrain, aquifer and ore veins come from the settings, as before.
 *
 * <p>Vanilla's generator asks {@link #getBiomeGenerationSettings} for a biome's carvers when it carves a chunk (the smoke
 * run's carved-position count shows the swap). The settings returned keep the biome's own features, so anything else
 * that asks gets the same features as before.
 */
public final class PvChunkGenerator extends NoiseBasedChunkGenerator {

    public static final MapCodec<PvChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(PvChunkGenerator::getBiomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(PvChunkGenerator::generatorSettings),
            RegistryOps.retrieveGetter(Registries.CARVER)
    ).apply(instance, instance.stable(PvChunkGenerator::new)));

    private final PvCarvers carvers;
    /** Each biome's settings with its Improved carvers, made once. */
    private final Map<Holder<Biome>, BiomeGenerationSettings> generationSettings = new ConcurrentHashMap<>();

    public PvChunkGenerator(BiomeSource biomeSource, Holder<NoiseGeneratorSettings> settings,
                            HolderGetter<WorldCarver> carvers) {
        super(biomeSource, settings);
        this.carvers = new PvCarvers(carvers);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public BiomeGenerationSettings getBiomeGenerationSettings(Holder<Biome> biome) {
        BiomeGenerationSettings cached = this.generationSettings.get(biome);
        if (cached != null) return cached;
        return this.generationSettings.computeIfAbsent(biome, entry -> this.carvers.withImprovedCarvers(entry, super.getBiomeGenerationSettings(entry)));
    }
}
