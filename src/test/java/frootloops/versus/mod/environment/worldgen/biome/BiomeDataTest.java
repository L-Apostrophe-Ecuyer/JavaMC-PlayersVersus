package frootloops.versus.mod.environment.worldgen.biome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    /** The game's own copy of a data file (the mod's files of the same name come first on the classpath, and shadow it). */
    private static JsonObject vanilla(String file) throws IOException {
        java.util.Enumeration<URL> found = BiomeDataTest.class.getClassLoader().getResources("data/" + file);
        while (found.hasMoreElements()) {
            URL url = found.nextElement();
            if (url.getProtocol().equals("jar")) {
                try (java.io.InputStream stream = url.openStream()) {
                    return JsonParser.parseString(new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                }
            }
        }
        throw new IOException("the game has no " + file);
    }

    private static double highest(JsonElement provider) {
        return provider.isJsonPrimitive() ? provider.getAsDouble() : provider.getAsJsonObject().get("max_inclusive").getAsDouble();
    }

    /**
     * The Tuscan grove is cypresses (a narrow column of spruce leaves, taller than an oak), oaks in olive-grey leaves and lemon
     * orchards, with fields of flowers and of dry grass.
     */
    @Test
    void theTuscanGroveGrowsCypressesAndHasFieldsOfFlowersAndDryGrass() throws IOException {
        JsonObject cypress = read(DATA.resolve("players-versus/worldgen/feature/cypress.json"));
        assertEquals("minecraft:tree", cypress.get("type").getAsString());
        assertEquals("minecraft:spruce_leaves", cypress.getAsJsonObject("foliage_provider").get("id").getAsString());
        assertEquals("minecraft:spruce_log", cypress.getAsJsonObject("trunk_provider").get("id").getAsString());
        assertTrue(highest(cypress.getAsJsonObject("foliage_placer").get("radius")) <= 1, "a cypress is a column: leaves one block from its trunk");
        JsonObject trunk = cypress.getAsJsonObject("trunk_placer");
        assertTrue(trunk.get("base_height").getAsInt() >= 10, "a cypress is taller than an oak");
        assertTrue(placedFeatureExists("players-versus:cypress_checked"), "players-versus:cypress_checked is no placed feature");
        assertTrue(Files.readString(DATA.resolve("players-versus/worldgen/feature/trees_tuscan_grove.json")).contains("players-versus:cypress_checked"),
                "the Tuscan grove doesn't grow cypresses");

        JsonObject grove = read(DATA.resolve("players-versus/worldgen/biome/tuscan_grove.json"));
        String leaves = grove.getAsJsonObject("effects").get("foliage_color").getAsString();
        int rgb = Integer.parseInt(leaves.substring(1), 16), red = rgb >> 16 & 255, green = rgb >> 8 & 255, blue = rgb & 255;
        // Olive-grey: green leads, but red is close behind and blue well under both
        assertTrue(green >= red && green - red <= 30 && blue < red - 20, "the oaks' leaves " + leaves + " aren't olive-grey");
        List<String> plants = vegetation("players-versus/worldgen/biome/tuscan_grove.json");
        for (String field : List.of("players-versus:tuscan_flower_field", "players-versus:tuscan_dry_grass")) {
            assertTrue(plants.contains(field), "the Tuscan grove has no " + field + ": " + plants);
            assertTrue(placedFeatureExists(field), field + " is no placed feature");
        }
    }

    /**
     * The warm oak forest, the dark jungle forest and the humid oak forest are denser than they were (their trees' noise ratio
     * up from 12, 16 and 12) and have undergrowth: bushes and ferns right after their trees.
     */
    @Test
    void theWarmForestsAreDenserAndHaveUndergrowth() throws IOException {
        Object[][] forests = {{"warm_oak_forest", 18}, {"dark_jungle_forest", 22}, {"humid_oak_forest", 17}};
        for (Object[] forest : forests) {
            String name = (String) forest[0];
            JsonObject trees = read(DATA.resolve("players-versus/worldgen/placed_feature/trees_" + name + ".json"));
            int ratio = trees.getAsJsonArray("placement").get(0).getAsJsonObject().get("noise_to_count_ratio").getAsInt();
            assertTrue(ratio >= (int) forest[1], name + "'s trees have a noise ratio of " + ratio);
            List<String> plants = vegetation("players-versus/worldgen/biome/" + name + ".json");
            int grown = plants.indexOf("players-versus:trees_" + name);
            for (String undergrowth : List.of("minecraft:patch_bush", "minecraft:patch_large_fern")) {
                assertTrue(plants.indexOf(undergrowth) > grown, name + " has no " + undergrowth + " after its trees: " + plants);
            }
        }
    }

    /** The mangrove swamp's foliage is a clear green in place of vanilla's yellow-brown one: the only thing its file changes. */
    @Test
    void theMangroveSwampKeepsVanillasFileButForItsGreenerFoliage() throws IOException {
        JsonObject ours = read(DATA.resolve("minecraft/worldgen/biome/mangrove_swamp.json")), theirs = vanilla("minecraft/worldgen/biome/mangrove_swamp.json");
        String greener = ours.getAsJsonObject("effects").get("foliage_color").getAsString(), original = theirs.getAsJsonObject("effects").get("foliage_color").getAsString();
        assertNotEquals(original, greener, "the foliage colour is vanilla's");
        int rgb = Integer.parseInt(greener.substring(1), 16);
        assertTrue((rgb >> 8 & 255) - (rgb >> 16 & 255) >= 50, greener + " isn't a clear green: green must lead red by 50");
        ours.getAsJsonObject("effects").addProperty("foliage_color", original);
        assertEquals(theirs, ours, "the mangrove swamp changes more than its foliage colour");
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
