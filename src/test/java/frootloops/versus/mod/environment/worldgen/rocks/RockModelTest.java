package frootloops.versus.mod.environment.worldgen.rocks;

import frootloops.versus.mod.environment.worldgen.rocks.RockPlan.Body;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntBinaryOperator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A body's model: rock of angular blocks, painted from above, its level tops overgrown, the ground risen around it. */
class RockModelTest {

    private static final int GROUND = 64;
    private static final IntBinaryOperator FLAT = (x, z) -> GROUND;
    private static final int[][] SIX = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private static double height(RockShape shape) {
        return (shape.minHeight + shape.maxHeight) / 2;
    }

    private static double radius(RockShape shape) {
        return (shape.minRadius + shape.maxRadius) / 2;
    }

    /** Every shape at the middle of its own sizes, in the order of {@link RockShape#values()}. */
    private static List<Body> everyShape(long seed) {
        List<Body> bodies = new ArrayList<>();
        for (RockShape shape : RockShape.values()) bodies.add(shape.body(seed, height(shape), radius(shape)));
        return bodies;
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

    /** 0 for tuff, 1 stone, 2 andesite, 3 clay; -1 for anything else. */
    private static int tone(RockTestCanvas canvas, int x, int y, int z) {
        String cell = canvas.at(x, y, z);
        if (cell == null) return -1;
        return switch (cell) {
            case "rock:SHADE" -> 0;
            case "rock:BASE" -> 1;
            case "rock:LIGHT" -> 2;
            case "rock:BRIGHT" -> 3;
            default -> -1;
        };
    }

    /** Whether a cell of the model is rock or its cover, or the ground (below 0). */
    private static boolean filled(RockModel model, int x, int y, int z) {
        return y < 0 || model.material(x, y, z) != null;
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
        for (long seed = 1; seed <= 8; seed++) {
            List<Body> bodies = everyShape(seed);
            for (int index = 0; index < bodies.size(); index++) {
                RockShape shape = RockShape.values()[index];
                int top = GROUND;
                for (long[] cell : rock(stamp(bodies.get(index), FLAT, RockTestCanvas.NO_WATER))) top = Math.max(top, (int) cell[1] + 1);
                double height = height(shape), tall = top - GROUND;
                assertTrue(tall >= 0.7 * height && tall <= 1.15 * height, shape + " seed " + seed + ": asked " + height + " tall, is " + tall);
            }
        }
    }

    @Test
    void theSmallestOfEveryShapeIsStillARock() {
        for (RockShape shape : RockShape.values()) {
            for (long seed = 1; seed <= 8; seed++) {
                int rock = rock(stamp(shape.body(seed, shape.minHeight, shape.minRadius), FLAT, RockTestCanvas.NO_WATER)).size();
                assertTrue(rock >= 20, shape + " at its smallest, seed " + seed + ": " + rock + " blocks of rock");
            }
        }
    }

    @Test
    void nothingFloats() {
        for (long seed = 1; seed <= 4; seed++) {
            for (Body body : everyShape(seed)) {
                assertGrounded(stamp(body, FLAT, RockTestCanvas.NO_WATER), body.blocks().size() + " blocks, seed " + seed);
            }
        }
    }

    /** Every block of rock is joined, face to face, to rock that stands in or on the ground; plants stand on grass. */
    private static void assertGrounded(RockTestCanvas canvas, String what) {
        Set<Long> reached = new HashSet<>();
        ArrayDeque<long[]> queue = new ArrayDeque<>();
        List<long[]> rock = rock(canvas);
        assertTrue(rock.size() >= 12, what + ": there is rock");
        for (long[] cell : rock) {
            int x = (int) cell[0], y = (int) cell[1], z = (int) cell[2];
            // On the ground as it was, or on ground that rose against it
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
            if (entry.getValue().equals("plant")) assertEquals("rock:GRASS", canvas.at(x, y - 1, z), what + ": a plant that isn't on grass at " + x + " " + y + " " + z);
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
            for (RockShape shape : RockShape.values()) {
                RockModel model = RockModel.build(shape.body(seed, height(shape), radius(shape)));
                RockTestCanvas canvas = new RockTestCanvas(FLAT, RockTestCanvas.NO_WATER);
                RockStamper.stamp(model, 0, GROUND, 0, canvas, -1000, -1000, 1000, 1000);
                int reach = shape.reach(height(shape), radius(shape));
                assertTrue(model.reach() > 0 && model.reach() <= reach, shape + " seed " + seed + ": reaches " + model.reach() + ", its shape says " + reach);
                for (long key : canvas.cells.keySet()) {
                    int far = Math.max(Math.abs(RockTestCanvas.x(key)), Math.abs(RockTestCanvas.z(key)));
                    assertTrue(far <= model.reach(), shape + " seed " + seed + ": wrote " + far + " blocks out, past its reach of " + model.reach());
                }
            }
        }
    }

    @Test
    void aRockLargerThanItsShapesRangeStaysWithinItsReach() {
        // A template may ask for more than the shape's own sizes: great boulders are boulders 12 to 18 tall
        for (long seed = 1; seed <= 20; seed++) {
            RockModel model = RockModel.build(RockShape.BOULDER.body(seed, 18, 10));
            assertTrue(model.reach() <= RockShape.BOULDER.reach(18, 10), "seed " + seed + ": reaches " + model.reach() + ", its shape says " + RockShape.BOULDER.reach(18, 10));
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
    void aFootIsNeverFilledMoreThanItsMost() {
        // At a cliff's edge: the ground 40 blocks lower all around the anchor
        IntBinaryOperator cliff = (x, z) -> x == 0 && z == 0 ? GROUND : GROUND - 40;
        RockModel model = RockModel.build(RockShape.TOWER.body(2, 38, 8.5));
        RockTestCanvas canvas = new RockTestCanvas(cliff, RockTestCanvas.NO_WATER);
        RockStamper.stamp(model, 0, GROUND, 0, canvas, -1000, -1000, 1000, 1000);
        int filled = 0;
        for (long[] cell : rock(canvas)) {
            int x = (int) cell[0], y = (int) cell[1], z = (int) cell[2];
            int lowest = model.lowest(x, z);
            assertTrue(lowest != RockModel.NONE && y >= GROUND + lowest - RockStamper.MAX_FILL, "rock at " + x + " " + y + " " + z + ", its model's lowest at " + lowest);
            if (y < GROUND + lowest) filled++;
        }
        assertTrue(filled > 100, "the foot is filled down: " + filled);
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
                    assertTrue(!cell.equals("rock:GRASS") && !cell.equals("rock:SOIL") && !cell.equals("rock:MUD") && !cell.equals("plant"),
                            cell + " under water at height " + RockTestCanvas.y(entry.getKey()));
                }
            }
        }
        assertTrue(wet > 200, "rocks stand in the water: " + wet + " blocks of them under it");
        assertTrue(grassAbove > 0, "the domes still have grass above the water");
    }

