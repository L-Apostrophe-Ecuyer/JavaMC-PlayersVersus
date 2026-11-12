package frootloops.versus.mod.environment.worldgen.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mod's biome files are whole: every feature a biome lists is a placed feature of the mod's data or the game's, the
 * new biomes are in the tags the overworld's biomes need and are named in the languages. (A biome naming a feature that
 * doesn't exist stops a world from loading, at its creation.)
 */
class BiomeDataTest {

    private static final Path DATA = Path.of("src/main/resources/data");
    private static final Path ASSETS = Path.of("src/main/resources/assets/players-versus/lang");
    /** The vegetation step's index in a biome's feature steps. */
    private static final int VEGETATION = 9;
    private static final List<String> NEW_BIOMES = List.of("warm_dark_forest", "grassy_wetlands", "mangrove_marsh");

    private static JsonObject read(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    /** Whether the placed feature {@code id} (namespace:path) is a file of the mod's data or of the game's. */
    private static boolean placedFeatureExists(String id) {
        String[] parts = id.split(":");
        String file = parts[0] + "/worldgen/placed_feature/" + parts[1] + ".json";
        return Files.exists(DATA.resolve(file)) || BiomeDataTest.class.getClassLoader().getResource("data/" + file) != null;
    }

    @Test
    void everyModBiomeNamesFeaturesThatExist() throws IOException {
        List<String> missing = new ArrayList<>();
        int biomes = 0;
        try (Stream<Path> files = Files.list(DATA.resolve("players-versus/worldgen/biome"))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".json")).toList()) {
                biomes++;
                for (JsonElement step : read(file).getAsJsonArray("features")) {
                    for (JsonElement feature : step.isJsonArray() ? step.getAsJsonArray() : new JsonArray()) {
                        if (feature.isJsonPrimitive() && !placedFeatureExists(feature.getAsString())) missing.add(file.getFileName() + ": " + feature.getAsString());
                    }
                }
            }
        }
        assertTrue(biomes > 15, "only " + biomes + " biomes found");
        assertTrue(missing.isEmpty(), "features that don't exist: " + missing);
    }

    /** The vegetation step's features of a biome file (the mod's or an override of the game's), in order. */
    private static List<String> vegetation(String biomeFile) throws IOException {
        List<String> names = new ArrayList<>();
        for (JsonElement feature : read(DATA.resolve(biomeFile)).getAsJsonArray("features").get(VEGETATION).getAsJsonArray()) names.add(feature.getAsString());
        return names;
    }

    /**
     * Clumps of waterlogged, persistent leaves float at the water line of the swamps' pools, after the biome's trees: oak in the
     * swamp and the grassy wetlands, spruce (mostly) and birch in the cold swamp.
     */
    @Test
    void theSwampsFloatWaterloggedLeavesAfterTheirTrees() throws IOException {
        String[][] swamps = {
                {"minecraft/worldgen/biome/swamp.json", "players-versus:trees_swamp", "players-versus:waterlogged_leaves"},
                {"players-versus/worldgen/biome/grassy_wetlands.json", "players-versus:trees_grassy_wetlands", "players-versus:waterlogged_leaves"},
                {"players-versus/worldgen/biome/cold_swamp.json", "players-versus:trees_cold_swamp", "players-versus:waterlogged_leaves_cold"}};
        for (String[] swamp : swamps) {
            List<String> plants = vegetation(swamp[0]);
            assertTrue(plants.contains(swamp[2]), swamp[0] + " doesn't list " + swamp[2] + ": " + plants);
            assertTrue(plants.indexOf(swamp[2]) > plants.indexOf(swamp[1]), swamp[0] + " lists " + swamp[2] + " before its trees: " + plants);
            assertTrue(placedFeatureExists(swamp[2]), swamp[2] + " has no placed feature");
        }
        // Every leaf block of the clumps is waterlogged (it holds the pool's water) and persistent (it would decay away from logs)
        for (String wood : List.of("", "spruce_", "birch_")) {
            JsonObject block = read(DATA.resolve("players-versus/worldgen/feature/waterlogged_" + wood + "leaves.json")).getAsJsonObject("to_place");
            assertEquals("minecraft:" + (wood.isEmpty() ? "oak_" : wood) + "leaves", block.get("id").getAsString());
            assertEquals("true", block.getAsJsonObject("properties").get("waterlogged").getAsString(), wood + "leaves aren't waterlogged");
            assertEquals("true", block.getAsJsonObject("properties").get("persistent").getAsString(), wood + "leaves aren't persistent");
        }
        String cold = Files.readString(DATA.resolve("players-versus/worldgen/feature/waterlogged_leaves_cold.json"));
        assertTrue(cold.contains("players-versus:waterlogged_spruce_leaves") && cold.contains("players-versus:waterlogged_birch_leaves"), "the cold swamp's leaves: " + cold);
    }

    @Test
    void theNewBiomesExistAreTaggedAndNamed() throws IOException {
        JsonArray overworld = read(DATA.resolve("minecraft/tags/worldgen/biome/is_overworld.json")).getAsJsonArray("values");
        JsonArray mineshafts = read(DATA.resolve("minecraft/tags/worldgen/biome/has_structure/mineshaft.json")).getAsJsonArray("values");
        JsonObject english = read(ASSETS.resolve("en_us.json")), french = read(ASSETS.resolve("fr_fr.json"));
        for (String name : NEW_BIOMES) {
            String id = "players-versus:" + name;
            assertTrue(Files.exists(DATA.resolve("players-versus/worldgen/biome/" + name + ".json")), name + " has no biome file");
            assertTrue(overworld.contains(new com.google.gson.JsonPrimitive(id)), name + " isn't in is_overworld");
            assertTrue(mineshafts.contains(new com.google.gson.JsonPrimitive(id)), name + " isn't in has_structure/mineshaft");
            assertTrue(english.has("biome.players-versus." + name), name + " has no English name");
            assertTrue(french.has("biome.players-versus." + name), name + " has no French name");
        }
    }
}
