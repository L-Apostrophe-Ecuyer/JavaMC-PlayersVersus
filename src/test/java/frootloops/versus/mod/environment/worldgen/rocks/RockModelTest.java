package frootloops.versus.mod.environment.worldgen.rocks;

import frootloops.versus.mod.environment.worldgen.rocks.RockPlan.Body;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.IntBinaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RockModelTest {

    private static final int GROUND = 64;
    private static final IntBinaryOperator FLAT = (x, z) -> GROUND;
    private static final int[][] SIX = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    /** Every shape at a usual size. */
    private static List<Body> everyShape(long seed) {
        return List.of(RockShape.BOULDER.body(seed, 4, 4), RockShape.PILE.body(seed, 5, 6), RockShape.CRAG.body(seed, 12, 6),
                RockShape.DOME.body(seed, 14, 11), RockShape.MONOLITH.body(seed, 30, 9), RockShape.TOWER.body(seed, 40, 8),
                RockShape.NEEDLE.body(seed, 26, 4), RockShape.SPIRE.body(seed, 60, 11), RockShape.MASSIF.body(seed, 45, 22));
    }

    private static RockTestCanvas stamp(Body body, IntBinaryOperator ground, int waterLevel) {
        RockTestCanvas canvas = new RockTestCanvas(ground, waterLevel);
        RockStamper.stamp(RockModel.build(body), body.dx(), ground.applyAsInt(body.dx(), body.dz()), body.dz(), canvas, -1000, -1000, 1000, 1000);
        return canvas;
    }

    private static List<long[]> rock(RockTestCanvas canvas) {
        List<long[]> rock = new ArrayList<>();
        for (long key : canvas.cells.keySet()) {
            int x = RockTestCanvas.x(key), y = RockTestCanvas.y(key), z = RockTestCanvas.z(key);
            if (canvas.isRock(x, y, z)) rock.add(new long[]{x, y, z});
        }
        return rock;
    }

    @Test
    void sameSeedSameRock() {
        Map<Long, String> first = stamp(RockShape.MONOLITH.body(42, 30, 9), FLAT, RockTestCanvas.NO_WATER).cells;
        Map<Long, String> second = stamp(RockShape.MONOLITH.body(42, 30, 9), FLAT, RockTestCanvas.NO_WATER).cells;
        assertTrue(first.size() > 2000, "a monolith thirty blocks tall is a few thousand blocks, not " + first.size());
        assertEquals(first, second);
    }

    @Test
    void anotherSeedAnotherRock() {
        assertNotEquals(stamp(RockShape.MONOLITH.body(42, 30, 9), FLAT, RockTestCanvas.NO_WATER).cells,
                stamp(RockShape.MONOLITH.body(43, 30, 9), FLAT, RockTestCanvas.NO_WATER).cells);
    }

    @Test
    void everyShapeIsAboutTheSizeAsked() {
        for (long seed = 1; seed <= 4; seed++) {
            double[][] sizes = {{4, 4}, {5, 6}, {12, 6}, {14, 11}, {30, 9}, {40, 8}, {26, 4}, {60, 11}, {45, 22}};
            List<Body> bodies = everyShape(seed);
            for (int index = 0; index < bodies.size(); index++) {
                RockTestCanvas canvas = stamp(bodies.get(index), FLAT, RockTestCanvas.NO_WATER);
                int top = GROUND, far = 0;
                for (long[] cell : rock(canvas)) {
                    top = Math.max(top, (int) cell[1] + 1);
                    if (cell[1] >= GROUND) far = Math.max(far, (int) Math.max(Math.abs(cell[0]), Math.abs(cell[2])));
                }
                String what = RockShape.values()[index] + " seed " + seed;
                double height = sizes[index][0], radius = sizes[index][1];
                assertTrue(top - GROUND >= 0.55 * height && top - GROUND <= 1.35 * height + 2, what + ": asked " + height + " tall, is " + (top - GROUND));
                assertTrue(far >= 0.5 * radius && far <= 2.2 * radius + 3, what + ": asked " + radius + " across, reaches " + far);
            }
        }
    }

    @Test
    void nothingFloats() {
        for (long seed = 1; seed <= 4; seed++) {
            for (Body body : everyShape(seed)) {
                assertGrounded(stamp(body, FLAT, RockTestCanvas.NO_WATER), body.lobes().size() + " lobes, seed " + seed);
            }
        }
    }

    /** Every block of rock is joined, face to face, to rock that stands in or on the ground; vines hang, plants stand. */
    private static void assertGrounded(RockTestCanvas canvas, String what) {
        Set<Long> reached = new HashSet<>();
        ArrayDeque<long[]> queue = new ArrayDeque<>();
        List<long[]> rock = rock(canvas);
        assertTrue(rock.size() >= 12, what + ": there is rock");
        for (long[] cell : rock) {
            int x = (int) cell[0], y = (int) cell[1], z = (int) cell[2];
            // On the ground as it was, or on ground the skirt raised
            if (y <= canvas.original(x, z) || "ground".equals(canvas.at(x, y - 1, z))) {
                reached.add(RockTestCanvas.key(x, y, z));
                queue.add(cell);
            }
        }
        while (!queue.isEmpty()) {
            long[] cell = queue.poll();
            for (int[] step : SIX) {
                int x = (int) cell[0] + step[0], y = (int) cell[1] + step[1], z = (int) cell[2] + step[2];
                if (canvas.isRock(x, y, z) && reached.add(RockTestCanvas.key(x, y, z))) queue.add(new long[]{x, y, z});
            }
        }
        assertEquals(rock.size(), reached.size(), what + ": rock that isn't joined to the ground");
        for (Map.Entry<Long, String> entry : canvas.cells.entrySet()) {
            int x = RockTestCanvas.x(entry.getKey()), y = RockTestCanvas.y(entry.getKey()), z = RockTestCanvas.z(entry.getKey());
            if (entry.getValue().startsWith("vine:")) {
                String[] toward = entry.getValue().substring(5).split(",");
                boolean held = canvas.isFullRock(x + Integer.parseInt(toward[0]), y, z + Integer.parseInt(toward[1]));
                String above = canvas.at(x, y + 1, z);
                assertTrue(held || above != null && above.equals(entry.getValue()), what + ": a vine on nothing at " + x + " " + y + " " + z);
            } else if (entry.getValue().equals("plant")) {
                assertEquals("rock:GRASS", canvas.at(x, y - 1, z), what + ": a plant that isn't on grass at " + x + " " + y + " " + z);
            }
        }
    }

    @Test
    void stampedChunkByChunkInAnyOrderIsTheRockStampedWhole() {
        IntBinaryOperator hills = (x, z) -> GROUND + (int) Math.round(3 * Math.sin(x / 9.0) + 2 * Math.cos(z / 7.0));
        Body body = RockShape.MASSIF.body(3, 45, 22).at(5, -7);
        RockModel model = RockModel.build(body);
        int anchor = hills.applyAsInt(5, -7);
        RockTestCanvas whole = new RockTestCanvas(hills, RockTestCanvas.NO_WATER);
        RockStamper.stamp(model, 5, anchor, -7, whole, -1000, -1000, 1000, 1000);
        assertTrue(whole.cells.size() > 10_000, "a massif is many thousands of blocks");

        List<int[]> chunks = new ArrayList<>();
        for (int chunkX = -8; chunkX <= 8; chunkX++) {
            for (int chunkZ = -8; chunkZ <= 8; chunkZ++) chunks.add(new int[]{chunkX, chunkZ});
        }
        Collections.shuffle(chunks, new Random(7));
        RockTestCanvas pieces = new RockTestCanvas(hills, RockTestCanvas.NO_WATER);
        for (int[] chunk : chunks) {
            RockStamper.stamp(model, 5, anchor, -7, pieces, chunk[0] * 16, chunk[1] * 16, chunk[0] * 16 + 15, chunk[1] * 16 + 15);
        }
        assertEquals(whole.cells, pieces.cells);
    }

    @Test
    void staysWithinItsReach() {
        for (long seed = 1; seed <= 4; seed++) {
            for (Body body : everyShape(seed)) {
                RockModel model = RockModel.build(body);
                RockTestCanvas canvas = new RockTestCanvas(FLAT, RockTestCanvas.NO_WATER);
                RockStamper.stamp(model, 0, GROUND, 0, canvas, -1000, -1000, 1000, 1000);
                assertTrue(model.reach() > 0, "a rock reaches somewhere");
                for (long key : canvas.cells.keySet()) {
                    int far = Math.max(Math.abs(RockTestCanvas.x(key)), Math.abs(RockTestCanvas.z(key)));
                    assertTrue(far <= model.reach(), "wrote " + far + " blocks out, past its reach of " + model.reach());
                }
            }
        }
    }

    @Test
    void stampsOnlyInsideTheBoxGiven() {
        RockTestCanvas canvas = new RockTestCanvas(FLAT, RockTestCanvas.NO_WATER);
        RockStamper.stamp(RockModel.build(RockShape.MONOLITH.body(5, 30, 9)), 0, GROUND, 0, canvas, 0, -3, 15, 12);
        assertTrue(canvas.cells.size() > 300, "the part of the monolith inside the box");
        for (long key : canvas.cells.keySet()) {
            int x = RockTestCanvas.x(key), z = RockTestCanvas.z(key);
            assertTrue(x >= 0 && x <= 15 && z >= -3 && z <= 12, "wrote outside the box at " + x + ", " + z);
        }
    }

    @Test
    void standsOnASlope() {
        // The ground falls a block every two blocks: twelve blocks under a monolith's width
        IntBinaryOperator slope = (x, z) -> GROUND - Math.floorDiv(x, 2);
        for (long seed = 1; seed <= 4; seed++) {
            assertGrounded(stamp(RockShape.MONOLITH.body(seed, 30, 9), slope, RockTestCanvas.NO_WATER), "monolith on a slope, seed " + seed);
            assertGrounded(stamp(RockShape.PILE.body(seed, 5, 6), slope, RockTestCanvas.NO_WATER), "pile on a slope, seed " + seed);
        }
    }

    @Test
    void aMassifHasSeveralTops() {
        for (long seed = 1; seed <= 6; seed++) {
            RockTestCanvas canvas = stamp(RockShape.MASSIF.body(seed, 45, 22), FLAT, RockTestCanvas.NO_WATER);
            int size = 121, half = 60;
            int[][] tops = new int[size][size];
            int highest = 0;
            for (long[] cell : rock(canvas)) {
                int gx = (int) cell[0] + half, gz = (int) cell[2] + half;
                if (gx < 0 || gx >= size || gz < 0 || gz >= size) continue;
                tops[gx][gz] = Math.max(tops[gx][gz], (int) cell[1] - GROUND + 1);
                highest = Math.max(highest, tops[gx][gz]);
            }
            int most = 0;
            for (double share = 0.5; share <= 0.97; share += 0.03) most = Math.max(most, peaks(tops, (int) (share * highest)));
            assertTrue(most >= 2, "seed " + seed + ": a massif has several tops with clefts between, not " + most);
        }
    }

    @Test
    void aSpireIsOneColumnWithAFlatOvergrownTop() {
        for (long seed = 1; seed <= 6; seed++) {
            RockTestCanvas canvas = stamp(RockShape.SPIRE.body(seed, 66, 12), FLAT, RockTestCanvas.NO_WATER);
            int size = 121, half = 60;
            int[][] tops = new int[size][size];
            int highest = 0;
            for (long[] cell : rock(canvas)) {
                int gx = (int) cell[0] + half, gz = (int) cell[2] + half;
                tops[gx][gz] = Math.max(tops[gx][gz], (int) cell[1] - GROUND + 1);
                highest = Math.max(highest, tops[gx][gz]);
            }
            int level = 0, green = 0;
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    if (tops[x][z] < highest - 2) continue;
                    level++;
                    String cell = canvas.at(x - half, GROUND + tops[x][z] - 1, z - half);
                    if ("rock:GRASS".equals(cell) || "rock:MOSS".equals(cell)) green++;
                }
            }
            assertEquals(1, peaks(tops, highest - 2), "seed " + seed + ": one top, not split into fingers");
            assertTrue(level >= 9, "seed " + seed + ": the top is flat: " + level + " columns within two blocks of it");
            assertTrue(green >= 0.5 * level, "seed " + seed + ": the top is overgrown: " + green + " of " + level);
        }
    }

    @Test
    void tallRocksAreFluted() {
        // Grooves down the faces, like organ pipes: open cells a block wide with rock on both sides; at least twice as many
        // as the same rock has without its flutes (cracks between its pieces make some of those)
        for (RockShape shape : new RockShape[]{RockShape.SPIRE, RockShape.TOWER}) {
            for (long seed = 1; seed <= 4; seed++) {
                double height = shape == RockShape.SPIRE ? 66 : 40, radius = shape == RockShape.SPIRE ? 12 : 8;
                Body body = shape.body(seed, height, radius).withCover(0);
                int fluted = grooves(stamp(body, FLAT, RockTestCanvas.NO_WATER), height), plain = grooves(stamp(withoutFlutes(body), FLAT, RockTestCanvas.NO_WATER), height);
                assertTrue(body.flutes() > 0, shape + " has flutes");
                assertTrue(fluted >= 2 * plain && fluted >= 14, shape + " seed " + seed + ": its faces are fluted: " + fluted + " groove cells, " + plain + " without flutes");
            }
        }
    }

    /** Groove cells in every third slice from a quarter of the height to seven tenths of it. */
    private static int grooves(RockTestCanvas canvas, double height) {
        int grooves = 0;
        for (int y = GROUND + (int) (0.25 * height); y <= GROUND + (int) (0.7 * height); y += 3) {
            for (int x = -40; x <= 40; x++) {
                for (int z = -40; z <= 40; z++) {
                    if (canvas.isRock(x, y, z)) continue;
                    if (canvas.isRock(x - 1, y, z) && canvas.isRock(x + 1, y, z) || canvas.isRock(x, y, z - 1) && canvas.isRock(x, y, z + 1)) grooves++;
                }
            }
        }
        return grooves;
    }

    private static Body withoutFlutes(Body body) {
        return new Body(body.dx(), body.dz(), body.lobes(), body.warp(), body.warpScale(), body.pieces(), body.chunks(), body.pinnacles(), body.pinnacleDrop(),
                body.cleft(), body.roughness(), 0, body.fluteScale(), body.dusk(), body.cover(), body.coverTop(), body.drape(), body.vines(), body.plants(), body.slabs(),
                body.skirtWidth(), body.skirtHeight(), body.scree(), body.seed());
    }

    @Test
    void aMonolithLeansAndItsUpperHalfIsOvergrown() {
        for (long seed = 1; seed <= 6; seed++) {
            Body body = RockShape.MONOLITH.body(seed, 32, 10);
            RockTestCanvas canvas = stamp(body, FLAT, RockTestCanvas.NO_WATER);
            int highest = 0;
            for (long[] cell : rock(canvas)) highest = Math.max(highest, (int) cell[1]);
            double footX = 0, footZ = 0, topX = 0, topZ = 0;
            int foot = 0, top = 0, open = 0, green = 0;
            for (long[] cell : rock(canvas)) {
                int x = (int) cell[0], y = (int) cell[1], z = (int) cell[2];
                if (y == GROUND + 1) {
                    footX += x;
                    footZ += z;
                    foot++;
                }
                if (y >= highest - 3) {
                    topX += x;
                    topZ += z;
                    top++;
                }
                boolean exposed = false;
                for (int[] step : SIX) exposed |= !canvas.isRock(x + step[0], y + step[1], z + step[2]);
                if (!exposed || y < GROUND + (highest - GROUND) / 2) continue;
                open++;
                String material = canvas.at(x, y, z);
                if (material.equals("rock:GRASS") || material.equals("rock:MOSS")) green++;
            }
            double lean = Math.hypot(topX / top - footX / foot, topZ / top - footZ / foot);
            assertTrue(lean >= 0.35 * 10, "seed " + seed + ": the monolith leans: its top is " + lean + " blocks off its foot");
            assertTrue(green >= 0.2 * open, "seed " + seed + ": its upper half is overgrown: " + green + " of " + open + " open blocks");
        }
    }

    @Test
    void aMonolithHasATerracedRampOnOneSide() {
        for (long seed = 1; seed <= 6; seed++) {
            RockModel model = RockModel.build(RockShape.MONOLITH.body(seed, 32, 10));
            int highest = 0;
            for (int x = model.groundMinX; x < model.groundMinX + model.groundSizeX; x++) {
                for (int z = model.groundMinZ; z < model.groundMinZ + model.groundSizeZ; z++) highest = Math.max(highest, model.rise(x, z));
            }
            assertTrue(highest >= 4, "seed " + seed + ": the ramp rises four blocks or more, not " + highest);
        }
    }

    /** How many separate groups of at least three columns reach this height. */
    private static int peaks(int[][] tops, int level) {
        boolean[][] seen = new boolean[tops.length][tops.length];
        int peaks = 0;
        for (int x = 0; x < tops.length; x++) {
            for (int z = 0; z < tops.length; z++) {
                if (seen[x][z] || tops[x][z] < level) continue;
                int columns = 0;
                ArrayDeque<int[]> queue = new ArrayDeque<>();
                queue.add(new int[]{x, z});
                seen[x][z] = true;
                while (!queue.isEmpty()) {
                    int[] column = queue.poll();
                    columns++;
                    for (int[] step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        int nx = column[0] + step[0], nz = column[1] + step[1];
                        if (nx < 0 || nz < 0 || nx >= tops.length || nz >= tops.length || seen[nx][nz] || tops[nx][nz] < level) continue;
                        seen[nx][nz] = true;
                        queue.add(new int[]{nx, nz});
                    }
                }
                if (columns >= 3) peaks++;
            }
        }
        return peaks;
    }

    @Test
    void grassOnlyUnderTheSkyAndMossOnlyOnTheSurface() {
        for (long seed = 1; seed <= 4; seed++) {
            for (Body body : everyShape(seed)) {
                RockTestCanvas canvas = stamp(body, FLAT, RockTestCanvas.NO_WATER);
                assertTrue(rock(canvas).size() >= 12, "there is rock");
                Map<Long, Integer> highest = new java.util.HashMap<>();
                for (long[] cell : rock(canvas)) highest.merge(RockTestCanvas.key((int) cell[0], 0, (int) cell[2]), (int) cell[1], Math::max);
                int grass = 0;
                for (long[] cell : rock(canvas)) {
                    int x = (int) cell[0], y = (int) cell[1], z = (int) cell[2];
                    String material = canvas.at(x, y, z);
                    if (material.equals("rock:GRASS")) {
                        grass++;
                        assertEquals(y, highest.get(RockTestCanvas.key(x, 0, z)), "grass with rock over it at " + x + " " + y + " " + z);
                    } else if (material.equals("rock:MOSS")) {
                        boolean open = false;
                        for (int[] step : SIX) open |= !canvas.isFullRock(x + step[0], y + step[1], z + step[2]);
                        assertTrue(open, "moss shut inside the rock at " + x + " " + y + " " + z);
                    }
                }
                if (body.cover() > 0.4 && body.lobes().getFirst().height() >= 12) assertTrue(grass > 0, "a rock with cover has grass on it");
            }
        }
    }

    @Test
    void nothingGrowsUnderWater() {
        int water = GROUND + 3, wet = 0, grassAbove = 0;
        for (long seed = 1; seed <= 4; seed++) {
            for (Body body : List.of(RockShape.PILE.body(seed, 5, 6), RockShape.DOME.body(seed, 14, 11))) {
                RockTestCanvas canvas = stamp(body, FLAT, water);
                for (Map.Entry<Long, String> entry : canvas.cells.entrySet()) {
                    String cell = entry.getValue();
                    if (RockTestCanvas.y(entry.getKey()) > water) {
                        if (cell.equals("rock:GRASS")) grassAbove++;
                        continue;
                    }
                    if (cell.startsWith("rock:")) wet++;
                    assertTrue(!cell.equals("rock:GRASS") && !cell.equals("rock:SOIL") && !cell.equals("plant") && !cell.startsWith("vine:"),
                            cell + " under water at height " + RockTestCanvas.y(entry.getKey()));
                }
            }
        }
        assertTrue(wet > 200, "rocks stand in the water: " + wet + " blocks of them under it");
        assertTrue(grassAbove > 0, "the domes still have grass above the water");
    }

    @Test
    void upwardFacesAreLighterThanOverhangs() {
        int compared = 0;
        for (long seed = 1; seed <= 6; seed++) {
            RockTestCanvas canvas = stamp(RockShape.MONOLITH.body(seed, 30, 9).withCover(0), FLAT, RockTestCanvas.NO_WATER);
            int[] up = new int[3], down = new int[3];
            for (long[] cell : rock(canvas)) {
                int x = (int) cell[0], y = (int) cell[1], z = (int) cell[2];
                int tone = tone(canvas, x, y, z);
                if (tone < 0 || y < GROUND + 2) continue;
                if (!canvas.isRock(x, y + 1, z)) up[tone]++;
                else if (!canvas.isRock(x, y - 1, z)) down[tone]++;
            }
            assertTrue(share(up, 2) > share(up, 0), "seed " + seed + ": upward faces are more often light than in shade");
            if (down[0] + down[1] + down[2] < 10) continue;
            compared++;
            assertTrue(share(up, 2) > share(down, 2) + 0.1, "seed " + seed + ": upward faces are lighter than the undersides of overhangs");
            assertTrue(share(down, 0) > share(up, 0) + 0.2, "seed " + seed + ": the undersides of overhangs are in shade");
        }
        assertTrue(compared >= 3, "monoliths have overhangs to compare: " + compared + " of 6 had ten blocks of them");
    }

    /** 0 for the rock in shade, 1 for the rock, 2 for the rock in the light; -1 for anything else. */
    private static int tone(RockTestCanvas canvas, int x, int y, int z) {
        String cell = canvas.at(x, y, z);
        if (cell == null) return -1;
        return switch (cell) {
            case "rock:SHADE" -> 0;
            case "rock:BASE" -> 1;
            case "rock:LIGHT" -> 2;
            default -> -1;
        };
    }

    @Test
    void tuffLinesTheCracksAndStoneIsTheWalls() {
        // A crack: an open cell a block wide with rock on both sides of it, that goes on two blocks deep or more (a
        // flute, a groove a block deep, is shaded by the game's own light). A wall: rock that faces open air
        for (long seed = 1; seed <= 4; seed++) {
            for (Body body : List.of(RockShape.MONOLITH.body(seed, 30, 9).withCover(0), RockShape.SPIRE.body(seed, 60, 11).withCover(0))) {
                RockTestCanvas canvas = stamp(body, FLAT, RockTestCanvas.NO_WATER);
                int[] crack = new int[3], wall = new int[3];
                for (long[] cell : rock(canvas)) {
                    int x = (int) cell[0], y = (int) cell[1], z = (int) cell[2];
                    int tone = tone(canvas, x, y, z);
                    if (tone < 0 || y < GROUND + 2 || !canvas.isRock(x, y + 1, z) || !canvas.isRock(x, y - 1, z)) continue;
                    boolean inCrack = false, open = false, inGroove = false;
                    for (int[] step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        int gapX = x + step[0], gapZ = z + step[1];
                        if (canvas.isRock(gapX, y, gapZ)) continue;
                        // The floor of a groove (a flute): it looks out between two walls, and is in their shadow
                        if (canvas.isRock(gapX + step[1], y, gapZ + step[0]) && canvas.isRock(gapX - step[1], y, gapZ - step[0])) inGroove = true;
                        if (canvas.isRock(gapX + step[0], y, gapZ + step[1])) {
                            // Deep: the gap goes on, across the line from this wall to the other, on one side or the other
                            for (int side = -1; side <= 1; side += 2) {
                                int nextX = gapX + side * step[1], nextZ = gapZ + side * step[0];
                                if (!canvas.isRock(nextX, y, nextZ) && canvas.isRock(nextX - step[0], y, nextZ - step[1]) && canvas.isRock(nextX + step[0], y, nextZ + step[1])) inCrack = true;
                            }
                        } else if (!canvas.isRock(x + 3 * step[0], y, z + 3 * step[1])) {
                            open = true;
                        }
                    }
                    if (inCrack) crack[tone]++;
                    else if (open && !inGroove) wall[tone]++;
                }
                assertTrue(crack[0] + crack[1] + crack[2] >= 15, "seed " + seed + ": the rock has cracks: " + (crack[0] + crack[1] + crack[2]));
                assertTrue(share(crack, 0) >= 0.6, "seed " + seed + ": cracks are lined with the rock in shade: " + share(crack, 0));
                assertTrue(share(wall, 1) >= 0.55, "seed " + seed + ": open walls are mostly plain rock: " + share(wall, 1));
                assertTrue(share(wall, 0) <= 0.2, "seed " + seed + ": little shade on open walls: " + share(wall, 0));
            }
        }
    }

    private static double share(int[] tones, int tone) {
        int all = tones[0] + tones[1] + tones[2];
        return all == 0 ? 0 : (double) tones[tone] / all;
    }

    @Test
    void stoneIsMostAndTheOthersAreAccents() {
        for (long seed = 1; seed <= 4; seed++) {
            for (Body body : List.of(RockShape.TOWER.body(seed, 40, 8).withCover(0), RockShape.MONOLITH.body(seed, 30, 9).withCover(0), RockShape.PILE.body(seed, 5, 6).withCover(0))) {
                RockTestCanvas canvas = stamp(body, FLAT, RockTestCanvas.NO_WATER);
                int[] tones = new int[3];
                for (long[] cell : rock(canvas)) {
                    int x = (int) cell[0], y = (int) cell[1], z = (int) cell[2];
                    boolean exposed = false;
                    for (int[] step : SIX) exposed |= !canvas.isRock(x + step[0], y + step[1], z + step[2]);
                    if (exposed && y >= GROUND && tone(canvas, x, y, z) >= 0) tones[tone(canvas, x, y, z)]++;
                }
                String what = body.lobes().size() + " lobes, seed " + seed + ": shade " + share(tones, 0) + ", stone " + share(tones, 1) + ", light " + share(tones, 2);
                assertTrue(share(tones, 1) >= 0.5, what + ": the rock is mostly plain rock");
                assertTrue(share(tones, 2) >= 0.08 && share(tones, 2) <= 0.34, what + ": the light is an accent");
                // The walls of every crack are shade, and a crack's walls are many blocks for the little that shows of them
                // (tuffLinesTheCracksAndStoneIsTheWalls looks at the open walls alone)
                assertTrue(share(tones, 0) >= 0.05 && share(tones, 0) <= 0.42, what + ": the shade is an accent");
            }
        }
    }

    @Test
    void aRockIsThreeTonesOfRockUnderItsPlants() {
        // No broken or mossy rock, no gravel: the rock, its shade and its light, and on its level tops grass, moss and earth
        java.util.Set<String> allowed = java.util.Set.of("rock:SHADE", "rock:BASE", "rock:LIGHT", "rock:GRASS", "rock:MOSS", "rock:SOIL",
                "slab:SHADE", "slab:BASE", "slab:LIGHT", "ground", "plant");
        for (long seed = 1; seed <= 3; seed++) {
            for (Body body : everyShape(seed)) {
                for (int water : new int[]{RockTestCanvas.NO_WATER, GROUND + 2}) {
                    for (String cell : stamp(body, FLAT, water).cells.values()) {
                        assertTrue(allowed.contains(cell) || cell.startsWith("vine:"), "a rock is not made of " + cell);
                    }
                }
            }
        }
    }

    @Test
    void aRockWithPlantsHasAFlatTopOfGrassAndMoss() {
        for (long seed = 1; seed <= 6; seed++) {
            for (Body body : List.of(RockShape.MONOLITH.body(seed, 30, 9), RockShape.TOWER.body(seed, 40, 8), RockShape.DOME.body(seed, 14, 11))) {
                RockTestCanvas canvas = stamp(body, FLAT, RockTestCanvas.NO_WATER);
                Map<Long, Integer> tops = new java.util.HashMap<>();
                int highest = 0;
                for (long[] cell : rock(canvas)) {
                    tops.merge(RockTestCanvas.key((int) cell[0], 0, (int) cell[2]), (int) cell[1], Math::max);
                    highest = Math.max(highest, (int) cell[1]);
                }
                // The plateau: the columns within two blocks of the top, against the rock's girth (its columns over half its height)
                int level = 0, girth = 0, green = 0, grass = 0, moss = 0;
                for (Map.Entry<Long, Integer> top : tops.entrySet()) {
                    if (top.getValue() >= GROUND + (highest - GROUND) / 2) girth++;
                    if (top.getValue() < highest - 2) continue;
                    level++;
                    String cell = canvas.at(RockTestCanvas.x(top.getKey()), top.getValue(), RockTestCanvas.z(top.getKey()));
                    if (cell.equals("rock:GRASS")) grass++;
                    if (cell.equals("rock:MOSS")) moss++;
                    if (cell.equals("rock:GRASS") || cell.equals("rock:MOSS")) green++;
                }
                String what = body.lobes().getFirst().height() + " tall, seed " + seed;
                assertTrue(level >= 0.27 * girth, what + ": the top is a plateau, not a point: " + level + " columns within two blocks of the top, of a girth of " + girth);
                assertTrue(green >= 0.6 * level, what + ": the plateau is overgrown: " + green + " of " + level);
                assertTrue(grass > 0 && moss > 0, what + ": with grass and with moss: " + grass + " and " + moss);
            }
        }
    }

    @Test
    void plantsKeepToLevelRock() {
        // Away from its level tops and ledges a rock is bare: most of its plant cover has open sky straight above it
        for (long seed = 1; seed <= 4; seed++) {
            RockTestCanvas canvas = stamp(RockShape.MONOLITH.body(seed, 30, 9), FLAT, RockTestCanvas.NO_WATER);
            int cover = 0, topped = 0, faces = 0;
            for (long[] cell : rock(canvas)) {
                int x = (int) cell[0], y = (int) cell[1], z = (int) cell[2];
                String material = canvas.at(x, y, z);
                boolean exposed = false;
                for (int[] step : SIX) exposed |= !canvas.isRock(x + step[0], y + step[1], z + step[2]);
                if (exposed && y >= GROUND) faces++;
                if (!material.equals("rock:GRASS") && !material.equals("rock:MOSS")) continue;
                cover++;
                if (!canvas.isRock(x, y + 1, z)) topped++;
            }
            // (moss hangs some way down from the edges of a level top, as in the reference picture of the monolith)
            assertTrue(topped >= 0.5 * cover, "seed " + seed + ": plants sit on upward faces: " + topped + " of " + cover);
            assertTrue(cover <= 0.35 * faces, "seed " + seed + ": most of the rock is bare: " + cover + " of " + faces + " open blocks are plants");
        }
    }

    @Test
    void aMonolithsFootIsNotRound() {
        // How much of the circle around it a shape fills: a disc fills it, an ellipse four fifths as wide fills four fifths
        for (long seed = 1; seed <= 8; seed++) {
            RockModel model = RockModel.build(RockShape.MONOLITH.body(seed, 30, 9));
            List<int[]> foot = new ArrayList<>(), raised = new ArrayList<>();
            for (int x = model.groundMinX; x < model.groundMinX + model.groundSizeX; x++) {
                for (int z = model.groundMinZ; z < model.groundMinZ + model.groundSizeZ; z++) {
                    boolean rock = model.material(x, 1, z) != null;
                    if (rock) foot.add(new int[]{x, z});
                    if (rock || model.rise(x, z) > 0) raised.add(new int[]{x, z});
                }
            }
            assertTrue(filled(foot) <= 0.7 && filled(foot) >= 0.3, "seed " + seed + ": the foot fills " + filled(foot) + " of the circle around it");
            assertTrue(filled(raised) <= 0.66, "seed " + seed + ": the foot with its skirt fills " + filled(raised) + " of the circle around them: a ring");
        }
    }

    private static double filled(List<int[]> columns) {
        double middleX = 0, middleZ = 0, widest = 0;
        for (int[] column : columns) {
            middleX += column[0];
            middleZ += column[1];
        }
        middleX /= columns.size();
        middleZ /= columns.size();
        for (int[] column : columns) widest = Math.max(widest, Math.hypot(column[0] - middleX, column[1] - middleZ) + 0.5);
        return columns.size() / (Math.PI * widest * widest);
    }

    @Test
    void aBoulderLooksLikeTwoOrThreeStones() {
        // Several humps: at some height its columns that reach it fall into two groups or more
        int several = 0;
        for (long seed = 1; seed <= 8; seed++) {
            RockTestCanvas canvas = stamp(RockShape.BOULDER.body(seed, 5, 4.5), FLAT, RockTestCanvas.NO_WATER);
            int size = 41, half = 20;
            int[][] tops = new int[size][size];
            int highest = 0;
            for (long[] cell : rock(canvas)) {
                int gx = (int) cell[0] + half, gz = (int) cell[2] + half;
                tops[gx][gz] = Math.max(tops[gx][gz], (int) cell[1] - GROUND + 1);
                highest = Math.max(highest, tops[gx][gz]);
            }
            int most = 0;
            for (int level = 2; level <= highest; level++) most = Math.max(most, peaks(tops, level));
            if (most >= 2) several++;
        }
        assertTrue(several >= 7, "boulders are two or three stones together: " + several + " of 8 have more than one hump");
    }

    @Test
    void aSmallRocksShadedSideAndFootAreInShade() {
        // The game draws no shadows: a boulder's own shade is in its stone, on the side turned from the sun (to the north)
        // and around its foot; the side toward the sun and its crown stay plain or light
        int[] away = new int[3], toward = new int[3], foot = new int[3], crown = new int[3];
        for (long seed = 1; seed <= 8; seed++) {
            RockTestCanvas canvas = stamp(RockShape.BOULDER.body(seed, 7, 4.5).withCover(0), FLAT, RockTestCanvas.NO_WATER);
            int top = GROUND;
            for (long[] cell : rock(canvas)) top = Math.max(top, (int) cell[1]);
            for (long[] cell : rock(canvas)) {
                int x = (int) cell[0], y = (int) cell[1], z = (int) cell[2];
                int tone = tone(canvas, x, y, z);
                // The upright faces above the ground that one sees from afar: under rock, open three blocks out to a side
                // (not the walls of creases and the undersides of overhangs, which are in shade whatever the sun)
                if (tone < 0 || y < GROUND || !canvas.isRock(x, y + 1, z)) continue;
                boolean north = false, south = false, broad = false;
                for (int[] step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    boolean open = true;
                    for (int out = 1; out <= 3; out++) open &= !canvas.isRock(x + out * step[0], y, z + out * step[1]);
                    broad |= open;
                    if (open && step[1] < 0) north = true;
                    if (open && step[1] > 0) south = true;
                }
                if (!broad) continue;
                if (north && !south) away[tone]++;
                if (south && !north) toward[tone]++;
                double height = (double) (y - GROUND) / Math.max(1, top - GROUND);
                if (height < 0.34) foot[tone]++;
                if (height > 0.6) crown[tone]++;
            }
        }
        String tones = "away " + share(away, 0) + ", toward " + share(toward, 0) + ", foot " + share(foot, 0) + ", crown " + share(crown, 0);
        // (Toward the sun there is still the shade under the bulges of a rounded stone: the sun stands high)
        assertTrue(share(away, 0) >= 0.45, "the side turned from the sun is much in shade: " + tones);
        assertTrue(share(toward, 0) <= 0.25 && share(away, 0) >= share(toward, 0) + 0.25, "the side toward it much less: " + tones);
        assertTrue(share(foot, 0) >= 0.38, "the foot is much in shade: " + tones);
        assertTrue(share(crown, 0) <= share(foot, 0) - 0.15, "the crown less than the foot: " + tones);
        assertTrue(share(crown, 1) + share(crown, 2) >= 0.7, "the crown is mostly plain or light: " + tones);
    }

    @Test
    void theSideTowardTheSunIsLighter() {
        // The sun stands to the south (+z): of the upright faces, those turned to it have more light and less shade than those turned away
        int[] toward = new int[3], away = new int[3];
        for (long seed = 1; seed <= 6; seed++) {
            RockTestCanvas canvas = stamp(RockShape.MONOLITH.body(seed, 30, 9).withCover(0), FLAT, RockTestCanvas.NO_WATER);
            for (long[] cell : rock(canvas)) {
                int x = (int) cell[0], y = (int) cell[1], z = (int) cell[2];
                int tone = tone(canvas, x, y, z);
                if (tone < 0 || y < GROUND + 2 || !canvas.isRock(x, y + 1, z) || !canvas.isRock(x, y - 1, z)) continue;
                boolean south = !canvas.isRock(x, y, z + 1) && !canvas.isRock(x, y, z + 2) && !canvas.isRock(x, y, z + 3);
                boolean north = !canvas.isRock(x, y, z - 1) && !canvas.isRock(x, y, z - 2) && !canvas.isRock(x, y, z - 3);
                if (south && !north) toward[tone]++;
                if (north && !south) away[tone]++;
            }
        }
        assertTrue(share(toward, 2) > share(away, 2) + 0.04, "more light toward the sun: " + share(toward, 2) + " against " + share(away, 2));
        assertTrue(share(away, 0) > share(toward, 0) + 0.04, "more shade away from it: " + share(away, 0) + " against " + share(toward, 0));
    }
    @Test
    void aPileHasSlabsOnBareRock() {
        int slabs = 0;
        for (long seed = 1; seed <= 5; seed++) {
            RockTestCanvas canvas = stamp(RockShape.PILE.body(seed, 5, 6), FLAT, RockTestCanvas.NO_WATER);
            for (Map.Entry<Long, String> entry : canvas.cells.entrySet()) {
                if (!entry.getValue().startsWith("slab:")) continue;
                slabs++;
                int x = RockTestCanvas.x(entry.getKey()), y = RockTestCanvas.y(entry.getKey()), z = RockTestCanvas.z(entry.getKey());
                String below = canvas.at(x, y - 1, z);
                assertTrue(below != null && below.startsWith("rock:") && RockMaterial.valueOf(below.substring(5)).isRock(), "a slab on " + below);
                assertEquals(below.substring(5), entry.getValue().substring(5), "a slab of the rock under it");
                assertTrue(!canvas.isRock(x, y + 1, z), "a slab with rock on it");
            }
        }
        assertTrue(slabs >= 10, "piles have slabs: " + slabs + " on five of them");
    }

    @Test
    void theGroundRisesTowardAMonolithsFoot() {
        for (long seed = 1; seed <= 4; seed++) {
            Body body = RockShape.MONOLITH.body(seed, 30, 9);
            RockModel model = RockModel.build(body);
            List<int[]> foot = new ArrayList<>();
            for (int x = model.minX; x < model.minX + model.sizeX; x++) {
                for (int z = model.minZ; z < model.minZ + model.sizeZ; z++) {
                    if (model.lowest(x, z) != RockModel.NONE && model.lowest(x, z) <= 0) foot.add(new int[]{x, z});
                }
            }
            double near = 0, far = 0;
            int nearCount = 0, farCount = 0;
            for (int x = model.groundMinX; x < model.groundMinX + model.groundSizeX; x++) {
                for (int z = model.groundMinZ; z < model.groundMinZ + model.groundSizeZ; z++) {
                    double away = Double.MAX_VALUE;
                    for (int[] column : foot) away = Math.min(away, Math.hypot(x - column[0], z - column[1]));
                    if (away == 0) continue;
                    int rise = model.rise(x, z);
                    assertTrue(rise <= body.skirtHeight() + 2, "the skirt is no higher than asked: " + rise);
                    if (away > body.skirtWidth() + 1.5) assertEquals(0, rise, "no skirt " + away + " blocks from the foot");
                    if (away <= 2) {
                        near += rise;
                        nearCount++;
                    } else if (away >= body.skirtWidth() - 1.5 && away <= body.skirtWidth()) {
                        far += rise;
                        farCount++;
                    }
                }
            }
            assertTrue(body.skirtWidth() >= 4, "a monolith has a skirt");
            // (along part of the foot only: a ramp, not a ring)
            assertTrue(near / nearCount >= 0.5, "seed " + seed + ": the ground rises at the foot: " + near / nearCount);
            assertTrue(near / nearCount > far / farCount + 0.3, "seed " + seed + ": higher at the foot than at the skirt's edge");
        }
    }
}
