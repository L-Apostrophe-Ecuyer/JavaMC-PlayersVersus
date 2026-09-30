package frootloops.versus.mod.environment.worldgen.aquifer;

import frootloops.versus.mod.environment.worldgen.TerrainPass;
import frootloops.versus.mod.environment.worldgen.WorldgenTestData;
import frootloops.versus.mod.environment.worldgen.density.PvFinalDensity;
import frootloops.versus.mod.environment.worldgen.density.PvHighRiver;
import frootloops.versus.mod.environment.worldgen.density.PvNoodle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;

import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_Y;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PvAquifer} in vanilla's terrain pass ({@link TerrainPass}), on this mod's real data:
 * <ul>
 *   <li>its {@link Lattice}s on the cell grid give what the pass's {@code interpolated} gives, bit for bit;</li>
 *   <li>it answers the terrain pass and carvers alike (before Phase 2b they got different F and S);</li>
 *   <li>what each position's own floodedness says ({@link PvAquiferRules#atPosition}) against the terrain pass's before
 *   Phase 2b, when the router's F and S were the JSON that stays in the mod's data, by band, with each change put down
 *   to F or S;</li>
 *   <li>water never touches open air, across chunk borders too, whatever the carvers open;</li>
 *   <li>the flooded corridors: the aquifer's corridor value is the final density's noodle, bit for bit, so it floods
 *   what they open;</li>
 *   <li>the high river: its water is exactly where its valley opens a block at or under its surface, and its bed's
 *   water never touches open air.</li>
 * </ul>
 */
class AquiferTerrainPassTest {

    private static final long SEED = 8675309L;
    /** The sea band is y -31..63 ({@code y > SEA_BAND_MIN_Y}), the basin band y -3..31. */
    private static final int[][] BANDS = {{-31, -4}, {-3, 7}, {8, 31}, {32, 47}, {48, 63}};
    private static final Aquifer.FluidPicker NO_FLUID_LEVELS = (x, y, z) -> {
        throw new UnsupportedOperationException("the test passes lavaLevel itself");
    };

    @Test
    void latticesGiveTheTerrainPassInterpolation() {
        RandomState config = WorldgenTestData.noiseConfig(SEED);
        NoiseGeneratorSettings settings = WorldgenTestData.pvSettings();
        AquiferInputs inputs = AquiferInputs.of(config, settings);
        NoiseSettings shape = settings.noiseSettings();
        int minY = shape.minY(), maxY = shape.minY() + shape.height();
        for (ChunkPos chunk : List.of(new ChunkPos(100, 100), new ChunkPos(-7, 3))) {
            TerrainPass pass = new TerrainPass(config, settings, chunk, NO_FLUID_LEVELS);
            // what the aquifer's lattices sample: the chunk's router depth, the settings' entrances
            Lattice depth = new Lattice(pass.register(config.router().depth()), chunk, minY, maxY, PvAquifer.CELL_HEIGHT);
            Lattice entrances = new Lattice(inputs.floodedness().entrances(), chunk, minY, maxY, PvAquifer.CELL_HEIGHT);
            // what the JSON F interpolated (the reference's depth and entrances: the same values, TerrainPortTest)
            DensityFunction interpolatedDepth = pass.register(interpolated(config, WorldgenTestData.REFERENCE + ":overworld/depth"));
            DensityFunction interpolatedEntrances = pass.register(interpolated(config, WorldgenTestData.REFERENCE + ":overworld/caves/entrances"));
            Mismatches depthMismatches = new Mismatches("depth"), entrancesMismatches = new Mismatches("entrances");
            pass.run((x, y, z, pos) -> {
                depthMismatches.compare(x, y, z, interpolatedDepth.compute(pos), depth.at(x, y, z));
                entrancesMismatches.compare(x, y, z, interpolatedEntrances.compute(pos), entrances.at(x, y, z));
            });
            System.out.println("[terrain pass] chunk " + chunk.x + "," + chunk.z + ": " + depthMismatches + "; " + entrancesMismatches);
            assertEquals(0, depthMismatches.count, depthMismatches::toString);
            assertEquals(0, entrancesMismatches.count, entrancesMismatches::toString);
            assertTrue(depthMismatches.compared > 16 * 16 * 300, "the pass covered " + depthMismatches.compared + " blocks");
        }
    }