    @Test
    void grassOnlyUnderTheSkyAndMossOnlyOnTheSurface() {
        int[] grassy = new int[RockShape.values().length];
        for (long seed = 1; seed <= 4; seed++) {
            List<Body> bodies = everyShape(seed);
            for (int index = 0; index < bodies.size(); index++) {
                Body body = bodies.get(index);
                RockTestCanvas canvas = stamp(body, FLAT, RockTestCanvas.NO_WATER);
                assertTrue(rock(canvas).size() >= 12, "there is rock");
                Map<Long, Integer> highest = new HashMap<>();
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
                grassy[index] += grass;
            }
        }
        // A rock with cover and a top wide enough has grass on it, if not in every seed (a needle's top is a few blocks)
        for (RockShape shape : RockShape.values()) {
            if (shape.cover() > 0.4 && height(shape) >= 12) assertTrue(grassy[shape.ordinal()] > 0, shape + ": a rock with cover has grass on it");
        }
    }

    @Test
    void theCoverLiesOnEarth() {
        // As on the ground around: grass on dirt, moss on packed mud, a layer of it between the cover and the rock (or
        // moss hanging over the edge)
        int grass = 0, moss = 0;
        for (long seed = 1; seed <= 3; seed++) {
            for (Body body : everyShape(seed)) {
                RockModel model = RockModel.build(body);
                for (int x = model.minX; x < model.minX + model.sizeX; x++) {
                    for (int z = model.minZ; z < model.minZ + model.sizeZ; z++) {
                        int top = model.highest(x, z);
                        if (top == RockModel.NONE || top < 1) continue;
                        RockMaterial cover = model.material(x, top, z), under = model.material(x, top - 1, z);
                        if (under == null || under == RockMaterial.MOSS) continue;
                        if (cover == RockMaterial.GRASS) {
                            assertEquals(RockMaterial.SOIL, under, "grass on " + under + " at " + x + " " + top + " " + z);
                            grass++;
                        } else if (cover == RockMaterial.MOSS) {
                            assertEquals(RockMaterial.MUD, under, "moss on " + under + " at " + x + " " + top + " " + z);
                            moss++;
                        }
                    }
                }
            }
        }
        assertTrue(grass > 0 && moss > 0, "grass on earth " + grass + ", moss on earth " + moss);
    }

