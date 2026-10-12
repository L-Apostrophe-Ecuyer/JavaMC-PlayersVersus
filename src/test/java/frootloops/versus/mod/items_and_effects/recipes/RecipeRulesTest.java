package frootloops.versus.mod.items_and_effects.recipes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeRulesTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    private static JsonObject apply(String json) {
        return RecipeRules.apply(JsonParser.parseString(json)).getAsJsonObject();
    }

    private static String pattern(JsonObject recipe) {
        return recipe.getAsJsonArray("pattern").toString();
    }

    private static int count(JsonObject recipe) {
        return recipe.getAsJsonObject("result").get("count").getAsInt();
    }

    // As 26.4's files have them
    private static final String OAK_STAIRS = """
            {"type": "minecraft:crafting_shaped", "category": "building", "group": "wooden_stairs", "key": {"#": "minecraft:oak_planks"},
             "pattern": ["#  ", "## ", "###"], "result": {"count": 4, "id": "minecraft:oak_stairs"}}""";
    private static final String OAK_SLAB = """
            {"type": "minecraft:crafting_shaped", "category": "building", "group": "wooden_slab", "key": {"#": "minecraft:oak_planks"},
             "pattern": ["###"], "result": {"count": 6, "id": "minecraft:oak_slab"}}""";
    private static final String OAK_PRESSURE_PLATE = """
            {"type": "minecraft:crafting_shaped", "category": "redstone", "group": "wooden_pressure_plate", "key": {"#": "minecraft:oak_planks"},
             "pattern": ["##"], "result": {"id": "minecraft:oak_pressure_plate"}}""";

    @Test
    void stairsTakeThreeBlocksForTheSameFour() {
        JsonObject stairs = apply(OAK_STAIRS);
        assertEquals("[\"# \",\"##\"]", pattern(stairs));
        assertEquals(4, count(stairs));
        assertEquals("building", stairs.get("category").getAsString(), "the recipe book's tab stays vanilla's");
    }

    @Test
    void slabsTakeTwoBlocksForFour() {
        JsonObject slab = apply(OAK_SLAB);
        assertEquals("[\"##\"]", pattern(slab));
        assertEquals(4, count(slab));
    }

    @Test
    void pressurePlatesTakeThreeBlocksForTwo() {
        JsonObject plate = apply(OAK_PRESSURE_PLATE);
        assertEquals("[\"###\"]", pattern(plate));
        assertEquals(2, count(plate));
    }

    @Test
    void aShapeOfItsOwnIsKept() {
        // A data pack's stairs made another way, and a recipe of two ingredients
        JsonObject own = apply("""
                {"type": "minecraft:crafting_shaped", "key": {"#": "minecraft:oak_planks"}, "pattern": ["###", "# #"],
                 "result": {"count": 8, "id": "minecraft:oak_stairs"}}""");
        assertEquals("[\"###\",\"# #\"]", pattern(own));
        assertEquals(8, count(own));
        JsonObject two = apply("""
                {"type": "minecraft:crafting_shaped", "key": {"#": "minecraft:oak_planks", "X": "minecraft:stick"}, "pattern": ["#X#"],
                 "result": {"count": 6, "id": "minecraft:oak_slab"}}""");
        assertEquals("[\"#X#\"]", pattern(two));
    }

    @Test
    void blocksAreGroupedByFamily() {
        assertEquals("oak", apply(OAK_STAIRS).get("group").getAsString());
        assertEquals("oak", RecipeRules.group("oak_planks"));
        assertEquals("oak", RecipeRules.group("oak_chest_boat"));
        assertEquals("dark_oak", RecipeRules.group("dark_oak_door"));
        assertEquals("bamboo", RecipeRules.group("bamboo_mosaic_slab"));
        assertEquals("deepslate", RecipeRules.group("cobbled_deepslate_wall"));
        assertEquals("deepslate", RecipeRules.group("deepslate_tile_stairs"));
        assertEquals("deepslate", RecipeRules.group("chiseled_deepslate"));
        assertEquals("tuff", RecipeRules.group("chiseled_tuff_bricks"));
        assertEquals("tuff", RecipeRules.group("polished_tuff_slab"));
        assertEquals("copper", RecipeRules.group("waxed_weathered_cut_copper_slab"));
        assertEquals("copper", RecipeRules.group("waxed_oxidized_copper_grate"));
        assertEquals("copper", RecipeRules.group("exposed_copper_bulb"));
        assertEquals("copper", RecipeRules.group("copper_door"));
        assertEquals("quartz", RecipeRules.group("chiseled_quartz_block"));
        assertEquals("stone", RecipeRules.group("mossy_stone_brick_wall"));
        assertEquals("cobblestone", RecipeRules.group("mossy_cobblestone_slab"));
        assertEquals("iron", RecipeRules.group("heavy_weighted_pressure_plate"));
        assertEquals("gold", RecipeRules.group("light_weighted_pressure_plate"));
        // A family's plain block, named after it
        assertEquals("andesite", RecipeRules.group("andesite"));
        assertEquals("sandstone", RecipeRules.group("sandstone"));
        // The mod's wool stairs and slabs make wool a family too
        assertEquals("white_wool", RecipeRules.group("white_wool_slab"));
        assertEquals("white_wool", RecipeRules.group("white_wool"));
        // Not a family's: the recipe keeps its own group
        assertNull(RecipeRules.group("torch"));
        assertNull(RecipeRules.group("bookshelf"));
    }

    @Test
    void toolsAndArmorAreGroupedByMaterial() {
        assertEquals("iron_tools", RecipeRules.group("iron_pickaxe"));
        assertEquals("wooden_tools", RecipeRules.group("wooden_sword"));
        assertEquals("golden_tools", RecipeRules.group("golden_spear"));
        assertEquals("diamond_armor", RecipeRules.group("diamond_chestplate"));
        assertEquals("leather_armor", RecipeRules.group("leather_boots"));
    }

    @Test
    void aRecipeNoRuleTouchesIsTheSameFile() {
        JsonElement torch = JsonParser.parseString("""
                {"type": "minecraft:crafting_shaped", "category": "misc", "key": {"#": "minecraft:stick", "X": "minecraft:coal"},
                 "pattern": ["X", "#"], "result": {"count": 4, "id": "minecraft:torch"}}""");
        assertSame(torch, RecipeRules.apply(torch));
    }

    /**
     * A recipe's category is read by its type: crafting recipes know building, redstone, equipment and misc; cooking ones
     * food, blocks and misc. Another one fails to load, and with it the whole recipe registry (a server won't start).
     */
    @Test
    void everyRecipeFileHasACategoryItsTypeKnows() throws IOException {
        Set<String> crafting = Set.of("building", "redstone", "equipment", "misc");
        Set<String> cooking = Set.of("food", "blocks", "misc");
        Set<String> cookingTypes = Set.of("minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking");
        List<String> wrong = new ArrayList<>();
        int checked = 0;
        for (String root : List.of("src/main/resources/data/minecraft/recipe", "src/main/resources/data/players-versus/recipe")) {
            Path directory = Path.of(root);
            if (!Files.isDirectory(directory)) continue;
            try (Stream<Path> files = Files.walk(directory)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                    JsonObject recipe = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                    checked++;
                    if (!recipe.has("category")) continue;
                    String type = recipe.get("type").getAsString();
                    String category = recipe.get("category").getAsString();
                    Set<String> known = cookingTypes.contains(type) ? cooking : type.startsWith("minecraft:crafting") ? crafting : null;
                    if (known != null && !known.contains(category)) wrong.add(file + ": " + category + " in a " + type);
                }
            }
        }
        assertTrue(checked > 100, "the recipe files were found (" + checked + ")");
        assertEquals(List.of(), wrong);
    }

    /** The recipe file mixin applies on 26.4: its handler is in the class that reads registry files. */
    @Test
    void theRecipeFileMixinApplies() throws ClassNotFoundException {
        Class<?> target = Class.forName("net.minecraft.resources.RegistryLoadTask$PendingRegistration");
        assertTrue(Arrays.stream(target.getDeclaredMethods()).map(Method::getName).anyMatch(name -> name.endsWith("playersVersus$recipeRules")));
    }
}
