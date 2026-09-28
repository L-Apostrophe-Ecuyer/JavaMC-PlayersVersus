package frootloops.versus.mod.environment.worldgen.density;

import frootloops.versus.mod.environment.worldgen.TerrainPass;
import frootloops.versus.mod.environment.worldgen.WorldgenTestData;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The terrain kernels ({@link PvFinalDensity}, {@link PvTerrain}, {@link PvDepth}, {@link PvEntrances},
 * {@link PvNoodle}) against the JSON they replaced ({@link WorldgenTestData#REFERENCE}): the same doubles at random
 * positions, and at every block of vanilla's terrain pass, where vanilla's caches and interpolation are in play.
 */
class TerrainPortTest {

    private static final long SEED = 8675309L;
    private static final String NEW = "players-versus:overworld/";
    private static final String OLD = WorldgenTestData.REFERENCE + ":overworld/";
    private static final List<String> PORTED = List.of("final_density", "depth", "caves/entrances", "caves/noodle");
    /** Every height where a band or gradient of the formulas starts or ends. */
    private static final int[] EDGES = {-64, -60, -52, -40, -32, -16, -10, -8, -4, 0, 8, 16, 18, 20, 28, 30, 32, 36, 40, 44,
            48, 50, 54, 56, 66, 68, 72, 74, 90, 96, 120, 128, 240, 256};
    private static final AquiferSampler.FluidLevelSampler NO_FLUID_LEVELS = (x, y, z) -> {
        throw new UnsupportedOperationException("not needed");
    };

    @Test
    void kernelsGiveTheJsonsValues() {
        NoiseConfig config = WorldgenTestData.noiseConfig(SEED);
        Map<String, DensityFunction[]> pairs = new LinkedHashMap<>();
        for (String name : PORTED) {
            pairs.put(name, new DensityFunction[]{WorldgenTestData.seeded(config, NEW + name), WorldgenTestData.seeded(config, OLD + name)});
        }
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
            DensityFunction.NoisePos pos = new DensityFunction.UnblendedNoisePos(x, y, z);
            for (Map.Entry<String, DensityFunction[]> pair : pairs.entrySet()) {
                double expected = pair.getValue()[1].sample(pos), actual = pair.getValue()[0].sample(pos);
                assertEquals(expected, actual, 0.0, () -> pair.getKey() + " at " + x + "," + y + "," + z);
            }
            double yValue = DensityOps.yValue(y), ridge = ridges.sample(pos), cheese = slopedCheese.sample(pos);
            if (yValue >= 48.0 && yValue < 256.0 && Math.abs(ridge) < 0.22) reached.merge("river valley", 1, Integer::sum);
            if (yValue >= 54.0 && yValue < 128.0 && Math.abs(ridge) < 0.2) reached.merge("river depth", 1, Integer::sum);
            if (jaggedness.sample(pos) != 0.0) reached.merge("jagged peaks", 1, Integer::sum);
            reached.merge(cheese < 1.5625 ? "surface branch" : "cave branch", 1, Integer::sum);
            if (yValue >= 0.0 && yValue < 44.0) reached.merge("ramen band", 1, Integer::sum);
            if (!(toggle.sample(pos) < -0.2)) reached.merge("noodles on", 1, Integer::sum);
        }
        System.out.println("[terrain port] " + PORTED + " equal the JSON at 20000 points; reached " + reached);
        for (String branch : List.of("river valley", "river depth", "jagged peaks", "surface branch", "cave branch", "ramen band", "noodles on")) {
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
        NoiseConfig config = WorldgenTestData.noiseConfig(SEED);
        ChunkGeneratorSettings settings = WorldgenTestData.pvSettings();
        for (ChunkPos chunk : testChunks(config)) {
            TerrainPass pass = new TerrainPass(config, settings, chunk, NO_FLUID_LEVELS);
            List<String> names = new ArrayList<>();
            List<DensityFunction> news = new ArrayList<>(), olds = new ArrayList<>();
            for (String name : PORTED) {
                names.add(name);
                news.add(pass.register(WorldgenTestData.seeded(config, NEW + name)));
                olds.add(pass.register(WorldgenTestData.seeded(config, OLD + name)));
            }
            int[] compared = new int[names.size()];
            pass.run((x, y, z, pos) -> {
                for (int i = 0; i < names.size(); i++) {
                    if (names.get(i).equals("depth") && (Math.floorMod(x, 4) != 0 || Math.floorMod(z, 4) != 0)) continue;
                    double expected = olds.get(i).sample(pos), actual = news.get(i).sample(pos);
                    int index = i;
                    assertEquals(expected, actual, 0.0, () -> names.get(index) + " at " + x + "," + y + "," + z + " (chunk " + chunk.x + "," + chunk.z + ")");
                    compared[i]++;
                }
            });
            System.out.printf(Locale.ROOT, "[terrain port] chunk %d,%d: %s equal the JSON at %s blocks%n", chunk.x, chunk.z, names,
                    Arrays.toString(compared));
        }
    }

    /**
     * What the final density costs in the terrain pass, the old JSON against the kernel: each gets a pass of its own
     * over the same chunks (the sampler's own router is the same in both, so the difference is the function's).
     */
    @Test
    void finalDensityCost() {
        NoiseConfig config = WorldgenTestData.noiseConfig(SEED);
        ChunkGeneratorSettings settings = WorldgenTestData.pvSettings();
        List<ChunkPos> chunks = testChunks(config);
        double[] sink = {0};
        long[] nanos = new long[2];
        for (int round = 0; round < 4; round++) {  // rounds 0 and 1 warm up the JIT
            for (int which = 0; which < 2; which++) {
                String id = (which == 0 ? OLD : NEW) + "final_density";
                long start = System.nanoTime();
                for (ChunkPos chunk : chunks) {
                    TerrainPass pass = new TerrainPass(config, settings, chunk, NO_FLUID_LEVELS);
                    DensityFunction finalDensity = pass.register(WorldgenTestData.seeded(config, id));
                    pass.run((x, y, z, pos) -> sink[0] += finalDensity.sample(pos));
                }
                if (round >= 2) nanos[which] += System.nanoTime() - start;
            }
        }
        System.out.printf(Locale.ROOT, "[terrain port] terrain pass with the old final density %.2f ms per chunk, with the kernel %.2f"
                        + " (both include the sampler's own router and the pass's reflection)%n",
                nanos[0] / 2e6 / chunks.size(), nanos[1] / 2e6 / chunks.size());
        assertTrue(Double.isFinite(sink[0]));
    }

    /** The smoke test's chunk, plus the first chunks found with a river valley and with jagged peaks. */
    private static List<ChunkPos> testChunks(NoiseConfig config) {
        DensityFunction ridges = WorldgenTestData.seeded(config, "minecraft:overworld/ridges");
        DensityFunction jaggedness = WorldgenTestData.seeded(config, "minecraft:overworld/jaggedness");
        DensityFunction offset = WorldgenTestData.seeded(config, "minecraft:overworld/offset");
        List<ChunkPos> chunks = new ArrayList<>(List.of(new ChunkPos(100, 100)));
        ChunkPos river = null, peaks = null;
        for (int step = 0; step < 4000 && (river == null || peaks == null); step++) {
            int chunkX = (step % 63) * 7 - 220, chunkZ = (step / 63) * 7 - 220;
            DensityFunction.NoisePos center = new DensityFunction.UnblendedNoisePos(chunkX * 16 + 8, 64, chunkZ * 16 + 8);
            if (river == null && Math.abs(ridges.sample(center)) < 0.05 && offset.sample(center) > 0.0) river = new ChunkPos(chunkX, chunkZ);
            if (peaks == null && jaggedness.sample(center) > 0.2) peaks = new ChunkPos(chunkX, chunkZ);
        }
        assertTrue(river != null && peaks != null, "no river or peak chunk found: " + river + ", " + peaks);
        chunks.add(river);
        chunks.add(peaks);
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