    @Test
    void levelTopsAreOvergrownMostlyWithMoss() {
        int moss = 0, grass = 0;
        for (long seed = 1; seed <= 3; seed++) {
            for (RockShape shape : List.of(RockShape.TOWER, RockShape.MONOLITH, RockShape.MASSIF)) {
                RockTestCanvas canvas = stamp(shape.body(seed, height(shape), radius(shape)), FLAT, RockTestCanvas.NO_WATER);
                for (String cell : canvas.cells.values()) {
                    if (cell.equals("rock:MOSS")) moss++;
                    else if (cell.equals("rock:GRASS")) grass++;
                }
            }
        }
        assertTrue(grass > 0 && moss > grass, "moss mostly, grass too: " + moss + " moss, " + grass + " grass");
    }

    @Test
    void onlyTheFourTonesUnderTheCover() {
        Set<String> allowed = Set.of("rock:SHADE", "rock:BASE", "rock:LIGHT", "rock:BRIGHT", "rock:GRASS", "rock:MOSS", "rock:SOIL", "rock:MUD", "ground", "plant");
        int[] tones = new int[4];
        for (long seed = 1; seed <= 2; seed++) {
            for (Body body : everyShape(seed)) {
                RockTestCanvas canvas = stamp(body, FLAT, RockTestCanvas.NO_WATER);
                for (Map.Entry<Long, String> entry : canvas.cells.entrySet()) {
                    assertTrue(allowed.contains(entry.getValue()), entry.getValue() + " in a rock");
                    int tone = tone(canvas, RockTestCanvas.x(entry.getKey()), RockTestCanvas.y(entry.getKey()), RockTestCanvas.z(entry.getKey()));
                    if (tone >= 0) tones[tone]++;
                }
            }
        }
        for (int tone = 0; tone < 4; tone++) assertTrue(tones[tone] > 0, "every tone is there: " + java.util.Arrays.toString(tones));
    }

    @Test
    void noNoiseOnTheFaces() {
        for (long seed = 1; seed <= 3; seed++) {
            for (Body body : everyShape(seed)) {
                RockModel model = RockModel.build(body);
                int surface = 0, odd = 0;
                for (int x = model.minX; x < model.minX + model.sizeX; x++) {
                    for (int z = model.minZ; z < model.minZ + model.sizeZ; z++) {
                        for (int y = 0; y < model.minY + model.sizeY; y++) {
                            int filled = 0;
                            for (int[] step : SIX) if (filled(model, x + step[0], y + step[1], z + step[2])) filled++;
                            if (model.material(x, y, z) != null) {
                                if (filled < 6) surface++;
                                if (filled <= 1) odd++;
                            } else if (filled >= 5) {
                                odd++;
                            }
                        }
                    }
                }
                assertTrue(surface > 0 && odd <= 0.01 * surface, body.blocks().size() + " blocks, seed " + seed + ": " + odd + " knobs and pits on " + surface + " faces");
            }
        }
    }

    @Test
    void bouldersAndPilesTopsAreNoSteeperThanThirtyDegrees() {
        // No pointed tips, no posts on top: a top sloping 30 degrees or flatter has some 9 columns or more within a block of it
        for (RockShape shape : List.of(RockShape.BOULDER, RockShape.DOME, RockShape.PILE)) {
            for (double size : new double[]{0, 0.5, 1}) {
                double height = shape.minHeight + size * (shape.maxHeight - shape.minHeight), radius = shape.minRadius + size * (shape.maxRadius - shape.minRadius);
                for (long seed = 1; seed <= 40; seed++) {
                    RockModel model = RockModel.build(shape.body(seed, height, radius));
                    int[] tip = RockTips.highest(model);
                    int cap = RockTips.cap(model, tip);
                    assertTrue(cap >= 9, shape + " " + height + " tall, seed " + seed + ": " + cap + " columns within a block of its top, at " + tip[0] + " " + tip[1]);
                }
            }
        }
    }

    @Test
    void aBoulderIsTwoBlocksOrMore() {
        for (long seed = 1; seed <= 20; seed++) {
            assertTrue(RockShape.BOULDER.body(seed, 8, 4).blocks().size() >= 2, "seed " + seed + ": a big block with a smaller one leaning on it at least");
        }
    }

