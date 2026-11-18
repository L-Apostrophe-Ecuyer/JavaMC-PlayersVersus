package frootloops.versus.mod.environment.blocks.leaves;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cypress leaves are leaves to the renderer too: their texture mipmapped as every vanilla leaves texture is (dark_cutout),
 * so that with fast leaves, which draw every leaves block opaque, their gaps show dark as the spruce leaves' around them
 * do, not filled in with their own green, and they fade with distance as spruce leaves do.
 */
class CypressLeavesDataTest {

    @Test
    void cypressLeavesAreMipmappedAsLeaves() throws IOException {
        Path meta = Path.of("src/main/resources/assets/players-versus/textures/block/cypress_leaves.png.mcmeta");
        assertTrue(Files.exists(meta), "cypress leaves' texture has no metadata");
        JsonObject texture = JsonParser.parseString(Files.readString(meta)).getAsJsonObject().getAsJsonObject("texture");
        assertEquals("dark_cutout", texture == null || !texture.has("mipmap_strategy") ? null : texture.get("mipmap_strategy").getAsString(),
                "cypress leaves aren't mipmapped as leaves");
    }
}