    @Test
    void aquiferKeepsTheTerrainPassDecisions() {
        RandomState config = WorldgenTestData.noiseConfig(SEED);
        NoiseGeneratorSettings settings = WorldgenTestData.pvSettings();
        int bands = BANDS.length;
        int[] total = new int[bands], same = new int[bands], sameForCarvers = new int[bands], beforeSameForCarvers = new int[bands];
        int[] sameWithNewF = new int[bands], sameWithNewS = new int[bands];
        int[] spreadChangesByLocalX = new int[16];
        Map<String, Integer> changes = new HashMap<>();
        Mismatches floodedness = new Mismatches("F"), spread = new Mismatches("S"), spreadAtCorners = new Mismatches("S at cell corners");
        int chunks = 0, latticePoints = 0;
        for (int chunkX = 99; chunkX <= 101; chunkX++) {
            for (int chunkZ = 99; chunkZ <= 101; chunkZ++) {
                ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
                TerrainPass pass = new TerrainPass(config, settings, chunk, NO_FLUID_LEVELS);
                PvAquifer aquifer = assertInstanceOf(PvAquifer.class, pass.aquifer(), "ChunkNoiseSamplerMixin didn't make the aquifer");
                // the router's F and S before Phase 2b
                DensityFunction jsonF = pass.register(WorldgenTestData.seeded(config, WorldgenTestData.REFERENCE + ":overworld/aquifer_fluid_level_floodedness"));
                DensityFunction jsonS = pass.register(WorldgenTestData.seeded(config, WorldgenTestData.REFERENCE + ":overworld/aquifer_fluid_level_spread"));
                pass.run((x, y, z, pos) -> {
                    int band = band(y);
                    if (band < 0) return;
                    DensityFunction.FunctionContext carverPos = new DensityFunction.SinglePointContext(x, y, z);
                    PvAquiferDecision now = aquifer.atPosition(x, y, z);
                    PvAquiferDecision before = PvAquiferRules.atPosition(pos, jsonF::compute, jsonS::compute);
                    total[band]++;
                    if (now == before) same[band]++;
                    else changes.merge(before + " -> " + now, 1, Integer::sum);
                    if (aquifer.decide(pos, 0.0, false) == aquifer.decide(carverPos, 0.0, false)) sameForCarvers[band]++;
                    if (before == PvAquiferRules.atPosition(carverPos, jsonF::compute, jsonS::compute)) beforeSameForCarvers[band]++;
                    if (before == PvAquiferRules.atPosition(pos, aquifer::floodedness, jsonS::compute)) sameWithNewF[band]++;
                    if (before == PvAquiferRules.atPosition(pos, jsonF::compute, aquifer::spread)) sameWithNewS[band]++;

                    floodedness.compare(x, y, z, jsonF.compute(pos), aquifer.floodedness(pos));
                    if (y > -4 && y < 32) {
                        double spreadBefore = jsonS.compute(pos), spreadNow = aquifer.spread(pos);
                        if (spread.compare(x, y, z, spreadBefore, spreadNow)) spreadChangesByLocalX[x - chunk.getMinBlockX()]++;
                        if (Math.floorMod(x, 4) == 0 && Math.floorMod(y, PvAquifer.CELL_HEIGHT) == 0 && Math.floorMod(z, 4) == 0) {
                            spreadAtCorners.compare(x, y, z, spreadBefore, spreadNow);
                        }
                    }
                });
                chunks++;
                for (int points : aquifer.latticeSamples()) latticePoints += points;
            }
        }

        int all = Arrays.stream(total).sum();
        for (int band = 0; band < bands; band++) {
            System.out.printf(Locale.ROOT, "[terrain pass] y %d..%d: %.3f%% of decisions as before (with only F new %.3f%%, with only S new %.3f%%);"
                            + " carvers get the terrain pass's decision %.3f%% now, %.3f%% before%n",
                    BANDS[band][0], BANDS[band][1], percent(same[band], total[band]), percent(sameWithNewF[band], total[band]),
                    percent(sameWithNewS[band], total[band]), percent(sameForCarvers[band], total[band]), percent(beforeSameForCarvers[band], total[band]));
        }
        System.out.println("[terrain pass] changes: " + changes.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(10)
                .map(entry -> String.format(Locale.ROOT, "%s %.3f%%", entry.getKey(), percent(entry.getValue(), all)))
                .collect(Collectors.joining(", ")));
        System.out.println("[terrain pass] " + floodedness + "; " + spread + "; " + spreadAtCorners);
        System.out.println("[terrain pass] S changes by x within the chunk: " + Arrays.toString(spreadChangesByLocalX));
        System.out.println("[terrain pass] " + latticePoints / chunks + " lattice points per chunk");

        for (int band = 0; band < bands; band++) {
            assertEquals(total[band], sameForCarvers[band], "y " + BANDS[band][0] + ".." + BANDS[band][1] + ": carvers and the terrain pass disagree");
            double agreement = (double) sameWithNewF[band] / total[band];
            assertTrue(agreement > 0.99, String.format(Locale.ROOT, "y %d..%d: F alone changed %.2f%% of decisions",
                    BANDS[band][0], BANDS[band][1], 100 * (1 - agreement)));
        }
    }

