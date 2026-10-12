package frootloops.versus.mod.items_and_effects.recipes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

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
        // Not a family's: the recipe keeps its own group
        assertNull(RecipeRules.group("white_wool"));
        assertNull(RecipeRules.group("torch"));
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

    /** The recipe file mixin applies on 26.4: its handler is in the class that reads registry files. */
    @Test
    void theRecipeFileMixinApplies() throws ClassNotFoundException {
        Class<?> target = Class.forName("net.minecraft.resources.RegistryLoadTask$PendingRegistration");
        assertTrue(Arrays.stream(target.getDeclaredMethods()).map(Method::getName).anyMatch(name -> name.endsWith("playersVersus$recipeRules")));
    }
}
