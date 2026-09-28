package frootloops.versus.mod.environment.worldgen.aquifer;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.Arrays;
import java.util.function.ToDoubleFunction;

/**
 * One smooth function on a lattice over one chunk and a band of y (4 blocks across, {@code stepY} blocks tall): each
 * lattice point is sampled the first time a block next to it needs it, and blocks in between get the trilinear
 * interpolation of their 8 points, computed like vanilla's {@code interpolated} (y first, then x, then z): with
 * vanilla's cell size and alignment, it gives what {@code interpolated} gives in the terrain pass, to the bit, for a
 * function without {@code interpolated} parts of its own ({@code AquiferTerrainPassTest}).
 *
 * <p>Only for functions without steps: interpolating across a step (a {@code range_choice} band, a threshold) moves
 * it. That's why the aquifer puts F's and S's smooth inputs here and evaluates F and S themselves per block.
 *
 * <p>Lattice points sit on absolute multiples of the step, so every caller that asks about a block (the terrain pass,
 * carvers, heightmap probes) gets the same value, and so does the neighbouring chunk along a shared border.
 *
 * <p>Not thread-safe; each instance belongs to one aquifer, which belongs to one chunk noise sampler.
 */
public final class Lattice implements ToDoubleFunction<DensityFunction.NoisePos> {

    /** What a lattice holds: a function of a block position. */
    @FunctionalInterface
    public interface Source {
        double sample(int x, int y, int z);

        /** A density function sampled at {@link DensityFunction.UnblendedNoisePos}, which a chunk's own router
         * answers with the raw function (no {@code interpolated} state involved) and, inside the chunk, its cached 2D values. */
        static Source of(DensityFunction function) {
            return (x, y, z) -> function.sample(new DensityFunction.UnblendedNoisePos(x, y, z));
        }
    }

    static final int STEP_XZ = 4;
    private static final int SIDE = 16 / STEP_XZ + 1;

    private final Source source;
    private final int originX, originZ, minY, stepY, levels;
    /** Sampled values by lattice column, allocated on first use; {@code NaN} marks a point not sampled yet. */
    private final double[][] columns = new double[SIDE * SIDE][];
    private int samples;

    /**
     * Covers the chunk's 16 x 16 columns and y from {@code minY} up to {@code maxY}, rounded out to the lattice.
     *
     * @param stepY vertical distance between lattice points; the terrain pass uses the noise settings' cell height
     */
    public Lattice(Source source, ChunkPos chunk, int minY, int maxY, int stepY) {
        this.source = source;
        this.originX = chunk.getStartX();
        this.originZ = chunk.getStartZ();
        this.stepY = stepY;
        this.minY = Math.floorDiv(minY, stepY) * stepY;
        this.levels = Math.floorDiv(maxY - this.minY + stepY - 1, stepY) + 1;
    }

    public Lattice(DensityFunction function, ChunkPos chunk, int minY, int maxY, int stepY) {
        this(Source.of(function), chunk, minY, maxY, stepY);
    }

    /** A 4 x 4 x 4 lattice. */
    public Lattice(DensityFunction function, ChunkPos chunk, int minY, int maxY) {
        this(Source.of(function), chunk, minY, maxY, STEP_XZ);
    }

    @Override
    public double applyAsDouble(DensityFunction.NoisePos pos) {
        return this.at(pos.blockX(), pos.blockY(), pos.blockZ());
    }

    /** The interpolated value at a block; exact outside the chunk or band (nothing in the game asks there). */
    public double at(int blockX, int blockY, int blockZ) {
        int x = blockX - this.originX;
        int y = blockY - this.minY;
        int z = blockZ - this.originZ;
        if (x < 0 || x > 15 || z < 0 || z > 15 || y < 0 || y >= (this.levels - 1) * this.stepY) {
            return this.source.sample(blockX, blockY, blockZ);
        }
        int ix = x / STEP_XZ, iy = y / this.stepY, iz = z / STEP_XZ;
        double deltaX = (x % STEP_XZ) / (double) STEP_XZ;
        double deltaY = (y % this.stepY) / (double) this.stepY;
        double deltaZ = (z % STEP_XZ) / (double) STEP_XZ;
        double x0z0 = MathHelper.lerp(deltaY, this.point(ix, iy, iz), this.point(ix, iy + 1, iz));
        double x1z0 = MathHelper.lerp(deltaY, this.point(ix + 1, iy, iz), this.point(ix + 1, iy + 1, iz));
        double x0z1 = MathHelper.lerp(deltaY, this.point(ix, iy, iz + 1), this.point(ix, iy + 1, iz + 1));
        double x1z1 = MathHelper.lerp(deltaY, this.point(ix + 1, iy, iz + 1), this.point(ix + 1, iy + 1, iz + 1));
        return MathHelper.lerp(deltaZ, MathHelper.lerp(deltaX, x0z0, x1z0), MathHelper.lerp(deltaX, x0z1, x1z1));
    }

    /** The exact value at a block: the stored lattice point if the block is one, the source otherwise. */
    public double exactAt(int blockX, int blockY, int blockZ) {
        int x = blockX - this.originX;
        int y = blockY - this.minY;
        int z = blockZ - this.originZ;
        if (x < 0 || x > 16 || z < 0 || z > 16 || y < 0 || y > (this.levels - 1) * this.stepY
                || x % STEP_XZ != 0 || y % this.stepY != 0 || z % STEP_XZ != 0) {
            return this.source.sample(blockX, blockY, blockZ);
        }
        return this.point(x / STEP_XZ, y / this.stepY, z / STEP_XZ);
    }

    private double point(int ix, int iy, int iz) {
        int index = ix * SIDE + iz;
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
