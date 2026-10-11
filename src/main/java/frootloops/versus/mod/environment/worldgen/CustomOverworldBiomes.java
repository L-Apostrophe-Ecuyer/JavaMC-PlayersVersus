package frootloops.versus.mod.environment.worldgen;

import frootloops.versus.VersusMod;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

public class CustomOverworldBiomes {

    private static final Climate.Parameter defaultParameter = Climate.Parameter.span(-1.0F, 1.0F);
    public static final ResourceKey<Biome> BIRCH_TAIGA_FOREST = keyOf("birch_taiga_forest");
    /** The birch taiga with dappled trees (26.3's poplars) where it has birch. */
    public static final ResourceKey<Biome> DAPPLED_TAIGA = keyOf("dappled_taiga");
    /** Dappled trees with a few birch and taiga trees, clumped with open ground between, like the savanna's. */
    public static final ResourceKey<Biome> SPARSE_DAPPLED_FOREST = keyOf("sparse_dappled_forest");
    /** Vanilla's dappled forest (new in 26.3), by its id. */
    public static final ResourceKey<Biome> DAPPLED_FOREST = ResourceKey.create(Registries.BIOME,
            Identifier.fromNamespaceAndPath("minecraft", "dappled_forest"));
    public static final ResourceKey<Biome> DARK_TAIGA_FOREST = keyOf("dark_taiga_forest");
    public static final ResourceKey<Biome> DARK_BIRCH_FOREST = keyOf("dark_birch_forest");
    public static final ResourceKey<Biome> COLD_BEACH = keyOf("cold_beach");
    public static final ResourceKey<Biome> COLD_PLAINS = keyOf("cold_plains");
    public static final ResourceKey<Biome> COLD_TAIGA = keyOf("cold_taiga");
    public static final ResourceKey<Biome> MOUNTAINSIDE_JUNGLE = keyOf("mountainside_jungle");
    public static final ResourceKey<Biome> MOUNTAINSIDE_FOREST = keyOf("mountainside_forest");
    public static final ResourceKey<Biome> MOUNTAINSIDE_FOREST_WARM = keyOf("mountainside_forest_warm");
    public static final ResourceKey<Biome> DESERT_OASIS = keyOf("desert_oasis");
    public static final ResourceKey<Biome> DESERT_CREEPER_CAVE = keyOf("caves/creeper_caves_desert");
    public static final ResourceKey<Biome> BADLANDS_CAVE = keyOf("caves/badlands_cave");
    public static final ResourceKey<Biome> CREEPER_CAVE = keyOf("caves/creeper_caves");
    public static final ResourceKey<Biome> REGULAR_CAVE = keyOf("caves/regular_cave");
    public static final ResourceKey<Biome> DEEP_CAVES = keyOf("caves/deep_caves");
    public static final ResourceKey<Biome> PALE_GROTTO = keyOf("caves/pale_grotto");

    public enum PlacedBiomeType {
        SKY, SURFACE, SURFACE_CAVE, CAVE, DEEP_CAVE, GENERIC_CAVE, GENERIC_DEEP_CAVE
    }

    public static PlacedBiome[] landBiomesToPlaceInOverorld = new PlacedBiome[]{
            // Add here new biomes to spawn on the surface
    };

