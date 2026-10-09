package frootloops.versus.mod.mobs.hostile.overworld;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.creaking.Creaking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Creakings hit twice as hard; each pale mob has its art, egg, names and loot; pale creepers also come up in the Pale Grotto. */
class PaleMobsTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final List<String> PALE_MOBS = List.of("pale_creeper", "pale_zombie", "pale_spider");

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    @Test
    void creakingsHitTwiceAsHard() {
        assertEquals(2 * 3.0, Creaking.createAttributes().build().getBaseValue(Attributes.ATTACK_DAMAGE));
    }

    @Test
    void paleMobsHaveTheirArtEggsNamesAndLoot() throws IOException {
        for (String mob : PALE_MOBS) {
            assertExists("assets/players-versus/textures/entity/" + mob + ".png");
            assertExists("assets/players-versus/textures/item/" + mob + "_spawn_egg.png");
            assertExists("assets/players-versus/items/" + mob + "_spawn_egg.json");
            assertExists("assets/players-versus/models/item/" + mob + "_spawn_egg.json");
            assertExists("data/players-versus/loot_table/entities/" + mob + ".json");
        }
        assertExists("assets/players-versus/textures/entity/pale_zombie_outer_layer.png");
        assertExists("assets/players-versus/textures/entity/pale_spider_eyes.png");
        for (String namespace : List.of("minecraft", "players-versus")) {
            JsonObject english = read("assets/" + namespace + "/lang/en_us.json");
            for (String mob : PALE_MOBS) {
                assertTrue(english.has("entity.players-versus." + mob), mob + " has no name in " + namespace);
                assertTrue(english.has("item.players-versus." + mob + "_spawn_egg"), mob + "'s egg has no name in " + namespace);
            }
        }
    }

    @Test
    void paleCreepersAlsoComeUpInThePaleGrotto() throws IOException {
        JsonArray monsters = read("data/players-versus/worldgen/biome/caves/pale_grotto.json")
                .getAsJsonObject("attributes").getAsJsonObject("minecraft:gameplay/natural_mob_spawns")
                .getAsJsonObject("argument").getAsJsonObject("spawns_by_category").getAsJsonArray("monster");
        boolean paleCreepers = false;
        for (JsonElement spawn : monsters) {
            paleCreepers |= spawn.getAsJsonObject().get("type").getAsString().equals("players-versus:pale_creeper");
        }
        assertTrue(paleCreepers, "no pale creepers in the Pale Grotto");
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
