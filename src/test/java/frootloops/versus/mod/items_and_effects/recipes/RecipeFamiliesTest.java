package frootloops.versus.mod.items_and_effects.recipes;

import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The recipe book's collections: what an item made by a recipe is gathered with, from its id. */
class RecipeFamiliesTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    @Test
    void blocksGoWithTheirMaterial() {
        assertEquals("oak", RecipeFamilies.collection("oak_planks"));
        assertEquals("oak", RecipeFamilies.collection("oak_stairs"));
        assertEquals("oak", RecipeFamilies.collection("oak_chest_boat"));
        assertEquals("dark_oak", RecipeFamilies.collection("dark_oak_door"));
        assertEquals("bamboo", RecipeFamilies.collection("bamboo_mosaic_slab"));
        assertEquals("deepslate", RecipeFamilies.collection("cobbled_deepslate_wall"));
        assertEquals("deepslate", RecipeFamilies.collection("deepslate_tile_stairs"));
        assertEquals("deepslate", RecipeFamilies.collection("chiseled_deepslate"));
        assertEquals("tuff", RecipeFamilies.collection("chiseled_tuff_bricks"));
        assertEquals("copper", RecipeFamilies.collection("waxed_weathered_cut_copper_slab"));
        assertEquals("copper", RecipeFamilies.collection("waxed_oxidized_copper_grate"));
        assertEquals("copper", RecipeFamilies.collection("copper_door"));
        assertEquals("quartz", RecipeFamilies.collection("chiseled_quartz_block"));
        assertEquals("stone", RecipeFamilies.collection("mossy_stone_brick_wall"));
        assertEquals("cobblestone", RecipeFamilies.collection("mossy_cobblestone_slab"));
        assertEquals("iron", RecipeFamilies.collection("heavy_weighted_pressure_plate"));
        assertEquals("gold", RecipeFamilies.collection("light_weighted_pressure_plate"));
        // A family's plain block, named after it
        assertEquals("andesite", RecipeFamilies.collection("andesite"));
        assertEquals("sandstone", RecipeFamilies.collection("sandstone"));
        // A colour word that isn't a dye's colour all round: red sandstone is a material
        assertEquals("red_sandstone", RecipeFamilies.collection("red_sandstone_wall"));
        assertEquals("bookshelf", RecipeFamilies.collection("chiseled_bookshelf"));
    }

    @Test
    void toolsAndArmorGoByMaterial() {
        assertEquals("iron_tools", RecipeFamilies.collection("iron_pickaxe"));
        assertEquals("wooden_tools", RecipeFamilies.collection("wooden_sword"));
        assertEquals("golden_tools", RecipeFamilies.collection("golden_spear"));
        assertEquals("diamond_armor", RecipeFamilies.collection("diamond_chestplate"));
        assertEquals("leather_armor", RecipeFamilies.collection("leather_boots"));
    }

    @Test
    void dyedThingsGoByKindWithTheirUndyedOne() {
        assertEquals("dyed:wool", RecipeFamilies.collection("white_wool"));
        assertEquals("dyed:wool", RecipeFamilies.collection("light_blue_wool"));
        assertEquals("dyed:bed", RecipeFamilies.collection("red_bed"));
        assertEquals("dyed:carpet", RecipeFamilies.collection("light_gray_carpet"));
        assertEquals("dyed:dye", RecipeFamilies.collection("black_dye"));
        assertEquals("dyed:stained_glass_pane", RecipeFamilies.collection("magenta_stained_glass_pane"));
        assertEquals("dyed:glazed_terracotta", RecipeFamilies.collection("cyan_glazed_terracotta"));
        assertEquals("dyed:candle", RecipeFamilies.collection("candle"));
        assertEquals("dyed:terracotta", RecipeFamilies.collection("terracotta"));
    }

    @Test
    void theRestStandsAlone() {
        assertNull(RecipeFamilies.collection("torch"));
        assertNull(RecipeFamilies.collection("crafting_table"));
        assertNull(RecipeFamilies.collection("bread"));
    }

    @Test
    void aCollectionHasATitle() {
        assertEquals("Oak", RecipeFamilies.title("oak"));
        assertEquals("Iron Tools", RecipeFamilies.title("iron_tools"));
        assertEquals("Dark Oak", RecipeFamilies.title("dark_oak"));
        assertEquals("Stained Glass Pane", RecipeFamilies.title("dyed:stained_glass_pane"));
    }
}
