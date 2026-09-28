package frootloops.versus.mod.environment.worldgen.aquifer;

import frootloops.versus.mod.environment.worldgen.WorldgenTestData;
import net.minecraft.util.math.ChunkPos;
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
 * ({@code players-versus:overworld/high_river}) with its water at y 80, cut into ground whose surface is a little above
 * y 80, so that its water spills where the ground drops away. Shapes played out on exact values over areas of such
 * ground: how much each carves and fills, where its water spills (waterfalls), how deep its channel is, with a map and
 * cross-sections of the first shape.
 */
class HighRiverSurveyTest {

    private static final long SEED = 8675309L;
    private static final int RIVER_Y = 80;
    /** The blocks of a column the survey looks at. */
    private static final int MIN_Y = 64, MAX_Y = 128, HEIGHT = MAX_Y - MIN_Y;
    private static final int AREA = 128, AREAS = 6;
    private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /** Blocks before the river (the terrain) and after. */
    private static final byte SOLID = 0, AIR = 1, CARVED = 2, SURFACE_WATER = 3, CORE_WATER = 4, PLUG = 5, BANK = 6;

    /**
     * A river shape. The river map {@code m = max(0, 1 - (r / halfWidth)^2)} ({@code r}: the river noise) narrows where
     * the ground rises: {@code s = m * clamp((top - d) / fade, 0, 1)}, {@code d} the depth at y 80, and there's no river
     * where the ground is below y 80 ({@code d <= 0}). At y 80, water where {@code s > surface}, whatever was there;
     * above, open up to {@code above} blocks higher where {@code s > surface - (y - 80) * slope} (at least {@code edge}),
     * so the banks slope back as they rise; below, with {@code g = max(0, 1 - (80 - y) / below)} and the ground factor
     * {@code f = min(1, d / full)}, water where {@code s g f >= core} and the four blocks beside and the one below are in
     * the river ({@code s g > 0}), and stone in the rest of the river, caves included.
     */
    private record Shape(String name, double halfWidth, double top, double fade, double surface, double slope, double edge,
                         int above, int below, double full, double core) {
        double strength(double river, double depth80) {
            if (depth80 <= 0.0) return 0.0;
            double across = river / this.halfWidth;
            return Math.max(0.0, 1.0 - across * across) * MathHelper.clamp((this.top - depth80) / this.fade, 0.0, 1.0);
        }

        double openFrom(int y) {
            return Math.max(this.edge, this.surface - (y - RIVER_Y) * this.slope);
        }

        double below(int y) {
            return Math.max(0.0, 1.0 - (double) (RIVER_Y - y) / this.below);
        }

        double ground(double depth80) {
            return Math.min(1.0, depth80 / this.full);
        }
    }

    private static final List<Shape> SHAPES = List.of(
            new Shape("half width 0.03, ground up to d 0.06", 0.03, 0.06, 0.02, 0.3, 0.06, 0.02, 12, 4, 0.02, 0.25),
            new Shape("half width 0.02, ground up to d 0.06", 0.02, 0.06, 0.02, 0.3, 0.06, 0.02, 12, 4, 0.02, 0.25),
            new Shape("half width 0.04, ground up to d 0.06", 0.04, 0.06, 0.02, 0.3, 0.06, 0.02, 12, 4, 0.02, 0.25),
            new Shape("half width 0.03, ground up to d 0.1", 0.03, 0.1, 0.03, 0.3, 0.06, 0.02, 16, 4, 0.02, 0.25));

