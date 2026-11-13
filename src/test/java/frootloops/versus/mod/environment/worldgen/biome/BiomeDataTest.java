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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private static final List<String> NEW_BIOMES = List.of("warm_dark_forest", "grassy_wetlands", "mangrove_marsh", "forest_edge", "mixed_forest",
            "old_oak_forest", "prairie", "pale_taiga", "pale_swamp", "rocky_snowy_plains");
    /** The rock formations (tools/rocks.py), by their structures' names. */
    private static final List<String> FORMATIONS = List.of("monolith", "spires", "towers", "great_boulders", "massif");

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
     * Clumps of waterlogged, persistent leaves float at the water line of the swamps' pools, after the biome's trees and by their
     * noise (the same factor and offset: where the trees are, the clumps are, so the open parts of the wetlands have none far from
     * a tree): oak in the swamp and the grassy wetlands, spruce (mostly) and birch in the cold swamp.
     */
    @Test
    void theSwampsFloatWaterloggedLeavesAfterTheirTreesAndByTheirNoise() throws IOException {
        String[][] swamps = {
                {"minecraft/worldgen/biome/swamp.json", "trees_swamp", "waterlogged_leaves"},
                {"players-versus/worldgen/biome/grassy_wetlands.json", "trees_grassy_wetlands", "waterlogged_leaves_wetlands"},
                {"players-versus/worldgen/biome/cold_swamp.json", "trees_cold_swamp", "waterlogged_leaves_cold"}};
        for (String[] swamp : swamps) {
            String trees = "players-versus:" + swamp[1], leaves = "players-versus:" + swamp[2];
            List<String> plants = vegetation(swamp[0]);
            assertTrue(plants.contains(leaves), swamp[0] + " doesn't list " + leaves + ": " + plants);
            assertTrue(plants.indexOf(leaves) > plants.indexOf(trees), swamp[0] + " lists " + leaves + " before its trees: " + plants);
            assertTrue(placedFeatureExists(leaves), leaves + " has no placed feature");
            JsonObject treeNoise = read(DATA.resolve("players-versus/worldgen/placed_feature/" + swamp[1] + ".json")).getAsJsonArray("placement").get(0).getAsJsonObject();
            JsonObject leafNoise = read(DATA.resolve("players-versus/worldgen/placed_feature/" + swamp[2] + ".json")).getAsJsonArray("placement").get(0).getAsJsonObject();
            assertEquals("minecraft:noise_based_count", leafNoise.get("type").getAsString(), swamp[2] + " isn't placed by the trees' noise");
            assertEquals(treeNoise.get("noise_factor"), leafNoise.get("noise_factor"), swamp[2] + "'s noise factor isn't its trees'");
            assertEquals(treeNoise.get("noise_offset"), leafNoise.get("noise_offset"), swamp[2] + "'s noise offset isn't its trees'");
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

    /**
     * The Tuscan grove is cypresses (a tall column of spruce leaves, taller than an oak; its width is the game test's), oaks in
     * olive-grey leaves and lemon orchards, with fields of flowers and of dry grass.
     */
    @Test
    void theTuscanGroveGrowsCypressesAndHasFieldsOfFlowersAndDryGrass() throws IOException {
        JsonObject cypress = read(DATA.resolve("players-versus/worldgen/feature/cypress.json"));
        assertEquals("minecraft:tree", cypress.get("type").getAsString());
        assertEquals("minecraft:spruce_leaves", cypress.getAsJsonObject("foliage_provider").get("id").getAsString());
        assertEquals("minecraft:spruce_log", cypress.getAsJsonObject("trunk_provider").get("id").getAsString());
        // How narrow it grows is the game test's (TuscanGroveGameTest): a foliage placer can add to the radius written here
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
     * The chance one try of a random selector plants a feature whose id contains {@code part}: the entries are tried in order, so
     * an entry's chance is its own times the chance that none before it was taken.
     */
    private static double chanceOf(JsonObject selector, String part) {
        double none = 1, chance = 0;
        for (JsonElement entry : selector.getAsJsonArray("features")) {
            double own = entry.getAsJsonObject().get("chance").getAsDouble();
            if (entry.getAsJsonObject().get("feature").toString().contains(part)) chance += none * own;
            none *= 1 - own;
        }
        return chance;
    }

    /**
     * The warm dark forest is a dark forest: its tries a chunk, at zero noise, at least three quarters of the dark forest's flat 16
     * (the census, over the same ground, has them as dense), and a third of its giant mushrooms, tried first as the dark forest's are
     * (an entry tried last takes only what the entries before it leave).
     */
    @Test
    void theWarmDarkForestIsAsDenseAsTheDarkForestWithAThirdOfItsMushrooms() throws IOException {
        int flat = vanilla("minecraft/worldgen/placed_feature/dark_forest_vegetation.json").getAsJsonArray("placement").get(0).getAsJsonObject().get("count").getAsInt();
        JsonObject count = read(DATA.resolve("players-versus/worldgen/placed_feature/trees_warm_dark_forest.json")).getAsJsonArray("placement").get(0).getAsJsonObject();
        assertEquals("minecraft:noise_based_count", count.get("type").getAsString());
        double atZeroNoise = Math.ceil(count.get("noise_offset").getAsDouble() * count.get("noise_to_count_ratio").getAsDouble());
        assertTrue(atZeroNoise >= 0.75 * flat, "the warm dark forest tries " + atZeroNoise + " times a chunk at zero noise; the dark forest's flat " + flat);

        double theirs = chanceOf(vanilla("minecraft/worldgen/feature/dark_forest_vegetation.json"), "huge_");
        double ours = chanceOf(read(DATA.resolve("players-versus/worldgen/feature/trees_warm_dark_forest.json")), "huge_");
        assertTrue(ours >= theirs * 0.25 && ours <= theirs * 0.5, "a try plants a giant mushroom " + ours + " of the time; the dark forest's " + theirs + ", a third is asked");
        assertTrue(chanceOf(read(DATA.resolve("players-versus/worldgen/feature/trees_warm_dark_forest.json")), "huge_red") > 0, "no giant red mushrooms");
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

    private static boolean tagged(String tag, String biome) throws IOException {
        Path file = DATA.resolve(tag);
        return Files.exists(file) && read(file).getAsJsonArray("values").contains(new com.google.gson.JsonPrimitive("players-versus:" + biome));
    }

    /**
     * The refinement pass's biomes are what they come from in the tags: the forest edge, the mixed forest and the old oak forest
     * forests, the mixed forest and the pale taiga taigas, the pale ones pale (where the pale mobs come out); the old oak forest
     * grows great oaks, taller than any oak.
     */
    @Test
    void theRefinedBiomesAreTaggedForWhatTheyAre() throws IOException {
        for (String forest : List.of("forest_edge", "mixed_forest", "old_oak_forest")) {
            assertTrue(tagged("minecraft/tags/worldgen/biome/is_forest.json", forest), forest + " isn't in is_forest");
        }
        for (String taiga : List.of("mixed_forest", "pale_taiga")) {
            assertTrue(tagged("minecraft/tags/worldgen/biome/is_taiga.json", taiga), taiga + " isn't in is_taiga");
        }
        for (String pale : List.of("pale_taiga", "pale_swamp")) {
            assertTrue(tagged("players-versus/tags/worldgen/biome/pale.json", pale), pale + " isn't in players-versus:pale");
        }
        assertTrue(tagged("minecraft/tags/worldgen/biome/has_structure/swamp_hut.json", "pale_swamp"), "the pale swamp has no swamp huts");

        JsonObject greatOak = read(DATA.resolve("players-versus/worldgen/feature/great_oak.json"));
        assertEquals("minecraft:tree", greatOak.get("type").getAsString());
        assertEquals("minecraft:oak_log", greatOak.getAsJsonObject("trunk_provider").get("id").getAsString());
        assertTrue(greatOak.getAsJsonObject("trunk_placer").get("base_height").getAsInt() >= 9, "a great oak is taller than a fancy oak");
        assertTrue(placedFeatureExists("players-versus:great_oak_checked"), "players-versus:great_oak_checked is no placed feature");
        assertTrue(Files.readString(DATA.resolve("players-versus/worldgen/feature/trees_old_oak_forest.json")).contains("players-versus:great_oak_checked"),
                "the old oak forest doesn't grow great oaks");
    }

    /**
     * The small rocks (a boulder, a pile or a crag on their own) are the rocky jungle's only: no other biome file lists one, and
     * the placed features that put them in the middle biomes, the meadows, the rocky plains and the rocky birch forest are gone.
     */
    @Test
    void theSmallRocksAreTheRockyJunglesOnly() throws IOException {
        List<String> rocky = new ArrayList<>();
        for (String folder : List.of("players-versus/worldgen/biome", "minecraft/worldgen/biome")) {
            if (!Files.isDirectory(DATA.resolve(folder))) continue;
            try (Stream<Path> files = Files.list(DATA.resolve(folder))) {
                for (Path file : files.filter(f -> f.toString().endsWith(".json")).sorted().toList()) {
                    if (read(file).getAsJsonArray("features").toString().contains("players-versus:rock/")) rocky.add(file.getFileName().toString());
                }
            }
        }
        assertEquals(List.of("rocky_jungle.json"), rocky, "the biomes with small rocks");
        for (String gone : List.of("sparse", "meadow_cluster", "rocky_plains", "rocky_birch_forest")) {
            assertFalse(Files.exists(DATA.resolve("players-versus/worldgen/placed_feature/rock/" + gone + ".json")), "rock/" + gone + " is still a placed feature");
        }
    }

    /**
     * The formations again, capped for the cold (packed ice in place of grass and moss, no plants): as dense as the other rocky
     * biomes' in the rocky snowy plains (in the formations' own structure set), scattered in the cold plains (a set of their
     * own, further apart). The rocky snowy plains have the snowy plains' igloos and snow foxes, and no villages or outposts
     * among their formations.
     */
    @Test
    void theIcyFormationsStandInTheRockySnowyPlainsAndTheColdPlains() throws IOException {
        Path structures = DATA.resolve("players-versus/worldgen/structure/rock"), sets = DATA.resolve("players-versus/worldgen/structure_set");
        String dense = read(sets.resolve("rock_formations.json")).toString();
        JsonObject scattered = read(sets.resolve("scattered_icy_rock_formations.json"));
        for (String name : FORMATIONS) {
            JsonObject gray = read(structures.resolve(name + ".json"));
            for (String[] kind : new String[][]{{"icy_", "icy_rock_formation"}, {"scattered_icy_", "scattered_icy_rock_formation"}}) {
                JsonObject icy = read(structures.resolve(kind[0] + name + ".json"));
                assertEquals("#players-versus:has_structure/" + kind[1], icy.get("biomes").getAsString(), kind[0] + name + "'s biomes");
                assertEquals(gray.getAsJsonObject("rock").get("bodies"), icy.getAsJsonObject("rock").get("bodies"), kind[0] + name + " is shaped as " + name);
                JsonObject palette = icy.getAsJsonObject("rock").getAsJsonObject("palette");
                assertEquals("minecraft:packed_ice", palette.get("grass").getAsString(), kind[0] + name + "'s cover");
                assertEquals("minecraft:packed_ice", palette.get("moss").getAsString(), kind[0] + name + "'s moss");
                assertEquals(0, palette.getAsJsonArray("plants").size(), kind[0] + name + "'s plants");
            }
            assertTrue(dense.contains("\"players-versus:rock/icy_" + name + "\""), "rock_formations doesn't place icy_" + name);
            assertTrue(scattered.toString().contains("\"players-versus:rock/scattered_icy_" + name + "\""), "the scattered set doesn't place scattered_icy_" + name);
        }
        int denseSpacing = read(sets.resolve("rock_formations.json")).getAsJsonObject("placement").get("spacing").getAsInt();
        assertTrue(scattered.getAsJsonObject("placement").get("spacing").getAsInt() > denseSpacing, "the cold plains' formations are scattered");
        assertTrue(tagged("players-versus/tags/worldgen/biome/has_structure/icy_rock_formation.json", "rocky_snowy_plains"), "no icy formations in the rocky snowy plains");
        assertTrue(tagged("players-versus/tags/worldgen/biome/has_structure/scattered_icy_rock_formation.json", "cold_plains"), "no icy formations in the cold plains");
        assertFalse(tagged("players-versus/tags/worldgen/biome/has_structure/icy_rock_formation.json", "cold_plains"), "the cold plains' formations are the scattered ones");
        assertTrue(tagged("minecraft/tags/worldgen/biome/has_structure/igloo.json", "rocky_snowy_plains"), "no igloos in the rocky snowy plains");
        assertTrue(tagged("minecraft/tags/worldgen/biome/spawns_snow_foxes.json", "rocky_snowy_plains"), "no snow foxes in the rocky snowy plains");
        assertFalse(tagged("minecraft/tags/worldgen/biome/has_structure/village_snowy.json", "rocky_snowy_plains"), "a village among the formations");
        assertFalse(tagged("minecraft/tags/worldgen/biome/has_structure/pillager_outpost.json", "rocky_snowy_plains"), "an outpost among the formations");
    }

    /** A mod biome's natural spawns of a category: the kinds (without their namespace) and their weights. */
    private static Map<String, Integer> spawns(String biome, String category) throws IOException {
        JsonObject spawns = read(DATA.resolve("players-versus/worldgen/biome/" + biome + ".json")).getAsJsonObject("attributes")
                .getAsJsonObject("minecraft:gameplay/natural_mob_spawns").getAsJsonObject("argument").getAsJsonObject("spawns_by_category");
        Map<String, Integer> found = new LinkedHashMap<>();
        if (spawns.has(category)) {
            for (JsonElement entry : spawns.getAsJsonArray(category)) {
                found.merge(entry.getAsJsonObject().get("type").getAsString().replace("minecraft:", ""), entry.getAsJsonObject().get("weight").getAsInt(), Integer::sum);
            }
        }
        return found;
    }

    /**
     * Every mod biome's animals are chosen for the place: no cattle or wolves in the warm, wet mangrove marsh; frogs and the
     * swamp's monsters in every wetland; camels, husks and parched in the oasis; horses, donkeys and rabbits on the prairie;
     * goats on the mountainsides and the rocky plains; parrots and ocelots toward the jungles; strays in the cold; no llamas in
     * the woods; no farm animals in the pale biomes.
     */
    @Test
    void theAnimalsAreChosenForThePlace() throws IOException {
        assertTrue(Collections.disjoint(spawns("mangrove_marsh", "creature").keySet(), Set.of("cow", "sheep", "pig", "wolf")),
                "the mangrove marsh's animals: " + spawns("mangrove_marsh", "creature"));
        for (String wetland : List.of("grassy_wetlands", "humid_oak_forest", "cold_swamp", "mangrove_marsh", "pale_swamp")) {
            assertTrue(spawns(wetland, "creature").containsKey("frog"), "no frogs in the " + wetland);
            assertTrue(spawns(wetland, "monster").keySet().containsAll(Set.of("bogged", "slime", "witch")), "the " + wetland + "'s monsters: " + spawns(wetland, "monster"));
        }
        Map<String, Integer> oasis = spawns("desert_oasis", "creature");
        assertTrue(oasis.containsKey("camel") && !oasis.containsKey("pig"), "the oasis's animals: " + oasis);
        assertTrue(spawns("desert_oasis", "monster").keySet().containsAll(Set.of("husk", "parched")), "the oasis's monsters: " + spawns("desert_oasis", "monster"));
        assertTrue(spawns("prairie", "creature").keySet().containsAll(Set.of("horse", "donkey", "rabbit")), "the prairie's animals: " + spawns("prairie", "creature"));
        for (String rocky : List.of("rocky_plains", "rocky_snowy_plains", "mountainside_forest", "mountainside_forest_warm", "mountainside_jungle")) {
            assertTrue(spawns(rocky, "creature").containsKey("goat"), "no goats in the " + rocky);
        }
        for (String jungleSide : List.of("warm_oak_forest", "dark_jungle_forest", "rocky_jungle", "mountainside_jungle")) {
            assertTrue(spawns(jungleSide, "creature").containsKey("parrot"), "no parrots in the " + jungleSide);
            assertTrue(spawns(jungleSide, "monster").containsKey("ocelot"), "no ocelots in the " + jungleSide);
        }
        for (String cold : List.of("cold_plains", "rocky_snowy_plains")) {
            assertTrue(spawns(cold, "monster").containsKey("stray"), "no strays in the " + cold);
        }
        for (String woods : List.of("birch_taiga_forest", "dappled_taiga", "sparse_dappled_forest")) {
            assertFalse(spawns(woods, "creature").containsKey("llama"), "llamas in the " + woods);
        }
        for (String pale : List.of("pale_taiga", "pale_swamp")) {
            assertTrue(Collections.disjoint(spawns(pale, "creature").keySet(), Set.of("cow", "sheep", "pig", "chicken")), "farm animals in the " + pale + ": " + spawns(pale, "creature"));
        }
    }

    /**
     * The animals' variants follow the climate: warm and cold farm animals and frogs by the game's tags, and wolves' coats by its
     * wolf variants (woods in the forests, rusty toward the jungles, ashen in the cold, chestnut by the old taigas), each variant
     * keeping vanilla's own conditions and adding one for the mod's biomes.
     */
    @Test
    void theAnimalsComeInTheirClimatesVariants() throws IOException {
        String tags = "minecraft/tags/worldgen/biome/";
        for (String warm : List.of("desert_oasis", "tuscan_grove", "rocky_plains", "warm_oak_forest", "dark_jungle_forest", "mangrove_marsh", "mountainside_jungle",
                "mountainside_forest_warm")) {
            assertTrue(tagged(tags + "spawns_warm_variant_farm_animals.json", warm), warm + "'s farm animals aren't the warm ones");
            assertTrue(tagged(tags + "spawns_warm_variant_frogs.json", warm), warm + "'s frogs aren't the warm ones");
        }
        for (String cold : List.of("cold_plains", "cold_taiga", "cold_beach", "rocky_snowy_plains", "birch_taiga_forest", "dappled_taiga", "sparse_dappled_forest",
                "dark_taiga_forest", "pale_taiga", "mixed_forest", "rocky_birch_forest", "cold_swamp")) {
            assertTrue(tagged(tags + "spawns_cold_variant_farm_animals.json", cold), cold + "'s farm animals aren't the cold ones");
        }
        for (String cold : List.of("cold_swamp", "cold_plains", "cold_taiga", "rocky_snowy_plains")) {
            assertTrue(tagged(tags + "spawns_cold_variant_frogs.json", cold), cold + "'s frogs aren't the cold ones");
        }
        Map<String, List<String>> coats = Map.of("woods", List.of("forest_edge", "old_oak_forest", "humid_oak_forest", "mixed_forest"),
                "rusty", List.of("mountainside_jungle"), "ashen", List.of("cold_taiga", "cold_plains", "rocky_snowy_plains"),
                "chestnut", List.of("pale_taiga", "dark_taiga_forest"));
        for (Map.Entry<String, List<String>> coat : coats.entrySet()) {
            Path file = DATA.resolve("minecraft/wolf_variant/" + coat.getKey() + ".json");
            assertTrue(Files.exists(file), "the " + coat.getKey() + " wolves have no override");
            JsonArray conditions = read(file).getAsJsonArray("spawn_conditions");
            JsonArray own = vanilla("minecraft/wolf_variant/" + coat.getKey() + ".json").getAsJsonArray("spawn_conditions");
            for (JsonElement condition : own) assertTrue(conditions.contains(condition), coat.getKey() + " lost vanilla's " + condition);
            String all = conditions.toString();
            for (String biome : coat.getValue()) assertTrue(all.contains("\"players-versus:" + biome + "\""), "no " + coat.getKey() + " wolves in the " + biome);
        }
    }

    /** A mod biome's vegetation step, by the features' ids. */
    private static List<String> modVegetation(String biome) throws IOException {
        return vegetation("players-versus/worldgen/biome/" + biome + ".json");
    }

    /**
     * The older biomes have the undergrowth the newer ones have: bushes, leaf litter under broad trees, ferns and berries in the
     * cold woods, fallen logs among their trees; the cold plains grow grass and ferns, not the badlands' dry grass.
     */
    @Test
    void theOlderBiomesHaveTheNewerOnesUndergrowth() throws IOException {
        for (String bushes : List.of("dark_birch_forest", "birch_taiga_forest", "dappled_taiga", "sparse_dappled_forest", "mountainside_forest", "mountainside_forest_warm")) {
            assertTrue(modVegetation(bushes).contains("minecraft:patch_bush"), "no bushes in the " + bushes);
        }
        for (String broad : List.of("dark_birch_forest", "dark_taiga_forest")) {
            assertTrue(modVegetation(broad).contains("minecraft:patch_leaf_litter"), "no leaf litter in the " + broad);
        }
        assertTrue(modVegetation("dark_taiga_forest").containsAll(List.of("minecraft:patch_large_fern", "minecraft:patch_berry_common")),
                "the dark taiga forest's ferns and berries: " + modVegetation("dark_taiga_forest"));
        for (String trees : List.of("dark_birch_forest_vegetation", "dark_taiga_forest_vegetation", "birch_taiga_vegetation", "dappled_taiga_vegetation",
                "sparse_dappled_vegetation")) {
            assertTrue(Files.readString(DATA.resolve("players-versus/worldgen/feature/" + trees + ".json")).contains("\"minecraft:fallen_"), "no fallen logs among " + trees);
        }
        assertFalse(modVegetation("cold_plains").contains("minecraft:patch_grass_badlands"), "the badlands' dry grass in the cold plains");
        assertTrue(modVegetation("cold_plains").contains("minecraft:patch_grass_taiga_2"), "no grass and ferns in the cold plains");
    }

    /** The cold beach is a beach: no flowers, no dry grass, no mushrooms or pumpkins on it. */
    @Test
    void theColdBeachIsABeach() throws IOException {
        List<String> beach = modVegetation("cold_beach");
        for (String inland : List.of("minecraft:flower_default", "minecraft:patch_grass_badlands", "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal",
                "minecraft:patch_pumpkin")) {
            assertFalse(beach.contains(inland), inland + " on the cold beach");
        }
    }

    /** The mangrove marsh's jungle bushes stand on dry land; its mangroves may stand in water. */
    @Test
    void theMangroveMarshsBushesStandOnLand() throws IOException {
        JsonObject trees = read(DATA.resolve("players-versus/worldgen/feature/trees_mangrove_marsh.json"));
        JsonElement bush = trees.get("default");
        assertTrue(bush.isJsonObject() && bush.getAsJsonObject().get("feature").getAsString().equals("minecraft:jungle_bush"), "the marsh's default tree is a jungle bush: " + bush);
        String placement = bush.getAsJsonObject().getAsJsonArray("placement").toString();
        assertTrue(placement.contains("\"minecraft:surface_water_depth_filter\"") && placement.contains("\"max_water_depth\":0"), "the bushes may stand in water: " + placement);
    }

    /** The waterlogged leaves, spread from their trees' places, are checked for their biome where they land too. */
    @Test
    void theWaterloggedLeavesStayInTheirBiome() throws IOException {
        for (String name : List.of("waterlogged_leaves", "waterlogged_leaves_wetlands", "waterlogged_leaves_cold")) {
            JsonArray placement = read(DATA.resolve("players-versus/worldgen/placed_feature/" + name + ".json")).getAsJsonArray("placement");
            int offset = -1, lastBiome = -1;
            for (int index = 0; index < placement.size(); index++) {
                String type = placement.get(index).getAsJsonObject().get("type").getAsString();
                if (type.equals("minecraft:offset")) offset = index;
                if (type.equals("minecraft:biome")) lastBiome = index;
            }
            assertTrue(offset >= 0 && lastBiome > offset, name + ": no biome check after the leaves are spread");
        }
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
