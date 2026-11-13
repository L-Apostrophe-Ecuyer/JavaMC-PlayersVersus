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
    /** Every value the climate's noise can take, beyond the usual -1 to 1. */
    private static final Climate.Parameter anyValue = Climate.Parameter.span(-2.0F, 2.0F);
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
    public static final ResourceKey<Biome> MUSHROOM_CAVES = keyOf("caves/mushroom_caves");
    /** The cool edge of savannas: an open, sunny grove of round oaks and lemon bushes on dry grass, a few acacias. */
    public static final ResourceKey<Biome> TUSCAN_GROVE = keyOf("tuscan_grove");
    /** The cool edge of the drier jungles, next to birch forests and forests: oaks mixed with jungle trees and bushes. */
    public static final ResourceKey<Biome> WARM_OAK_FOREST = keyOf("warm_oak_forest");
    /** The cool edge of the wettest jungles, next to dark forests: dark oaks with jungle trees, vines and cocoa. */
    public static final ResourceKey<Biome> DARK_JUNGLE_FOREST = keyOf("dark_jungle_forest");
    /** The rim of every swamp: oaks with vines on damp ground, mud and puddles. */
    public static final ResourceKey<Biome> HUMID_OAK_FOREST = keyOf("humid_oak_forest");
    /** The dark forest's warm side, next to the dark jungle forest: dark oaks with oaks, fancy oaks and jungle bushes, giant mushrooms. */
    public static final ResourceKey<Biome> WARM_DARK_FOREST = keyOf("warm_dark_forest");
    /** The swamps' drier part: an open wet meadow of pools, channels and grass, few oaks, reeds and flowers. */
    public static final ResourceKey<Biome> GRASSY_WETLANDS = keyOf("grassy_wetlands");
    /** The mangrove swamp's rim and drier part: mud flats and grass, small mangroves and jungle bushes. */
    public static final ResourceKey<Biome> MANGROVE_MARSH = keyOf("mangrove_marsh");
    /** Between plains and forest: oaks and birches in small groves on open grass, bushes and flowers. */
    public static final ResourceKey<Biome> FOREST_EDGE = keyOf("forest_edge");
    /** Between forest and taiga: spruces with oaks and birches, ferns and berries, fallen logs. */
    public static final ResourceKey<Biome> MIXED_FOREST = keyOf("mixed_forest");
    /** A pocket of the forest: great oaks and fancy oaks over moss and azaleas, fallen logs, ferns and mushrooms. */
    public static final ResourceKey<Biome> OLD_OAK_FOREST = keyOf("old_oak_forest");
    /** A pocket of the plains: a sea of tall grass and wildflowers, a lone oak now and then. */
    public static final ResourceKey<Biome> PRAIRIE = keyOf("prairie");
    /** The pale garden's cold edge, next to the old growth taigas: spruces and pale oaks over pale moss. */
    public static final ResourceKey<Biome> PALE_TAIGA = keyOf("pale_taiga");
    /** The wettest, weirdest swamps: swamp oaks and pale oaks, creaking hearts, pale moss at the water's edge. */
    public static final ResourceKey<Biome> PALE_SWAMP = keyOf("pale_swamp");
    /** The cold half of swamps, next to taigas: spruce and birch over mud, podzol and moss, ferns and berries. */
    public static final ResourceKey<Biome> COLD_SWAMP = keyOf("cold_swamp");
    /** The level stretches of savannas' cool edge: dry grass, a few oaks and acacias, and rocks everywhere. */
    public static final ResourceKey<Biome> ROCKY_PLAINS = keyOf("rocky_plains");
    /** The level jungle just past the warm band: jungle trees thinned out among crags and towers of rock. */
    public static final ResourceKey<Biome> ROCKY_JUNGLE = keyOf("rocky_jungle");
    /** The cold, level side of old growth birch forests: tall birches and spruces among boulders. */
    public static final ResourceKey<Biome> ROCKY_BIRCH_FOREST = keyOf("rocky_birch_forest");
    /** The level snowy plains beside the cold plains: rock formations capped with packed ice, a lone spruce now and then. */
    public static final ResourceKey<Biome> ROCKY_SNOWY_PLAINS = keyOf("rocky_snowy_plains");

    /** Where a biome goes; ALL_DEPTHS is a cave biome from the surface caves down to the deep caves. */
    public enum PlacedBiomeType {
        SKY, SURFACE, SURFACE_CAVE, CAVE, DEEP_CAVE, GENERIC_CAVE, GENERIC_DEEP_CAVE, ALL_DEPTHS
    }

    public static PlacedBiome[] landBiomesToPlaceInOverorld = new PlacedBiome[]{
            // Add here new biomes to spawn on the surface
            // The mushroom fields over their caves, whatever the rest of the climate, as the caves below them (vanilla's
            // entry stops at the usual -1 to 1, where the caves' would take the surface)
            new PlacedBiome(Biomes.MUSHROOM_FIELDS, PlacedBiomeType.SURFACE, false, anyValue, anyValue, Climate.Parameter.span(-2.0f, -1.05f), anyValue, anyValue)
    };

    public static PlacedBiome[] caveBiomesToPlaceInOverorld = new PlacedBiome[]{
            // Add here new cave biomes
            // Vanilla's ice caves (26.4) where the frosted caves were, under the coldest land.
            new PlacedBiome(Biomes.ICE_CAVES, PlacedBiomeType.SURFACE_CAVE, false, Climate.Parameter.span(-1.0f, -0.4f), defaultParameter, Climate.Parameter.span(-0.1f, 1.0f), Climate.Parameter.span(-1.0f, -0.65f), Climate.Parameter.span(-1.0f, -0.2f)),
            new PlacedBiome(Biomes.ICE_CAVES, PlacedBiomeType.SURFACE_CAVE, false, Climate.Parameter.span(-1.0f, -0.4f), defaultParameter, Climate.Parameter.span(-0.1f, 1.0f), Climate.Parameter.span(-1.0f, -0.65f), Climate.Parameter.span(0.2f, 1.0f)),
            new PlacedBiome(Biomes.ICE_CAVES, PlacedBiomeType.SURFACE_CAVE, false, Climate.Parameter.span(-1.0f, -0.6f), defaultParameter, defaultParameter, defaultParameter, defaultParameter),
            new PlacedBiome(PALE_GROTTO, PlacedBiomeType.CAVE, true, Climate.Parameter.span(0.0f, 0.3f), Climate.Parameter.span(0.2f, 0.6f), Climate.Parameter.span(0.2f, 1.0f), Climate.Parameter.span(-0.2f, 0.8f), Climate.Parameter.span(0.2f, 1.0f)),
            new PlacedBiome(MUSHROOM_CAVES, PlacedBiomeType.ALL_DEPTHS, false, anyValue, anyValue, Climate.Parameter.span(-2.0f, -1.05f), anyValue, anyValue),
            new PlacedBiome(BADLANDS_CAVE, PlacedBiomeType.SURFACE_CAVE, true, Climate.Parameter.span(0.8f, 1.0f), Climate.Parameter.span(-1.0f, -0.0f), defaultParameter, defaultParameter, defaultParameter),
            new PlacedBiome(DESERT_CREEPER_CAVE, PlacedBiomeType.CAVE, true, Climate.Parameter.span(0.8f, 1.0f), Climate.Parameter.span(-1.0f, -0.5f), Climate.Parameter.span(0.0f, 0.5f), Climate.Parameter.span(0.1f, 1.0f), Climate.Parameter.span(-1.0f, -0.3f)),
            new PlacedBiome(DESERT_CREEPER_CAVE, PlacedBiomeType.CAVE, true, Climate.Parameter.span(0.8f, 1.0f), Climate.Parameter.span(-1.0f, -0.5f), Climate.Parameter.span(0.0f, 0.5f), Climate.Parameter.span(0.1f, 1.0f), Climate.Parameter.span(0.2f, 1.0f)),
            new PlacedBiome(CREEPER_CAVE, PlacedBiomeType.CAVE, true, Climate.Parameter.span(-0.3f, 0.7f), Climate.Parameter.span(-0.4f, 1.1f), Climate.Parameter.span(0.35f, 0.5f), Climate.Parameter.span(-0.1f, 1.0f), Climate.Parameter.span(-0.75f, -0.6f)),
            new PlacedBiome(Biomes.LUSH_CAVES, PlacedBiomeType.CAVE, false, Climate.Parameter.span(-0.3f, 0.7f), Climate.Parameter.span(-0.4f, 1.1f), Climate.Parameter.span(0.35f, 0.5f), Climate.Parameter.span(-0.1f, 1.0f), Climate.Parameter.span(0.4f, 0.8f)),
            new PlacedBiome(Biomes.LUSH_CAVES, PlacedBiomeType.SURFACE_CAVE, true, Climate.Parameter.span(-0.3f, 0.7f), Climate.Parameter.span(-0.4f, 1.1f), Climate.Parameter.span(0.35f, 0.5f), Climate.Parameter.span(-0.1f, 1.0f), Climate.Parameter.span(-0.4f, -0.2f)),
            new PlacedBiome(Biomes.DRIPSTONE_CAVES, PlacedBiomeType.CAVE, true, Climate.Parameter.span(0.0f, 0.7f), defaultParameter, Climate.Parameter.span(0.4f, 1.0f), Climate.Parameter.span(-0.8f, 0.0f), Climate.Parameter.span(-0.75f, 0.3f)),
            new PlacedBiome(Biomes.DEEP_DARK, PlacedBiomeType.DEEP_CAVE, false, defaultParameter, defaultParameter, defaultParameter, Climate.Parameter.span(-1.0f, -0.8f), defaultParameter),

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
            Map.entry(Biomes.SAVANNA_PLATEAU, Biomes.STONY_PEAKS));

    private static final Map<ResourceKey<Biome>, ResourceKey<Biome>> FROZEN_BIOME_REPLACEMENTS = Map.ofEntries(
            Map.entry(Biomes.SNOWY_BEACH,   COLD_BEACH),
            Map.entry(Biomes.TAIGA,         COLD_TAIGA),
            Map.entry(Biomes.FOREST,        Biomes.TAIGA),
            Map.entry(Biomes.BIRCH_FOREST,  BIRCH_TAIGA_FOREST),
            Map.entry(Biomes.SNOWY_TAIGA,   COLD_TAIGA),
            Map.entry(Biomes.PLAINS,        Biomes.MEADOW),
            Map.entry(Biomes.SNOWY_PLAINS,  COLD_PLAINS),
            Map.entry(Biomes.FROZEN_OCEAN,  Biomes.COLD_OCEAN),
            Map.entry(Biomes.ICE_SPIKES,    Biomes.SNOWY_PLAINS));

    private static final Map<ResourceKey<Biome>, ResourceKey<Biome>> HUMID_BIOME_REPLACEMENTS = Map.ofEntries(
            // The dark forest's side of the strip is PvBiomeLayout's dark-birch rule, narrower
            Map.entry(Biomes.BIRCH_FOREST,      DARK_BIRCH_FOREST),
            Map.entry(Biomes.JUNGLE,            Biomes.SPARSE_JUNGLE),
            Map.entry(Biomes.DESERT,            DESERT_OASIS),
            Map.entry(Biomes.BADLANDS,          Biomes.DESERT));

    private static final Map<ResourceKey<Biome>, ResourceKey<Biome>> SURFACE_CAVE_BIOME_REPLACEMENTS = Map.ofEntries(
            Map.entry(Biomes.FROZEN_PEAKS,      Biomes.ICE_CAVES),
            Map.entry(Biomes.SNOWY_SLOPES,      Biomes.ICE_CAVES),
            Map.entry(Biomes.DESERT,            DESERT_CREEPER_CAVE),
            Map.entry(Biomes.BADLANDS,          BADLANDS_CAVE),
            Map.entry(Biomes.ERODED_BADLANDS,   BADLANDS_CAVE),
            Map.entry(Biomes.WOODED_BADLANDS,   BADLANDS_CAVE),
            Map.entry(Biomes.PALE_GARDEN,       PALE_GROTTO),
            Map.entry(Biomes.MUSHROOM_FIELDS,   MUSHROOM_CAVES));

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
