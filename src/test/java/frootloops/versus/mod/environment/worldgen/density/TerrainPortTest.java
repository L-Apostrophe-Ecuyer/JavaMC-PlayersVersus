package frootloops.versus.mod.environment.worldgen.density;

import frootloops.versus.mod.environment.worldgen.TerrainPass;
import frootloops.versus.mod.environment.worldgen.WorldgenTestData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The terrain kernels ({@link PvFinalDensity}, {@link PvTerrain}, {@link PvDepth}, {@link PvEntrances},
 * {@link PvNoodle}) against the JSON they replaced ({@link WorldgenTestData#REFERENCE}): the same doubles at random
 * positions, and at every block of vanilla's terrain pass, where vanilla's caches and interpolation are in play. The
 * final density is also checked in the vanilla types it takes for C2ME's compiler ({@link PvFinalDensity#asVanillaTypes}).
 * In the flooded corridors' zone ({@link PvNoodle#corridorBias}), which the JSON never had, the final density is
 * checked against its vanilla types only; in the high river's heights, against the JSON's value cut by the river's
 * valley ({@link PvHighRiver}).
 */
class TerrainPortTest {

    private static final long SEED = 8675309L;
    private static final String NEW = "players-versus:overworld/";
    private static final String OLD = WorldgenTestData.REFERENCE + ":overworld/";
    private static final List<String> PORTED = List.of("final_density", "depth", "caves/entrances", "caves/noodle");
    private static final String AS_VANILLA_TYPES = "final_density as vanilla types";
    /** Every height where a band or gradient of the formulas starts or ends. */
    private static final int[] EDGES = {-64, -60, -52, -40, -32, -16, -10, -8, -4, 0, 8, 16, 18, 20, 28, 30, 32, 36, 38, 40, 44,
            48, 50, 54, 56, 66, 68, 72, 74, 90, 96, 120, 128, 240, 256};
    private static final Aquifer.FluidPicker NO_FLUID_LEVELS = (x, y, z) -> {
        throw new UnsupportedOperationException("not needed");
    };

    @Test
    void kernelsGiveTheJsonsValues() {
        RandomState config = WorldgenTestData.noiseConfig(SEED);
        Map<String, DensityFunction[]> pairs = new LinkedHashMap<>();
        for (String name : PORTED) {
            pairs.put(name, new DensityFunction[]{WorldgenTestData.seeded(config, NEW + name), WorldgenTestData.seeded(config, OLD + name)});
        }
        pairs.put(AS_VANILLA_TYPES, new DensityFunction[]{asVanillaTypes(config), WorldgenTestData.seeded(config, OLD + "final_density")});
        DensityFunction corridorEntrances = kernel(config).entrances();
        DensityFunction highRiver = kernel(config).highRiver();
        // what each sample exercised, so the comparison can't pass without reaching every branch
        DensityFunction ridges = WorldgenTestData.seeded(config, "minecraft:overworld/ridges");
        DensityFunction jaggedness = WorldgenTestData.seeded(config, "minecraft:overworld/jaggedness");
        DensityFunction slopedCheese = WorldgenTestData.seeded(config, OLD + "sloped_cheese");
        DensityFunction toggle = WorldgenTestData.seeded(config, NEW + "caves/noodle_toggle");
        Map<String, Integer> reached = new LinkedHashMap<>();
        List<Integer> edgeYs = edgeYs();

        Random random = new Random(SEED);
        for (int i = 0; i < 20000; i++) {
            int x = random.nextInt(12000) - 6000, z = random.nextInt(12000) - 6000;
            int y = i % 2 == 0 ? edgeYs.get(random.nextInt(edgeYs.size())) : random.nextInt(336) - 64;
            DensityFunction.FunctionContext pos = new DensityFunction.SinglePointContext(x, y, z);
            boolean corridor = inCorridorZone(y, corridorEntrances, pos);
            double valley = riverValley(y, highRiver, pos);
            for (Map.Entry<String, DensityFunction[]> pair : pairs.entrySet()) {
                boolean finalDensity = pair.getKey().equals("final_density") || pair.getKey().equals(AS_VANILLA_TYPES);
                double expected = corridor && finalDensity ? pairs.get("final_density")[0].compute(pos)
                        : finalDensity ? Math.min(pair.getValue()[1].compute(pos), valley) : pair.getValue()[1].compute(pos);
                double actual = pair.getValue()[0].compute(pos);
                assertEquals(expected, actual, 0.0, () -> pair.getKey() + " at " + x + "," + y + "," + z + (corridor ? " (in the corridors' zone)" : ""));
            }
            if (corridor) reached.merge("corridors' zone", 1, Integer::sum);
            if (valley < 0.0) reached.merge("high river's valley", 1, Integer::sum);
            double yValue = DensityOps.yValue(y), ridge = ridges.compute(pos), cheese = slopedCheese.compute(pos);
            if (yValue >= 48.0 && yValue < 256.0 && Math.abs(ridge) < 0.22) reached.merge("river valley", 1, Integer::sum);
            if (yValue >= 54.0 && yValue < 128.0 && Math.abs(ridge) < 0.2) reached.merge("river depth", 1, Integer::sum);
            if (jaggedness.compute(pos) != 0.0) reached.merge("jagged peaks", 1, Integer::sum);
            reached.merge(cheese < 1.5625 ? "surface branch" : "cave branch", 1, Integer::sum);
            if (yValue >= 0.0 && yValue < 44.0) reached.merge("ramen band", 1, Integer::sum);
            if (!(toggle.compute(pos) < -0.2)) reached.merge("noodles on", 1, Integer::sum);
        }
        System.out.println("[terrain port] " + pairs.keySet() + " equal the JSON at 20000 points; reached " + reached);
        for (String branch : List.of("river valley", "river depth", "jagged peaks", "surface branch", "cave branch", "ramen band", "noodles on",
                "corridors' zone")) {
            assertTrue(reached.getOrDefault(branch, 0) >= 50, "too few samples reached " + branch + ": " + reached);
        }
    }

    /**
     * Vanilla's terrain pass with the old JSON and the kernels side by side, chunk-mapped (flat and 2D caches, cache_once,
     * interpolation): the same final density, entrances and noodle at every block, and the same depth at every
     * biome-grid column, which is where anything reads it. (Between those columns the kernel's river term comes from
     * vanilla's per-column ridge cache, which holds the value at the column's corner.)
     */
    @Test
    void kernelsGiveTheJsonsValuesInTheTerrainPass() {
        RandomState config = WorldgenTestData.noiseConfig(SEED);
        NoiseGeneratorSettings settings = WorldgenTestData.pvSettings();
        long totalValleyBlocks = 0;
        for (ChunkPos chunk : testChunks(config)) {
            TerrainPass pass = new TerrainPass(config, settings, chunk, NO_FLUID_LEVELS);
            int[] valleyBlocks = {0};
            List<String> names = new ArrayList<>();
            List<DensityFunction> news = new ArrayList<>(), olds = new ArrayList<>();
            for (String name : PORTED) {
                names.add(name);
                news.add(pass.register(WorldgenTestData.seeded(config, NEW + name)));
                olds.add(pass.register(WorldgenTestData.seeded(config, OLD + name)));
            }
            names.add(AS_VANILLA_TYPES);
            news.add(pass.register(asVanillaTypes(config)));
            olds.add(pass.register(WorldgenTestData.seeded(config, OLD + "final_density")));
            DensityFunction corridorEntrances = pass.register(kernel(config).entrances());
            DensityFunction highRiver = pass.register(kernel(config).highRiver());
            int finalDensity = names.indexOf("final_density");
            int[] compared = new int[names.size()];
            int[] corridors = {0};
            pass.run((x, y, z, pos) -> {
                boolean corridor = inCorridorZone(y, corridorEntrances, pos);
                if (corridor) corridors[0]++;
                double valley = riverValley(y, highRiver, pos);
                if (valley < 0.0) valleyBlocks[0]++;
                for (int i = 0; i < names.size(); i++) {
                    if (names.get(i).equals("depth") && (Math.floorMod(x, 4) != 0 || Math.floorMod(z, 4) != 0)) continue;
                    boolean isFinalDensity = i == finalDensity || names.get(i).equals(AS_VANILLA_TYPES);
                    boolean againstKernel = corridor && isFinalDensity;
                    double expected = againstKernel ? news.get(finalDensity).compute(pos)
                            : isFinalDensity ? Math.min(olds.get(i).compute(pos), valley) : olds.get(i).compute(pos);
                    double actual = news.get(i).compute(pos);
                    int index = i;
                    assertEquals(expected, actual, 0.0, () -> names.get(index) + " at " + x + "," + y + "," + z + " (chunk " + chunk.x + "," + chunk.z
                            + (againstKernel ? ", in the corridors' zone" : "") + ")");
                    compared[i]++;
                }
            });
            System.out.printf(Locale.ROOT, "[terrain port] chunk %d,%d: %s equal the JSON at %s blocks (the final density: the kernel's own"
                            + " vanilla types at the %d blocks of the corridors' zone, the JSON's cut by the high river at %d blocks it opens)%n",
                    chunk.x, chunk.z, names, Arrays.toString(compared), corridors[0], valleyBlocks[0]);
            totalValleyBlocks += valleyBlocks[0];
        }
        assertTrue(totalValleyBlocks > 0, "the chunks tried never reached the high river's valley");
    }

    /**
     * What the final density costs in the terrain pass: the old JSON, the kernel, and the kernel in vanilla types, all
     * run by vanilla's own code (no C2ME here). Each gets a pass of its own over the same chunks; the sampler's own
     * router is the same in all, so the differences are the function's.
     */
    @Test
    void finalDensityCost() {
        RandomState config = WorldgenTestData.noiseConfig(SEED);
        NoiseGeneratorSettings settings = WorldgenTestData.pvSettings();
        List<ChunkPos> chunks = testChunks(config);
        Map<String, Supplier<DensityFunction>> variants = new LinkedHashMap<>();
        variants.put("the old final density", () -> WorldgenTestData.seeded(config, OLD + "final_density"));
        variants.put("the kernel", () -> WorldgenTestData.seeded(config, NEW + "final_density"));
        variants.put("the kernel in vanilla types", () -> asVanillaTypes(config));
        double[] sink = {0};
        Map<String, Long> nanos = new LinkedHashMap<>();
        for (int round = 0; round < 4; round++) {  // rounds 0 and 1 warm up the JIT
            for (Map.Entry<String, Supplier<DensityFunction>> variant : variants.entrySet()) {
                long start = System.nanoTime();
                for (ChunkPos chunk : chunks) {
                    TerrainPass pass = new TerrainPass(config, settings, chunk, NO_FLUID_LEVELS);
                    DensityFunction finalDensity = pass.register(variant.getValue().get());
                    pass.run((x, y, z, pos) -> sink[0] += finalDensity.compute(pos));
                }
                if (round >= 2) nanos.merge(variant.getKey(), System.nanoTime() - start, Long::sum);
            }
        }
        System.out.println("[terrain port] terrain pass per chunk (with the sampler's own router and the pass's reflection): "
                + nanos.entrySet().stream().map(entry -> String.format(Locale.ROOT, "with %s %.2f ms", entry.getKey(),
                        entry.getValue() / 2e6 / chunks.size())).collect(Collectors.joining(", ")));
        assertTrue(Double.isFinite(sink[0]));
    }

    private static PvFinalDensity kernel(RandomState config) {
        return assertInstanceOf(PvFinalDensity.class, WorldgenTestData.seeded(config, NEW + "final_density"));
    }

    /** The high river's valley at a block, which the JSON never had: infinite outside its heights, as the final density asks. */
    private static double riverValley(int y, DensityFunction highRiver, DensityFunction.FunctionContext pos) {
        return y >= PvHighRiver.MIN_Y && y < PvHighRiver.MAX_Y ? highRiver.compute(pos) : Double.POSITIVE_INFINITY;
    }

    /** Whether the flooded corridors change the noodle's bias at a block, which the JSON never did. */
    private static boolean inCorridorZone(int y, DensityFunction corridorEntrances, DensityFunction.FunctionContext pos) {
        return PvNoodle.inCorridorLayers(y) && PvNoodle.corridorBias(y, corridorEntrances.compute(pos)) != PvNoodle.bias(y);
    }

    /** The final density in the vanilla types it takes when C2ME's compiler is active. */
    private static DensityFunction asVanillaTypes(RandomState config) {
        return assertInstanceOf(PvFinalDensity.class, WorldgenTestData.seeded(config, NEW + "final_density")).asVanillaTypes();
    }

    /** The smoke test's chunk, plus the first chunks found with a river valley, with jagged peaks and with the high river. */
    private static List<ChunkPos> testChunks(RandomState config) {
        DensityFunction ridges = WorldgenTestData.seeded(config, "minecraft:overworld/ridges");
        DensityFunction jaggedness = WorldgenTestData.seeded(config, "minecraft:overworld/jaggedness");
        // inland (vanilla's coast starts at continentalness -0.11), so the valley is cut into land
        DensityFunction continents = WorldgenTestData.seeded(config, "minecraft:overworld/continents");
        List<ChunkPos> chunks = new ArrayList<>(List.of(new ChunkPos(100, 100)));
        ChunkPos river = null, peaks = null;
        for (int step = 0; step < 4000 && (river == null || peaks == null); step++) {
            int chunkX = (step % 63) * 7 - 220, chunkZ = (step / 63) * 7 - 220;
            DensityFunction.FunctionContext center = new DensityFunction.SinglePointContext(chunkX * 16 + 8, 64, chunkZ * 16 + 8);
            if (river == null && Math.abs(ridges.compute(center)) < 0.05 && continents.compute(center) > -0.11) river = new ChunkPos(chunkX, chunkZ);
            if (peaks == null && jaggedness.compute(center) > 0.2) peaks = new ChunkPos(chunkX, chunkZ);
        }
        assertTrue(river != null && peaks != null, "no river or peak chunk found: " + river + ", " + peaks);
        chunks.add(river);
        chunks.add(peaks);
        chunks.add(WorldgenTestData.highRiverChunk(config));
        return chunks;
    }

    private static List<Integer> edgeYs() {
        TreeSet<Integer> ys = new TreeSet<>();
        for (int edge : EDGES) {
            for (int y = edge - 1; y <= edge + 1; y++) {
                if (y >= -64 && y < 272) ys.add(y);
            }
        }
        return new ArrayList<>(ys);
    }
}
