package frootloops.versus.mod.environment.worldgen.rocks;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The placement filter's settings: rocks ask for a rock's climate unless told not to (a rocky biome's), and ground
 * as level as a rock's unless told how level (a rocky biome's, steeper).
 */
class RockGroundFilterTest {

    private static RockGroundFilter read(String json) {
        return RockGroundFilter.CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    @Test
    void theClimateIsCheckedUnlessSaid() {
        assertTrue(read("{}").climate(), "by default, a rock's climate");
        assertFalse(read("{\"climate\": false}").climate(), "a rocky biome's rocks: level ground only");
    }

    @Test
    void howLevelTheGroundIsUnlessSaid() {
        assertEquals(RockGround.LEVEL, read("{}").level(), "by default, a rock's");
        assertEquals(6, read("{\"level\": 6}").level(), "a rocky biome's: steeper");
        assertTrue(RockGroundFilter.CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString("{\"level\": 17}")).isError(), "no more than the 16 blocks read");
    }
}
