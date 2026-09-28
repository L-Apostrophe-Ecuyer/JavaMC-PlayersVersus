package frootloops.versus.mod.environment.worldgen.aquifer;

import frootloops.versus.mod.environment.worldgen.TerrainPass;
import frootloops.versus.mod.environment.worldgen.WorldgenTestData;
import frootloops.versus.mod.environment.worldgen.density.DensityOps;
import frootloops.versus.mod.environment.worldgen.density.PvFinalDensity;
import frootloops.versus.mod.environment.worldgen.density.PvNoodle;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Noodle caves back in the basin layers, only around the flooded caves (the refactor plan, Section 10, question 7):
 * candidate rules played out in vanilla's terrain pass over areas of 5 x 5 chunks. The noodle's height bias keeps it
 * out of y -8..20 today ({@link PvNoodle#bias}); a candidate lowers that bias where the basin floodedness S is above
 * a threshold, so noodles open only where the aquifer floods them. Per candidate: the blocks it opens, what the aquifer
 * makes of them (water, stone, dry air), and how the water bodies of y -3..23 join up (neighbours across the whole
 * area).
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

    /**
     * A rule for the noodle's height bias in y -3..23: where S passes {@code spreadFrom} (or the basin water threshold,
     * if that's higher), the bias moves from today's towards {@code bias}, over {@code taper} of S (at once if 0).
     */
    private record Candidate(String name, double spreadFrom, double taper, double bias) {
        double biasAt(int y, double spread) {
            double now = PvNoodle.bias(y);
            double threshold = Math.max(this.spreadFrom, PvAquiferRules.basinWaterThreshold(y));
            double share = this.taper == 0.0 ? (spread > threshold ? 1.0 : 0.0)
                    : MathHelper.clamp((spread - threshold) / this.taper, 0.0, 1.0);
            return now + (this.bias - now) * share;
        }
    }

    private static final List<Candidate> CANDIDATES = List.of(
            new Candidate("S > 0.5, bias 0", 0.5, 0.0, 0.0),
            new Candidate("S > 0.5 over 0.1, bias 0", 0.5, 0.1, 0.0),
            new Candidate("S > 0.5 over 0.1, bias -0.02", 0.5, 0.1, -0.02),
            new Candidate("S > 0.6 over 0.1, bias 0", 0.6, 0.1, 0.0),
            new Candidate("S > 0.3 over 0.1, bias 0", 0.3, 0.1, 0.0));

    @Test
    void survey() {
        NoiseConfig config = WorldgenTestData.noiseConfig(SEED);
        ChunkGeneratorSettings settings = WorldgenTestData.pvSettings();
        PvFinalDensity finalDensity = finalDensity(config.getNoiseRouter().finalDensity());
        List<ChunkPos> centers = AquiferSurveyTest.surveyChunks(config).entrySet().stream()
                .filter(entry -> entry.getValue().equals("anywhere")).map(Map.Entry::getKey).limit(AREAS).toList();
        int variants = CANDIDATES.size() + 1;
        Stats[] stats = new Stats[variants];
        for (int v = 0; v < variants; v++) stats[v] = new Stats();
        long[] routerMismatches = {0};
        for (ChunkPos center : centers) {
            byte[][] kinds = new byte[variants][SIDE * SIDE * LAYERS];
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
                    pass.run((x, y, z, pos) -> {
                        if (y < MIN_Y || y >= MAX_Y) return;
                        double terrainValue = DensityOps.squeeze(terrain.sample(pos) * 0.64);
                        double tunnel = PvNoodle.tunnel(pos, toggle, thickness, ridgeA, ridgeB);
                        double now = Math.min(terrainValue, PvNoodle.bias(y) + tunnel);
                        if (now != router.sample(pos)) routerMismatches[0]++;
                        int i = index(x - originX, y, z - originZ);
                        kinds[0][i] = kind(now, aquifer.decide(pos, now, false));
                        double spread = aquifer.spread(pos);
                        for (int c = 0; c < CANDIDATES.size(); c++) {
                            double candidate = Math.min(terrainValue, CANDIDATES.get(c).biasAt(y, spread) + tunnel);
                            kinds[c + 1][i] = candidate == now ? kinds[0][i] : kind(candidate, aquifer.decide(pos, candidate, false));
                        }
                    });
                }
            }
            for (int v = 0; v < variants; v++) stats[v].add(kinds[0], kinds[v]);
        }

        double chunks = AREAS * AREA_CHUNKS * AREA_CHUNKS;
        System.out.printf(Locale.ROOT, "[noodles] %d areas of %d x %d chunks around %s, y %d..%d; per chunk:%n",
                AREAS, AREA_CHUNKS, AREA_CHUNKS, centers, MIN_Y, MAX_Y - 1);
        for (int v = 0; v < variants; v++) {
            String name = v == 0 ? "now" : CANDIDATES.get(v - 1).name();
            System.out.println("[noodles] " + name + ": " + stats[v].describe(chunks));
        }
        assertEquals(0, routerMismatches[0], "the survey's noodle differs from the router's final density");
        assertTrue(stats[0].water > 0, "the survey found no water in the basin layers");
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
            text.append(String.format(Locale.ROOT, "; water bodies %.1f, of which %.2f join %.2f of today's, %.2f meet no cave (%.1f blocks)",
                    this.bodies / chunks, this.joining / chunks, this.joined / chunks, this.newBodies / chunks, this.newBodyBlocks / chunks));
            return text.toString();
        }
    }
}
