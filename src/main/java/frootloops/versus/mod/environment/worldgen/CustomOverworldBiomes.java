package frootloops.versus.mod.environment.worldgen;

import frootloops.versus.VersusMod;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class CustomOverworldBiomes {

    private static final MultiNoiseUtil.ParameterRange defaultParameter = MultiNoiseUtil.ParameterRange.of(-1.0F, 1.0F);
    public static final RegistryKey<Biome> BIRCH_TAIGA_FOREST = keyOf("birch_taiga_forest");
    public static final RegistryKey<Biome> DARK_TAIGA_FOREST = keyOf("dark_taiga_forest");
    public static final RegistryKey<Biome> DARK_BIRCH_FOREST = keyOf("dark_birch_forest");
    public static final RegistryKey<Biome> COLD_BEACH = keyOf("cold_beach");
    public static final RegistryKey<Biome> COLD_PLAINS = keyOf("cold_plains");
    public static final RegistryKey<Biome> COLD_TAIGA = keyOf("cold_taiga");
    public static final RegistryKey<Biome> MOUNTAINSIDE_JUNGLE = keyOf("mountainside_jungle");
    public static final RegistryKey<Biome> MOUNTAINSIDE_FOREST = keyOf("mountainside_forest");
    public static final RegistryKey<Biome> MOUNTAINSIDE_FOREST_WARM = keyOf("mountainside_forest_warm");
    public static final RegistryKey<Biome> DESERT_OASIS = keyOf("desert_oasis");
    public static final RegistryKey<Biome> DESERT_CREEPER_CAVE = keyOf("caves/creeper_caves_desert");
    public static final RegistryKey<Biome> BADLANDS_CAVE = keyOf("caves/badlands_cave");
    public static final RegistryKey<Biome> CREEPER_CAVE = keyOf("caves/creeper_caves");
    public static final RegistryKey<Biome> FROSTED_CAVE = keyOf("caves/frosted_caves");
    public static final RegistryKey<Biome> REGULAR_CAVE = keyOf("caves/regular_cave");
    public static final RegistryKey<Biome> DEEP_CAVES = keyOf("caves/deep_caves");

    public enum PlacedBiomeType {
        SKY, SURFACE, SURFACE_CAVE, CAVE, DEEP_CAVE, GENERIC_CAVE, GENERIC_DEEP_CAVE
    }

    public static PlacedBiome[] landBiomesToPlaceInOverorld = new PlacedBiome[]{
            // Add here new biomes to spawn on the surface
    };

    public static PlacedBiome[] caveBiomesToPlaceInOverorld = new PlacedBiome[]{
            // Add here new cave biomes
            new PlacedBiome(FROSTED_CAVE, PlacedBiomeType.SURFACE_CAVE, false, MultiNoiseUtil.ParameterRange.of(-1.0f, -0.4f), defaultParameter, MultiNoiseUtil.ParameterRange.of(-0.1f, 1.0f), MultiNoiseUtil.ParameterRange.of(-1.0f, -0.65f), MultiNoiseUtil.ParameterRange.of(-1.0f, -0.2f)),
            new PlacedBiome(FROSTED_CAVE, PlacedBiomeType.SURFACE_CAVE, false, MultiNoiseUtil.ParameterRange.of(-1.0f, -0.4f), defaultParameter, MultiNoiseUtil.ParameterRange.of(-0.1f, 1.0f), MultiNoiseUtil.ParameterRange.of(-1.0f, -0.65f), MultiNoiseUtil.ParameterRange.of(0.2f, 1.0f)),
            new PlacedBiome(FROSTED_CAVE, PlacedBiomeType.SURFACE_CAVE, false, MultiNoiseUtil.ParameterRange.of(-1.0f, -0.6f), defaultParameter, defaultParameter, defaultParameter, defaultParameter),
            new PlacedBiome(BADLANDS_CAVE, PlacedBiomeType.SURFACE_CAVE, true, MultiNoiseUtil.ParameterRange.of(0.8f, 1.0f), MultiNoiseUtil.ParameterRange.of(-1.0f, -0.0f), defaultParameter, defaultParameter, defaultParameter),
            new PlacedBiome(DESERT_CREEPER_CAVE, PlacedBiomeType.CAVE, true, MultiNoiseUtil.ParameterRange.of(0.8f, 1.0f), MultiNoiseUtil.ParameterRange.of(-1.0f, -0.5f), MultiNoiseUtil.ParameterRange.of(0.0f, 0.5f), MultiNoiseUtil.ParameterRange.of(0.1f, 1.0f), MultiNoiseUtil.ParameterRange.of(-1.0f, -0.3f)),
            new PlacedBiome(DESERT_CREEPER_CAVE, PlacedBiomeType.CAVE, true, MultiNoiseUtil.ParameterRange.of(0.8f, 1.0f), MultiNoiseUtil.ParameterRange.of(-1.0f, -0.5f), MultiNoiseUtil.ParameterRange.of(0.0f, 0.5f), MultiNoiseUtil.ParameterRange.of(0.1f, 1.0f), MultiNoiseUtil.ParameterRange.of(0.2f, 1.0f)),
            new PlacedBiome(CREEPER_CAVE, PlacedBiomeType.CAVE, true, MultiNoiseUtil.ParameterRange.of(-0.3f, 0.7f), MultiNoiseUtil.ParameterRange.of(-0.4f, 1.1f), MultiNoiseUtil.ParameterRange.of(0.35f, 0.5f), MultiNoiseUtil.ParameterRange.of(-0.1f, 1.0f), MultiNoiseUtil.ParameterRange.of(-0.75f, -0.6f)),
            new PlacedBiome(BiomeKeys.LUSH_CAVES, PlacedBiomeType.CAVE, false, MultiNoiseUtil.ParameterRange.of(-0.3f, 0.7f), MultiNoiseUtil.ParameterRange.of(-0.4f, 1.1f), MultiNoiseUtil.ParameterRange.of(0.35f, 0.5f), MultiNoiseUtil.ParameterRange.of(-0.1f, 1.0f), MultiNoiseUtil.ParameterRange.of(0.4f, 0.8f)),
            new PlacedBiome(BiomeKeys.LUSH_CAVES, PlacedBiomeType.SURFACE_CAVE, true, MultiNoiseUtil.ParameterRange.of(-0.3f, 0.7f), MultiNoiseUtil.ParameterRange.of(-0.4f, 1.1f), MultiNoiseUtil.ParameterRange.of(0.35f, 0.5f), MultiNoiseUtil.ParameterRange.of(-0.1f, 1.0f), MultiNoiseUtil.ParameterRange.of(-0.4f, -0.2f)),
            new PlacedBiome(BiomeKeys.DRIPSTONE_CAVES, PlacedBiomeType.CAVE, true, MultiNoiseUtil.ParameterRange.of(0.0f, 0.7f), defaultParameter, MultiNoiseUtil.ParameterRange.of(0.4f, 1.0f), MultiNoiseUtil.ParameterRange.of(-0.8f, 0.0f), MultiNoiseUtil.ParameterRange.of(-0.75f, 0.3f)),
            new PlacedBiome(BiomeKeys.DEEP_DARK, PlacedBiomeType.DEEP_CAVE, false, defaultParameter, defaultParameter, defaultParameter, MultiNoiseUtil.ParameterRange.of(-1.0f, -0.8f), defaultParameter),

            new PlacedBiome(REGULAR_CAVE, PlacedBiomeType.GENERIC_CAVE, false, MultiNoiseUtil.ParameterRange.of(-0.7f, 1.0f), defaultParameter, defaultParameter, defaultParameter, defaultParameter),
            new PlacedBiome(DEEP_CAVES, PlacedBiomeType.GENERIC_DEEP_CAVE, false, defaultParameter, defaultParameter, defaultParameter, defaultParameter, defaultParameter)
    };


    private static final Map<RegistryKey<Biome>, RegistryKey<Biome>> MOUNTAIN_BIOME_REPLACEMENTS = Map.ofEntries(
            Map.entry(BiomeKeys.OLD_GROWTH_BIRCH_FOREST, BIRCH_TAIGA_FOREST),
            Map.entry(BiomeKeys.BIRCH_FOREST, BIRCH_TAIGA_FOREST),
            Map.entry(BiomeKeys.DARK_FOREST, DARK_TAIGA_FOREST),
            Map.entry(BiomeKeys.JUNGLE, MOUNTAINSIDE_JUNGLE),
            Map.entry(BiomeKeys.SPARSE_JUNGLE, MOUNTAINSIDE_JUNGLE),
            Map.entry(BiomeKeys.BAMBOO_JUNGLE, MOUNTAINSIDE_JUNGLE),
            Map.entry(BiomeKeys.TAIGA, BiomeKeys.WINDSWEPT_FOREST),
            Map.entry(BiomeKeys.FOREST, MOUNTAINSIDE_FOREST),
            Map.entry(BiomeKeys.SNOWY_TAIGA, BiomeKeys.GROVE),
            Map.entry(BiomeKeys.PLAINS, BiomeKeys.MEADOW),
            Map.entry(BiomeKeys.SNOWY_PLAINS, BiomeKeys.SNOWY_SLOPES),
            Map.entry(BiomeKeys.SAVANNA, MOUNTAINSIDE_FOREST_WARM),
            Map.entry(BiomeKeys.SAVANNA_PLATEAU, BiomeKeys.WINDSWEPT_SAVANNA));

    private static final Map<RegistryKey<Biome>, RegistryKey<Biome>> FROZEN_BIOME_REPLACEMENTS = Map.ofEntries(
            Map.entry(BiomeKeys.SNOWY_BEACH, COLD_BEACH),
            Map.entry(BiomeKeys.TAIGA, COLD_TAIGA),
            Map.entry(BiomeKeys.FOREST, BiomeKeys.TAIGA),
            Map.entry(BiomeKeys.BIRCH_FOREST, BIRCH_TAIGA_FOREST),
            Map.entry(BiomeKeys.SNOWY_TAIGA, COLD_TAIGA),
            Map.entry(BiomeKeys.PLAINS, BiomeKeys.MEADOW),
            Map.entry(BiomeKeys.SNOWY_PLAINS, COLD_PLAINS),
            Map.entry(BiomeKeys.FROZEN_OCEAN, BiomeKeys.COLD_OCEAN),
            Map.entry(BiomeKeys.ICE_SPIKES, BiomeKeys.SNOWY_PLAINS));

    private static final Map<RegistryKey<Biome>, RegistryKey<Biome>> HUMID_BIOME_REPLACEMENTS = Map.ofEntries(
            Map.entry(BiomeKeys.BIRCH_FOREST, DARK_BIRCH_FOREST),
            Map.entry(BiomeKeys.DARK_FOREST, DARK_BIRCH_FOREST),
            Map.entry(BiomeKeys.JUNGLE, BiomeKeys.SPARSE_JUNGLE),
            Map.entry(BiomeKeys.DESERT, DESERT_OASIS),
            Map.entry(BiomeKeys.BADLANDS, BiomeKeys.DESERT));

    private static final Map<RegistryKey<Biome>, RegistryKey<Biome>> SURFACE_CAVE_BIOME_REPLACEMENTS = Map.ofEntries(
            Map.entry(BiomeKeys.FROZEN_PEAKS, FROSTED_CAVE),
            Map.entry(BiomeKeys.SNOWY_SLOPES, FROSTED_CAVE),
            Map.entry(BiomeKeys.DESERT, DESERT_CREEPER_CAVE),
            Map.entry(BiomeKeys.BADLANDS, BADLANDS_CAVE),
            Map.entry(BiomeKeys.ERODED_BADLANDS, BADLANDS_CAVE),
            Map.entry(BiomeKeys.WOODED_BADLANDS, BADLANDS_CAVE));

    @Nullable
    public static RegistryKey<Biome> getMountainTransitionBiome(RegistryKey<Biome> originalBiome) {
        return MOUNTAIN_BIOME_REPLACEMENTS.get(originalBiome);
    }

    @Nullable
    public static RegistryKey<Biome> getSnowyToTemperateTransitionBiome(RegistryKey<Biome> originalBiome) {
        return FROZEN_BIOME_REPLACEMENTS.get(originalBiome);
    }

    @Nullable
    public static RegistryKey<Biome> getHumidTransitionBiome(RegistryKey<Biome> originalBiome) {
        return HUMID_BIOME_REPLACEMENTS.get(originalBiome);
    }

    @Nullable
    public static RegistryKey<Biome> getSurfaceCaveBiome(RegistryKey<Biome> originalBiome) {
        return SURFACE_CAVE_BIOME_REPLACEMENTS.get(originalBiome);
    }

    private static RegistryKey<Biome> keyOf(String id) {
        return RegistryKey.of(RegistryKeys.BIOME, Identifier.of(VersusMod.MOD_ID, id));
    }

    public record PlacedBiome(RegistryKey<Biome> biome, PlacedBiomeType type, boolean isRare,
                              MultiNoiseUtil.ParameterRange temperature,
                              MultiNoiseUtil.ParameterRange humidity,
                              MultiNoiseUtil.ParameterRange continentalness,
                              MultiNoiseUtil.ParameterRange erosion,
                              MultiNoiseUtil.ParameterRange weirdness){}
}
