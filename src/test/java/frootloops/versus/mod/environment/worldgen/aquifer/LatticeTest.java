package frootloops.versus.mod.environment.worldgen.aquifer;

import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.function.ToDoubleFunction;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.DensityFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatticeTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    /** A test function that counts its samples. */
    private static final class Counting implements DensityFunction.SimpleFunction {
        private final ToDoubleFunction<FunctionContext> function;
        int samples;

        Counting(ToDoubleFunction<FunctionContext> function) {
            this.function = function;
        }

        @Override
        public double compute(FunctionContext pos) {
            this.samples++;
            return this.function.applyAsDouble(pos);
        }

        @Override
        public double minValue() {
            return Double.NEGATIVE_INFINITY;
        }

        @Override
        public double maxValue() {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException("test function");
        }
    }

    private static DensityFunction.FunctionContext at(int x, int y, int z) {
        return new DensityFunction.SinglePointContext(x, y, z);
    }

    @Test
    void linearFunctionsComeOutExactAndEachPointIsSampledOnce() {
        Counting linear = new Counting(pos -> 0.25 * pos.blockX() - 0.5 * pos.blockY() + 0.125 * pos.blockZ() + 3.0);
        ChunkPos chunk = new ChunkPos(-3, 7);
        Lattice lattice = new Lattice(linear, chunk, -32, 64);
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x++) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z++) {
                for (int y = -32; y < 64; y++) {
                    double expected = 0.25 * x - 0.5 * y + 0.125 * z + 3.0;
                    assertEquals(expected, lattice.applyAsDouble(at(x, y, z)), 1e-9, "at " + x + "," + y + "," + z);
                }
            }
        }
        // 5 x 5 lattice columns, 25 levels (y -32..64 in steps of 4)
        assertEquals(5 * 5 * 25, lattice.samples());
        assertEquals(5 * 5 * 25, linear.samples);
    }

    @Test
    void latticePointsKeepTheirExactValue() {
        Counting wavy = new Counting(pos -> Math.sin(pos.blockX() * 0.7) * Math.cos(pos.blockY() * 0.3) + pos.blockZ() * pos.blockZ() * 0.01);
        ChunkPos chunk = new ChunkPos(12, -40);
        Lattice lattice = new Lattice(wavy, chunk, -4, 32);
        for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x += 4) {
            for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z += 4) {
                for (int y = -4; y < 32; y += 4) {
                    assertEquals(wavy.function.applyAsDouble(at(x, y, z)), lattice.applyAsDouble(at(x, y, z)), 0.0);
                }
            }
        }
    }

    /** Blocks one cell past the chunk's sides get what the neighbouring chunk's own lattice gives, to the bit. */
    @Test
    void blocksNextToTheChunkGetTheNeighbouringChunksValues() {
        Counting wavy = new Counting(pos -> Math.sin(pos.blockX() * 0.7) + Math.cos(pos.blockY() * 0.3 + pos.blockZ()));
        ChunkPos chunk = new ChunkPos(0, 0);
        Lattice lattice = new Lattice(wavy, chunk, -4, 32);
        int[][] next = {{16, 10, 5}, {19, 11, 5}, {-1, 10, 5}, {-4, 13, 5}, {5, 10, 16}, {5, 10, -1}, {-1, 10, -1}, {16, 10, 16},
                {-3, 30, 18}};
        for (int[] p : next) {
            Lattice own = new Lattice(wavy, new ChunkPos(Math.floorDiv(p[0], 16), Math.floorDiv(p[2], 16)), -4, 32);
            assertEquals(own.at(p[0], p[1], p[2]), lattice.at(p[0], p[1], p[2]), 0.0, "at " + p[0] + "," + p[1] + "," + p[2]);
        }
    }

    @Test
    void positionsFurtherOutOrOutsideTheBandAreExact() {
        Counting wavy = new Counting(pos -> Math.sin(pos.blockX() * 0.7) + Math.cos(pos.blockY() * 0.3 + pos.blockZ()));
        ChunkPos chunk = new ChunkPos(0, 0);
        Lattice lattice = new Lattice(wavy, chunk, -4, 32);
        int[][] outside = {{20, 10, 5}, {-5, 10, 5}, {5, 10, 20}, {5, 10, -5}, {5, -5, 5}, {5, 32, 5}, {5, 100, 5}};
        for (int[] p : outside) {
            assertEquals(wavy.function.applyAsDouble(at(p[0], p[1], p[2])), lattice.applyAsDouble(at(p[0], p[1], p[2])), 0.0);
        }
        assertEquals(0, lattice.samples());
        assertTrue(wavy.samples == outside.length);
    }
}
