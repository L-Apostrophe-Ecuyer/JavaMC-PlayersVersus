package frootloops.versus.mod.environment.worldgen.terrain;

import frootloops.versus.mod.environment.worldgen.WorldgenTestData;
import frootloops.versus.mod.environment.worldgen.terrain.TerrainSamples.Column;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The terrain's shape: vanilla's offset and factor splines hold a bump at erosion 0.40 to 0.58 (the ground about 22
 * blocks higher, the factor from 4.7 to 6.3 down to 1.4 to 1.8, so the 3D noise shows), the mod's own keep it only inside
 * the 3D-noise zone and take it out elsewhere.
 */
class TerrainShapeTest {

    private static final DensityFunction OFFSET = WorldgenTestData.density("players-versus:overworld/offset");
    private static final DensityFunction FACTOR = WorldgenTestData.density("players-versus:overworld/factor");
    private static final DensityFunction VANILLA_OFFSET = WorldgenTestData.density("minecraft:overworld/offset");
    private static final DensityFunction VANILLA_FACTOR = WorldgenTestData.density("minecraft:overworld/factor");

    private static List<Column> columns;

    @BeforeAll
    static void sample() {
        columns = TerrainSamples.random(11L, 200_000);
    }

    private static double offset(Column c) {
        return TerrainSamples.at(OFFSET, c.x(), c.z());
    }

    private static double factor(Column c) {
        return TerrainSamples.at(FACTOR, c.x(), c.z());
    }

    private static double vanillaOffset(Column c) {
        return TerrainSamples.at(VANILLA_OFFSET, c.x(), c.z());
    }

    private static double vanillaFactor(Column c) {
        return TerrainSamples.at(VANILLA_FACTOR, c.x(), c.z());
    }

    /** Where vanilla's splines hold no bump and the window is 0 the mod's are vanilla's: if vanilla's move, this fails. */
    @Test
    void theTerrainOutsideTheBumpIsVanillas() {
        int checked = 0;
        for (Column c : columns) {
            if (c.zone() != 0 || (c.erosion() >= 0.3 && c.erosion() <= 0.63)) continue;
            checked++;
            assertEquals(vanillaOffset(c), offset(c), 1e-4, "offset at " + c);
            assertEquals(vanillaFactor(c), factor(c), 1e-4, "factor at " + c);
            if (checked >= 3_000) break;
        }
        assertTrue(checked >= 3_000, "only " + checked + " columns checked");
    }

    @Test
    void theBumpIsGoneOutsideTheZone() {
        int checked = 0;
        double factors = 0, vanillaFactors = 0;
        for (Column c : columns) {
            if (c.zone() != 0 || c.continentalness() < 0.3 || c.erosion() < 0.47 || c.erosion() > 0.53) continue;
            checked++;
            assertTrue(offset(c) <= -0.40, "the ground is raised outside the zone: offset " + offset(c) + " at " + c);
            factors += factor(c);
            vanillaFactors += vanillaFactor(c);
        }
        assertTrue(checked >= 100, "only " + checked + " columns in the band outside the zone");
        System.out.printf("[terrain] the band outside the zone: %d columns, mean factor %.2f (vanilla's %.2f)%n", checked, factors / checked, vanillaFactors / checked);
        assertTrue(factors / checked >= 4.0, "the noise still shows outside the zone: mean factor " + factors / checked);
    }

    @Test
    void theBumpHoldsInsideTheZone() {
        int plateau = 0, flanks = 0;
        double flankOffsets = 0, flankFactors = 0, vanillaOffsets = 0, vanillaFactors = 0;
        for (Column c : columns) {
            if (c.zone() < 0.999 || c.continentalness() < 0.3) continue;
            if (c.erosion() >= 0.47 && c.erosion() <= 0.53) {
                plateau++;
                assertEquals(vanillaOffset(c), offset(c), 1e-4, "the plateau's offset at " + c);
                assertEquals(vanillaFactor(c), factor(c), 1e-4, "the plateau's factor at " + c);
            } else if (c.erosion() >= 0.37 && c.erosion() <= 0.43) {
                flanks++;
                flankOffsets += offset(c);
                flankFactors += factor(c);
                vanillaOffsets += vanillaOffset(c);
                vanillaFactors += vanillaFactor(c);
            }
        }
        assertTrue(plateau >= 100 && flanks >= 100, "only " + plateau + " plateau columns and " + flanks + " on the flanks");
        System.out.printf("[terrain] in the zone, erosion 0.37 to 0.43: %d columns, mean offset %.3f (vanilla's %.3f), mean factor %.2f (vanilla's %.2f)%n", flanks,
                flankOffsets / flanks, vanillaOffsets / flanks, flankFactors / flanks, vanillaFactors / flanks);
        // Vanilla's terrain there is plain; the zone's is the bump's: higher (by 0.17 off the valleys, 0.06 on average) and rougher
        assertTrue(flankOffsets / flanks >= vanillaOffsets / flanks + 0.04, "the flanks of the zone are plain: mean offset " + flankOffsets / flanks);
        assertTrue(flankFactors / flanks <= vanillaFactors / flanks - 1.0, "the flanks of the zone are smooth: mean factor " + flankFactors / flanks);
    }

