package frootloops.versus.mod.environment.worldgen.aquifer;

import frootloops.versus.mod.environment.worldgen.WorldgenTestData;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A prototype of the owner's second river (the refactor plan, Section 10, question 7): a river on its own noise
 * ({@code players-versus:overworld/high_river}) with its water at y 80, in ground that rises above y 80, spilling where
 * the ground drops away. Shapes played out on exact values over areas of such ground, with a map and cross-sections of
 * the first.
 *
 * <p>The first prototypes let the aquifer carve a channel at a fixed height, from the depth at y 80; but the ground
 * stands up to 10 or more blocks above what the depth says, so their channels came out as slots with ground over them.
 * Here the river is a valley cut into the final density, as the old rivers' valleys are: open above y 80 where the
 * river noise is within a half width that grows with height (so ground at any height over the river is cut back into
 * banks, never left over it), water at y 80 and in a shallow bed under it, and stone where the bed's water could flow
 * into dry air beside or below it (the aquifer's walls). The water at y 80 has no wall: where the ground drops away
 * beside it, it spills.
 */
class HighRiverSurveyTest {

    private static final long SEED = 8675309L;
    private static final int RIVER_Y = 80;
    /** The blocks of a column the survey looks at. */
    private static final int MIN_Y = 64, MAX_Y = 128, HEIGHT = MAX_Y - MIN_Y;
    private static final int AREA = 128, AREAS = 6;
    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /** Blocks: the terrain's, and what the river makes of them. */
    private static final byte SOLID = 0, AIR = 1, CARVED = 2, SURFACE_WATER = 3, BED_WATER = 4, WALL = 5;

    /**
     * A river shape. Along the river, its activity {@code a} is 0 where the ground at the column is below y 80 (the
     * depth at y 80, interpolated between the 4-block corners where vanilla's caches hold it, is at most 0), 1 from a
     * depth of 0.01 up to {@code top}, and fades to 0 over {@code fade} beyond. Across it, a block is the river's where
     * the river noise's magnitude is below the half width: {@code a * (halfWidth + (y - 80) * widening)} above y 80,
     * {@code a * halfWidth} at y 80, and in the bed below, down to {@code bed} blocks, narrowing to nothing.
     */
    private record Shape(String name, double halfWidth, double widening, int bed, double top, double fade) {
        double activity(double depth80) {
            if (depth80 <= 0.0) return 0.0;
            return MathHelper.clamp(depth80 * 100.0, 0.0, 1.0) * MathHelper.clamp((this.top + this.fade - depth80) / this.fade, 0.0, 1.0);
        }

        double halfWidth(int y, double activity) {
            if (y >= RIVER_Y) return activity * (this.halfWidth + (y - RIVER_Y) * this.widening);
            if (y < RIVER_Y - this.bed) return 0.0;
            return activity * this.halfWidth * (1.0 - (double) (RIVER_Y - y) / (this.bed + 1));
        }
    }

    private static final List<Shape> SHAPES = List.of(
            new Shape("half width 0.03, widening 0.006, bed 3, ground up to d 0.25", 0.03, 0.006, 3, 0.25, 0.08),
            new Shape("half width 0.02, widening 0.004, bed 2, ground up to d 0.25", 0.02, 0.004, 2, 0.25, 0.08),
            new Shape("half width 0.03, widening 0.012, bed 3, ground up to d 0.25", 0.03, 0.012, 3, 0.25, 0.08),
            new Shape("half width 0.03, widening 0.006, bed 3, ground up to d 0.12", 0.03, 0.006, 3, 0.12, 0.04));

    @Test
    void survey() {
        NoiseConfig config = WorldgenTestData.noiseConfig(SEED);
        DensityFunction river = WorldgenTestData.seeded(config, "players-versus:overworld/high_river");
        DensityFunction depth = config.getNoiseRouter().depth(), finalDensity = config.getNoiseRouter().finalDensity();
        List<int[]> origins = hillyAreas(depth);
        Stats[] stats = new Stats[SHAPES.size()];
        for (int s = 0; s < stats.length; s++) stats[s] = new Stats();
        double gradient = 0.0;
        long gradients = 0;
        int cornersPerSide = AREA / 4 + 1;
        for (int a = 0; a < origins.size(); a++) {
            int originX = origins.get(a)[0], originZ = origins.get(a)[1];
            double[] noise = new double[AREA * AREA], depth80 = new double[AREA * AREA];
            double[] corners = new double[cornersPerSide * cornersPerSide];
            for (int cx = 0; cx < cornersPerSide; cx++) {
                for (int cz = 0; cz < cornersPerSide; cz++) {
                    corners[cx * cornersPerSide + cz] = depth.sample(new DensityFunction.UnblendedNoisePos(originX + cx * 4, RIVER_Y, originZ + cz * 4));
                }
            }
            for (int x = 0; x < AREA; x++) {
                for (int z = 0; z < AREA; z++) {
                    noise[x * AREA + z] = river.sample(new DensityFunction.UnblendedNoisePos(originX + x, 0, originZ + z));
                    int cx = x >> 2, cz = z >> 2;
                    double fx = (x & 3) / 4.0, fz = (z & 3) / 4.0;
                    double d00 = corners[cx * cornersPerSide + cz], d10 = corners[(cx + 1) * cornersPerSide + cz];
                    double d01 = corners[cx * cornersPerSide + cz + 1], d11 = corners[(cx + 1) * cornersPerSide + cz + 1];
                    depth80[x * AREA + z] = MathHelper.lerp(fz, MathHelper.lerp(fx, d00, d10), MathHelper.lerp(fx, d01, d11));
                    if (x > 0) {
                        gradient += Math.abs(noise[x * AREA + z] - noise[(x - 1) * AREA + z]);
                        gradients++;
                    }
                }
            }
            Terrain terrain = new Terrain(finalDensity, originX, originZ);
            for (int s = 0; s < SHAPES.size(); s++) {
                Shape shape = SHAPES.get(s);
                byte[][] after = apply(shape, noise, depth80, terrain);
                stats[s].add(terrain, after);
                if (a == 0 && s == 0) print(shape, noise, depth80, terrain, after, originX, originZ);
            }
        }
        double chunks = origins.size() * AREA * AREA / 256.0;
        System.out.printf(Locale.ROOT, "[high river] %d areas of %d x %d blocks at %s; the river noise changes by %.4f per block on"
                        + " average; per chunk:%n", origins.size(), AREA, AREA, origins.stream().map(o -> o[0] + "," + o[1]).toList(),
                gradient / gradients);
        for (int s = 0; s < SHAPES.size(); s++) System.out.println("[high river] " + SHAPES.get(s).name() + ": " + stats[s].describe(chunks));
        assertTrue(stats[0].surfaceWater > 0, "the first shape made no river in the survey's areas");
    }

    /**
     * The areas: of 400 random places, those where the most of a coarse grid has ground a little above y 80 (depth at
     * y 80 in 0..0.12), at least an area apart.
     */
    private static List<int[]> hillyAreas(DensityFunction depth) {
        Random random = new Random(SEED);
        List<int[]> places = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            int x = (random.nextInt(4000) - 2000) & ~15, z = (random.nextInt(4000) - 2000) & ~15;
            int score = 0;
            for (int dx = 0; dx < AREA; dx += 16) {
                for (int dz = 0; dz < AREA; dz += 16) {
                    double d = depth.sample(new DensityFunction.UnblendedNoisePos(x + dx, RIVER_Y, z + dz));
                    if (d > 0.0 && d < 0.12) score++;
                }
            }
            places.add(new int[]{x, z});
            scores.add(score);
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < places.size(); i++) order.add(i);
        order.sort((p, q) -> Integer.compare(scores.get(q), scores.get(p)));
        List<int[]> areas = new ArrayList<>();
        for (int i : order) {
            int[] place = places.get(i);
            if (areas.stream().anyMatch(o -> Math.abs(o[0] - place[0]) < AREA && Math.abs(o[1] - place[1]) < AREA)) continue;
            areas.add(place);
            if (areas.size() == AREAS) break;
        }
        return areas;
    }

    /** The terrain in y 64..127 of an area's columns, sampled when first needed (exact values, before the river). */
    private static final class Terrain {
        private final DensityFunction finalDensity;
        private final int originX, originZ;
        private final byte[][] columns = new byte[AREA * AREA][];

        Terrain(DensityFunction finalDensity, int originX, int originZ) {
            this.finalDensity = finalDensity;
            this.originX = originX;
            this.originZ = originZ;
        }

        byte[] column(int x, int z) {
            int i = x * AREA + z;
            if (this.columns[i] == null) {
                byte[] column = new byte[HEIGHT];
                for (int y = MIN_Y; y < MAX_Y; y++) {
                    double density = this.finalDensity.sample(new DensityFunction.UnblendedNoisePos(this.originX + x, y, this.originZ + z));
                    column[y - MIN_Y] = density > 0.0 ? SOLID : AIR;
                }
                this.columns[i] = column;
            }
            return this.columns[i];
        }

        /** The highest solid block of the column in y 64..127, or 63 if none. */
        int surface(int x, int z) {
            byte[] column = this.column(x, z);
            for (int y = MAX_Y - 1; y >= MIN_Y; y--) if (column[y - MIN_Y] == SOLID) return y;
            return MIN_Y - 1;
        }
    }

    private static boolean inArea(int x, int z) {
        return x >= 0 && x < AREA && z >= 0 && z < AREA;
    }

    /** The river's blocks by column (null where the river leaves the column as it was), walls included. */
    private static byte[][] apply(Shape shape, double[] noise, double[] depth80, Terrain terrain) {
        byte[][] after = new byte[AREA * AREA][];
        for (int x = 0; x < AREA; x++) {
            for (int z = 0; z < AREA; z++) {
                double activity = shape.activity(depth80[x * AREA + z]);
                double across = Math.abs(noise[x * AREA + z]);
                if (activity <= 0.0 || across >= shape.halfWidth(MAX_Y - 1, activity)) continue;
                byte[] column = terrain.column(x, z).clone();
                boolean changed = false;
                for (int y = RIVER_Y - shape.bed(); y < MAX_Y; y++) {
                    if (across >= shape.halfWidth(y, activity)) continue;
                    int i = y - MIN_Y;
                    if (y > RIVER_Y) {
                        if (column[i] == SOLID) column[i] = CARVED;
                    } else {
                        column[i] = y == RIVER_Y ? SURFACE_WATER : BED_WATER;
                    }
                    changed = true;
                }
                if (changed) after[x * AREA + z] = column;
            }
        }
        // walls: open blocks beside or below the bed's water become stone (the water at y 80 may spill)
        for (int x = 0; x < AREA; x++) {
            for (int z = 0; z < AREA; z++) {
                byte[] column = after[x * AREA + z];
                if (column == null) continue;
                for (int y = RIVER_Y - shape.bed(); y < RIVER_Y; y++) {
                    if (column[y - MIN_Y] != BED_WATER) continue;
                    if (open(column[y - 1 - MIN_Y])) column[y - 1 - MIN_Y] = WALL;
                    for (int[] side : SIDES) {
                        int nx = x + side[0], nz = z + side[1];
                        if (!inArea(nx, nz)) continue;
                        byte[] neighbour = after[nx * AREA + nz];
                        if (neighbour == null) {
                            if (!open(terrain.column(nx, nz)[y - MIN_Y])) continue;
                            neighbour = terrain.column(nx, nz).clone();
                            after[nx * AREA + nz] = neighbour;
                        }
                        if (open(neighbour[y - MIN_Y])) neighbour[y - MIN_Y] = WALL;
                    }
                }
            }
        }
        return after;
    }

    private static byte at(byte[][] after, Terrain terrain, int x, int z, int y) {
        byte[] column = after[x * AREA + z];
        return column != null ? column[y - MIN_Y] : terrain.column(x, z)[y - MIN_Y];
    }

    private static boolean open(byte block) {
        return block == AIR || block == CARVED;
    }

    private static boolean water(byte block) {
        return block == SURFACE_WATER || block == BED_WATER;
    }

    /** Totals for one shape over every area. */
    private static final class Stats {
        long riverColumns, surfaceWater, bedWater, filledOpen, carved, walls, sideSpills, spillColumns, downSpills, roofed;
        /** River columns by the ground's height above y 80: none (below 81), 1..3, 4..7, 8..11, 12..15, 16 and more. */
        final long[] heights = new long[6];

        void add(Terrain terrain, byte[][] after) {
            for (int x = 0; x < AREA; x++) {
                for (int z = 0; z < AREA; z++) {
                    byte[] column = after[x * AREA + z];
                    if (column == null) continue;
                    byte[] before = terrain.column(x, z);
                    for (int y = MIN_Y; y < MAX_Y; y++) {
                        int i = y - MIN_Y;
                        switch (column[i]) {
                            case SURFACE_WATER -> this.surfaceWater++;
                            case BED_WATER -> this.bedWater++;
                            case CARVED -> this.carved++;
                            case WALL -> this.walls++;
                            default -> {
                            }
                        }
                        if (water(column[i]) && before[i] == AIR) this.filledOpen++;
                    }
                    if (column[RIVER_Y - MIN_Y] != SURFACE_WATER) continue;
                    this.riverColumns++;
                    int above = terrain.surface(x, z) - RIVER_Y;
                    this.heights[above <= 0 ? 0 : above < 4 ? 1 : above < 8 ? 2 : above < 12 ? 3 : above < 16 ? 4 : 5]++;
                    boolean spilling = false;
                    for (int[] side : SIDES) {
                        int nx = x + side[0], nz = z + side[1];
                        if (inArea(nx, nz) && open(at(after, terrain, nx, nz, RIVER_Y))) {
                            this.sideSpills++;
                            spilling = true;
                        }
                    }
                    if (spilling) this.spillColumns++;
                    if (open(column[RIVER_Y - 1 - MIN_Y])) this.downSpills++;
                    boolean openSeen = false;
                    for (int y = RIVER_Y + 1; y < MAX_Y; y++) {
                        byte block = column[y - MIN_Y];
                        if (open(block)) openSeen = true;
                        else if (openSeen && block == SOLID) {
                            this.roofed++;
                            break;
                        }
                    }
                }
            }
        }

        String describe(double chunks) {
            return String.format(Locale.ROOT, "river columns %.1f (ground above y 80 by 0: %.1f, 1..3: %.1f, 4..7: %.1f, 8..11: %.1f,"
                            + " 12..15: %.1f, 16+: %.1f); water at y 80 %.1f, in the bed %.1f (%.2f of it where the terrain was open);"
                            + " carved above y 80 %.1f; walls %.2f; water faces at y 80 spilling sideways %.2f (from %.2f columns), down %.2f;"
                            + " columns with solid ground over open air above the water %.2f",
                    this.riverColumns / chunks, this.heights[0] / chunks, this.heights[1] / chunks, this.heights[2] / chunks,
                    this.heights[3] / chunks, this.heights[4] / chunks, this.heights[5] / chunks, this.surfaceWater / chunks,
                    this.bedWater / chunks, this.filledOpen / chunks, this.carved / chunks, this.walls / chunks,
                    this.sideSpills / chunks, this.spillColumns / chunks, this.downSpills / chunks, this.roofed / chunks);
        }
    }

    /**
     * A map of the area (a character per 2 x 2 columns, north up: {@code *} water at y 80 that spills, {@code ~} river
     * water, {@code .} ground at or above y 81, {@code ,} ground at y 64..80, space lower) and cross-sections through three
     * points near the rivers' middles (y 104 down to 72; {@code #} solid, space open, {@code -} carved, {@code ~} water at
     * y 80, {@code =} water in the bed, {@code o} a wall).
     */
    private static void print(Shape shape, double[] noise, double[] depth80, Terrain terrain, byte[][] after, int originX, int originZ) {
        System.out.println("[high river] map of the area at " + originX + "," + originZ + ", " + shape.name() + ":");
        for (int z = 0; z < AREA; z += 2) {
            StringBuilder row = new StringBuilder("[high river]   ");
            for (int x = 0; x < AREA; x += 2) {
                char c = ' ';
                for (int dx = 0; dx < 2; dx++) {
                    for (int dz = 0; dz < 2; dz++) {
                        char here = cell(terrain, after, x + dx, z + dz);
                        if (rank(here) > rank(c)) c = here;
                    }
                }
                row.append(c);
            }
            System.out.println(row);
        }
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < noise.length; i++) {
            int x = i / AREA, z = i % AREA;
            if (after[i] != null && after[i][RIVER_Y - MIN_Y] == SURFACE_WATER && x >= 24 && x < AREA - 24 && z >= 24 && z < AREA - 24) {
                candidates.add(i);
            }
        }
        candidates.sort((i, j) -> Double.compare(Math.abs(noise[i]), Math.abs(noise[j])));
        List<Integer> chosen = new ArrayList<>();
        for (int i : candidates) {
            int x = i / AREA, z = i % AREA;
            if (chosen.stream().anyMatch(j -> Math.abs(j / AREA - x) + Math.abs(j % AREA - z) < 32)) continue;
            chosen.add(i);
            if (chosen.size() == 3) break;
        }
        for (int i : chosen) {
            int x0 = i / AREA, z0 = i % AREA;
            for (int axis = 0; axis < 2; axis++) {
                System.out.printf(Locale.ROOT, "[high river] cross-section along %s through %d,%d (depth at y 80 %.3f):%n", axis == 0 ? "x" : "z",
                        originX + x0, originZ + z0, depth80[i]);
                for (int y = 104; y >= 72; y--) {
                    StringBuilder row = new StringBuilder(String.format(Locale.ROOT, "[high river] %3d ", y));
                    for (int t = -24; t <= 24; t++) {
                        int x = axis == 0 ? x0 + t : x0, z = axis == 0 ? z0 : z0 + t;
                        row.append(switch (at(after, terrain, x, z, y)) {
                            case SOLID -> '#';
                            case CARVED -> '-';
                            case SURFACE_WATER -> '~';
                            case BED_WATER -> '=';
                            case WALL -> 'o';
                            default -> ' ';
                        });
                    }
                    System.out.println(row);
                }
            }
        }
    }

    private static char cell(Terrain terrain, byte[][] after, int x, int z) {
        byte[] column = after[x * AREA + z];
        if (column != null && column[RIVER_Y - MIN_Y] == SURFACE_WATER) {
            for (int[] side : SIDES) {
                int nx = x + side[0], nz = z + side[1];
                if (inArea(nx, nz) && open(at(after, terrain, nx, nz, RIVER_Y))) return '*';
            }
            return '~';
        }
        int surface = terrain.surface(x, z);
        return surface > RIVER_Y ? '.' : surface >= MIN_Y ? ',' : ' ';
    }

    private static int rank(char c) {
        return switch (c) {
            case '*' -> 4;
            case '~' -> 3;
            case '.' -> 2;
            case ',' -> 1;
            default -> 0;
        };
    }
}
