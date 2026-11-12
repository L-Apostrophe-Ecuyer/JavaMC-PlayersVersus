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

    /**
     * Along lines out of the zone, over its fades, the ground and the noise's weight change a little at a time: the ground
     * rises 22 blocks (0.17) over a fade of some 30 blocks, as steep as vanilla's own bump edge, and by no more than 0.12
     * in 4 blocks where the climate changes fastest; a step in the data would be the whole 0.17 at once.
     */
    @Test
    void theTransitionIsSmooth() {
        int lines = 0;
        double worstOffset = 0, worstFactor = 0;
        for (Column start : columns) {
            if (start.zone() < 0.999 || start.continentalness() < 0.4) continue;
            for (int direction : new int[]{1, -1}) {
                boolean crossed = false;
                double previousOffset = Double.NaN, previousFactor = Double.NaN;
                for (int step = 0; step <= 150; step++) {
                    int x = start.x() + direction * 4 * step;
                    double zone = TerrainSamples.at(WorldgenTestData.density("players-versus:overworld/noise_zone"), x, start.z());
                    double continents = TerrainSamples.at(WorldgenTestData.router().continents(), x, start.z());
                    double offset = TerrainSamples.at(OFFSET, x, start.z()), factor = TerrainSamples.at(FACTOR, x, start.z());
                    if (continents < 0.3) break;
                    if (zone < 0.999) crossed = true;
                    if (!Double.isNaN(previousOffset)) {
                        worstOffset = Math.max(worstOffset, Math.abs(offset - previousOffset));
                        worstFactor = Math.max(worstFactor, Math.abs(factor - previousFactor));
                    }
                    previousOffset = offset;
                    previousFactor = factor;
                    if (zone == 0) break;
                }
                if (crossed) lines++;
            }
            if (lines >= 40) break;
        }
        System.out.printf("[terrain] %d lines over a fade: the most the offset moves in 4 blocks %.3f, the factor %.2f%n", lines, worstOffset, worstFactor);
        assertTrue(lines >= 20, "only " + lines + " lines crossed a fade");
        assertTrue(worstOffset <= 0.12, "the ground steps by " + worstOffset + " in 4 blocks");
        assertTrue(worstFactor <= 2.5, "the noise's weight steps by " + worstFactor + " in 4 blocks");
    }
}
