package frootloops.versus.mod.environment.worldgen.density;

import static frootloops.versus.mod.environment.worldgen.density.DensityOps.gradient;

/**
 * The river terms of the Players Versus terrain, shared by {@link PvDepth} and {@link PvTerrain}. Rivers follow the
 * ridge noise {@code R} ({@code minecraft:overworld/ridges}: the shifted ridge noise at xz 0.25, y 0) where it's near
 * 0. The old JSON sampled that noise again at every use; it doesn't depend on y, and vanilla already keeps it per
 * column in the chunk, so every term here takes it as a value.
 *
 * <p>Each method gives the doubles of the JSON it replaced ({@code river_carver}, {@code river_carver_depth},
 * {@code depth}); {@code TerrainPortTest} checks that.
 */
public final class TerrainFormulas {

    private TerrainFormulas() {
    }

    /** Whether {@link #riverCarver} or {@link #riverDepth} can read the ridge noise at this height. */
    public static boolean inRiverBand(double yValue) {
        return yValue >= 48.0 && yValue < 256.0;
    }

    /**
     * RC ({@code river_carver}): 1 away from rivers; along a river (|R| < 0.22) from y 48 up, lower, so that the
     * terrain's {@code min} with it cuts the valley.
     *
     * @param yValue {@link DensityOps#yValue} of {@code y}
     * @param ridge  R; only read inside the band
     */
    public static double riverCarver(int y, double yValue, double ridge) {
        if (!(yValue >= 48.0 && yValue < 256.0)) return 1.0;
        if (!(ridge >= -0.22 && ridge < 0.22)) return 1.0;
        double scaled = ridge * 7.0;
        return (gradient(y, 50, 74, 0.8, -1.2) + (gradient(y, 68, 90, 0.0, 0.3) + gradient(y, 68, 128, 0.0, 0.89))) + scaled * scaled;
    }

    /**
     * RCD ({@code river_carver_depth}): lowers depth along rivers (|R| < 0.2) in y 54..127, down to -1; 0 elsewhere.
     *
     * @param yValue {@link DensityOps#yValue} of {@code y}
     * @param ridge  R; only read inside the band
     */
    public static double riverDepth(int y, double yValue, double ridge) {
        if (!(yValue >= 54.0 && yValue < 128.0)) return 0.0;
        if (!(ridge >= -0.2 && ridge < 0.2)) return 0.0;
        double scaled = ridge * 6.75;
        return Math.min(0.0, (gradient(y, 54, 74, 0.0, -1.0) + gradient(y, 56, 120, 0.0, 1.0)) + scaled * scaled);
    }

    /** The lowest {@link #riverDepth}: its two gradients add up to at least -1, and the square is at least 0. */
    public static final double RIVER_DEPTH_MIN = -1.0;

    /** Depth: vanilla's height gradient plus {@code offset}, lowered along rivers. */
    public static double depth(int y, double offset, double riverDepth) {
        return (gradient(y, -64, 320, 1.5, -1.5) + offset) + riverDepth;
    }
}
