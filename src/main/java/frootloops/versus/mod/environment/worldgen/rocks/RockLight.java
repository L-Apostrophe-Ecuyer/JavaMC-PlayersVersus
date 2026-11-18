package frootloops.versus.mod.environment.worldgen.rocks;

import java.util.Arrays;

/**
 * Light painted into the stone from above; Minecraft has no sun direction, so nothing here depends on one. What sees the
 * sky, faces up, stands out and stands high is light; what faces down, is shut in or lies low is dark. Undersides, inside
 * corners and the drawn cracks are tuff; the rest goes by its light, the darkest 30% tuff, then 46% stone, 21% andesite
 * and the brightest 3% clay.
 */
final class RockLight {

    /** Straight up, and tipped half a right angle four ways. */
    private static final int[][] RAYS = {{0, 1, 0}, {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1}};
    /** How far a ray goes before it counts as seeing the sky. */
    private static final int SKY = 16;

    private RockLight() {
    }

    /** The tone of each cell, by {@link RockVoxels#index}: null for air; below the ground and inside, stone. */
    static RockMaterial[] paint(RockVoxels voxels, long seed) {
        RockMaterial[] tones = new RockMaterial[voxels.sizeX * voxels.sizeY * voxels.sizeZ];
        Counts counts = new Counts(voxels);
        int top = Math.max(1, voxels.top());
        int[] lit = new int[256];
        double[] light = new double[256];
        int many = 0;
        for (int x = voxels.minX; x < voxels.minX + voxels.sizeX; x++) {
            for (int z = voxels.minZ; z < voxels.minZ + voxels.sizeZ; z++) {
                for (int y = voxels.minY; y < voxels.minY + voxels.sizeY; y++) {
                    if (!voxels.solid(x, y, z)) continue;
                    int index = voxels.index(x, y, z);
                    tones[index] = RockMaterial.BASE;
                    if (y < 0 || !exposed(voxels, x, y, z)) continue;
                    // The share of open cells within two blocks: about a half on a flat face, more on an edge, less in a crease
                    double open = (125 - counts.box(x - 2, y - 2, z - 2, x + 2, y + 2, z + 2)) / 124.0;
                    double up = up(counts, x, y, z);
                    boolean under = y > 0 && !voxels.filled(x, y - 1, z) && up < -0.3;
                    if (under || voxels.seam(x, y, z) && open < 0.55 || open < 0.36) {
                        tones[index] = RockMaterial.SHADE;
                        continue;
                    }
                    double patch = 0.07 * RockNoise.noise(seed + 50, x / 4.0, y / 4.0, z / 4.0) + 0.05 * (RockNoise.unit(RockNoise.hash(seed + 51, x, y, z)) - 0.5);
                    if (many == lit.length) {
                        lit = Arrays.copyOf(lit, 2 * many);
                        light = Arrays.copyOf(light, 2 * many);
                    }
                    lit[many] = index;
                    light[many++] = 0.35 * sky(voxels, x, y, z) + 0.25 * up + 0.8 * (open - 0.5) + 0.65 * y / top + patch;
                }
            }
        }
        // The tones by their share of the lit faces
        double[] sorted = Arrays.copyOf(light, many);
        Arrays.sort(sorted);
        double shade = quantile(sorted, 0.30), base = quantile(sorted, 0.76), bright = quantile(sorted, 0.97);
        for (int i = 0; i < many; i++) {
            double value = light[i];
            tones[lit[i]] = value < shade ? RockMaterial.SHADE : value < base ? RockMaterial.BASE : value < bright ? RockMaterial.LIGHT : RockMaterial.BRIGHT;
        }
        return tones;
    }

    private static boolean exposed(RockVoxels voxels, int x, int y, int z) {
        return !voxels.filled(x + 1, y, z) || !voxels.filled(x - 1, y, z) || !voxels.filled(x, y + 1, z) || !voxels.filled(x, y - 1, z)
                || !voxels.filled(x, y, z + 1) || !voxels.filled(x, y, z - 1);
    }

