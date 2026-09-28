package frootloops.versus.mod.environment.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generator type {@code players-versus:noise}, the Improved world type's overworld: vanilla's noise generator, whose
 * biomes carve with the Players Versus carvers ({@link PvCarvers}). Everything else is vanilla's
 * {@link NoiseChunkGenerator}; the terrain, aquifer and ore veins come from the settings, as before.
 *
 * <p>Vanilla's generator asks {@link #getGenerationSettings} for a biome's carvers when it carves a chunk (the smoke
 * run's carved-position count shows the swap). The settings returned keep the biome's own features, so anything else
 * that asks gets the same features as before.
 */
public final class PvChunkGenerator extends NoiseChunkGenerator {

    public static final MapCodec<PvChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(PvChunkGenerator::getBiomeSource),
            ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter(PvChunkGenerator::getSettings),
            RegistryOps.getEntryLookupCodec(RegistryKeys.CONFIGURED_CARVER)
    ).apply(instance, instance.stable(PvChunkGenerator::new)));

    private final PvCarvers carvers;
    /** Each biome's settings with its Improved carvers, made once. */
    private final Map<RegistryEntry<Biome>, GenerationSettings> generationSettings = new ConcurrentHashMap<>();

    public PvChunkGenerator(BiomeSource biomeSource, RegistryEntry<ChunkGeneratorSettings> settings,
                            RegistryEntryLookup<ConfiguredCarver<?>> carvers) {
        super(biomeSource, settings);
        this.carvers = new PvCarvers(carvers);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public GenerationSettings getGenerationSettings(RegistryEntry<Biome> biome) {
        GenerationSettings cached = this.generationSettings.get(biome);
        if (cached != null) return cached;
        return this.generationSettings.computeIfAbsent(biome, entry -> this.carvers.withImprovedCarvers(entry, super.getGenerationSettings(entry)));
    }
}
