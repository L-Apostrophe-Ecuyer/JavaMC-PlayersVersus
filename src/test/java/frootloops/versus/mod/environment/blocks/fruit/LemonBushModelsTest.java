package frootloops.versus.mod.environment.blocks.fruit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The lemon bush looks like a tiny tree: its lower half a trunk at every stage, its upper half a crown of leaves (boxes,
 * not a cross) wider than the trunk, carrying its stage (bare, blossoms, lemons) on its faces.
 */
class LemonBushModelsTest {

    private static final Path ASSETS = Path.of("src/main/resources/assets/players-versus");
    private static final List<String> STAGES = List.of("", "_blossoms", "_lemons");

    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve(path))).getAsJsonObject();
    }

    private static JsonObject model(String id) throws IOException {
        return read("models/" + id.substring(id.indexOf(':') + 1) + ".json");
    }

    /** The widest an element of the model is, in pixels across x or z. */
    private static double widest(JsonObject model) {
        double widest = 0;
        for (JsonElement element : model.getAsJsonArray("elements")) {
            JsonArray from = element.getAsJsonObject().getAsJsonArray("from"), to = element.getAsJsonObject().getAsJsonArray("to");
            widest = Math.max(widest, Math.max(to.get(0).getAsDouble() - from.get(0).getAsDouble(), to.get(2).getAsDouble() - from.get(2).getAsDouble()));
        }
        return widest;
    }

    @Test
    void theLowerHalfIsATrunkAndTheUpperACrownAtEveryStage() throws IOException {
        JsonObject variants = read("blockstates/lemon_bush.json").getAsJsonObject("variants");
        for (int fruit = 0; fruit < 3; fruit++) {
            assertEquals("players-versus:block/lemon_bush_trunk", variants.getAsJsonObject("fruit=" + fruit + ",half=lower").get("model").getAsString(), "stage " + fruit + "'s lower half");
            assertEquals("players-versus:block/lemon_bush_crown" + STAGES.get(fruit), variants.getAsJsonObject("fruit=" + fruit + ",half=upper").get("model").getAsString(), "stage " + fruit + "'s upper half");
        }
    }

    @Test
    void theCrownIsABushOfLeavesOverAThinTrunk() throws IOException {
        JsonObject trunk = model("players-versus:block/lemon_bush_trunk");
        assertTrue(trunk.has("elements") && widest(trunk) <= 4, "the trunk isn't a thin stem: " + trunk);
        for (int fruit = 0; fruit < 3; fruit++) {
            JsonObject crown = model("players-versus:block/lemon_bush_crown" + STAGES.get(fruit));
            assertNotEquals("minecraft:block/cross", crown.has("parent") ? crown.get("parent").getAsString() : "", "stage " + fruit + "'s crown is a cross");
            assertTrue(crown.has("elements") && crown.getAsJsonArray("elements").size() >= 2 && widest(crown) >= 12, "stage " + fruit + "'s crown isn't a bush: " + crown);
            String textures = crown.getAsJsonObject("textures").toString();
            assertTrue(textures.contains("lemon_bush_leaves"), "stage " + fruit + "'s crown isn't of the bush's leaves: " + textures);
            if (fruit == 1) assertTrue(textures.contains("lemon_leaves_blossoms"), "the blossoming crown has no blossoms: " + textures);
            if (fruit == 2) assertTrue(textures.contains("lemon_leaves_lemons"), "the fruiting crown has no lemons: " + textures);
        }
    }
}
