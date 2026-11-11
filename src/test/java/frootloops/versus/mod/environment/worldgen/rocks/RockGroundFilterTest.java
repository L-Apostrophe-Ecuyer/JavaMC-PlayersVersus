package frootloops.versus.mod.environment.worldgen.rocks;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The placement filter's setting: rocks ask for a rock's climate unless told not to (a meadow's cluster). */
class RockGroundFilterTest {

    private static RockGroundFilter read(String json) {
        return RockGroundFilter.CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    @Test
    void theClimateIsCheckedUnlessSaid() {
        assertTrue(read("{}").climate(), "by default, a rock's climate");
        assertFalse(read("{\"climate\": false}").climate(), "a meadow's cluster: level ground only");
    }
}