    public static PlacedBiome[] caveBiomesToPlaceInOverorld = new PlacedBiome[]{
            // Add here new cave biomes
            // Vanilla's ice caves (26.4) where the frosted caves were, under the coldest land.
            new PlacedBiome(Biomes.ICE_CAVES, PlacedBiomeType.SURFACE_CAVE, false, Climate.Parameter.span(-1.0f, -0.4f), defaultParameter, Climate.Parameter.span(-0.1f, 1.0f), Climate.Parameter.span(-1.0f, -0.65f), Climate.Parameter.span(-1.0f, -0.2f)),
            new PlacedBiome(Biomes.ICE_CAVES, PlacedBiomeType.SURFACE_CAVE, false, Climate.Parameter.span(-1.0f, -0.4f), defaultParameter, Climate.Parameter.span(-0.1f, 1.0f), Climate.Parameter.span(-1.0f, -0.65f), Climate.Parameter.span(0.2f, 1.0f)),
            new PlacedBiome(Biomes.ICE_CAVES, PlacedBiomeType.SURFACE_CAVE, false, Climate.Parameter.span(-1.0f, -0.6f), defaultParameter, defaultParameter, defaultParameter, defaultParameter),
            new PlacedBiome(BADLANDS_CAVE, PlacedBiomeType.SURFACE_CAVE, true, Climate.Parameter.span(0.8f, 1.0f), Climate.Parameter.span(-1.0f, -0.0f), defaultParameter, defaultParameter, defaultParameter),
            new PlacedBiome(DESERT_CREEPER_CAVE, PlacedBiomeType.CAVE, true, Climate.Parameter.span(0.8f, 1.0f), Climate.Parameter.span(-1.0f, -0.5f), Climate.Parameter.span(0.0f, 0.5f), Climate.Parameter.span(0.1f, 1.0f), Climate.Parameter.span(-1.0f, -0.3f)),
            new PlacedBiome(DESERT_CREEPER_CAVE, PlacedBiomeType.CAVE, true, Climate.Parameter.span(0.8f, 1.0f), Climate.Parameter.span(-1.0f, -0.5f), Climate.Parameter.span(0.0f, 0.5f), Climate.Parameter.span(0.1f, 1.0f), Climate.Parameter.span(0.2f, 1.0f)),
            new PlacedBiome(CREEPER_CAVE, PlacedBiomeType.CAVE, true, Climate.Parameter.span(-0.3f, 0.7f), Climate.Parameter.span(-0.4f, 1.1f), Climate.Parameter.span(0.35f, 0.5f), Climate.Parameter.span(-0.1f, 1.0f), Climate.Parameter.span(-0.75f, -0.6f)),
            new PlacedBiome(Biomes.LUSH_CAVES, PlacedBiomeType.CAVE, false, Climate.Parameter.span(-0.3f, 0.7f), Climate.Parameter.span(-0.4f, 1.1f), Climate.Parameter.span(0.35f, 0.5f), Climate.Parameter.span(-0.1f, 1.0f), Climate.Parameter.span(0.4f, 0.8f)),
            new PlacedBiome(Biomes.LUSH_CAVES, PlacedBiomeType.SURFACE_CAVE, true, Climate.Parameter.span(-0.3f, 0.7f), Climate.Parameter.span(-0.4f, 1.1f), Climate.Parameter.span(0.35f, 0.5f), Climate.Parameter.span(-0.1f, 1.0f), Climate.Parameter.span(-0.4f, -0.2f)),
            new PlacedBiome(Biomes.DRIPSTONE_CAVES, PlacedBiomeType.CAVE, true, Climate.Parameter.span(0.0f, 0.7f), defaultParameter, Climate.Parameter.span(0.4f, 1.0f), Climate.Parameter.span(-0.8f, 0.0f), Climate.Parameter.span(-0.75f, 0.3f)),
            new PlacedBiome(Biomes.DEEP_DARK, PlacedBiomeType.DEEP_CAVE, false, defaultParameter, defaultParameter, defaultParameter, Climate.Parameter.span(-1.0f, -0.8f), defaultParameter),
            new PlacedBiome(PALE_GROTTO, PlacedBiomeType.CAVE, true, Climate.Parameter.span(-0.45f, 0.2f), Climate.Parameter.span(0.275f, 1.0f), Climate.Parameter.span(0.0f, 1.0f), Climate.Parameter.span(-0.4f, 1.0f), defaultParameter),

            new PlacedBiome(REGULAR_CAVE, PlacedBiomeType.GENERIC_CAVE, false, Climate.Parameter.span(-0.7f, 1.0f), defaultParameter, defaultParameter, defaultParameter, defaultParameter),
            new PlacedBiome(DEEP_CAVES, PlacedBiomeType.GENERIC_DEEP_CAVE, false, defaultParameter, defaultParameter, defaultParameter, defaultParameter, defaultParameter)
    };


