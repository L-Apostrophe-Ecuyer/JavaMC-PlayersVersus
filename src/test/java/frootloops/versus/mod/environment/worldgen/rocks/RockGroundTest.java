package frootloops.versus.mod.environment.worldgen.rocks;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Where rock may stand: middle temperatures, above the mountainsides, out of the 3D-noise terrain, on level ground. */
class RockGroundTest {

    @Test
    void rocksStandAtMiddleTemperatures() {
        assertTrue(RockGround.climate(0.1F, 0, 0), "plains and forests");
        assertTrue(RockGround.climate(-0.1F, 0, 0), "a cool forest");
        assertTrue(RockGround.climate(0.5F, 0, 0), "savannas and jungles");
        assertFalse(RockGround.climate(-0.2F, 0, 0), "taigas and colder");
        assertFalse(RockGround.climate(0.6F, 0, 0), "deserts and badlands");
    }

    @Test
    void notOnMountainsides() {
        assertTrue(RockGround.climate(0.1F, 0, -0.25F), "erosion above -0.3");
        assertFalse(RockGround.climate(0.1F, 0, -0.35F), "erosion below it");
    }

    @Test
    void notInTheThreeDimensionalNoise() {
        assertFalse(RockGround.climate(0.1F, 0.1F, 0.4F), "humidity 0 to 0.3 with erosion 0.3 to 0.6");
        assertTrue(RockGround.climate(0.1F, -0.1F, 0.4F), "drier");
        assertTrue(RockGround.climate(0.1F, 0.35F, 0.4F), "wetter");
        assertTrue(RockGround.climate(0.1F, 0.1F, 0.25F), "less eroded");
        assertTrue(RockGround.climate(0.1F, 0.1F, 0.65F), "more eroded");
    }

    @Test
    void levelGroundOnly() {
        assertEquals(17, RockGround.AROUND.length, "the middle, and eight points at each of two distances");
        int[] heights = new int[RockGround.AROUND.length];
        Arrays.fill(heights, 64);
        assertTrue(RockGround.level(heights), "flat");
        heights[5] = 67;
        assertTrue(RockGround.level(heights), "three blocks up somewhere: a gentle slope over 16 blocks");
        heights[5] = 68;
        assertFalse(RockGround.level(heights), "four blocks up");
        heights[5] = 64;
        heights[12] = 60;
        assertFalse(RockGround.level(heights), "four blocks down");
    }
}
