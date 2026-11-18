package frootloops.versus.mod.environment.worldgen.rocks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RockNoiseTest {

    @Test
    void hashesSpreadEvenly() {
        double sum = 0;
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (int i = 0; i < 20_000; i++) {
            long hash = RockNoise.hash(7, i, i % 13, -i);
            seen.add(hash);
            double unit = RockNoise.unit(hash);
            assertTrue(unit >= 0 && unit < 1, "a hash as a number is in [0, 1)");
            sum += unit;
        }
        assertEquals(0.5, sum / 20_000, 0.02, "hashes average a half");
        assertEquals(20_000, seen.size(), "every position has its own hash");
    }

    @Test
    void noiseIsSmoothBoundedAndVaries() {
        double sum = 0, squares = 0;
        int samples = 0;
        for (double x = -40; x < 40; x += 1.37) {
            for (double z = -40; z < 40; z += 1.91) {
                double here = RockNoise.noise(11, x, 0.4 * z, z);
                assertTrue(Math.abs(here) <= 1.05, "noise stays within about -1 to 1: " + here);
                double near = RockNoise.noise(11, x + 0.01, 0.4 * z, z);
                assertTrue(Math.abs(near - here) < 0.05, "a hundredth of a unit away the noise is nearly the same");
                sum += here;
                squares += here * here;
                samples++;
            }
        }
        double mean = sum / samples, spread = Math.sqrt(squares / samples - mean * mean);
        assertEquals(0, mean, 0.05, "noise averages zero");
        assertTrue(spread > 0.15, "noise varies: its spread is " + spread);
        assertTrue(RockNoise.noise(11, 3.3, 1.1, 7.7) != RockNoise.noise(12, 3.3, 1.1, 7.7), "another seed, another noise");
    }

    @Test
    void cellsFindTheNearestSiteAndTheBorder() {
        // Cells four blocks across and nine tall; distances are measured in cells, which is what makes the cells tall
        RockNoise.Cells cells = new RockNoise.Cells(), probe = new RockNoise.Cells();
        int crossed = 0, samples = 0;
        for (double x = -30; x < 30; x += 2.3) {
            for (double z = -30; z < 30; z += 3.1) {
                double y = 0.37 * x - 0.21 * z;
                RockNoise.cells(5, x, y, z, 4, 9, 4, cells);
                assertTrue(cells.f1 <= cells.f2, "the nearest site is no further than the second");
                assertTrue(cells.id1 != cells.id2, "two sites, two cells");
                assertEquals(cells.f1, Math.sqrt(square((x - cells.x1) / 4) + square((y - cells.y1) / 9) + square((z - cells.z1) / 4)), 1e-9, "f1 is the distance to the site given, in cells");
                assertEquals(cells.f2, Math.sqrt(square((x - cells.x2) / 4) + square((y - cells.y2) / 9) + square((z - cells.z2) / 4)), 1e-9, "f2 to the second site");
                // The border lies between half the distances' difference and half their sum away; a cell is 4 to 9 blocks
                assertTrue(cells.edge >= 4 * (cells.f2 - cells.f1) / 2 - 1e-9 && cells.edge <= 9 * (cells.f1 + cells.f2) / 2 + 1e-9, "the border's distance in blocks: " + cells.edge);
                // At the site itself, the site is nearest
                RockNoise.cells(5, cells.x1, cells.y1, cells.z1, 4, 9, 4, probe);
                assertEquals(0, probe.f1, 1e-9);
                assertEquals(cells.id1, probe.id1, "a site is in its own cell");
                // Just past the border toward the second site, that one is nearest (unless a third cell cuts in)
                double ux = (cells.x2 - cells.x1) / 4, uy = (cells.y2 - cells.y1) / 9, uz = (cells.z2 - cells.z1) / 4;
                double between = Math.sqrt(ux * ux + uy * uy + uz * uz);
                double step = (cells.f2 * cells.f2 - cells.f1 * cells.f1) / (2 * between) + 0.02;
                RockNoise.cells(5, x + step * ux / between * 4, y + step * uy / between * 9, z + step * uz / between * 4, 4, 9, 4, probe);
                if (probe.id1 == cells.id2) crossed++;
                samples++;
            }
        }
        assertTrue(crossed > 0.85 * samples, "past the border the other cell begins: " + crossed + " of " + samples);
    }

    @Test
    void theBorderIsAsFarAsItSays() {
        // Walking straight at the border (across it, in blocks) reaches the other cell after the distance given
        RockNoise.Cells cells = new RockNoise.Cells(), probe = new RockNoise.Cells();
        int exact = 0, samples = 0;
        for (double x = -20; x < 20; x += 1.7) {
            for (double z = -20; z < 20; z += 2.9) {
                double y = 0.2 * x + 0.3 * z;
                RockNoise.cells(8, x, y, z, 3, 7, 3, cells);
                // The border's normal in blocks: the direction between the sites in cells, divided again by the cell's size
                double nx = (cells.x2 - cells.x1) / 9, ny = (cells.y2 - cells.y1) / 49, nz = (cells.z2 - cells.z1) / 9;
                double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
                RockNoise.cells(8, x + (cells.edge - 0.03) * nx / length, y + (cells.edge - 0.03) * ny / length, z + (cells.edge - 0.03) * nz / length, 3, 7, 3, probe);
                boolean before = probe.id1 == cells.id1;
                RockNoise.cells(8, x + (cells.edge + 0.03) * nx / length, y + (cells.edge + 0.03) * ny / length, z + (cells.edge + 0.03) * nz / length, 3, 7, 3, probe);
                if (before && probe.id1 == cells.id2) exact++;
                samples++;
            }
        }
        assertTrue(exact > 0.85 * samples, "the border is where edge says: " + exact + " of " + samples);
    }

    @Test
    void cellsOverThePlaneAreUpright() {
        RockNoise.Cells cells = new RockNoise.Cells(), probe = new RockNoise.Cells();
        for (double x = -30; x < 30; x += 3.3) {
            for (double z = -30; z < 30; z += 4.1) {
                RockNoise.cells(3, x, z, 10, cells);
                assertEquals(cells.f1 * 10, Math.hypot(x - cells.x1, z - cells.z1), 1e-9);
                assertTrue(cells.f1 <= cells.f2 && cells.edge >= 0 && cells.edge <= 10 * (cells.f1 + cells.f2) / 2 + 1e-9);
                RockNoise.cells(3, cells.x1, cells.z1, 10, probe);
                assertEquals(cells.id1, probe.id1, "a site is in its own cell");
            }
        }
    }

    @Test
    void cellsAreAsLargeAsAsked() {
        // Sites are about a cell apart: tall cells put them further apart up and down than across
        RockNoise.Cells cells = new RockNoise.Cells();
        double across = 0, up = 0;
        int samples = 0;
        for (int i = 0; i < 4000; i++) {
            double x = 61 * RockNoise.unit(RockNoise.hash(1, i, 0, 0)), y = 61 * RockNoise.unit(RockNoise.hash(1, i, 1, 0)), z = 61 * RockNoise.unit(RockNoise.hash(1, i, 2, 0));
            RockNoise.cells(9, x, y, z, 3, 12, 3, cells);
            across += Math.abs(cells.x2 - cells.x1) + Math.abs(cells.z2 - cells.z1);
            up += Math.abs(cells.y2 - cells.y1);
            samples++;
        }
        assertTrue(up / samples > 0.2, "neighbouring sites differ in height");
        assertTrue(across / samples < 12, "sites three blocks apart across, not twelve: " + across / samples);
    }

    private static double square(double value) {
        return value * value;
    }
}
