package frootloops.versus.mod.environment.worldgen.aquifer;

import frootloops.versus.mod.environment.worldgen.TerrainPass;
import frootloops.versus.mod.environment.worldgen.WorldgenTestData;
import frootloops.versus.mod.environment.worldgen.density.PvTerrain;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A survey of the Players Versus water in vanilla's terrain pass, on this mod's real data, over chunks anywhere, on
 * coasts and along rivers:
 * <ul>
 *   <li>where water touches open air beside or below it, so it would spill there, or stand as a wall of water until
 *   something updates it: nowhere, including once carvers open the blocks around it;</li>
 *   <li>the barrier stone: open terrain the aquifer fills to keep water from air, and how much of it touches water;</li>
 *   <li>how much of each height is cave, now and with candidate height gradients for the cave entrances.</li>
 * </ul>
 * Neighbours are only looked at inside a chunk, so each chunk is surveyed on its own ({@code AquiferTerrainPassTest}
 * looks across chunk borders).
 */
class AquiferSurveyTest {

    private static final long SEED = 8675309L;
    /** Vanilla's overworld lava level: open blocks below it are lava. */
    private static final int LAVA_BELOW_Y = -54;
    private static final int SEA_LEVEL = 64;
    private static final int BAND = 8;
    /** Heights the tables print, in bands of {@link #BAND}. */
    private static final int TABLE_MIN_Y = -64, TABLE_MAX_Y = 128;
    private static final PvAquiferDecision[] DECISIONS = PvAquiferDecision.values();
    private static final AquiferSampler.FluidLevelSampler NO_FLUID_LEVELS = (x, y, z) -> {
        throw new UnsupportedOperationException("the survey passes lavaLevel itself");
    };

    /**
     * Candidate height profiles for the caves: terms added to the entrances, each {from y, to y, from value, to value}
     * of a {@code y_clamped_gradient}. "before" is the profile until revision 6 of the refactor plan: the entrances'
     * {@code (48 -> 38, 0 -> -0.155)} was {@code (48 -> 36, 0 -> -0.165)}, and {@code (28 -> 18, 0 -> 0.265)} was
     * {@code (28 -> 18, 0 -> 0.275)}.
     */
    private static final Map<String, double[][]> CANDIDATES = candidates();

    private static Map<String, double[][]> candidates() {
        Map<String, double[][]> candidates = new LinkedHashMap<>();
        candidates.put("now", new double[0][]);
        candidates.put("before", new double[][]{{48, 36, 0.0, -0.165}, {48, 38, 0.0, 0.155}, {28, 18, 0.0, 0.275}, {28, 18, 0.0, -0.265}});
        return candidates;
    }

