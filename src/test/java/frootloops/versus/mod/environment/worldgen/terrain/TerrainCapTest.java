package frootloops.versus.mod.environment.worldgen.terrain;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import frootloops.versus.mod.environment.worldgen.WorldgenTestData;
import frootloops.versus.mod.environment.worldgen.terrain.TerrainSamples.Column;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Beta-style heights: in the 3D-noise zone the base noise fades out between y 100 and 128, so none of the zone's ground
 * stands over y 128 (the noise alone used to lift pieces of it, the world's top fading only from y 240); elsewhere the
 * terrain is what it was.
 */
class TerrainCapTest {

    private static final RandomState STATE = TerrainSamples.state();
    private static final DensityFunction FINAL_DENSITY = WorldgenTestData.router().finalDensity();

    private static List<Column> zone;
    private static int[] tops;

    @BeforeAll
    static void sample() {
        zone = TerrainSamples.inTheZone(21L, 50, 8);
        tops = new int[zone.size()];
        for (int i = 0; i < tops.length; i++) tops[i] = top(zone.get(i));
        Arrays.sort(tops);
    }

    /** The highest y, in steps of 2 from 255 down, where the final density is positive (ground), or 0. */
    private static int top(Column column) {
        for (int y = 255; y > 0; y -= 2) {
            if (STATE.sampleBlockValueUncached(FINAL_DENSITY, column.x(), y, column.z()) > 0) return y;
        }
        return 0;
    }

    /** The mechanism: over the cap the zone's sloped cheese has no base noise in it at all. */
    @Test
    void theBaseNoiseIsGoneOverTheCapInTheZone() {
        DensityFunction smooth = DensityFunction.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, WorldgenTestData.registries()), JsonParser.parseString("""
                {"type": "minecraft:min", "left": 1.0, "right": {"type": "minecraft:mul", "left": {"type": "minecraft:quarter_negative", "input": {"type": "minecraft:mul",
                    "left": {"type": "minecraft:add", "left": "players-versus:overworld/depth", "right": {"type": "minecraft:mul", "left": "minecraft:overworld/jaggedness",
                      "right": {"type": "minecraft:half_negative", "input": {"type": "minecraft:noise", "noise": "minecraft:jagged", "xz_scale": 1500.0, "y_scale": 0.0}}}},
                    "right": "players-versus:overworld/factor"}}, "right": 4.0}}
                """)).getOrThrow();
        DensityFunction cheese = WorldgenTestData.density("players-versus:overworld/sloped_cheese");
        Random random = new Random(9);
        int checked = 0;
        for (Column column : zone) {
            if (Math.abs(column.ridges()) < 0.25) continue;
            for (int i = 0; i < 3; i++) {
                int y = 128 + random.nextInt(120);
                // The window is at least 0.999 here, so a thousandth of the noise (1.5 at most) may be left
                assertEquals(STATE.sampleBlockValueUncached(smooth, column.x(), y, column.z()), STATE.sampleBlockValueUncached(cheese, column.x(), y, column.z()), 2e-3,
                        "the sloped cheese at " + column.x() + " " + y + " " + column.z());
                checked++;
            }
        }
        assertTrue(checked >= 200, "only " + checked + " columns checked");
    }

    @Test
    void nothingStandsOverTheCapInTheZone() {
        assertTrue(zone.size() >= 300, "only " + zone.size() + " columns in the zone");
        int over = 0;
        StringBuilder where = new StringBuilder();
        for (int i = 0; i < zone.size(); i++) {
            Column column = zone.get(i);
            for (int y = 129; y < 256; y += 3) {
                if (STATE.sampleBlockValueUncached(FINAL_DENSITY, column.x(), y, column.z()) > 0) {
                    over++;
                    if (where.length() < 200) where.append(" (").append(column.x()).append(' ').append(y).append(' ').append(column.z()).append(')');
                    break;
                }
            }
        }
        assertEquals(0, over, over + " of " + zone.size() + " columns have ground over y 128:" + where);
    }

    @Test
    void theZonesTopsAreBetaLike() {
        int n = tops.length;
        System.out.printf("[terrain] the zone's tops over %d columns: min %d, p10 %d, median %d, p90 %d, p99 %d, max %d%n", n, tops[0], tops[n / 10], tops[n / 2], tops[n * 9 / 10], tops[n * 99 / 100], tops[n - 1]);
        // Beta's hills: above the sea (64) mostly, hills to the 100s, rugged (a spread of 25 blocks or more between the tenth and the ninetieth percentile; plain terrain's is 10)
        assertTrue(tops[n / 2] >= 65 && tops[n / 2] <= 90, "the median top is " + tops[n / 2]);
        assertTrue(tops[n * 9 / 10] - tops[n / 10] >= 25, "the tops spread by only " + (tops[n * 9 / 10] - tops[n / 10]) + " blocks");
        assertTrue(tops[n * 9 / 10] <= 115, "the 90th percentile of the tops is " + tops[n * 9 / 10]);
        assertTrue(tops[n - 1] <= 128, "the highest top is " + tops[n - 1]);
    }

    /** Where the window is 0 the sloped cheese is the old formula's: depth with the peaks, the factor, the base noise, capped at 1 off the rivers. */
    @Test
    void theCapLeavesOtherTerrainAlone() {
        DensityFunction reference = DensityFunction.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, WorldgenTestData.registries()), JsonParser.parseString("""
                {"type": "minecraft:min", "left": 1.0, "right": {"type": "minecraft:add",
                  "left": {"type": "minecraft:mul", "right": 4.0, "left": {"type": "minecraft:quarter_negative", "input": {"type": "minecraft:mul",
                    "left": {"type": "minecraft:add", "left": "players-versus:overworld/depth", "right": {"type": "minecraft:mul", "left": "minecraft:overworld/jaggedness",
                      "right": {"type": "minecraft:half_negative", "input": {"type": "minecraft:noise", "noise": "minecraft:jagged", "xz_scale": 1500.0, "y_scale": 0.0}}}},
                    "right": "players-versus:overworld/factor"}}},
                  "right": "minecraft:overworld/base_3d_noise"}}
                """)).getOrThrow();
        DensityFunction cheese = WorldgenTestData.density("players-versus:overworld/sloped_cheese");
        List<Column> outside = TerrainSamples.random(31L, 6_000);
        Random random = new Random(5);
        int checked = 0;
        for (Column column : outside) {
            if (column.zone() != 0 || Math.abs(column.ridges()) < 0.25) continue;
            int y = 50 + random.nextInt(200);
            float expected = STATE.sampleBlockValueUncached(reference, column.x(), y, column.z());
            float actual = STATE.sampleBlockValueUncached(cheese, column.x(), y, column.z());
            assertEquals(expected, actual, 1e-4, "the sloped cheese at " + column.x() + " " + y + " " + column.z());
            checked++;
        }
        assertTrue(checked >= 1_000, "only " + checked + " columns checked");
    }
}