    private static double step(DensityFunction function, Column c, int dx, int dz) {
        return Math.abs(TerrainSamples.at(function, c.x() + dx, c.z() + dz) - TerrainSamples.at(function, c.x(), c.z()));
    }

    private static double percentile(List<Double> values, double share) {
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(null);
        return sorted.get((int) Math.min(sorted.size() - 1, Math.round(share * (sorted.size() - 1))));
    }

    /**
     * Over the zone's fades the ground and the noise's weight change no faster than along vanilla's own bump edges
     * (erosion 0.40 to 0.45 and 0.55 to 0.58): the 90th percentile of what the offset and the factor move in 8 blocks, over
     * the columns in the fades and on the bump's slopes, well inland, is at most 1.25 times vanilla's. (Measured: 0.030
     * against 0.034 for the offset, 0.74 against 0.94 for the factor; the humidity noise changes about a seventh faster per
     * block than the erosion's, so a fade of its own width is a seventh steeper, and the fades are as wide as vanilla's.)
     */
    @Test
    void theZonesEdgesAreNoSteeperThanVanillasBump() {
        List<Double> zoneOffsets = new ArrayList<>(), zoneFactors = new ArrayList<>(), vanillaOffsets = new ArrayList<>(), vanillaFactors = new ArrayList<>();
        int alternate = 0;
        for (Column c : columns) {
            if (c.continentalness() < 0.3) continue;
            boolean zoneEdge = c.zone() > 0.02 && c.zone() < 0.98;
            boolean bumpEdge = (c.erosion() > 0.402 && c.erosion() < 0.448) || (c.erosion() > 0.552 && c.erosion() < 0.578);
            if (!zoneEdge && !bumpEdge) continue;
            // Along x and along z by turns
            int dx = alternate++ % 2 == 0 ? 8 : 0, dz = 8 - dx;
            if (zoneEdge) {
                zoneOffsets.add(step(OFFSET, c, dx, dz));
                zoneFactors.add(step(FACTOR, c, dx, dz));
            }
            if (bumpEdge) {
                vanillaOffsets.add(step(VANILLA_OFFSET, c, dx, dz));
                vanillaFactors.add(step(VANILLA_FACTOR, c, dx, dz));
            }
        }
        assertTrue(zoneOffsets.size() >= 300 && vanillaOffsets.size() >= 300, "only " + zoneOffsets.size() + " columns in the zone's fades and " + vanillaOffsets.size() + " on vanilla's bump");
        double zoneOffset = percentile(zoneOffsets, 0.9), vanillaOffset = percentile(vanillaOffsets, 0.9);
        double zoneFactor = percentile(zoneFactors, 0.9), vanillaFactor = percentile(vanillaFactors, 0.9);
        System.out.printf("[terrain] 8-block steps, 90th percentile: offset %.4f in the zone's fades (%d columns), %.4f on vanilla's bump (%d); factor %.3f against %.3f%n",
                zoneOffset, zoneOffsets.size(), vanillaOffset, vanillaOffsets.size(), zoneFactor, vanillaFactor);
        assertTrue(zoneOffset <= 1.25 * vanillaOffset, "the ground steps by " + zoneOffset + " in 8 blocks along the zone's edges, vanilla's bump by " + vanillaOffset);
        assertTrue(zoneFactor <= 1.25 * vanillaFactor, "the noise's weight steps by " + zoneFactor + " in 8 blocks along the zone's edges, vanilla's bump by " + vanillaFactor);
    }
}
