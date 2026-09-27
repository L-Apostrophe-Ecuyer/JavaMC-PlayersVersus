package frootloops.versus.mod.environment.worldgen.aquifer;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.Arrays;
import java.util.function.ToDoubleFunction;

/**
 * One density function on a 4-block lattice over one chunk and a band of y: each lattice point is sampled the first
 * time a block next to it needs it, and blocks in between get the trilinear interpolation of their 8 points.
 *
 * <p>Lattice points sit on absolute multiples of 4, so every caller that asks about a block (the terrain pass,
 * carvers, heightmap probes) gets the same value, and so does the neighbouring chunk along a shared border. Points
 * are sampled with {@link DensityFunction.UnblendedNoisePos}, which a chunk's own router answers with the raw function
 * (no {@code interpolated} state involved) and, inside the chunk, its cached 2D values.
 *
 * <p>Not thread-safe; each instance belongs to one aquifer, which belongs to one chunk noise sampler.
 */
public final class Lattice implements ToDoubleFunction<DensityFunction.NoisePos> {

    static final int STEP = 4;
    private static final int SIDE = 16 / STEP + 1;

    private final DensityFunction source;
    private final int originX, originZ, minY, levels;
    /** Sampled values by lattice column, allocated on first use; {@code NaN} marks a point not sampled yet. */
    private final double[][] columns = new double[SIDE * SIDE][];
    private int samples;

    /** Covers the chunk's 16 x 16 columns and y from {@code minY} up to {@code maxY}, rounded out to the lattice. */
    public Lattice(DensityFunction source, ChunkPos chunk, int minY, int maxY) {
        this.source = source;
        this.originX = chunk.getStartX();
        this.originZ = chunk.getStartZ();
        this.minY = Math.floorDiv(minY, STEP) * STEP;
        this.levels = Math.floorDiv(maxY - this.minY + STEP - 1, STEP) + 1;
    }

    @Override
    public double applyAsDouble(DensityFunction.NoisePos pos) {
        int x = pos.blockX() - this.originX;
        int y = pos.blockY() - this.minY;
        int z = pos.blockZ() - this.originZ;
        if (x < 0 || x > 15 || z < 0 || z > 15 || y < 0 || y >= (this.levels - 1) * STEP) {
            // Nothing in the game asks outside the chunk's band; answer exactly if something does.
            return this.source.sample(new DensityFunction.UnblendedNoisePos(pos.blockX(), pos.blockY(), pos.blockZ()));
        }
        int ix = x / STEP, iy = y / STEP, iz = z / STEP;
        return MathHelper.lerp3((x % STEP) / (double) STEP, (y % STEP) / (double) STEP, (z % STEP) / (double) STEP,
                this.point(ix, iy, iz), this.point(ix + 1, iy, iz), this.point(ix, iy + 1, iz), this.point(ix + 1, iy + 1, iz),
                this.point(ix, iy, iz + 1), this.point(ix + 1, iy, iz + 1), this.point(ix, iy + 1, iz + 1), this.point(ix + 1, iy + 1, iz + 1));
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
            value = this.source.sample(new DensityFunction.UnblendedNoisePos(
                    this.originX + ix * STEP, this.minY + iy * STEP, this.originZ + iz * STEP));
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
