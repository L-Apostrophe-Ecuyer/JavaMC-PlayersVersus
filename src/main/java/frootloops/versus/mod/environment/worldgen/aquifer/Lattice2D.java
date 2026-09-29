package frootloops.versus.mod.environment.worldgen.aquifer;

import java.util.Arrays;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * {@link Lattice} for a function that doesn't depend on y, on the terrain pass's columns of cell corners (every 4
 * blocks, over the chunk and one cell around it): each point is sampled the first time a column next to it needs it,
 * and columns in between get the bilinear interpolation of their 4 points, x first, then z. For such a function that's
 * what vanilla's {@code interpolated} gives in the terrain pass, to the bit, in both of its interpolations (a block's,
 * which goes y, x, z, and a cell cache's, which goes x, y, z), since interpolating between two equal values gives that
 * value exactly.
 *
 * <p>Not thread-safe; each instance belongs to one aquifer, which belongs to one chunk noise sampler.
 */
public final class Lattice2D {

    private static final int STEP = Lattice.STEP_XZ;
    /** Cells the lattice reaches past each side of the chunk. */
    private static final int BORDER = 1;
    private static final int SIDE = 16 / STEP + 1 + 2 * BORDER;
    /** The columns the lattice interpolates, relative to the chunk's start: this one included, that one not. */
    private static final int FIRST = -BORDER * STEP, END = 16 + BORDER * STEP;

    private final DensityFunction function;
    private final int originX, originZ, y;
    /** Sampled values; {@code NaN} marks a point not sampled yet. */
    private final double[] points = new double[SIDE * SIDE];
    private int samples;

    /** @param y the height the function is sampled at (any, for a function of the column alone) */
    public Lattice2D(DensityFunction function, ChunkPos chunk, int y) {
        this.function = function;
        this.originX = chunk.getMinBlockX();
        this.originZ = chunk.getMinBlockZ();
        this.y = y;
        Arrays.fill(this.points, Double.NaN);
    }

    /** The interpolated value at a column; exact beyond the chunk's border cells (nothing in the game asks there). */
    public double at(int blockX, int blockZ) {
        int x = blockX - this.originX;
        int z = blockZ - this.originZ;
        if (x < FIRST || x >= END || z < FIRST || z >= END) return this.sample(blockX, blockZ);
        int ix = Math.floorDiv(x, STEP), iz = Math.floorDiv(z, STEP);
        double deltaX = Math.floorMod(x, STEP) / (double) STEP;
        double deltaZ = Math.floorMod(z, STEP) / (double) STEP;
        return Mth.lerp(deltaZ, Mth.lerp(deltaX, this.point(ix, iz), this.point(ix + 1, iz)),
                Mth.lerp(deltaX, this.point(ix, iz + 1), this.point(ix + 1, iz + 1)));
    }

    /** The point at column {@code ix, iz} of the corners (-1 for the border cell before the chunk). */
    private double point(int ix, int iz) {
        int index = (ix + BORDER) * SIDE + (iz + BORDER);
        double value = this.points[index];
        if (Double.isNaN(value)) {
            value = this.sample(this.originX + ix * STEP, this.originZ + iz * STEP);
            this.points[index] = value;
            this.samples++;
        }
        return value;
    }

    private double sample(int x, int z) {
        return this.function.compute(new DensityFunction.SinglePointContext(x, this.y, z));
    }

    /** How many points were sampled so far, for tests and the benchmark. */
    public int samples() {
        return this.samples;
    }
}
