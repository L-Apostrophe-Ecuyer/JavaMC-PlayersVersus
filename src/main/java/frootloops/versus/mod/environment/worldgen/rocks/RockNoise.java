package frootloops.versus.mod.environment.worldgen.rocks;

/**
 * The rocks' own noise: gradient noise and Voronoi cells from hashed lattice points, with no tables and no game classes,
 * so a rock keeps its shape whatever the game's noises become, and the shapes can be built and drawn without the game
 * ({@code RockPreview}).
 */
public final class RockNoise {

    private RockNoise() {
    }

    /**
     * The two nearest sites of a Voronoi diagram to a point, and the border between their cells. Distances {@code f1}
     * and {@code f2} are in cells (1 is a cell's size: cells that are taller than wide are measured so, which is what
     * makes them tall); the sites and {@code edge}, the distance to the border, are in blocks.
     */
    public static final class Cells {
        public double f1, f2, edge, x1, y1, z1, x2, y2, z2;
        public long id1, id2;
    }

    /** A well mixed 64-bit value (the finalizer of MurmurHash3). */
    public static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    public static long hash(long seed, int x, int y, int z) {
        return mix(mix(mix(seed ^ x * 0x9E3779B97F4A7C15L) ^ y * 0xC2B2AE3D27D4EB4FL) ^ z * 0x165667B19E3779F9L);
    }

    public static long hash(long seed, long salt) {
        return mix(mix(seed) ^ salt * 0x9E3779B97F4A7C15L);
    }

    /** A hash as a number from 0 (inclusive) to 1 (exclusive). */
    public static double unit(long hash) {
        return (hash >>> 11) * 0x1.0p-53;
    }

    /** Gradient noise, about -1 to 1, one feature a unit. */
    public static double noise(long seed, double x, double y, double z) {
        int x0 = floor(x), y0 = floor(y), z0 = floor(z);
        double fx = x - x0, fy = y - y0, fz = z - z0;
        double u = fade(fx), v = fade(fy), w = fade(fz);
        double n000 = corner(seed, x0, y0, z0, fx, fy, fz), n100 = corner(seed, x0 + 1, y0, z0, fx - 1, fy, fz);
        double n010 = corner(seed, x0, y0 + 1, z0, fx, fy - 1, fz), n110 = corner(seed, x0 + 1, y0 + 1, z0, fx - 1, fy - 1, fz);
        double n001 = corner(seed, x0, y0, z0 + 1, fx, fy, fz - 1), n101 = corner(seed, x0 + 1, y0, z0 + 1, fx - 1, fy, fz - 1);
        double n011 = corner(seed, x0, y0 + 1, z0 + 1, fx, fy - 1, fz - 1), n111 = corner(seed, x0 + 1, y0 + 1, z0 + 1, fx - 1, fy - 1, fz - 1);
        double low = lerp(v, lerp(u, n000, n100), lerp(u, n010, n110));
        double high = lerp(v, lerp(u, n001, n101), lerp(u, n011, n111));
        return lerp(w, low, high);
    }

    /** Noise over the plane. */
    public static double noise(long seed, double x, double z) {
        return noise(seed, x, 0.37, z);
    }

    /** Octaves of noise, each twice as fine and half as strong; about -1 to 1. */
    public static double fbm(long seed, double x, double y, double z, int octaves) {
        double sum = 0, amplitude = 1, total = 0;
        for (int octave = 0; octave < octaves; octave++) {
            sum += amplitude * noise(seed + octave * 0x632BE59BD9B4E019L, x, y, z);
            total += amplitude;
            amplitude *= 0.5;
            x *= 2;
            y *= 2;
            z *= 2;
        }
        return sum / total;
    }

    private static double corner(long seed, int x, int y, int z, double dx, double dy, double dz) {
        // Twelve directions to the edges of a cube, as Perlin's improved noise has them
        return switch ((int) ((hash(seed, x, y, z) >>> 40) % 12)) {
            case 0 -> dx + dy;
            case 1 -> -dx + dy;
            case 2 -> dx - dy;
            case 3 -> -dx - dy;
            case 4 -> dx + dz;
            case 5 -> -dx + dz;
            case 6 -> dx - dz;
            case 7 -> -dx - dz;
            case 8 -> dy + dz;
            case 9 -> -dy + dz;
            case 10 -> dy - dz;
            default -> -dy - dz;
        };
    }