    /**
     * The terrain pass over 3 x 3 blocks of chunks (the smoke test's inland one, and one on a coast), each chunk with its
     * own aquifer: no open water has open dry air beside or below it, and no block a carver would leave dry has water
     * beside or above it, across chunk borders too.
     */
    @Test
    void waterNeverTouchesOpenAirAcrossChunkBorders() {
        RandomState config = WorldgenTestData.noiseConfig(SEED);
        NoiseGeneratorSettings settings = WorldgenTestData.pvSettings();
        int water = 0, borderPairs = 0;
        for (ChunkPos center : List.of(new ChunkPos(100, 100), coastalChunk(config))) {
            int[] counts = checkWalls(config, settings, center);
            water += counts[0];
            borderPairs += counts[1];
        }
        assertTrue(water > 1000 && borderPairs > 1000, "too little water to check: " + water + " blocks, " + borderPairs + " border pairs");
    }

    /** @return open water blocks, and water-neighbour pairs across chunk borders, in the 3 x 3 chunks around {@code center} */
    private static int[] checkWalls(RandomState config, NoiseGeneratorSettings settings, ChunkPos center) {
        int size = 3 * 16, minX = (center.x - 1) * 16, minZ = (center.z - 1) * 16, minY = -32, levels = 96;
        byte[] state = new byte[size * size * levels], carved = new byte[size * size * levels];
        for (int chunkX = center.x - 1; chunkX <= center.x + 1; chunkX++) {
            for (int chunkZ = center.z - 1; chunkZ <= center.z + 1; chunkZ++) {
                TerrainPass pass = new TerrainPass(config, settings, new ChunkPos(chunkX, chunkZ), NO_FLUID_LEVELS);
                PvAquifer aquifer = assertInstanceOf(PvAquifer.class, pass.aquifer());
                DensityFunction finalDensity = pass.register(config.router().finalDensity());
                pass.run((x, y, z, pos) -> {
                    if (y < minY || y >= minY + levels) return;
                    int i = ((y - minY) * size + (x - minX)) * size + (z - minZ);
                    PvAquiferDecision decision = aquifer.decide(pos, finalDensity.compute(pos), false);
                    state[i] = (byte) decision.ordinal();
                    carved[i] = (byte) (decision == PvAquiferDecision.SOLID ? aquifer.decide(pos, 0.0, false) : decision).ordinal();
                });
            }
        }
        PvAquiferDecision[] decisions = PvAquiferDecision.values();
        int[][] sidesAndBelow = {{-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}, {0, -1, 0}};
        int water = 0, walls = 0, leaks = 0, carvableLeaks = 0, borderPairs = 0;
        List<String> examples = new ArrayList<>();
        for (int y = minY; y < minY + levels; y++) {
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    int i = ((y - minY) * size + x) * size + z;
                    PvAquiferDecision here = decisions[state[i]], hereCarved = decisions[carved[i]];
                    if (here == PvAquiferDecision.SEA_BARRIER || here == PvAquiferDecision.BASIN_BARRIER) walls++;
                    boolean open = here != PvAquiferDecision.SOLID;
                    if (open && PvAquiferRules.isWater(here)) water++;
                    if (!PvAquiferRules.isWater(hereCarved)) continue;
                    // water here, where it's open or once carved, and the blocks it would flow into
                    for (int[] offset : sidesAndBelow) {
                        int nx = x + offset[0], ny = y + offset[1], nz = z + offset[2];
                        if (nx < 0 || nx >= size || nz < 0 || nz >= size || ny < minY) continue;
                        int n = ((ny - minY) * size + nx) * size + nz;
                        if ((nx >> 4) != (x >> 4) || (nz >> 4) != (z >> 4)) borderPairs++;
                        String pair = (minX + x) + "," + y + "," + (minZ + z) + " -> " + (minX + nx) + "," + ny + "," + (minZ + nz);
                        if (open && PvAquiferRules.isWater(here) && decisions[state[n]] == PvAquiferDecision.AIR) {
                            leaks++;
                            if (examples.size() < 5) examples.add("open " + pair);
                        }
                        if (decisions[carved[n]] == PvAquiferDecision.AIR) {
                            carvableLeaks++;
                            if (examples.size() < 5) examples.add("carved " + pair);
                        }
                    }
                }
            }
        }
        System.out.printf(Locale.ROOT, "[terrain pass] 3 x 3 chunks around %d,%d, y %d..%d: %d open water blocks, %d wall blocks in open terrain;"
                        + " water next to open dry air %d, next to dry air once carved %d (%d water-neighbour pairs across chunk borders checked)%s%n",
                center.x, center.z, minY, minY + levels - 1, water, walls, leaks, carvableLeaks, borderPairs, examples.isEmpty() ? "" : ", e.g. " + examples);
        assertEquals(0, leaks, () -> "water next to open air: " + examples);
        assertEquals(0, carvableLeaks, () -> "water next to air once carved: " + examples);
        return new int[]{water, borderPairs};
    }

    /**
     * What the aquifer costs in the terrain pass, for the open blocks the pass asks about, in three ways: each block's
     * own floodedness only (the rule before walls: a band is stone), plus the walls where water could flow in, plus the
     * bands within 2 steps of water (the rule in the code). Each gets fresh passes over the same chunks; the differences
     * are the walls'. Also counts the lattice points and the blocks whose own floodedness each needed.
     */
    /**
     * The flooded corridors (the refactor plan, Section 10, question 7): at every block of their layers, the aquifer's
     * corridor value ({@link PvAquifer#corridor}, from its lattices) is the final density's noodle with the corridors'
     * bias, to the bit; where that opens the block, the final density is open and the aquifer's water fills it (except
     * where the sea's band comes first).
     */
    @Test
    void corridorsFloodWhatTheTerrainOpens() {
        RandomState config = WorldgenTestData.noiseConfig(SEED);
        NoiseGeneratorSettings settings = WorldgenTestData.pvSettings();
        PvFinalDensity kernel = assertInstanceOf(PvFinalDensity.class, WorldgenTestData.seeded(config, "players-versus:overworld/final_density"));
        long[] counts = new long[4];
        // a chunk of the smoke region, and one full of flooded caves (FloodedNoodleSurveyTest's first area)
        for (ChunkPos chunk : List.of(new ChunkPos(100, 100), new ChunkPos(96, 128), new ChunkPos(97, 128))) {
            TerrainPass pass = new TerrainPass(config, settings, chunk, NO_FLUID_LEVELS);
            PvAquifer aquifer = assertInstanceOf(PvAquifer.class, pass.aquifer(), "ChunkNoiseSamplerMixin didn't make the aquifer");
            DensityFunction router = pass.register(config.router().finalDensity());
            DensityFunction entrances = pass.register(kernel.entrances());
            DensityFunction toggle = pass.register(kernel.noodleToggle()), thickness = pass.register(kernel.noodleThickness());
            DensityFunction ridgeA = pass.register(kernel.noodleRidgeA()), ridgeB = pass.register(kernel.noodleRidgeB());
            pass.run((x, y, z, pos) -> {
                if (!PvNoodle.inCorridorLayers(y)) return;
                double noodle = PvNoodle.corridorBias(y, entrances.compute(pos)) + PvNoodle.tunnel(pos, toggle, thickness, ridgeA, ridgeB);
                assertEquals(noodle, aquifer.corridor(pos), 0.0, () -> "the corridors' noodle at " + x + "," + y + "," + z);
                counts[0]++;
                if (noodle > 0.0) return;
                counts[1]++;
                double density = router.compute(pos);
                assertTrue(density <= 0.0, () -> "the corridors' noodle opens " + x + "," + y + "," + z + " but the final density is " + density);
                PvAquiferDecision decision = aquifer.decide(pos, density, false);
                if (PvAquiferRules.isWater(decision)) counts[2]++;
                else if (aquifer.atPosition(x, y, z) == PvAquiferDecision.SEA_BARRIER) counts[3]++;
            });
        }
        System.out.printf(Locale.ROOT, "[terrain pass] corridors: %d blocks compared, %d opened by the corridors' noodle, %d of them water,"
                + " %d the sea's band%n", counts[0], counts[1], counts[2], counts[3]);
        assertTrue(counts[1] > 0, "no block opened by the corridors in the chunks tried");
        assertEquals(counts[1], counts[2] + counts[3], "blocks the corridors open that are neither water nor the sea's band");
    }

    /**
     * The high river (the refactor plan, Section 10, question 7): at every block of its bed and surface, the aquifer
     * puts its water ({@link PvAquifer#highRiverAt}, from lattices of its inputs) exactly where its valley, as the terrain
     * pass interpolates its inputs, opens the block; the final density is open there, and the aquifer fills it.
     */
    @Test
    void highRiverFloodsWhatTheValleyOpens() {
        RandomState config = WorldgenTestData.noiseConfig(SEED);
        NoiseGeneratorSettings settings = WorldgenTestData.pvSettings();
        PvHighRiver river = WorldgenTestData.highRiver(config);
        ChunkPos center = WorldgenTestData.highRiverChunk(config);
        long[] counts = new long[4];
        for (ChunkPos chunk : List.of(center, new ChunkPos(center.x + 1, center.z), new ChunkPos(center.x, center.z - 1))) {
            TerrainPass pass = new TerrainPass(config, settings, chunk, NO_FLUID_LEVELS);
            PvAquifer aquifer = assertInstanceOf(PvAquifer.class, pass.aquifer(), "ChunkNoiseSamplerMixin didn't make the aquifer");
            DensityFunction router = pass.register(config.router().finalDensity());
            DensityFunction valley = pass.register(river);
            pass.run((x, y, z, pos) -> {
                if (y < PvHighRiver.MIN_Y || y > HIGH_RIVER_Y) return;
                double opening = valley.compute(pos);
                PvAquiferDecision own = aquifer.highRiverAt(x, y, z);
                assertEquals(opening < 0.0, own != PvAquiferDecision.AIR, () -> "the valley at " + x + "," + y + "," + z + " is " + opening
                        + " but the aquifer's own decision is " + own);
                counts[0]++;
                if (!(opening < 0.0)) return;
                counts[1]++;
                double density = router.compute(pos);
                assertTrue(density <= 0.0, () -> "the valley opens " + x + "," + y + "," + z + " but the final density is " + density);
                PvAquiferDecision decision = aquifer.decide(pos, density, false);
                assertEquals(y == HIGH_RIVER_Y ? PvAquiferDecision.HIGH_RIVER_WATER : PvAquiferDecision.HIGH_RIVER_BED_WATER, decision,
                        () -> "at " + x + "," + y + "," + z);
                counts[y == HIGH_RIVER_Y ? 2 : 3]++;
            });
        }
        System.out.printf(Locale.ROOT, "[terrain pass] high river around chunk %d,%d: %d blocks of its heights compared, %d opened by the"
                + " valley, %d water at the surface, %d in the bed%n", center.x, center.z, counts[0], counts[1], counts[2], counts[3]);
        assertTrue(counts[2] > 0 && counts[3] > 0, "no river water in the chunks tried");
    }

    /**
     * Around a chunk the high river runs through: its bed's water never has open dry air beside or under it, whether the
     * terrain or a carver opened that air, and its surface's water none under it. Beside the surface's water, open air
     * stays open: the water spills there.
     */
    @Test
    void highRiverBedNeverTouchesOpenAir() {
        RandomState config = WorldgenTestData.noiseConfig(SEED);
        NoiseGeneratorSettings settings = WorldgenTestData.pvSettings();
        ChunkPos center = WorldgenTestData.highRiverChunk(config);
        int size = 3 * 16, minX = (center.x - 1) * 16, minZ = (center.z - 1) * 16, minY = PvHighRiver.MIN_Y - 1, levels = HIGH_RIVER_Y + 2 - minY;
        byte[] state = new byte[size * size * levels], carved = new byte[size * size * levels];
        for (int chunkX = center.x - 1; chunkX <= center.x + 1; chunkX++) {
            for (int chunkZ = center.z - 1; chunkZ <= center.z + 1; chunkZ++) {
                TerrainPass pass = new TerrainPass(config, settings, new ChunkPos(chunkX, chunkZ), NO_FLUID_LEVELS);
                PvAquifer aquifer = assertInstanceOf(PvAquifer.class, pass.aquifer());
                DensityFunction finalDensity = pass.register(config.router().finalDensity());
                pass.run((x, y, z, pos) -> {
                    if (y < minY || y >= minY + levels) return;
                    int i = ((y - minY) * size + (x - minX)) * size + (z - minZ);
                    PvAquiferDecision decision = aquifer.decide(pos, finalDensity.compute(pos), false);
                    state[i] = (byte) decision.ordinal();
                    carved[i] = (byte) (decision == PvAquiferDecision.SOLID ? aquifer.decide(pos, 0.0, false) : decision).ordinal();
                });
            }
        }
        PvAquiferDecision[] decisions = PvAquiferDecision.values();
        int[][] sidesAndBelow = {{-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}, {0, -1, 0}};
        int surface = 0, bed = 0, spills = 0, leaks = 0;
        List<String> examples = new ArrayList<>();
        for (int y = minY; y < minY + levels; y++) {
            for (int x = 0; x < size; x++) {
                for (int z = 0; z < size; z++) {
                    int i = ((y - minY) * size + x) * size + z;
                    for (byte[] blocks : new byte[][]{state, carved}) {
                        PvAquiferDecision here = decisions[blocks[i]];
                        if (here != PvAquiferDecision.HIGH_RIVER_WATER && here != PvAquiferDecision.HIGH_RIVER_BED_WATER) continue;
                        if (blocks == state) {
                            if (here == PvAquiferDecision.HIGH_RIVER_WATER) surface++;
                            else bed++;
                        }
                        for (int[] offset : sidesAndBelow) {
                            int nx = x + offset[0], ny = y + offset[1], nz = z + offset[2];
                            if (nx < 0 || nx >= size || nz < 0 || nz >= size || ny < minY) continue;
                            if (decisions[blocks[((ny - minY) * size + nx) * size + nz]] != PvAquiferDecision.AIR_ABOVE_SEA) continue;
                            if (here == PvAquiferDecision.HIGH_RIVER_WATER && offset[1] == 0) {
                                if (blocks == state) spills++;
                                continue;
                            }
                            leaks++;
                            if (examples.size() < 5) {
                                examples.add((blocks == state ? "open " : "carved ") + here + " at " + (minX + x) + "," + y + "," + (minZ + z)
                                        + " -> " + (minX + nx) + "," + ny + "," + (minZ + nz));
                            }
                        }
                    }
                }
            }
        }
        System.out.printf(Locale.ROOT, "[terrain pass] 3 x 3 chunks around the high river's chunk %d,%d: water at the surface %d, in the bed %d;"
                + " open faces beside the surface's water (spills) %d; water beside or over open dry air %d%s%n", center.x, center.z, surface, bed,
                spills, leaks, examples.isEmpty() ? "" : ", e.g. " + examples);
        assertTrue(surface > 0 && bed > 0, "no river water around the chunk");
        assertEquals(0, leaks, () -> "the river's water next to open air: " + examples);
    }

    @Test
    void aquiferCost() {
        RandomState config = WorldgenTestData.noiseConfig(SEED);
        NoiseGeneratorSettings settings = WorldgenTestData.pvSettings();
        List<ChunkPos> chunks = new ArrayList<>();
        for (ChunkPos center : List.of(new ChunkPos(100, 100), coastalChunk(config))) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) chunks.add(new ChunkPos(center.x + dx, center.z + dz));
            }
        }
        String[] variants = {"own floodedness only", "and walls where water flows in", "and bands within 2 steps (the code)"};
        long[] nanos = new long[variants.length];
        long[][] counts = new long[variants.length][3];  // lattice points, blocks computed, open blocks asked about
        double[] sink = {0};
        for (int round = 0; round < 4; round++) {  // rounds 0 and 1 warm up the JIT
            for (int v = 0; v < variants.length; v++) {
                int variant = v;
                long start = System.nanoTime();
                for (ChunkPos chunk : chunks) {
                    TerrainPass pass = new TerrainPass(config, settings, chunk, NO_FLUID_LEVELS);
                    PvAquifer aquifer = assertInstanceOf(PvAquifer.class, pass.aquifer());
                    DensityFunction finalDensity = pass.register(config.router().finalDensity());
                    long[] asked = {0};
                    pass.run((x, y, z, pos) -> {
                        double density = finalDensity.compute(pos);
                        if (density > 0.0) return;
                        asked[0]++;
                        PvAquiferDecision decision = switch (variant) {
                            case 0 -> aquifer.atPosition(x, y, z);
                            case 1 -> {
                                PvAquiferDecision own = aquifer.atPosition(x, y, z);
                                yield PvAquiferRules.isWater(own) || y >= 64 ? own : inflowWall(aquifer, x, y, z);
                            }
                            default -> aquifer.decide(pos, density, false);
                        };
                        sink[0] += decision.ordinal();
                    });
                    if (round >= 2) {
                        for (int points : aquifer.latticeSamples()) counts[variant][0] += points;
                        counts[variant][1] += aquifer.computedPositions();
                        counts[variant][2] += asked[0];
                    }
                }
                if (round >= 2) nanos[variant] += System.nanoTime() - start;
            }
        }
        double runs = 2.0 * chunks.size();
        for (int v = 0; v < variants.length; v++) {
            System.out.printf(Locale.ROOT, "[terrain pass] aquifer cost, %s: %.2f ms per chunk (the pass with its reflection included);"
                            + " %.0f lattice points, %.0f blocks' own floodedness computed, %.0f open blocks asked about%n",
                    variants[v], nanos[v] / 1e6 / runs, counts[v][0] / runs, counts[v][1] / runs, counts[v][2] / runs);
        }
        assertTrue(sink[0] > 0);
    }

    private static PvAquiferDecision inflowWall(PvAquifer aquifer, int x, int y, int z) {
        int[][] inflow = {{-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}, {0, 1, 0}};
        for (int[] offset : inflow) {
            if (PvAquiferRules.isWater(aquifer.atPosition(x + offset[0], y + offset[1], z + offset[2]))) return PvAquiferDecision.SEA_BARRIER;
        }
        return PvAquiferDecision.AIR;
    }

    /** The first chunk found on a coast (vanilla's coast is continentalness -0.19..-0.11), in a fixed search. */
    private static ChunkPos coastalChunk(RandomState config) {
        DensityFunction continents = config.router().continents();
        for (int step = 0; step < 4000; step++) {
            ChunkPos chunk = new ChunkPos((step % 63) * 7 - 220, (step / 63) * 7 - 220);
            double continentalness = continents.compute(new DensityFunction.SinglePointContext(chunk.getMiddleBlockX(), 64, chunk.getMiddleBlockZ()));
            if (continentalness >= -0.19 && continentalness < -0.11) return chunk;
        }
        throw new AssertionError("no coastal chunk found");
    }

    private static DensityFunction interpolated(RandomState config, String function) {
        return WorldgenTestData.parse(config, "{\"type\": \"minecraft:interpolated\", \"argument\": \"" + function + "\"}");
    }

    private static int band(int y) {
        for (int band = 0; band < BANDS.length; band++) {
            if (y >= BANDS[band][0] && y <= BANDS[band][1]) return band;
        }
        return -1;
    }

    private static double percent(int part, int whole) {
        return 100.0 * part / whole;
    }

    /** Where two computations of one value differ. */
    private static final class Mismatches {
        private final String name;
        private final List<String> examples = new ArrayList<>();
        int compared, count;
        double maxDifference;

        Mismatches(String name) {
            this.name = name;
        }

        /** @return whether they differ */
        boolean compare(int x, int y, int z, double expected, double actual) {
            this.compared++;
            if (expected == actual || Double.isNaN(expected) && Double.isNaN(actual)) return false;
            this.count++;
            this.maxDifference = Math.max(this.maxDifference, Math.abs(expected - actual));
            if (this.examples.size() < 4) this.examples.add(x + "," + y + "," + z + ": " + expected + " vs " + actual);
            return true;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "%s differs at %d of %d blocks (by up to %.3g)%s", this.name, this.count, this.compared,
                    this.maxDifference, this.examples.isEmpty() ? "" : ", e.g. " + this.examples);
        }
    }
}