    @Test
    void aMonolithLeansAndHasItsShoulder() {
        double height = height(RockShape.MONOLITH);
        for (long seed = 1; seed <= 8; seed++) {
            Body body = RockShape.MONOLITH.body(seed, height, radius(RockShape.MONOLITH));
            RockModel model = RockModel.build(body);
            int top = Integer.MIN_VALUE;
            for (int x = model.minX; x < model.minX + model.sizeX; x++) {
                for (int z = model.minZ; z < model.minZ + model.sizeZ; z++) top = Math.max(top, model.highest(x, z));
            }
            // The middles of its top and of its foot
            double[] high = new double[3], foot = new double[3];
            for (int x = model.minX; x < model.minX + model.sizeX; x++) {
                for (int z = model.minZ; z < model.minZ + model.sizeZ; z++) {
                    int here = model.highest(x, z);
                    if (here == RockModel.NONE) continue;
                    if (here >= top - 2) add(high, x, z);
                    if (model.material(x, 0, z) != null) add(foot, x, z);
                }
            }
            double lean = Math.hypot(high[0] / high[2] - foot[0] / foot[2], high[1] / high[2] - foot[1] / foot[2]);
            assertTrue(lean >= 0.15 * height, "seed " + seed + ": its top " + lean + " from its foot, " + height + " tall");
            // Its shoulder: a block of its own standing in the ground against it, 0.4 to 0.75 of its height, rock up there
            boolean shoulder = false;
            for (RockBlock block : body.blocks()) {
                double[] span = span(block);
                int there = model.highest((int) Math.floor(block.cx()), (int) Math.floor(block.cz()));
                shoulder |= span[0] < 0 && span[1] >= 0.4 * height && span[1] <= 0.75 * height && there >= 0.4 * height;
            }
            assertTrue(shoulder, "seed " + seed + ": a shoulder against it");
        }
    }

    /** How low and how high a block is rock, along the upright line through its centre. */
    private static double[] span(RockBlock block) {
        double low = Double.MAX_VALUE, high = -Double.MAX_VALUE;
        for (double y = block.cy() - block.reach(); y <= block.cy() + block.reach(); y += 0.25) {
            if (block.distance(block.cx(), y, block.cz()) >= 0) continue;
            low = Math.min(low, y);
            high = Math.max(high, y);
        }
        return new double[]{low, high};
    }

    private static void add(double[] sum, int x, int z) {
        sum[0] += x;
        sum[1] += z;
        sum[2]++;
    }

    @Test
    void aTowersColumnsDifferInHeight() {
        double height = height(RockShape.TOWER), spread = 0;
        int several = 0;
        for (long seed = 1; seed <= 8; seed++) {
            Body body = RockShape.TOWER.body(seed, height, radius(RockShape.TOWER));
            RockModel model = RockModel.build(body);
            // Each column's top: the highest rock over its lowest block's middle
            Map<Integer, RockBlock> feet = new HashMap<>();
            for (RockBlock block : body.blocks()) {
                if (block.group() >= 0) feet.merge(block.group(), block, (one, two) -> one.cy() <= two.cy() ? one : two);
            }
            if (feet.size() < 2) continue;
            several++;
            int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
            for (RockBlock foot : feet.values()) {
                int top = model.highest((int) Math.floor(foot.cx()), (int) Math.floor(foot.cz()));
                low = Math.min(low, top);
                high = Math.max(high, top);
            }
            assertTrue(high - low >= 2, "seed " + seed + ": its " + feet.size() + " columns' tops from " + low + " to " + high);
            spread += high - low;
        }
        assertTrue(several >= 6, "towers of several columns: " + several + " of 8");
        assertTrue(spread / several >= 0.1 * height, "their tops apart by " + spread / several + " on the whole");
    }

    @Test
    void aTallRockIsWiderAtItsFootAndPropped() {
        for (RockShape shape : List.of(RockShape.MONOLITH, RockShape.TOWER)) {
            double height = height(shape);
            int propped = 0;
            for (long seed = 1; seed <= 8; seed++) {
                Body body = shape.body(seed, height, radius(shape));
                RockModel model = RockModel.build(body);
                int foot = 0, waist = 0;
                for (int x = model.minX; x < model.minX + model.sizeX; x++) {
                    for (int z = model.minZ; z < model.minZ + model.sizeZ; z++) {
                        if (model.material(x, 0, z) != null) foot++;
                        if (model.material(x, (int) (height / 2), z) != null) waist++;
                    }
                }
                assertTrue(foot >= 1.2 * waist, shape + " seed " + seed + ": " + foot + " at its foot, " + waist + " halfway up");
                // A prop: a block standing on its own in the ground, not as tall as 0.4 of the rock
                for (RockBlock block : body.blocks()) {
                    if (block.group() >= 0) continue;
                    double[] span = span(block);
                    if (span[0] < 0 && span[1] <= 0.4 * height) {
                        propped++;
                        break;
                    }
                }
            }
            assertTrue(propped >= 7, shape + ": propped at its foot in " + propped + " of 8");
        }
    }

