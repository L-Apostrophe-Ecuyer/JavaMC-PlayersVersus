package frootloops.versus.mod.environment.worldgen.aquifer;

import frootloops.versus.mod.environment.worldgen.TerrainPass;
import frootloops.versus.mod.environment.worldgen.WorldgenTestData;
import frootloops.versus.mod.environment.worldgen.density.DensityOps;
import frootloops.versus.mod.environment.worldgen.density.PvFinalDensity;
import frootloops.versus.mod.environment.worldgen.density.PvNoodle;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The flooded corridors (the refactor plan, Section 10, question 7) in vanilla's terrain pass, over areas of 5 x 5
 * chunks picked for their lakes: noodle caves back in the basin layers near the flooded caves, filled by the aquifer.
 * Against the same terrain without them (the noodle's old height bias, {@link PvNoodle#bias}): the blocks they open and
 * what they become, how much of their water reaches a lake, how the water bodies of y -3..23 join up (neighbours across
 * the whole area), and that no water has dry air beside or below it; with maps of the middle of the first area. The
 * router's final density must be the corridors' formula ({@link PvNoodle#corridorBias}), to the bit.
 *
 * <p>The rules were chosen here: noodles opened only where S would flood them added about 2 water blocks per chunk
 * (S falls off within a few blocks of the lakes); opened by the entrance value and left to S, they came out mostly dry;
 * filled by the aquifer, they need no walls, and widening them nearer the caves took the share of their water that
 * joins a lake from 14% to about 60%.
 */
class FloodedNoodleSurveyTest {

    private static final long SEED = 8675309L;
    /** The basin water's layers, y -3..23. */
    private static final int MIN_Y = -3, MAX_Y = 24, LAYERS = MAX_Y - MIN_Y;
    private static final int[] BAND_TOPS = {8, 16, 24};
    private static final int AREA_CHUNKS = 5, AREAS = 3, SIDE = AREA_CHUNKS * 16;
    private static final int[][] NEIGHBOURS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    private static final AquiferSampler.FluidLevelSampler NO_FLUID_LEVELS = (x, y, z) -> {
        throw new UnsupportedOperationException("the survey passes lavaLevel itself");
    };

    /** What a block ends up as. */
    private static final byte SOLID = 0, WATER = 1, STONE = 2, AIR = 3;
    private static final String[] KINDS = {"solid", "water", "stone", "air"};

    /** The heights the maps show. */
    private static final int[] MAP_YS = {4, 12, 20};
    private static final int MAP_SIDE = 48;
    private static final int[][] SIDES_AND_BELOW = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, -1, 0}};

    @Test
    void survey() {
        NoiseConfig config = WorldgenTestData.noiseConfig(SEED);
        ChunkGeneratorSettings settings = WorldgenTestData.pvSettings();
        PvFinalDensity finalDensity = finalDensity(config.getNoiseRouter().finalDensity());
        AquiferInputs inputs = AquiferInputs.of(config, settings);
        List<ChunkPos> centers = lakeCenters(config, inputs);
        Stats without = new Stats(), with = new Stats();
        long[] routerMismatches = {0};
        for (ChunkPos center : centers) {
            byte[] before = new byte[SIDE * SIDE * LAYERS], after = new byte[SIDE * SIDE * LAYERS];
            int originX = (center.x - AREA_CHUNKS / 2) * 16, originZ = (center.z - AREA_CHUNKS / 2) * 16;
            for (int chunkX = 0; chunkX < AREA_CHUNKS; chunkX++) {
                for (int chunkZ = 0; chunkZ < AREA_CHUNKS; chunkZ++) {
                    ChunkPos chunk = new ChunkPos((originX >> 4) + chunkX, (originZ >> 4) + chunkZ);
                    TerrainPass pass = new TerrainPass(config, settings, chunk, NO_FLUID_LEVELS);
                    PvAquifer aquifer = assertInstanceOf(PvAquifer.class, pass.aquifer(), "ChunkNoiseSamplerMixin didn't make the aquifer");
                    DensityFunction router = pass.register(config.getNoiseRouter().finalDensity());
                    DensityFunction terrain = pass.register(finalDensity.terrain());
                    DensityFunction toggle = pass.register(finalDensity.noodleToggle());
                    DensityFunction thickness = pass.register(finalDensity.noodleThickness());
                    DensityFunction ridgeA = pass.register(finalDensity.noodleRidgeA());
                    DensityFunction ridgeB = pass.register(finalDensity.noodleRidgeB());
                    DensityFunction entrances = pass.register(finalDensity.entrances());
                    pass.run((x, y, z, pos) -> {
                        if (y < MIN_Y || y >= MAX_Y) return;
                        double terrainValue = DensityOps.squeeze(terrain.sample(pos) * 0.64);
                        double tunnel = PvNoodle.tunnel(pos, toggle, thickness, ridgeA, ridgeB);
                        double old = Math.min(terrainValue, PvNoodle.bias(y) + tunnel);
                        double now = router.sample(pos);
                        if (now != Math.min(terrainValue, PvNoodle.corridorBias(y, entrances.sample(pos)) + tunnel)) routerMismatches[0]++;
                        int i = index(x - originX, y, z - originZ);
                        after[i] = kind(now, aquifer.decide(pos, now, false));
                        before[i] = old == now ? after[i] : kind(old, aquifer.decide(pos, old, false));
                    });
                }
            }
            without.add(before, before);
            with.add(before, after);
            with.leaks += leaks(after);
            if (center.equals(centers.get(0))) {
                for (int y : MAP_YS) printMap(center, y, before, after);
            }
        }

        double chunks = AREAS * AREA_CHUNKS * AREA_CHUNKS;
        System.out.printf(Locale.ROOT, "[noodles] %d areas of %d x %d chunks around %s, y %d..%d; per chunk:%n",
                AREAS, AREA_CHUNKS, AREA_CHUNKS, centers, MIN_Y, MAX_Y - 1);
        System.out.println("[noodles] without the corridors: " + without.describe(chunks));
        System.out.println("[noodles] with the corridors: " + with.describe(chunks));
        assertEquals(0, routerMismatches[0], "the router's final density isn't the corridors' formula");
        assertTrue(without.water > 0, "the survey found no water in the basin layers");
        assertTrue(with.water > without.water, "the corridors added no water");
        assertEquals(0, with.leaks, "water with dry air beside or below it");
    }

    /** Water blocks with dry open air beside or below them, inside the area. */
    private static int leaks(byte[] kinds) {
        int leaks = 0;
        for (int i = 0; i < kinds.length; i++) {
            if (kinds[i] != WATER) continue;
            int layer = i / (SIDE * SIDE), x = (i / SIDE) % SIDE, z = i % SIDE;
            for (int[] offset : SIDES_AND_BELOW) {
                int nx = x + offset[0], ny = layer + offset[1], nz = z + offset[2];
                if (nx < 0 || nx >= SIDE || nz < 0 || nz >= SIDE || ny < 0) continue;
                if (kinds[(ny * SIDE + nx) * SIDE + nz] == AIR) leaks++;
            }
        }
        return leaks;
    }

    /**
     * Centers for the areas: of 80 random chunks, the ones with the most lake (open blocks of y 4..20 with S above the
     * basin water threshold, on a coarse grid of exact values), at least an area apart.
     */
    private static List<ChunkPos> lakeCenters(NoiseConfig config, AquiferInputs inputs) {
        DensityFunction finalDensity = config.getNoiseRouter().finalDensity();
        Random random = new Random(SEED);
        List<ChunkPos> chunks = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            ChunkPos chunk = new ChunkPos(random.nextInt(800) - 400, random.nextInt(800) - 400);
            int score = 0;
            for (int dx = 0; dx < 16; dx += 4) {
                for (int dz = 0; dz < 16; dz += 4) {
                    for (int y = 4; y <= 20; y += 4) {
                        DensityFunction.NoisePos pos = new DensityFunction.UnblendedNoisePos(chunk.getStartX() + dx, y, chunk.getStartZ() + dz);
                        if (finalDensity.sample(pos) <= 0.0 && inputs.spread().sample(pos) > PvAquiferRules.basinWaterThreshold(y)) score++;
                    }
                }
            }
            chunks.add(chunk);
            scores.add(score);
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) order.add(i);
        order.sort((a, b) -> Integer.compare(scores.get(b), scores.get(a)));
        List<ChunkPos> centers = new ArrayList<>();
        for (int i : order) {
            ChunkPos chunk = chunks.get(i);
            if (centers.stream().anyMatch(other -> Math.abs(other.x - chunk.x) < AREA_CHUNKS && Math.abs(other.z - chunk.z) < AREA_CHUNKS)) continue;
            centers.add(chunk);
            System.out.println("[noodles] area around chunk " + chunk + ": " + scores.get(i) + " of 80 coarse samples are lake");
            if (centers.size() == AREAS) break;
        }
        return centers;
    }

    /**
     * The middle of an area at one height, north up: {@code #} solid, {@code .} dry, {@code ~} water, {@code +} water the
     * corridors add, {@code o} stone they add, {@code -} dry air they add.
     */
    private static void printMap(ChunkPos center, int y, byte[] now, byte[] candidate) {
        System.out.println("[noodles] map with the corridors, y " + y + ", " + MAP_SIDE + " x " + MAP_SIDE + " blocks around chunk " + center + ":");
        int start = (SIDE - MAP_SIDE) / 2;
        for (int z = start; z < start + MAP_SIDE; z++) {
            StringBuilder row = new StringBuilder("[noodles]   ");
            for (int x = start; x < start + MAP_SIDE; x++) {
                int i = index(x, y, z);
                byte before = now[i], after = candidate[i];
                row.append(before == after ? switch (after) {
                    case SOLID, STONE -> '#';
                    case WATER -> '~';
                    default -> '.';
                } : switch (after) {
                    case WATER -> '+';
                    case STONE -> 'o';
                    case AIR -> '-';
                    default -> '!';
                });
            }
            System.out.println(row);
        }
    }

    private static byte kind(double density, PvAquiferDecision decision) {
        if (density > 0.0) return SOLID;
        if (PvAquiferRules.isWater(decision)) return WATER;
        return decision.state == null ? STONE : AIR;
    }

    private static int index(int localX, int y, int localZ) {
        return ((y - MIN_Y) * SIDE + localX) * SIDE + localZ;
    }

    private static int band(int y) {
        int band = 0;
        while (y >= BAND_TOPS[band]) band++;
        return band;
    }

    private static PvFinalDensity finalDensity(DensityFunction router) {
        List<PvFinalDensity> found = new ArrayList<>();
        router.apply(function -> {
            if (function instanceof PvFinalDensity candidate) found.add(candidate);
            return function;
        });
        assertEquals(1, found.size(), "the router's final density should hold one players-versus:final_density");
        return found.get(0);
    }

    /** Totals for one variant over every area. */
    private static final class Stats {
        long water;
        /** Blocks the variant opens that are solid now, by what they become and by band. */
        final long[][] opened = new long[KINDS.length][BAND_TOPS.length];
        /** Water bodies (6-connected, inside the area), and those holding two or more of today's bodies. */
        long bodies, joining, joined;
        /** Bodies with no water of today's: flooded noodles that meet no cave. */
        long newBodies, newBodyBlocks;
        /** New water (solid today) in a body that holds some of today's water: corridors reaching a lake. */
        long connected;
        /** Water blocks with dry air beside or below them. */
        long leaks;

        void add(byte[] now, byte[] kinds) {
            for (int i = 0; i < kinds.length; i++) {
                if (kinds[i] == WATER) this.water++;
                if (now[i] == SOLID && kinds[i] != SOLID) this.opened[kinds[i]][band(i / (SIDE * SIDE) + MIN_Y)]++;
            }
            // today's bodies, labelled, then the variant's bodies over them
            int[] nowLabel = label(now);
            int[] label = label(kinds);
            int bodies = 0;
            for (int value : label) bodies = Math.max(bodies, value);
            this.bodies += bodies;
            List<Set<Integer>> held = new ArrayList<>();
            for (int b = 0; b <= bodies; b++) held.add(new HashSet<>());
            long[] size = new long[bodies + 1];
            for (int i = 0; i < label.length; i++) {
                if (label[i] == 0) continue;
                size[label[i]]++;
                if (nowLabel[i] != 0) held.get(label[i]).add(nowLabel[i]);
            }
            for (int i = 0; i < label.length; i++) {
                if (label[i] != 0 && now[i] == SOLID && !held.get(label[i]).isEmpty()) this.connected++;
            }
            for (int b = 1; b <= bodies; b++) {
                int count = held.get(b).size();
                if (count >= 2) {
                    this.joining++;
                    this.joined += count;
                }
                if (count == 0) {
                    this.newBodies++;
                    this.newBodyBlocks += size[b];
                }
            }
        }

        /** Water bodies by flood fill: 0 for blocks that aren't water, else the body's number from 1. */
        private static int[] label(byte[] kinds) {
            int[] label = new int[kinds.length];
            int[] queue = new int[kinds.length];
            int next = 0;
            for (int start = 0; start < kinds.length; start++) {
                if (kinds[start] != WATER || label[start] != 0) continue;
                next++;
                int head = 0, tail = 0;
                queue[tail++] = start;
                label[start] = next;
                while (head < tail) {
                    int i = queue[head++];
                    int layer = i / (SIDE * SIDE), x = (i / SIDE) % SIDE, z = i % SIDE;
                    for (int[] offset : NEIGHBOURS) {
                        int nx = x + offset[0], ny = layer + offset[1], nz = z + offset[2];
                        if (nx < 0 || nx >= SIDE || nz < 0 || nz >= SIDE || ny < 0 || ny >= LAYERS) continue;
                        int n = (ny * SIDE + nx) * SIDE + nz;
                        if (kinds[n] != WATER || label[n] != 0) continue;
                        label[n] = next;
                        queue[tail++] = n;
                    }
                }
            }
            return label;
        }

        String describe(double chunks) {
            StringBuilder text = new StringBuilder(String.format(Locale.ROOT, "water %.1f; opened", this.water / chunks));
            for (int kind = 1; kind < KINDS.length; kind++) {
                text.append(String.format(Locale.ROOT, " %s", KINDS[kind]));
                for (int band = 0; band < BAND_TOPS.length; band++) {
                    text.append(String.format(Locale.ROOT, " %.1f", this.opened[kind][band] / chunks));
                }
                text.append(kind + 1 < KINDS.length ? "," : ";");
            }
            text.append(String.format(Locale.ROOT, " (bands y %d..%d, %d..%d, %d..%d)", MIN_Y, BAND_TOPS[0] - 1,
                    BAND_TOPS[0], BAND_TOPS[1] - 1, BAND_TOPS[1], BAND_TOPS[2] - 1));
            text.append(String.format(Locale.ROOT, "; new water reaching a lake %.1f; water bodies %.1f, of which %.2f join %.2f of those"
                            + " without the corridors, %.2f meet no cave (%.1f blocks); water with dry air beside or below %d",
                    this.connected / chunks, this.bodies / chunks, this.joining / chunks, this.joined / chunks, this.newBodies / chunks,
                    this.newBodyBlocks / chunks, this.leaks));
            return text.toString();
        }
    }
}
