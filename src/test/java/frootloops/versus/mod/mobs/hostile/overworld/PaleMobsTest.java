package frootloops.versus.mod.mobs.hostile.overworld;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.creaking.Creaking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Creakings hit twice as hard; each pale mob (and the zombie wolf) has its art, egg, names and loot; pale creepers also
 * come up in the Pale Grotto.
 */
class PaleMobsTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final List<String> PALE_MOBS = List.of("pale_creeper", "pale_zombie", "pale_spider", "pale_wolf", "zombie_wolf");

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
        // A wolf's skins: wild, angry and (for one that can be tame) tame; the pale wolf's eyes glow
        for (String skin : List.of("pale_wolf_angry", "pale_wolf_eyes", "zombie_wolf_tame", "zombie_wolf_angry")) {
            assertExists("assets/players-versus/textures/entity/" + skin + ".png");
        }
        for (String namespace : List.of("minecraft", "players-versus")) {
            JsonObject english = read("assets/" + namespace + "/lang/en_us.json");
            for (String mob : PALE_MOBS) {
                assertTrue(english.has("entity.players-versus." + mob), mob + " has no name in " + namespace);
                assertTrue(english.has("item.players-versus." + mob + "_spawn_egg"), mob + "'s egg has no name in " + namespace);
            }
        }
    }

    /**
     * The pale wolf is a skeleton: its body's sides have gaps between the ribs (a rib, then a gap, from the shoulders),
     * where the wolf has fur; the spine down its back is whole.
     */
    @Test
    void thePaleWolfIsASkeleton() throws IOException {
        BufferedImage skin = ImageIO.read(RESOURCES.resolve("assets/players-versus/textures/entity/pale_wolf.png").toFile());
        assertTrue(opaque(skin, 18, 20), "the first rib is missing");
        assertTrue(!opaque(skin, 18, 21) && !opaque(skin, 33, 23), "the rib cage has no gaps");
        for (int row = 20; row <= 28; row++) assertTrue(opaque(skin, 38, row), "the spine breaks at row " + row);
    }

    /** It howls and rattles, hurts and dies with sounds of its own, each with a subtitle. */
    @Test
    void thePaleWolfHowlsAndRattles() throws IOException {
        JsonObject sounds = read("assets/players-versus/sounds.json");
        JsonObject english = read("assets/players-versus/lang/en_us.json");
        for (String sound : List.of("howl", "rattle", "hurt", "death")) {
            String event = "entity.pale_wolf." + sound;
            assertTrue(sounds.has(event), event + " isn't a sound");
            assertTrue(english.has(sounds.getAsJsonObject(event).get("subtitle").getAsString()), event + " has no subtitle");
        }
    }

    private static boolean opaque(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) >>> 24) != 0;
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