    @Test
    void survey() {
        NoiseConfig config = WorldgenTestData.noiseConfig(SEED);
        ChunkGeneratorSettings settings = WorldgenTestData.pvSettings();
        GenerationShapeConfig shape = settings.generationShapeConfig();
        DensityFunction finalDensity = config.getNoiseRouter().finalDensity();
        List<String> names = new ArrayList<>(CANDIDATES.keySet());
        List<DensityFunction> candidates = names.stream()
                .map(name -> withEntrancesDelta(finalDensity, delta(CANDIDATES.get(name)))).toList();
        Map<ChunkPos, String> chunks = surveyChunks(config);
        Tally tally = new Tally(names.size(), shape.minimumY(), shape.height());
        long[] unchangedMismatches = {0};
        for (Map.Entry<ChunkPos, String> entry : chunks.entrySet()) {
            ChunkPos chunk = entry.getKey();
            TerrainPass pass = new TerrainPass(config, settings, chunk, NO_FLUID_LEVELS);
            PvAquifer aquifer = assertInstanceOf(PvAquifer.class, pass.aquifer(), "ChunkNoiseSamplerMixin didn't make the aquifer");
            DensityFunction density = pass.register(finalDensity);
            DensityFunction[] registered = candidates.stream().map(pass::register).toArray(DensityFunction[]::new);
            ChunkBlocks blocks = new ChunkBlocks(names.size(), shape.minimumY(), shape.height());
            pass.run((x, y, z, pos) -> {
                int i = blocks.index(x - chunk.getStartX(), y, z - chunk.getStartZ());
                double value = density.sample(pos);
                boolean lava = y < LAVA_BELOW_Y;
                PvAquiferDecision decision = aquifer.decide(pos, value, lava);
                blocks.state[i] = (byte) decision.ordinal();
                PvAquiferDecision carved = decision == PvAquiferDecision.SOLID ? aquifer.decide(pos, 0.0, lava) : decision;
                blocks.state0[i] = (byte) carved.ordinal();
                for (int c = 0; c < registered.length; c++) {
                    double candidate = registered[c].sample(pos);
                    blocks.open[c][i] = candidate <= 0.0;
                    if (c == 0 && candidate != value) unchangedMismatches[0]++;
                }
            });
            tally.add(blocks, entry.getValue());
        }
        tally.print(names);
        assertEquals(0, unchangedMismatches[0], "the survey's unchanged candidate differs from the router's final density");
        assertTrue(tally.openWater > 0 && tally.caveBlocks(0) > 0, "the survey found no water or no caves");
        long leaking = tally.leaking[0][0] + tally.leaking[0][1] + tally.leaking[1][0] + tally.leaking[1][1];
        assertEquals(0, leaking, "water blocks next to open air");
        assertEquals(0, tally.wallAnywhere, "dry blocks water could flow into once carved");
    }

    /** {@code finalDensity} with {@code delta} added to the entrances its terrain reads. */
    private static DensityFunction withEntrancesDelta(DensityFunction finalDensity, DensityFunction delta) {
        int[] replaced = {0};
        DensityFunction result = finalDensity.apply(function -> {
            if (!(function instanceof PvTerrain terrain)) return function;
            replaced[0]++;
            return new PvTerrain(terrain.offset(), terrain.factor(), terrain.jaggedness(), terrain.jagged(), terrain.ridges(),
                    terrain.base3d(), DensityFunctionTypes.add(terrain.entrances(), delta), terrain.spaghettiRoughness(),
                    terrain.caveLayer(), terrain.caveCheese(), terrain.pillar());
        });
        assertEquals(1, replaced[0], "the final density should hold one terrain");
        return result;
    }

    private static DensityFunction delta(double[][] terms) {
        DensityFunction delta = DensityFunctionTypes.constant(0.0);
        for (double[] term : terms) {
            delta = DensityFunctionTypes.add(delta, DensityFunctionTypes.yClampedGradient((int) term[0], (int) term[1], term[2], term[3]));
        }
        return delta;
    }

    /** Chunks to survey, with the kind of place each is: anywhere, on a coast, along a river inland. */
    private static Map<ChunkPos, String> surveyChunks(NoiseConfig config) {
        DensityFunction continents = config.getNoiseRouter().continents(), ridges = config.getNoiseRouter().ridges();
        Map<ChunkPos, String> chunks = new LinkedHashMap<>();
        Map<String, Integer> wanted = new LinkedHashMap<>();
        wanted.put("anywhere", 20);
        wanted.put("coast", 10);
        wanted.put("river", 10);
        Map<String, Integer> found = new LinkedHashMap<>();
        Random random = new Random(SEED);
        for (int attempt = 0; attempt < 100000 && chunks.size() < 40; attempt++) {
            ChunkPos chunk = new ChunkPos(random.nextInt(800) - 400, random.nextInt(800) - 400);
            if (chunks.containsKey(chunk)) continue;
            DensityFunction.NoisePos center = new DensityFunction.UnblendedNoisePos(chunk.getCenterX(), SEA_LEVEL, chunk.getCenterZ());
            double continentalness = continents.sample(center), ridge = ridges.sample(center);
            // vanilla's coast is continentalness -0.19..-0.11
            String kind = found.getOrDefault("anywhere", 0) < wanted.get("anywhere") ? "anywhere"
                    : continentalness >= -0.19 && continentalness < -0.11 ? "coast"
                    : continentalness >= -0.11 && Math.abs(ridge) < 0.05 ? "river" : null;
            if (kind == null || found.getOrDefault(kind, 0) >= wanted.get(kind)) continue;
            found.merge(kind, 1, Integer::sum);
            chunks.put(chunk, kind);
        }
        assertEquals(wanted, found, "not enough chunks of each kind found");
        return chunks;
    }

