package frootloops.versus.mod.environment.worldgen.aquifer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatticeTest {

    /** A test source that counts its samples. */
    private static final class Counting implements Lattice.Source {
        private final Lattice.Source function;
        int samples;

        Counting(Lattice.Source function) {
            this.function = function;
        }

        @Override
        public double sample(int x, int y, int z) {
            this.samples++;
            return this.function.sample(x, y, z);
        }
    }

    @Test
    void linearFunctionsComeOutExactAndEachPointIsSampledOnce() {
        Counting linear = new Counting((x, y, z) -> 0.25 * x - 0.5 * y + 0.125 * z + 3.0);
        int originX = -3 * 16, originZ = 7 * 16;
        Lattice lattice = new Lattice(linear, originX, originZ, -32, 64, 4);
        for (int x = originX; x < originX + 16; x++) {
            for (int z = originZ; z < originZ + 16; z++) {
                for (int y = -32; y < 64; y++) {
                    double expected = 0.25 * x - 0.5 * y + 0.125 * z + 3.0;
                    assertEquals(expected, lattice.at(x, y, z), 1e-9, "at " + x + "," + y + "," + z);
                }
            }
        }
        // 5 x 5 lattice columns, 25 levels (y -32..64 in steps of 4)
        assertEquals(5 * 5 * 25, lattice.samples());
        assertEquals(5 * 5 * 25, linear.samples);
    }

    @Test
    void latticePointsKeepTheirExactValue() {
        Counting wavy = new Counting((x, y, z) -> Math.sin(x * 0.7) * Math.cos(y * 0.3) + z * z * 0.01);
        int originX = 12 * 16, originZ = -40 * 16;
        Lattice lattice = new Lattice(wavy, originX, originZ, -4, 32, 4);
        for (int x = originX; x < originX + 16; x += 4) {
            for (int z = originZ; z < originZ + 16; z += 4) {
                for (int y = -4; y < 32; y += 4) {
                    assertEquals(wavy.function.sample(x, y, z), lattice.at(x, y, z), 0.0);
                    assertEquals(wavy.function.sample(x, y, z), lattice.exactAt(x, y, z), 0.0);
                }
            }
        }
    }

    /** Blocks one cell past the chunk's sides get what the neighbouring chunk's own lattice gives, to the bit. */
    @Test
    void blocksNextToTheChunkGetTheNeighbouringChunksValues() {
        Counting wavy = new Counting((x, y, z) -> Math.sin(x * 0.7) + Math.cos(y * 0.3 + z));
        Lattice lattice = new Lattice(wavy, 0, 0, -4, 32, 4);
        int[][] next = {{16, 10, 5}, {19, 11, 5}, {-1, 10, 5}, {-4, 13, 5}, {5, 10, 16}, {5, 10, -1}, {-1, 10, -1}, {16, 10, 16},
                {-3, 30, 18}};
        for (int[] p : next) {
            Lattice own = new Lattice(wavy, Math.floorDiv(p[0], 16) * 16, Math.floorDiv(p[2], 16) * 16, -4, 32, 4);
            assertEquals(own.at(p[0], p[1], p[2]), lattice.at(p[0], p[1], p[2]), 0.0, "at " + p[0] + "," + p[1] + "," + p[2]);
        }
    }

    @Test
    void positionsFurtherOutOrOutsideTheBandAreExact() {
        Counting wavy = new Counting((x, y, z) -> Math.sin(x * 0.7) + Math.cos(y * 0.3 + z));
        Lattice lattice = new Lattice(wavy, 0, 0, -4, 32, 4);
        int[][] outside = {{20, 10, 5}, {-5, 10, 5}, {5, 10, 20}, {5, 10, -5}, {5, -5, 5}, {5, 32, 5}, {5, 100, 5}};
        for (int[] p : outside) {
            assertEquals(wavy.function.sample(p[0], p[1], p[2]), lattice.at(p[0], p[1], p[2]), 0.0);
        }
        assertEquals(0, lattice.samples());
        assertTrue(wavy.samples == outside.length);
    }

    @Test
    void exactValuesComeFromTheSource() {
        Counting wavy = new Counting((x, y, z) -> Math.sin(x * 0.7) + Math.cos(y * 0.3 + z));
        Lattice lattice = new Lattice(wavy, 0, 0, -4, 32, 4);
        // between lattice points the source answers, not the interpolation
        assertEquals(wavy.function.sample(9, 13, 5), lattice.exactAt(9, 13, 5), 0.0);
        assertTrue(lattice.at(9, 13, 5) != lattice.exactAt(9, 13, 5), "a wavy function isn't linear between lattice points");
    }
}
