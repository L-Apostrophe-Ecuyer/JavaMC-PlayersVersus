package frootloops.versus.mod.environment.worldgen.aquifer;

import java.util.Arrays;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.densityfunction.DensitySampler;

/**
 * One smooth function on a lattice over one chunk and a band of y (4 blocks across, {@code stepY} blocks tall): each
 * lattice point is sampled the first time a block next to it needs it, and blocks in between get the trilinear
 * interpolation of their 8 points (y first, then x, then z), as 1.21.10's {@code interpolated} did on the terrain's
 * cells, which the aquifer's inputs were defined on.
 *
 * <p>Only for functions without steps: interpolating across a step (a {@code range_choice} band, a threshold) moves
 * it. That's why the aquifer puts F's and S's smooth inputs here and evaluates F and S themselves per block.
 *
 * <p>Lattice points sit on absolute multiples of the step, so every caller that asks about a block (the terrain pass,
 * carvers, heightmap probes) gets the same value, and so does the neighbouring chunk along a shared border. The lattice
 * reaches one cell past each side of the chunk, so a block just outside it (the aquifer looks at its blocks'
 * neighbours) gets what the neighbouring chunk's own lattice gives there.
 *
 * <p>Not thread-safe; each instance belongs to one aquifer, which belongs to one chunk's terrain pass.
 */
public final class Lattice {

    /** What a lattice holds: a function of a block position. */
    @FunctionalInterface
    public interface Source {
        double sample(int x, int y, int z);

        /** A compiled density function bound to the chunk's sampling context (its caches). */
        static Source of(DensitySampler.Bound sampler) {
            return sampler::sampleValue;
        }
    }

    static final int STEP_XZ = 4;
    /** Cells the lattice reaches past each side of the chunk. */
    private static final int BORDER = 1;
    private static final int SIDE = 16 / STEP_XZ + 1 + 2 * BORDER;
    /** The blocks the lattice interpolates, relative to the chunk's start: this one included, that one not. */
    private static final int FIRST = -BORDER * STEP_XZ, END = 16 + BORDER * STEP_XZ;

    private final Source source;
    private final int originX, originZ, minY, stepY, levels;
    /** Sampled values by lattice column, allocated on first use; {@code NaN} marks a point not sampled yet. */
    private final double[][] columns = new double[SIDE * SIDE][];
    private int samples;

    /**
     * Covers the chunk's 16 x 16 columns, one cell around them, and y from {@code minY} up to {@code maxY}, rounded
     * out to the lattice.
     *
     * @param originX the chunk's first block's x
     * @param originZ the chunk's first block's z
     * @param stepY   vertical distance between lattice points: the terrain's cell height
     */
    public Lattice(Source source, int originX, int originZ, int minY, int maxY, int stepY) {
        this.source = source;
        this.originX = originX;
        this.originZ = originZ;
        this.stepY = stepY;
        this.minY = Math.floorDiv(minY, stepY) * stepY;
        this.levels = Math.floorDiv(maxY - this.minY + stepY - 1, stepY) + 1;
    }

    /**
     * The interpolated value at a block; exact beyond the chunk's border cells or outside the band (nothing in the
     * game asks there).
     */
    public double at(int blockX, int blockY, int blockZ) {
        int x = blockX - this.originX;
        int y = blockY - this.minY;
        int z = blockZ - this.originZ;
        if (x < FIRST || x >= END || z < FIRST || z >= END || y < 0 || y >= (this.levels - 1) * this.stepY) {
            return this.source.sample(blockX, blockY, blockZ);
        }
        int ix = Math.floorDiv(x, STEP_XZ), iy = y / this.stepY, iz = Math.floorDiv(z, STEP_XZ);
        double deltaX = Math.floorMod(x, STEP_XZ) / (double) STEP_XZ;
        double deltaY = (y % this.stepY) / (double) this.stepY;
        double deltaZ = Math.floorMod(z, STEP_XZ) / (double) STEP_XZ;
        double x0z0 = Mth.lerp(deltaY, this.point(ix, iy, iz), this.point(ix, iy + 1, iz));
        double x1z0 = Mth.lerp(deltaY, this.point(ix + 1, iy, iz), this.point(ix + 1, iy + 1, iz));
        double x0z1 = Mth.lerp(deltaY, this.point(ix, iy, iz + 1), this.point(ix, iy + 1, iz + 1));
        double x1z1 = Mth.lerp(deltaY, this.point(ix + 1, iy, iz + 1), this.point(ix + 1, iy + 1, iz + 1));
        return Mth.lerp(deltaZ, Mth.lerp(deltaX, x0z0, x1z0), Mth.lerp(deltaX, x0z1, x1z1));
    }

    /** The exact value at a block: the stored lattice point if the block is one, the source otherwise. */
    public double exactAt(int blockX, int blockY, int blockZ) {
        int x = blockX - this.originX;
        int y = blockY - this.minY;
        int z = blockZ - this.originZ;
        if (x < FIRST || x > END || z < FIRST || z > END || y < 0 || y > (this.levels - 1) * this.stepY
                || Math.floorMod(x, STEP_XZ) != 0 || y % this.stepY != 0 || Math.floorMod(z, STEP_XZ) != 0) {
            return this.source.sample(blockX, blockY, blockZ);
        }
        return this.point(Math.floorDiv(x, STEP_XZ), y / this.stepY, Math.floorDiv(z, STEP_XZ));
    }

    /** The lattice point at column {@code ix, iz} (-1 for the border cell before the chunk) and level {@code iy}. */
    private double point(int ix, int iy, int iz) {
        int index = (ix + BORDER) * SIDE + (iz + BORDER);
        double[] column = this.columns[index];
        if (column == null) {
            column = new double[this.levels];
            Arrays.fill(column, Double.NaN);
            this.columns[index] = column;
        }
        double value = column[iy];
        if (Double.isNaN(value)) {
            value = this.source.sample(this.originX + ix * STEP_XZ, this.minY + iy * this.stepY, this.originZ + iz * STEP_XZ);
            column[iy] = value;
            this.samples++;
        }
        return value;
    }

    /** How many lattice points were sampled so far, for tests and the benchmark. */
    public int samples() {
        return this.samples;
    }
}
