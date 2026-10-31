package frootloops.versus.mod.mobs.hostile.overworld;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The mushroom caves' creepers and spiders have their art, eggs, names and loot, and are all that spawns in the mushroom
 * caves, each costing much of an area's spawn budget so they come rarely.
 */
class MushroomMobsTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final List<String> MUSHROOM_MOBS = List.of("mushroom_creeper", "mushroom_spider");

    @Test
    void mushroomMobsHaveTheirArtEggsNamesAndLoot() throws IOException {
        for (String mob : MUSHROOM_MOBS) {
            assertExists("assets/players-versus/textures/entity/" + mob + ".png");
            assertExists("assets/players-versus/textures/item/" + mob + "_spawn_egg.png");
            assertExists("assets/players-versus/items/" + mob + "_spawn_egg.json");
            assertExists("assets/players-versus/models/item/" + mob + "_spawn_egg.json");
            assertExists("data/players-versus/loot_table/entities/" + mob + ".json");
        }
        for (String namespace : List.of("minecraft", "players-versus")) {
            JsonObject english = read("assets/" + namespace + "/lang/en_us.json");
            for (String mob : MUSHROOM_MOBS) {
                assertTrue(english.has("entity.players-versus." + mob), mob + " has no name in " + namespace);
                assertTrue(english.has("item.players-versus." + mob + "_spawn_egg"), mob + "'s egg has no name in " + namespace);
            }
            assertTrue(english.has("biome.players-versus.caves/mushroom_caves"), "the mushroom caves have no name in " + namespace);
        }
    }

    @Test
    void onlyMushroomMobsSpawnInTheMushroomCavesAndRarely() throws IOException {
        JsonObject spawns = read("data/players-versus/worldgen/biome/caves/mushroom_caves.json")
                .getAsJsonObject("attributes").getAsJsonObject("minecraft:gameplay/natural_mob_spawns").getAsJsonObject("argument");
        Set<String> mobs = new HashSet<>();
        for (var category : spawns.getAsJsonObject("spawns_by_category").entrySet()) {
            for (JsonElement spawn : category.getValue().getAsJsonArray()) {
                assertEquals("monster", category.getKey(), "a " + category.getKey() + " spawns in the mushroom caves");
                mobs.add(spawn.getAsJsonObject().get("type").getAsString());
            }
        }
        assertEquals(Set.of("players-versus:mushroom_creeper", "players-versus:mushroom_spider"), mobs);
        for (String mob : mobs) {
            assertTrue(spawns.getAsJsonObject("spawn_costs").has(mob), mob + " spawns at no cost");
        }
    }

    private static void assertExists(String path) {
        assertTrue(Files.isRegularFile(RESOURCES.resolve(path)), path + " is missing");
    }

    private static JsonObject read(String path) throws IOException {
        try (Reader reader = Files.newBufferedReader(RESOURCES.resolve(path))) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