    @Test
    void theGroundRisesAllAroundTheFoot() {
        for (long seed = 1; seed <= 4; seed++) {
            RockModel model = RockModel.build(RockShape.MONOLITH.body(seed, 30, 9));
            int beside = 0, risen = 0;
            for (int x = model.groundMinX; x < model.groundMinX + model.groundSizeX; x++) {
                for (int z = model.groundMinZ; z < model.groundMinZ + model.groundSizeZ; z++) {
                    assertTrue(model.rise(x, z) <= 3, "seed " + seed + ": the ground rises " + model.rise(x, z) + " at " + x + " " + z);
                    if (model.material(x, 0, z) != null) {
                        assertEquals(0, model.rise(x, z), "seed " + seed + ": the ground rises under rock at " + x + " " + z);
                        continue;
                    }
                    boolean next = model.material(x + 1, 0, z) != null || model.material(x - 1, 0, z) != null || model.material(x, 0, z + 1) != null
                            || model.material(x, 0, z - 1) != null;
                    if (!next) continue;
                    beside++;
                    if (model.rise(x, z) >= 1) risen++;
                }
            }
            assertTrue(risen >= 0.8 * beside, "seed " + seed + ": the ground rises beside " + risen + " of the " + beside + " columns along its foot");
        }
    }

    @Test
    void theGroundRisesOverRockUnderIt() {
        // Under the ground a rock's foot is a little wider in places than at it: the ground risen around the foot covers
        // that rock, rather than stopping short of it and leaving a trench along the foot
        int buried = 0;
        for (long seed = 1; seed <= 4; seed++) {
            for (Body body : List.of(RockShape.TOWER.body(seed, 38, 8.5), RockShape.MONOLITH.body(seed, 32, 9.5), RockShape.BOULDER.body(seed, 8, 4))) {
                RockModel model = RockModel.build(body);
                RockTestCanvas canvas = new RockTestCanvas(FLAT, RockTestCanvas.NO_WATER);
                RockStamper.stamp(model, 0, GROUND, 0, canvas, -1000, -1000, 1000, 1000);
                for (int x = model.minX; x < model.minX + model.sizeX; x++) {
                    for (int z = model.minZ; z < model.minZ + model.sizeZ; z++) {
                        int rise = model.rise(x, z);
                        if (rise == 0 || model.lowest(x, z) == RockModel.NONE || model.lowest(x, z) >= 0) continue;
                        boolean overhead = false;
                        for (int y = 0; y < rise; y++) overhead |= model.material(x, y, z) != null;
                        if (overhead) continue;
                        buried++;
                        assertEquals("ground", canvas.at(x, GROUND + rise - 1, z), "seed " + seed + ": no ground over the rock under it at " + x + " " + z);
                    }
                }
            }
        }
        assertTrue(buried > 0, "rock under the ground beside the foot: " + buried);
    }

    @Test
    void readingTheModelFromManyThreadsGivesTheSameRock() throws Exception {
        RockModel model = RockModel.build(RockShape.MASSIF.body(7, 45, 22));
        RockMaterial[] alone = read(model);
        ExecutorService threads = Executors.newFixedThreadPool(8);
        try {
            List<Future<RockMaterial[]>> reads = new ArrayList<>();
            for (int thread = 0; thread < 8; thread++) reads.add(threads.submit(() -> read(model)));
            for (Future<RockMaterial[]> read : reads) assertArrayEquals(alone, read.get());
        } finally {
            threads.shutdown();
        }
    }

    private static RockMaterial[] read(RockModel model) {
        RockMaterial[] cells = new RockMaterial[model.sizeX * model.sizeY * model.sizeZ];
        int index = 0;
        for (int x = model.minX; x < model.minX + model.sizeX; x++) {
            for (int z = model.minZ; z < model.minZ + model.sizeZ; z++) {
                for (int y = model.minY; y < model.minY + model.sizeY; y++) cells[index++] = model.material(x, y, z);
            }
        }
        return cells;
    }

    @Test
    void aBigFormationIsBuiltQuickly() {
        Body body = RockShape.MASSIF.body(11, 56, 26);
        long best = Long.MAX_VALUE;
        for (int attempt = 0; attempt < 3; attempt++) {
            long start = System.nanoTime();
            RockModel.build(body);
            best = Math.min(best, (System.nanoTime() - start) / 1_000_000);
        }
        assertTrue(best <= 300, "the largest massif is built in " + best + " ms");
    }
}