    static boolean water(PvAquiferDecision decision) {
        return decision == PvAquiferDecision.SEA_WATER || decision == PvAquiferDecision.SEA_WATER_TICKING
                || decision == PvAquiferDecision.BASIN_WATER || decision == PvAquiferDecision.BASIN_WATER_TICKING;
    }

    static boolean seaWater(PvAquiferDecision decision) {
        return decision == PvAquiferDecision.SEA_WATER || decision == PvAquiferDecision.SEA_WATER_TICKING;
    }

    static boolean barrier(PvAquiferDecision decision) {
        return decision == PvAquiferDecision.SEA_BARRIER || decision == PvAquiferDecision.BASIN_BARRIER;
    }

    /** Solid in the world: the terrain's, or a barrier's. */
    static boolean solid(PvAquiferDecision decision) {
        return decision == PvAquiferDecision.SOLID || barrier(decision);
    }

    /** One chunk's decisions and candidate caves, by block. */
    private static final class ChunkBlocks {
        final int minY, height;
        /** Decision ordinals: at the block's own density, and at density 0 (what a carver gets there). */
        final byte[] state, state0;
        /** Per candidate: whether its terrain leaves the block open. */
        final boolean[][] open;

        ChunkBlocks(int candidates, int minY, int height) {
            this.minY = minY;
            this.height = height;
            this.state = new byte[16 * 16 * height];
            this.state0 = new byte[16 * 16 * height];
            this.open = new boolean[candidates][16 * 16 * height];
        }

        int index(int localX, int y, int localZ) {
            return ((y - this.minY) * 16 + localX) * 16 + localZ;
        }

        PvAquiferDecision at(int localX, int y, int localZ) {
            return DECISIONS[this.state[this.index(localX, y, localZ)]];
        }

        PvAquiferDecision carvedAt(int localX, int y, int localZ) {
            return DECISIONS[this.state0[this.index(localX, y, localZ)]];
        }

        boolean inside(int localX, int y, int localZ) {
            return localX >= 0 && localX < 16 && localZ >= 0 && localZ < 16 && y >= this.minY && y < this.minY + this.height;
        }
    }

    /** The survey's counts, over all chunks. */
    private static final class Tally {
        /** Where water can flow from a block: the four sides, then below. */
        private static final int[][] FLOW = {{-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}, {0, -1, 0}};
        /** Where water can flow into a block from: the four sides, then above. */
        private static final int[][] INFLOW = {{-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}, {0, 1, 0}};
        private static final int[][] ALL_SIDES = {{-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}, {0, 1, 0}, {0, -1, 0}};