    /**
     * The Voronoi diagram of one site in each cell of a lattice, each somewhere in the middle three quarters of its cell.
     * Cells are {@code sizeX} by {@code sizeY} by {@code sizeZ} blocks, and distances are measured in cells, so a lattice
     * of tall cells gives tall Voronoi cells.
     */
    public static void cells(long seed, double x, double y, double z, double sizeX, double sizeY, double sizeZ, Cells out) {
        double u = x / sizeX, v = y / sizeY, w = z / sizeZ;
        int cellX = floor(u), cellY = floor(v), cellZ = floor(w);
        double best = Double.MAX_VALUE, second = Double.MAX_VALUE;
        for (int i = cellX - 1; i <= cellX + 1; i++) {
            for (int j = cellY - 1; j <= cellY + 1; j++) {
                for (int k = cellZ - 1; k <= cellZ + 1; k++) {
                    long id = hash(seed, i, j, k);
                    double siteU = i + 0.125 + 0.75 * unit(id), siteV = j + 0.125 + 0.75 * unit(mix(id + 1)), siteW = k + 0.125 + 0.75 * unit(mix(id + 2));
                    double distance = (u - siteU) * (u - siteU) + (v - siteV) * (v - siteV) + (w - siteW) * (w - siteW);
                    if (distance < best) {
                        second = best;
                        out.id2 = out.id1;
                        out.x2 = out.x1;
                        out.y2 = out.y1;
                        out.z2 = out.z1;
                        best = distance;
                        out.id1 = id;
                        out.x1 = siteU * sizeX;
                        out.y1 = siteV * sizeY;
                        out.z1 = siteW * sizeZ;
                    } else if (distance < second) {
                        second = distance;
                        out.id2 = id;
                        out.x2 = siteU * sizeX;
                        out.y2 = siteV * sizeY;
                        out.z2 = siteW * sizeZ;
                    }
                }
            }
        }
        out.f1 = Math.sqrt(best);
        out.f2 = Math.sqrt(second);
        // The border is the plane halfway between the two sites, in cells; its distance in blocks depends on which way it faces
        double nu = (out.x2 - out.x1) / sizeX, nv = (out.y2 - out.y1) / sizeY, nw = (out.z2 - out.z1) / sizeZ;
        double between = Math.sqrt(nu * nu + nv * nv + nw * nw);
        double inCells = (second - best) / (2 * between);
        nu /= between * sizeX;
        nv /= between * sizeY;
        nw /= between * sizeZ;
        out.edge = inCells / Math.sqrt(nu * nu + nv * nv + nw * nw);
    }

    /** The Voronoi diagram over the plane: upright cells {@code size} blocks across. {@code y} values are left at 0. */
    public static void cells(long seed, double x, double z, double size, Cells out) {
        double u = x / size, w = z / size;
        int cellX = floor(u), cellZ = floor(w);
        double best = Double.MAX_VALUE, second = Double.MAX_VALUE;
        for (int i = cellX - 1; i <= cellX + 1; i++) {
            for (int k = cellZ - 1; k <= cellZ + 1; k++) {
                long id = hash(seed, i, 0, k);
                double siteU = i + 0.125 + 0.75 * unit(id), siteW = k + 0.125 + 0.75 * unit(mix(id + 2));
                double distance = (u - siteU) * (u - siteU) + (w - siteW) * (w - siteW);
                if (distance < best) {
                    second = best;
                    out.id2 = out.id1;
                    out.x2 = out.x1;
                    out.z2 = out.z1;
                    best = distance;
                    out.id1 = id;
                    out.x1 = siteU * size;
                    out.z1 = siteW * size;
                } else if (distance < second) {
                    second = distance;
                    out.id2 = id;
                    out.x2 = siteU * size;
                    out.z2 = siteW * size;
                }
            }
        }
        out.y1 = out.y2 = 0;
        out.f1 = Math.sqrt(best);
        out.f2 = Math.sqrt(second);
        double between = Math.hypot(out.x2 - out.x1, out.z2 - out.z1) / size;
        out.edge = (second - best) / (2 * between) * size;
    }

    public static int floor(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    public static double lerp(double t, double from, double to) {
        return from + t * (to - from);
    }

    public static double smoothstep(double from, double to, double value) {
        double t = Math.clamp((value - from) / (to - from), 0, 1);
        return t * t * (3 - 2 * t);
    }
}