    @Test
    void survey() {
        NoiseConfig config = WorldgenTestData.noiseConfig(SEED);
        DensityFunction river = WorldgenTestData.seeded(config, "players-versus:overworld/high_river");
        DensityFunction depth = config.getNoiseRouter().depth(), finalDensity = config.getNoiseRouter().finalDensity();
        List<int[]> origins = hillyAreas(depth);
        Stats[] stats = new Stats[SHAPES.size()];
        for (int s = 0; s < stats.length; s++) stats[s] = new Stats();
        for (int a = 0; a < origins.size(); a++) {
            int originX = origins.get(a)[0], originZ = origins.get(a)[1];
            double[] noise = new double[AREA * AREA], depth80 = new double[AREA * AREA];
            for (int x = 0; x < AREA; x++) {
                for (int z = 0; z < AREA; z++) {
                    noise[x * AREA + z] = river.sample(new DensityFunction.UnblendedNoisePos(originX + x, 0, originZ + z));
                    depth80[x * AREA + z] = depth.sample(new DensityFunction.UnblendedNoisePos(originX + x, RIVER_Y, originZ + z));
                }
            }
            Terrain terrain = new Terrain(finalDensity, originX, originZ);
            for (int s = 0; s < SHAPES.size(); s++) {
                Shape shape = SHAPES.get(s);
                double[] strength = new double[AREA * AREA];
                for (int i = 0; i < strength.length; i++) strength[i] = shape.strength(noise[i], depth80[i]);
                byte[][] after = apply(shape, strength, depth80, terrain);
                stats[s].add(shape, strength, terrain, after);
                if (a == 0 && s == 0) print(shape, strength, terrain, after, originX, originZ);
            }
        }
        double chunks = origins.size() * AREA * AREA / 256.0;
        System.out.printf(Locale.ROOT, "[high river] %d areas of %d x %d blocks at %s; per chunk:%n", origins.size(), AREA, AREA,
                origins.stream().map(o -> o[0] + "," + o[1]).toList());
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
            System.out.println("[high river] area at " + place[0] + "," + place[1] + ": " + scores.get(i) + " of 64 coarse columns have ground at y 80..95");
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

    /** The river's blocks by column: null where the river leaves the column as it was. */
    private static byte[][] apply(Shape shape, double[] strength, double[] depth80, Terrain terrain) {
        byte[][] after = new byte[AREA * AREA][];
        for (int x = 0; x < AREA; x++) {
            for (int z = 0; z < AREA; z++) {
                double s = strength[x * AREA + z];
                if (s <= 0.0) continue;
                double ground = shape.ground(depth80[x * AREA + z]);
                byte[] column = terrain.column(x, z).clone();
                for (int y = MIN_Y; y < MAX_Y; y++) {
                    int i = y - MIN_Y;
                    if (y > RIVER_Y) {
                        if (y <= RIVER_Y + shape.above() && s > shape.openFrom(y) && column[i] == SOLID) column[i] = CARVED;
                    } else if (y == RIVER_Y) {
                        if (s > shape.surface()) column[i] = SURFACE_WATER;
                    } else {
                        double g = shape.below(y);
                        if (g <= 0.0) continue;
                        boolean core = s * g * ground >= shape.core() && shape.below(y - 1) > 0.0;
                        for (int[] side : SIDES) {
                            int nx = x + side[0], nz = z + side[1];
                            if (!inArea(nx, nz) || strength[nx * AREA + nz] <= 0.0) core = false;
                        }
                        column[i] = core ? CORE_WATER : column[i] == SOLID ? BANK : PLUG;
                    }
                }
                after[x * AREA + z] = column;
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

    /** Totals for one shape over every area. */
    private static final class Stats {
        long riverColumns, surfaceWater, naturalAirAtRiver, coreWater, carved, plugs, spills, spillColumns, roofed;
        /** River columns by the ground's height above y 80: none (below 81), 1..3, 4..7, 8..11, 12..15, 16 and more. */
        final long[] depths = new long[6];

        void add(Shape shape, double[] strength, Terrain terrain, byte[][] after) {
            for (int x = 0; x < AREA; x++) {
                for (int z = 0; z < AREA; z++) {
                    byte[] column = after[x * AREA + z];
                    if (column == null || column[RIVER_Y - MIN_Y] != SURFACE_WATER) continue;
                    this.riverColumns++;
                    int above = terrain.surface(x, z) - RIVER_Y;
                    this.depths[above <= 0 ? 0 : above < 4 ? 1 : above < 8 ? 2 : above < 12 ? 3 : above < 16 ? 4 : 5]++;
                    boolean spilling = false;
                    for (int y = MIN_Y; y < MAX_Y; y++) {
                        switch (column[y - MIN_Y]) {
                            case SURFACE_WATER -> this.surfaceWater++;
                            case CORE_WATER -> this.coreWater++;
                            case CARVED -> this.carved++;
                            case PLUG -> this.plugs++;
                            default -> {
                            }
                        }
                    }
                    if (terrain.column(x, z)[RIVER_Y - MIN_Y] == AIR) this.naturalAirAtRiver++;
                    boolean openSeen = false;
                    for (int y = RIVER_Y + 1; y < MAX_Y; y++) {
                        byte block = column[y - MIN_Y];
                        if (open(block)) openSeen = true;
                        else if (openSeen && block == SOLID) {
                            this.roofed++;
                            break;
                        }
                    }
                    for (int[] side : SIDES) {
                        int nx = x + side[0], nz = z + side[1];
                        if (inArea(nx, nz) && open(at(after, terrain, nx, nz, RIVER_Y))) {
                            this.spills++;
                            spilling = true;
                        }
                    }
                    if (spilling) this.spillColumns++;
                }
            }
        }

        String describe(double chunks) {
            return String.format(Locale.ROOT, "river columns %.1f (ground above y 80 by 0: %.1f, 1..3: %.1f, 4..7: %.1f, 8..11: %.1f,"
                            + " 12..15: %.1f, 16+: %.1f); water at y 80 %.1f (%.2f where it was open), below %.1f; carved above y 80 %.1f;"
                            + " caves plugged %.1f; water faces spilling %.2f, from %.2f columns; columns with solid over open above the water %.2f",
                    this.riverColumns / chunks, this.depths[0] / chunks, this.depths[1] / chunks, this.depths[2] / chunks,
                    this.depths[3] / chunks, this.depths[4] / chunks, this.depths[5] / chunks, this.surfaceWater / chunks,
                    this.naturalAirAtRiver / chunks, this.coreWater / chunks, this.carved / chunks, this.plugs / chunks,
                    this.spills / chunks, this.spillColumns / chunks, this.roofed / chunks);
        }
    }

    /**
     * A map of the area (a character per 2 x 2 columns, north up: {@code *} water that spills, {@code ~} river water,
     * {@code .} ground at or above y 81, {@code ,} ground at y 64..80, space lower) and cross-sections through the
     * three strongest river points (y 96 down to 70; {@code #} solid, space open, {@code -} carved, {@code ~} water at
     * y 80, {@code =} water below, {@code o} a cave plugged, {@code b} bank stone).
     */
    private static void print(Shape shape, double[] strength, Terrain terrain, byte[][] after, int originX, int originZ) {
        System.out.println("[high river] map of the area at " + originX + "," + originZ + ", " + shape.name() + ":");
        for (int z = 0; z < AREA; z += 2) {
            StringBuilder row = new StringBuilder("[high river]   ");
            for (int x = 0; x < AREA; x += 2) {
                char c = ' ';
                for (int dx = 0; dx < 2; dx++) {
                    for (int dz = 0; dz < 2; dz++) {
                        char here = cell(shape, strength, terrain, after, x + dx, z + dz);
                        if (rank(here) > rank(c)) c = here;
                    }
                }
                row.append(c);
            }
            System.out.println(row);
        }
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < strength.length; i++) {
            int x = i / AREA, z = i % AREA;
            if (strength[i] > 0.0 && x >= 24 && x < AREA - 24 && z >= 24 && z < AREA - 24) candidates.add(i);
        }
        candidates.sort((i, j) -> Double.compare(strength[j], strength[i]));
        List<Integer> strongest = new ArrayList<>();
        for (int i : candidates) {
            int x = i / AREA, z = i % AREA;
            if (strongest.stream().anyMatch(j -> Math.abs(j / AREA - x) + Math.abs(j % AREA - z) < 32)) continue;
            strongest.add(i);
            if (strongest.size() == 3) break;
        }
        for (int i : strongest) {
            int x0 = i / AREA, z0 = i % AREA;
            for (int axis = 0; axis < 2; axis++) {
                System.out.printf(Locale.ROOT, "[high river] cross-section along %s through %d,%d:%n", axis == 0 ? "x" : "z",
                        originX + x0, originZ + z0);
                for (int y = 96; y >= 70; y--) {
                    StringBuilder row = new StringBuilder(String.format(Locale.ROOT, "[high river] %3d ", y));
                    for (int t = -20; t <= 20; t++) {
                        int x = axis == 0 ? x0 + t : x0, z = axis == 0 ? z0 : z0 + t;
                        row.append(switch (at(after, terrain, x, z, y)) {
                            case SOLID -> '#';
                            case CARVED -> '-';
                            case SURFACE_WATER -> '~';
                            case CORE_WATER -> '=';
                            case PLUG -> 'o';
                            case BANK -> 'b';
                            default -> ' ';
                        });
                    }
                    System.out.println(row);
                }
            }
        }
    }

    private static char cell(Shape shape, double[] strength, Terrain terrain, byte[][] after, int x, int z) {
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