        final int minY, height, bands;
        int chunks;
        final Map<String, Integer> kinds = new LinkedHashMap<>();
        long openWater;
        // by band: open water and air in caves (under solid blocks) and under the sky
        final long[] caveSea, caveBasin, caveAir, skySea, skyBasin, skyAirBelowSea;
        // by band: barrier stone (open terrain the aquifer fills), in caves and under the sky
        final long[] barrierCave, barrierSky;
        // by band: water blocks with open air beside or below them, and the open air blocks a wall would fill
        final long[] leakingByBand, wallByBand;
        /** Water blocks next to open air, by [sea 0, basin 1][ticking]. */
        final long[][] leaking = new long[2][2];
        /** Pairs of water and open air, by [beside 0, below 1]. */
        final long[] leakPairs = new long[2];
        /** Open air next to water (beside, or below it), by [in a cave 0, under the sky 1]. */
        final long[] exposedAir = new long[2];
        /** Open air a wall at the water's edge fills, by [cave 0, sky 1][for sea water 0, for basin water 1]. */
        final long[][] wallOpen = new long[2][2];
        /** Blocks a wall at the water's edge makes solid wherever they are opened, the terrain's solid ones included. */
        long wallAnywhere;
        /** Open water a wall on the water's side would turn to stone, by [sea, basin][cave, sky]. */
        final long[][] waterSideWall = new long[2][2];
        // barrier stone away from the chunk's edges, by [sea, basin][cave, sky]
        final long[][] barrierTotal = new long[2][2], barrierNeeded = new long[2][2], barrierTouching = new long[2][2],
                barrierSeenUnneeded = new long[2][2];
        /** Open air in the sea band's reach (y -32..63), and solid blocks beside or above it, where a wall would read the aquifer. */
        long airInReach, solidNextToAir;
        /** Barrier stone with open water right above it and open air right below: a floor one block thick. */
        long thinFloors;
        // per candidate, by band: blocks under the highest solid block of the terrain now, and the open ones among them
        final long[][] candidateCovered, candidateCave;
        /** Per candidate: columns whose highest solid block now is open, a new way into the caves from the surface. */
        final long[] candidateOpenedSurface;
        // per kind of place: chunks, leaking water, wall blocks in caves, wall blocks under the sky, barrier stone
        final Map<String, long[]> byKind = new LinkedHashMap<>();

        Tally(int candidates, int minY, int height) {
            this.minY = minY;
            this.height = height;
            this.bands = height / BAND;
            this.caveSea = new long[this.bands];
            this.caveBasin = new long[this.bands];
            this.caveAir = new long[this.bands];
            this.skySea = new long[this.bands];
            this.skyBasin = new long[this.bands];
            this.skyAirBelowSea = new long[this.bands];
            this.barrierCave = new long[this.bands];
            this.barrierSky = new long[this.bands];
            this.leakingByBand = new long[this.bands];
            this.wallByBand = new long[this.bands];
            this.candidateCovered = new long[candidates][this.bands];
            this.candidateCave = new long[candidates][this.bands];
            this.candidateOpenedSurface = new long[candidates];
        }

        long caveBlocks(int candidate) {
            long sum = 0;
            for (long blocks : this.candidateCave[candidate]) sum += blocks;
            return sum;
        }