    /**
     * How far the face points up: the open cells within two blocks pull toward the open side; the pull's height share.
     * Summed slab by slab: those two above pull twice as hard as those one above, and so on.
     */
    private static double up(Counts counts, int x, int y, int z) {
        double sx = 0, sy = 0, sz = 0;
        for (int d = 1; d <= 2; d++) {
            // The more filled below than above, the more open above
            sx += d * (counts.box(x - d, y - 2, z - 2, x - d, y + 2, z + 2) - counts.box(x + d, y - 2, z - 2, x + d, y + 2, z + 2));
            sy += d * (counts.box(x - 2, y - d, z - 2, x + 2, y - d, z + 2) - counts.box(x - 2, y + d, z - 2, x + 2, y + d, z + 2));
            sz += d * (counts.box(x - 2, y - 2, z - d, x + 2, y + 2, z - d) - counts.box(x - 2, y - 2, z + d, x + 2, y + 2, z + d));
        }
        double length = Math.sqrt(sx * sx + sy * sy + sz * sz);
        return length < 1e-6 ? 0 : sy / length;
    }

    /** The share of the five rays that get out. */
    private static double sky(RockVoxels voxels, int x, int y, int z) {
        int free = 0;
        for (int[] ray : RAYS) {
            boolean blocked = false;
            for (int step = 1; step <= SKY && !blocked; step++) blocked = voxels.filled(x + ray[0] * step, y + ray[1] * step, z + ray[2] * step);
            if (!blocked) free++;
        }
        return free / (double) RAYS.length;
    }

    private static double quantile(double[] sorted, double share) {
        return sorted.length == 0 ? 0 : sorted[Math.min(sorted.length - 1, (int) (share * sorted.length))];
    }

    /** Filled cells (rock, or the ground below 0) counted in any box at once: a summed-volume table, two cells past the grid. */
    private static final class Counts {

        private final int x0, y0, z0, sizeX, sizeY, sizeZ;
        private final int[] sums;

        Counts(RockVoxels voxels) {
            this.x0 = voxels.minX - 2;
            this.y0 = voxels.minY - 2;
            this.z0 = voxels.minZ - 2;
            this.sizeX = voxels.sizeX + 4;
            this.sizeY = voxels.sizeY + 4;
            this.sizeZ = voxels.sizeZ + 4;
            this.sums = new int[(this.sizeX + 1) * (this.sizeY + 1) * (this.sizeZ + 1)];
            for (int i = 0; i < this.sizeX; i++) {
                for (int j = 0; j < this.sizeY; j++) {
                    for (int k = 0; k < this.sizeZ; k++) {
                        int cell = voxels.filled(this.x0 + i, this.y0 + j, this.z0 + k) ? 1 : 0;
                        this.sums[this.at(i + 1, j + 1, k + 1)] = cell + this.sums[this.at(i, j + 1, k + 1)] + this.sums[this.at(i + 1, j, k + 1)]
                                + this.sums[this.at(i + 1, j + 1, k)] - this.sums[this.at(i, j, k + 1)] - this.sums[this.at(i, j + 1, k)]
                                - this.sums[this.at(i + 1, j, k)] + this.sums[this.at(i, j, k)];
                    }
                }
            }
        }

        private int at(int i, int j, int k) {
            return (i * (this.sizeY + 1) + j) * (this.sizeZ + 1) + k;
        }

        /** How many filled cells from (x0, y0, z0) to (x1, y1, z1), both corners included. */
        int box(int x0, int y0, int z0, int x1, int y1, int z1) {
            int i0 = x0 - this.x0, j0 = y0 - this.y0, k0 = z0 - this.z0, i1 = x1 - this.x0 + 1, j1 = y1 - this.y0 + 1, k1 = z1 - this.z0 + 1;
            return this.sums[this.at(i1, j1, k1)] - this.sums[this.at(i0, j1, k1)] - this.sums[this.at(i1, j0, k1)] - this.sums[this.at(i1, j1, k0)]
                    + this.sums[this.at(i0, j0, k1)] + this.sums[this.at(i0, j1, k0)] + this.sums[this.at(i1, j0, k0)] - this.sums[this.at(i0, j0, k0)];
        }
    }
}