    private static final Map<ResourceKey<Biome>, ResourceKey<Biome>> MOUNTAIN_BIOME_REPLACEMENTS = Map.ofEntries(
            Map.entry(Biomes.OLD_GROWTH_BIRCH_FOREST, BIRCH_TAIGA_FOREST),
            Map.entry(Biomes.BIRCH_FOREST, BIRCH_TAIGA_FOREST),
            Map.entry(Biomes.DARK_FOREST, DARK_TAIGA_FOREST),
            Map.entry(Biomes.JUNGLE, MOUNTAINSIDE_JUNGLE),
            Map.entry(Biomes.SPARSE_JUNGLE, MOUNTAINSIDE_JUNGLE),
            Map.entry(Biomes.BAMBOO_JUNGLE, MOUNTAINSIDE_JUNGLE),
            Map.entry(Biomes.TAIGA, Biomes.WINDSWEPT_FOREST),
            Map.entry(Biomes.FOREST, MOUNTAINSIDE_FOREST),
            Map.entry(Biomes.SNOWY_TAIGA, Biomes.GROVE),
            Map.entry(Biomes.PLAINS, Biomes.MEADOW),
            Map.entry(Biomes.SNOWY_PLAINS, Biomes.SNOWY_SLOPES),
            Map.entry(Biomes.SAVANNA, MOUNTAINSIDE_FOREST_WARM),
            Map.entry(Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA));

    private static final Map<ResourceKey<Biome>, ResourceKey<Biome>> FROZEN_BIOME_REPLACEMENTS = Map.ofEntries(
            Map.entry(Biomes.SNOWY_BEACH, COLD_BEACH),
            Map.entry(Biomes.TAIGA, COLD_TAIGA),
            Map.entry(Biomes.FOREST, Biomes.TAIGA),
            Map.entry(Biomes.BIRCH_FOREST, BIRCH_TAIGA_FOREST),
            Map.entry(Biomes.SNOWY_TAIGA, COLD_TAIGA),
            Map.entry(Biomes.PLAINS, Biomes.MEADOW),
            Map.entry(Biomes.SNOWY_PLAINS, COLD_PLAINS),
            Map.entry(Biomes.FROZEN_OCEAN, Biomes.COLD_OCEAN),
            Map.entry(Biomes.ICE_SPIKES, Biomes.SNOWY_PLAINS));

    private static final Map<ResourceKey<Biome>, ResourceKey<Biome>> HUMID_BIOME_REPLACEMENTS = Map.ofEntries(
            Map.entry(Biomes.BIRCH_FOREST, DARK_BIRCH_FOREST),
            Map.entry(Biomes.DARK_FOREST, DARK_BIRCH_FOREST),
            Map.entry(Biomes.JUNGLE, Biomes.SPARSE_JUNGLE),
            Map.entry(Biomes.DESERT, DESERT_OASIS),
            Map.entry(Biomes.BADLANDS, Biomes.DESERT));

    private static final Map<ResourceKey<Biome>, ResourceKey<Biome>> SURFACE_CAVE_BIOME_REPLACEMENTS = Map.ofEntries(
            Map.entry(Biomes.FROZEN_PEAKS, Biomes.ICE_CAVES),
            Map.entry(Biomes.SNOWY_SLOPES, Biomes.ICE_CAVES),
            Map.entry(Biomes.DESERT, DESERT_CREEPER_CAVE),
            Map.entry(Biomes.BADLANDS, BADLANDS_CAVE),
            Map.entry(Biomes.ERODED_BADLANDS, BADLANDS_CAVE),
            Map.entry(Biomes.WOODED_BADLANDS, BADLANDS_CAVE));

    @Nullable
    public static ResourceKey<Biome> getMountainTransitionBiome(ResourceKey<Biome> originalBiome) {
        return MOUNTAIN_BIOME_REPLACEMENTS.get(originalBiome);
    }

    @Nullable
    public static ResourceKey<Biome> getSnowyToTemperateTransitionBiome(ResourceKey<Biome> originalBiome) {
        return FROZEN_BIOME_REPLACEMENTS.get(originalBiome);
    }

    @Nullable
    public static ResourceKey<Biome> getHumidTransitionBiome(ResourceKey<Biome> originalBiome) {
        return HUMID_BIOME_REPLACEMENTS.get(originalBiome);
    }

    @Nullable
    public static ResourceKey<Biome> getSurfaceCaveBiome(ResourceKey<Biome> originalBiome) {
        return SURFACE_CAVE_BIOME_REPLACEMENTS.get(originalBiome);
    }

    private static ResourceKey<Biome> keyOf(String id) {
        return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, id));
    }

    public record PlacedBiome(ResourceKey<Biome> biome, PlacedBiomeType type, boolean isRare,
                              Climate.Parameter temperature,
                              Climate.Parameter humidity,
                              Climate.Parameter continentalness,
                              Climate.Parameter erosion,
                              Climate.Parameter weirdness){}
}