        void add(ChunkBlocks blocks, String kind) {
            this.chunks++;
            this.kinds.merge(kind, 1, Integer::sum);
            long[] kindCounts = this.byKind.computeIfAbsent(kind, key -> new long[5]);
            kindCounts[0]++;
            int maxY = this.minY + this.height;
            // the highest solid block of each column: open blocks under it are in caves
            int[][] top = new int[16][16];
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    top[x][z] = this.minY - 1;
                    for (int y = maxY - 1; y >= this.minY; y--) {
                        if (solid(blocks.at(x, y, z))) {
                            top[x][z] = y;
                            break;
                        }
                    }
                }
            }
            boolean[] countedSolid = new boolean[blocks.state.length];
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = this.minY; y < maxY; y++) {
                        int band = (y - this.minY) / BAND;
                        boolean cave = y < top[x][z];
                        int where = cave ? 0 : 1;
                        PvAquiferDecision decision = blocks.at(x, y, z);
                        boolean interior = x > 0 && x < 15 && z > 0 && z < 15;
                        if (water(decision)) {
                            this.openWater++;
                            int type = seaWater(decision) ? 0 : 1;
                            if (type == 0) (cave ? this.caveSea : this.skySea)[band]++;
                            else (cave ? this.caveBasin : this.skyBasin)[band]++;
                            boolean leaks = false, besideCarvableAir = false;
                            for (int d = 0; d < FLOW.length; d++) {
                                int nx = x + FLOW[d][0], ny = y + FLOW[d][1], nz = z + FLOW[d][2];
                                if (!blocks.inside(nx, ny, nz)) continue;
                                if (blocks.at(nx, ny, nz) == PvAquiferDecision.AIR) {
                                    leaks = true;
                                    this.leakPairs[d < 4 ? 0 : 1]++;
                                }
                                if (blocks.carvedAt(nx, ny, nz) == PvAquiferDecision.AIR) besideCarvableAir = true;
                            }
                            if (leaks) {
                                this.leaking[type][decision.needsFluidTick ? 1 : 0]++;
                                this.leakingByBand[band]++;
                                kindCounts[1]++;
                            }
                            if (besideCarvableAir) this.waterSideWall[type][where]++;
                        } else if (decision == PvAquiferDecision.AIR) {
                            (cave ? this.caveAir : this.skyAirBelowSea)[band]++;
                            if (y >= -32) {
                                this.airInReach++;
                                for (int[] offset : INFLOW) {
                                    int nx = x + offset[0], ny = y + offset[1], nz = z + offset[2];
                                    if (!blocks.inside(nx, ny, nz)) continue;
                                    int n = blocks.index(nx, ny, nz);
                                    if (blocks.at(nx, ny, nz) != PvAquiferDecision.AIR && !countedSolid[n]) {
                                        countedSolid[n] = true;
                                        this.solidNextToAir++;
                                    }
                                }
                            }
                            int wall = this.wallReason(blocks, x, y, z, false);
                            if (wall >= 0) {
                                this.wallOpen[where][wall]++;
                                this.wallByBand[band]++;
                                kindCounts[cave ? 2 : 3]++;
                            }
                            if (this.wallReason(blocks, x, y, z, true) >= 0) this.exposedAir[where]++;
                        } else if (barrier(decision)) {
                            (cave ? this.barrierCave : this.barrierSky)[band]++;
                            kindCounts[4]++;
                            if (y > this.minY && y + 1 < maxY && water(blocks.at(x, y + 1, z)) && blocks.at(x, y - 1, z) == PvAquiferDecision.AIR) {
                                this.thinFloors++;
                            }
                            if (interior) {
                                int type = decision == PvAquiferDecision.SEA_BARRIER ? 0 : 1;
                                this.barrierTotal[type][where]++;
                                boolean needed = this.wallReason(blocks, x, y, z, false) >= 0;
                                if (needed) this.barrierNeeded[type][where]++;
                                if (this.wallReason(blocks, x, y, z, true) >= 0) this.barrierTouching[type][where]++;
                                if (!needed && this.nextToOpenAir(blocks, x, y, z)) this.barrierSeenUnneeded[type][where]++;
                            }
                        }
                        if (blocks.carvedAt(x, y, z) == PvAquiferDecision.AIR && this.wallReason(blocks, x, y, z, false) >= 0) {
                            this.wallAnywhere++;
                        }
                    }
                    // one surface for every candidate, the terrain's now: a candidate that opens a cave to the sky
                    // shouldn't take the cave under it out of the count
                    int surface = this.minY - 1;
                    for (int y = maxY - 1; y >= this.minY; y--) {
                        if (!blocks.open[0][blocks.index(x, y, z)]) {
                            surface = y;
                            break;
                        }
                    }
                    for (int c = 0; c < this.candidateCave.length; c++) {
                        if (surface >= this.minY && blocks.open[c][blocks.index(x, surface, z)]) this.candidateOpenedSurface[c]++;
                        for (int y = this.minY; y < surface; y++) {
                            int band = (y - this.minY) / BAND;
                            this.candidateCovered[c][band]++;
                            if (blocks.open[c][blocks.index(x, y, z)]) this.candidateCave[c][band]++;
                        }
                    }
                }
            }
        }

        /**
         * Whether water could flow into this block, from beside or above it: -1 if not, else 0 for sea water, 1 for
         * basin water. With {@code open}, only water the terrain left open counts; else any block the aquifer would
         * fill with water where it's open (a carver may open it later).
         */
        private int wallReason(ChunkBlocks blocks, int x, int y, int z, boolean open) {
            int reason = -1;
            for (int[] offset : INFLOW) {
                int nx = x + offset[0], ny = y + offset[1], nz = z + offset[2];
                if (!blocks.inside(nx, ny, nz)) continue;
                PvAquiferDecision neighbour = open ? blocks.at(nx, ny, nz) : blocks.carvedAt(nx, ny, nz);
                if (!water(neighbour)) continue;
                if (seaWater(neighbour)) return 0;
                reason = 1;
            }
            return reason;
        }

        private boolean nextToOpenAir(ChunkBlocks blocks, int x, int y, int z) {
            for (int[] offset : ALL_SIDES) {
                int nx = x + offset[0], ny = y + offset[1], nz = z + offset[2];
                if (blocks.inside(nx, ny, nz) && blocks.at(nx, ny, nz) == PvAquiferDecision.AIR) return true;
            }
            return false;
        }

        void print(List<String> names) {
            double n = this.chunks;
            System.out.println("[survey] " + this.chunks + " chunks " + this.kinds + "; per chunk:");
            System.out.printf(Locale.ROOT, "[survey] open water: sea %.0f in caves, %.0f under the sky; basin %.0f in caves, %.0f under the sky%n",
                    sum(this.caveSea) / n, sum(this.skySea) / n, sum(this.caveBasin) / n, sum(this.skyBasin) / n);
            System.out.printf(Locale.ROOT, "[survey] barrier stone (open terrain the aquifer fills): %.1f in caves, %.1f under the sky;"
                    + " floors one block thick between water and open air %.1f%n", sum(this.barrierCave) / n, sum(this.barrierSky) / n, this.thinFloors / n);
            System.out.printf(Locale.ROOT, "[survey] water next to open air (beside it or above it): sea %.1f (%.1f ticking), basin %.1f (%.1f ticking);"
                            + " %.1f pairs beside, %.1f above; the air: %.1f blocks in caves, %.1f under the sky%n",
                    (this.leaking[0][0] + this.leaking[0][1]) / n, this.leaking[0][1] / n, (this.leaking[1][0] + this.leaking[1][1]) / n,
                    this.leaking[1][1] / n, this.leakPairs[0] / n, this.leakPairs[1] / n, this.exposedAir[0] / n, this.exposedAir[1] / n);
            System.out.printf(Locale.ROOT, "[survey] a wall where water meets air, on the air's side, fills: in caves %.1f for sea water, %.1f for basins;"
                            + " under the sky %.1f for sea water, %.1f for basins; %.1f blocks wherever opened (carvers too)%n",
                    this.wallOpen[0][0] / n, this.wallOpen[0][1] / n, this.wallOpen[1][0] / n, this.wallOpen[1][1] / n, this.wallAnywhere / n);
            System.out.printf(Locale.ROOT, "[survey] a wall on the water's side turns to stone: sea %.1f in caves, %.1f under the sky; basin %.1f in caves, %.1f under the sky%n",
                    this.waterSideWall[0][0] / n, this.waterSideWall[0][1] / n, this.waterSideWall[1][0] / n, this.waterSideWall[1][1] / n);
            String[] types = {"sea", "basin"}, places = {"in caves", "under the sky"};
            for (int type = 0; type < 2; type++) {
                for (int where = 0; where < 2; where++) {
                    long total = this.barrierTotal[type][where];
                    if (total == 0) continue;
                    System.out.printf(Locale.ROOT, "[survey] %s barrier stone %s, away from chunk edges: %.1f per chunk; %.1f%% keeps water (open or not) from"
                                    + " flowing into it, %.1f%% touches open water; %.1f%% keeps no water and borders open air%n",
                            types[type], places[where], total / n, 100.0 * this.barrierNeeded[type][where] / total,
                            100.0 * this.barrierTouching[type][where] / total, 100.0 * this.barrierSeenUnneeded[type][where] / total);
                }
            }
            System.out.printf(Locale.ROOT, "[survey] for a wall's cost: %.0f open air blocks in y -32..63 per chunk, with %.0f other blocks beside or above them%n",
                    this.airInReach / n, this.solidNextToAir / n);
            for (Map.Entry<String, long[]> kind : this.byKind.entrySet()) {
                long[] counts = kind.getValue();
                double chunks = counts[0];
                System.out.printf(Locale.ROOT, "[survey] %s: per chunk %.1f water blocks next to open air, a wall would fill %.1f in caves and %.1f under the sky,"
                        + " %.0f barrier stone%n", kind.getKey(), counts[1] / chunks, counts[2] / chunks, counts[3] / chunks, counts[4] / chunks);
            }

            System.out.println("[survey] per chunk by height:   y  sea(cave/sky)  basin(cave/sky)  air(cave/sky<64)  barrier(cave/sky)  leaking  wall");
            for (int band = 0; band < this.bands; band++) {
                int y = this.minY + band * BAND;
                if (y < TABLE_MIN_Y || y >= TABLE_MAX_Y) continue;
                System.out.printf(Locale.ROOT, "[survey] %4d..%-4d %7.0f/%-7.0f %7.0f/%-7.0f %7.0f/%-7.0f %7.0f/%-7.0f %7.1f %7.1f%n", y, y + BAND - 1,
                        this.caveSea[band] / n, this.skySea[band] / n, this.caveBasin[band] / n, this.skyBasin[band] / n,
                        this.caveAir[band] / n, this.skyAirBelowSea[band] / n, this.barrierCave[band] / n, this.barrierSky[band] / n,
                        this.leakingByBand[band] / n, this.wallByBand[band] / n);
            }

            StringBuilder header = new StringBuilder("[survey] caves (share of the blocks under the terrain's surface now) by height:       y");
            for (String name : names) header.append(String.format(Locale.ROOT, " | %-16s", name));
            System.out.println(header);
            for (int band = 0; band < this.bands; band++) {
                int y = this.minY + band * BAND;
                if (y < TABLE_MIN_Y || y >= TABLE_MAX_Y) continue;
                StringBuilder row = new StringBuilder(String.format(Locale.ROOT, "[survey] %4d..%-4d", y, y + BAND - 1));
                long now = this.candidateCave[0][band];
                for (int c = 0; c < names.size(); c++) {
                    long covered = this.candidateCovered[c][band], cave = this.candidateCave[c][band];
                    double share = covered == 0 ? 0.0 : 100.0 * cave / covered;
                    row.append(c == 0 ? String.format(Locale.ROOT, " | %5.2f%% %8.0f", share, cave / n)
                            : String.format(Locale.ROOT, " | %5.2f%% %+6.1f%%", share, now == 0 ? 0.0 : 100.0 * (cave - now) / now));
                }
                System.out.println(row);
            }
            StringBuilder totals = new StringBuilder("[survey] cave blocks per chunk, all heights (columns per chunk whose surface block opens):");
            for (int c = 0; c < names.size(); c++) {
                totals.append(String.format(Locale.ROOT, " %s %.0f (%.2f);", names.get(c), this.caveBlocks(c) / n, this.candidateOpenedSurface[c] / n));
            }
            System.out.println(totals);
        }

        private static long sum(long[] values) {
            long sum = 0;
            for (long value : values) sum += value;
            return sum;
        }
    }
}
