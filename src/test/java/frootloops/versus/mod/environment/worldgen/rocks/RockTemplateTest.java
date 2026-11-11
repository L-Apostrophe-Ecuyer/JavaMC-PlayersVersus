package frootloops.versus.mod.environment.worldgen.rocks;

import com.google.gson.JsonParser;
import frootloops.versus.mod.environment.worldgen.rocks.RockPlan.Body;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RockTemplateTest {

    private static final String MONOLITH_AND_BOULDERS = """
            {"bodies": [
              {"shape": "monolith", "height": [26, 38], "radius": [8, 11]},
              {"shape": "boulder", "count": [2, 5], "distance": [9, 14], "height": [2, 4], "radius": [2, 3.5]}
            ]}""";

    private static RockTemplate template(String json) {
        return RockTemplate.fromJson(JsonParser.parseString(json).getAsJsonObject());
    }

    @Test
    void aShapeAloneUsesItsOwnSizes() {
        RockPlan plan = template("{\"bodies\": [{\"shape\": \"boulder\"}]}").plan(1);
        assertEquals(1, plan.bodies().size());
        Body boulder = plan.bodies().getFirst();
        assertTrue(boulder.lobes().size() >= 2, "a boulder is two stones or more");
        double height = boulder.lobes().getFirst().height();
        assertTrue(height >= 4 && height <= 8, "a boulder left to itself is a few blocks tall, not " + height);
        assertEquals(0, boulder.dx());
        assertEquals(0, boulder.dz());
        assertEquals(RockPalette.GRAY, plan.palette());
    }

    @Test
    void sizesComeFromTheRangesGiven() {
        RockTemplate template = template(MONOLITH_AND_BOULDERS);
        Set<Long> heights = new HashSet<>();
        for (long seed = 1; seed <= 20; seed++) {
            RockPlan.Lobe main = template.plan(seed).bodies().getFirst().lobes().getFirst();
            assertTrue(main.height() >= 26 && main.height() <= 38, "the monolith's height is in its range: " + main.height());
            assertTrue(main.radius() >= 8 && main.radius() <= 11, "the monolith's radius is in its range: " + main.radius());
            heights.add(Math.round(main.height()));
        }
        assertTrue(heights.size() >= 4, "seeds pick different heights: " + heights);
    }

    @Test
    void countsAndDistancesPlaceBodiesAround() {
        RockTemplate template = template(MONOLITH_AND_BOULDERS);
        Set<Integer> counts = new HashSet<>();
        for (long seed = 1; seed <= 20; seed++) {
            RockPlan plan = template.plan(seed);
            counts.add(plan.bodies().size());
            assertTrue(plan.bodies().size() >= 3 && plan.bodies().size() <= 6, "a monolith and two to five boulders, not " + plan.bodies().size());
            for (Body body : plan.bodies().subList(1, plan.bodies().size())) {
                double away = Math.hypot(body.dx(), body.dz());
                assertTrue(away >= 8.2 && away <= 14.8, "a boulder " + away + " blocks out, asked 9 to 14");
            }
        }
        assertTrue(counts.size() >= 3, "seeds pick different numbers of boulders: " + counts);
    }

    @Test
    void anAngleSaysWhereAroundTheMiddle() {
        // Degrees from east (+x) toward south (+z); without one, copies are spread around
        RockTemplate template = template("""
                {"bodies": [{"shape": "monolith"},
                  {"shape": "boulder", "distance": 10, "angle": 90},
                  {"shape": "boulder", "distance": 20, "angle": 180}]}""");
        for (long seed = 1; seed <= 5; seed++) {
            RockPlan plan = template.plan(seed);
            assertEquals(0, plan.bodies().get(1).dx());
            assertEquals(10, plan.bodies().get(1).dz());
            assertEquals(-20, plan.bodies().get(2).dx());
            assertEquals(0, plan.bodies().get(2).dz());
        }
        assertNames("bodies[0].angle", "{\"bodies\": [{\"shape\": \"crag\", \"angle\": \"north\"}]}");
    }

    @Test
    void theSameSeedGivesTheSamePlan() {
        RockTemplate template = template(MONOLITH_AND_BOULDERS);
        assertEquals(template.plan(77), template.plan(77));
        assertFalse(template.plan(77).equals(template.plan(78)), "another seed, another plan");
    }

    @Test
    void numbersGivenReplaceTheShapesOwn() {
        Body bare = template("{\"bodies\": [{\"shape\": \"monolith\", \"cover\": 0, \"vines\": 0, \"slabs\": true, \"scree\": 1}]}").plan(3).bodies().getFirst();
        assertEquals(0, bare.cover());
        assertEquals(0, bare.vines());
        assertTrue(bare.slabs());
        assertEquals(1, bare.scree());
        Body usual = template("{\"bodies\": [{\"shape\": \"monolith\"}]}").plan(3).bodies().getFirst();
        assertTrue(usual.cover() > 0.3, "a monolith left to itself has plants on it");
    }

    @Test
    void aPaletteNamesItsBlocks() {
        RockPlan plan = template("""
                {"bodies": [{"shape": "crag"}], "palette": {"shade": "minecraft:deepslate", "light": "minecraft:diorite",
                 "slabs": {"minecraft:diorite": "minecraft:diorite_slab"}, "plants": ["minecraft:dead_bush"], "vine": "minecraft:glow_lichen"}}""").plan(1);
        assertEquals("minecraft:deepslate", plan.palette().block(RockMaterial.SHADE));
        assertEquals("minecraft:diorite", plan.palette().block(RockMaterial.LIGHT));
        assertEquals("minecraft:stone", plan.palette().block(RockMaterial.BASE), "what isn't named stays gray stone's");
        assertEquals("minecraft:diorite_slab", plan.palette().slab(RockMaterial.LIGHT));
        assertEquals(java.util.List.of("minecraft:dead_bush"), plan.palette().plants());
        assertEquals("minecraft:glow_lichen", plan.palette().vine());
    }

    @Test
    void mistakesNameTheirField() {
        assertNames("bodies[0].shape", "{\"bodies\": [{\"shape\": \"pebble\"}]}");
        assertNames("bodies[1].height", "{\"bodies\": [{\"shape\": \"crag\"}, {\"shape\": \"boulder\", \"height\": [5, 2]}]}");
        assertNames("bodies[0].hieght", "{\"bodies\": [{\"shape\": \"crag\", \"hieght\": 4}]}");
        assertNames("bodies[0].count", "{\"bodies\": [{\"shape\": \"crag\", \"count\": \"many\"}]}");
        assertNames("bodies[0].radius", "{\"bodies\": [{\"shape\": \"crag\", \"radius\": -3}]}");
        assertNames("bodies", "{\"bodies\": []}");
        assertNames("bodies", "{}");
        assertNames("palette.colour", "{\"bodies\": [{\"shape\": \"crag\"}], \"palette\": {\"colour\": \"gray\"}}");
        assertNames("palette.rubble", "{\"bodies\": [{\"shape\": \"crag\"}], \"palette\": {\"rubble\": \"minecraft:cobblestone\"}}");
        assertNames("bodies[0].weathering", "{\"bodies\": [{\"shape\": \"crag\", \"weathering\": 0.5}]}");
        assertNames("pallete", "{\"bodies\": [{\"shape\": \"crag\"}], \"pallete\": {}}");
    }

    private static void assertNames(String field, String json) {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> template(json), json);
        assertTrue(thrown.getMessage().startsWith(field + ":"), "the message starts with the field " + field + ": " + thrown.getMessage());
    }

    @Test
    void itsReachCoversAllItStamps() {
        RockTemplate template = template(MONOLITH_AND_BOULDERS);
        assertTrue(template.reach() > 14 && template.reach() < 40, "a monolith with boulders out to 14 blocks reaches " + template.reach());
        for (long seed = 1; seed <= 12; seed++) {
            RockTestCanvas canvas = new RockTestCanvas((x, z) -> 64, RockTestCanvas.NO_WATER);
            RockStamper.stamp(template.plan(seed), 0, 0, canvas, -1000, -1000, 1000, 1000);
            assertTrue(canvas.cells.size() > 2000, "the formation is stamped");
            for (long key : canvas.cells.keySet()) {
                int far = Math.max(Math.abs(RockTestCanvas.x(key)), Math.abs(RockTestCanvas.z(key)));
                assertTrue(far <= template.reach(), "seed " + seed + ": wrote " + far + " blocks out, past the template's reach of " + template.reach());
            }
        }
    }

    @Test
    void smallRocksFitAFeature() {
        // What a feature may hold: everything within 15 blocks of its origin
        for (String shape : new String[]{"boulder", "pile", "crag"}) {
            assertTrue(template("{\"bodies\": [{\"shape\": \"" + shape + "\"}]}").reach() <= 15, shape + " left to itself fits a feature");
        }
        assertTrue(template("{\"bodies\": [{\"shape\": \"massif\"}]}").reach() > 15, "a massif doesn't");
    }

    @Test
    void backToJsonAndAgainIsTheSameTemplate() {
        RockTemplate template = template(MONOLITH_AND_BOULDERS);
        RockTemplate again = RockTemplate.fromJson(template.toJson());
        assertTrue(template.plan(5).bodies().size() >= 3, "the plan has its bodies");
        assertEquals(template.plan(5), again.plan(5));
        assertEquals(template.reach(), again.reach());
    }
}
