package frootloops.versus.mod.environment.worldgen.rocks;

import net.minecraft.world.level.biome.Climate;
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

    /**
     * A formation stands where its biomes' tags put it, cold ones too (the rocky snowy plains, the cold plains): the ground
     * decides, not the temperature. Still not on mountainsides, nor in the 3D-noise terrain.
     */
    @Test
    void formationsStandWhateverTheTemperature() {
        assertTrue(RockGround.terrain(-0.3F, 0.2F), "a snowy plain's ground");
        assertTrue(RockGround.terrain(0.35F, 0.4F), "wetter than the 3D-noise terrain");
        assertFalse(RockGround.terrain(0.0F, -0.35F), "a mountainside");
        assertFalse(RockGround.terrain(0.1F, 0.4F), "the 3D-noise terrain");
        assertTrue(RockFormationStructure.suits(Climate.target(-0.8F, -0.3F, 0.3F, 0.2F, 0.0F, 0.0F)), "a formation in a snowy plain's climate");
        assertTrue(RockFormationStructure.suits(Climate.target(0.1F, -0.3F, 0.3F, 0.2F, 0.0F, 0.0F)), "and in a plain's");
        assertFalse(RockFormationStructure.suits(Climate.target(-0.8F, 0.1F, 0.3F, 0.4F, 0.0F, 0.0F)), "not in the 3D-noise terrain's");
        assertFalse(RockFormationStructure.suits(Climate.target(-0.8F, -0.3F, 0.3F, -0.5F, 0.0F, 0.0F)), "nor on a mountainside");
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

    @Test
    void steeperGroundWhereAskedFor() {
        int[] heights = new int[RockGround.AROUND.length];
        Arrays.fill(heights, 64);
        heights[9] = 70;
        assertFalse(RockGround.level(heights), "six blocks up: too steep for a rock here and there");
        assertTrue(RockGround.level(heights, 6), "a rocky biome's: rocks on its hillsides too");
        heights[9] = 71;
        assertFalse(RockGround.level(heights, 6), "seven blocks up");
    }
}
