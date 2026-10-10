package frootloops.versus.mod.environment.worldgen.debug;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.PvWorldgen;
import frootloops.versus.mod.environment.worldgen.PvWorldgenConstants;
import frootloops.versus.mod.environment.worldgen.density.AquiferSpread;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Worldgen benchmark and smoke test: generates a square of chunks one status at a time, times each status, measures a
 * few invariants, and writes map images, so two builds or two world types can be compared.
 *
 * <p>Run it with {@code /pvwg bench <radius>} in a fresh area, or headless with {@code ./gradlew runWorldgenSmoke}
 * (system property {@code pv.worldgen.bench=<radius>}, optional {@code pv.worldgen.bench.center=<chunkX>,<chunkZ>}),
 * which stops the server when done. Output goes to {@code <game dir>/pvwg/bench-<time>-<settings>/}:
 * <ul>
 *   <li>{@code report.txt}: timings per status and the metrics below;</li>
 *   <li>{@code surface.png}: top block colored by type and shaded by height, water shaded by depth;</li>
 *   <li>{@code biomes-surface.png}, {@code biomes-y<Y>.png}: biome layers, with {@code biomes-legend.txt};</li>
 *   <li>{@code slice-y<Y>.png}: horizontal cuts (air black, water blue, lava orange, stone light, deepslate dark).</li>
 * </ul>
 *
 * <p>Metrics, measured right after {@code TERRAIN} (26.3's one status for the terrain, the surface and the carvers):
 * <ul>
 *   <li>water at or above y 64: the Players Versus aquifer never places any there but the high river's, at y 77..80 and
 *   94..96;</li>
 *   <li>basin seam ratio: water/non-water changes across chunk borders divided by the same count across chunk
 *   middles, for y 0..31. About 1 means no seams;</li>
 *   <li>water in y 0..31 by x (and z) offset inside the chunk, relative to the mean; z is the control;</li>
 *   <li>fluid ticks queued: fluid blocks the aquifer marked for a fluid update, per chunk, overall and for y 0..31 next to
 *   the water blocks there. Each one runs when its chunk becomes a full chunk;</li>
 *   <li>water beside or above air in y -31..63 (neighbours inside the chunk): where water spills, or stands as a wall of
 *   water until something updates it. Split by height, and by whether the water has a fluid tick queued;</li>
 *   <li>water by height, per chunk: below y -31, where the aquifer places none; y -31..-9, which should stay dry
 *   (Section 10, question 7 of the refactor plan); y -8..-1, 0..23, 24..47 and 48..63;</li>
 *   <li>the high river's water and where it spills, for each of its layers; its water over a hollow (air a few blocks
 *   under it) and its bed's walls one block thick, which is how it looks where it runs over open ground;</li>
 *   <li>the air of the dry caves' heights (y -16..32), and the bodies of air that connect their top to their bottom: the
 *   ways down they're meant to give.</li>
 * </ul>
 * The report also holds text maps (one character per 8x8 blocks, north up) of the surface and its biomes, so results
 * can be compared without the images; cross sections of the high river, where the region has it; hashes of every block
 * after {@code TERRAIN}, by 16-block layer and by chunk, so two runs that should agree can be checked block for block and
 * their differences located; and the structure starts in the region.
 */
public final class WorldgenBench {

    public static final int MAX_RADIUS = 32;

    private static final List<ChunkStatus> STAGES = List.of(
            ChunkStatus.BIOMES, ChunkStatus.TERRAIN, ChunkStatus.FEATURES, ChunkStatus.FULL);
    private static final int[] SLICE_YS = {-40, -20, 0, 16, 28, 40, 56, 62};
    private static final int[] BIOME_LAYER_YS = {-40, 0, 32};
    private static final int BASIN_SEAM_MIN_Y = 0;
    private static final int BASIN_SEAM_MAX_Y = 32;
    private static final int WATER_CEILING_Y = 64;
    /** The sea band, y -31..63 (its water only from y -8 up, since revision 6 of the refactor plan). */
    private static final int LEAK_MIN_Y = -31;
    private static final int LEAK_MAX_Y = 64;
    /** Heights the leak metric is split by: y -31..-1, 0..23, 24..47, 48..63. */
    private static final int[] LEAK_BAND_TOPS = {0, 24, 48, 64};
    /** Heights the water count is split by: y -31..-9, -8..-1, 0..23, 24..47, 48..63 (and all water below y -31). */
    private static final int[] WATER_BAND_TOPS = {-8, 0, 24, 48, 64};
    /** Where water flows from a block: the four sides, then below. */
    private static final int[][] SIDES_AND_BELOW = {{-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}, {0, -1, 0}};
    /**
     * The high river's layers, each its water surface and its bed's bottom, with its metric's name; above sea level, only
     * their water is there before features run.
     */
    private static final int[][] HIGH_RIVER_LAYERS = {
            {PvWorldgenConstants.HIGH_RIVER_Y, PvWorldgenConstants.HIGH_RIVER_MIN_Y},
            {PvWorldgenConstants.HIGH_RIVER_UPPER_Y, PvWorldgenConstants.HIGH_RIVER_UPPER_MIN_Y}};
    private static final String[] HIGH_RIVER_METRICS = {"high_river_per_chunk", "high_river_upper_per_chunk"};
    /** River water this close above air sits over a hollow: on a floor too thin to be the ground. */
    private static final int RIVER_HOLLOW_REACH = 6;
    /** The high river's cross sections in the report: their heights, how far they reach either side of the water, how many. */
    private static final int SECTION_MIN_Y = 60, SECTION_MAX_Y = 112, SECTION_REACH = 40, SECTIONS_PER_AXIS = 3, SECTION_SPACING = 64;
    /** Regions wider than this get no cross sections: their blocks would take too much memory. */
    private static final int SECTION_MAX_SIZE = 33 * 16;
    /** The dry caves' heights (the refactor plan, Section 6.2, 2d revised): their air is counted by y -16..-1, 0..15 and 16..32. */
    private static final int DESCENT_BOTTOM_Y = PvWorldgenConstants.DRY_NOODLE_MIN_Y, DESCENT_TOP_Y = PvWorldgenConstants.DRY_NOODLE_MAX_Y;
    private static final int DESCENT_LAYERS = DESCENT_TOP_Y - DESCENT_BOTTOM_Y + 1;
    private static final int[] DESCENT_BAND_TOPS = {0, 16, DESCENT_TOP_Y + 1};
    /** The caves' cross sections in the report: their heights, and how far they reach either side of their column. */
    private static final int CAVE_SECTION_MIN_Y = DESCENT_BOTTOM_Y - 4, CAVE_SECTION_MAX_Y = DESCENT_TOP_Y + 4, CAVE_SECTION_REACH = 60;
    private static final int CAVE_SECTION_LAYERS = CAVE_SECTION_MAX_Y - CAVE_SECTION_MIN_Y + 1;
    /** The basins' level is sampled every this many blocks over the region. */
    private static final int BASIN_LEVEL_STEP = 4;
    private static final int MAP_CELL = 8;

    private WorldgenBench() {
    }

    /** Headless mode for {@code runWorldgenSmoke}: run once when the server has started, then stop it. */
    public static void registerHeadlessRun() {
        String radiusProperty = System.getProperty("pv.worldgen.bench");
        if (radiusProperty == null || radiusProperty.isBlank()) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                int radius = Math.clamp(Integer.parseInt(radiusProperty.trim()), 1, MAX_RADIUS);
                ChunkPos center = parseCenter(System.getProperty("pv.worldgen.bench.center", "100,100"));
                Path out = run(server.overworld(), center, radius, VersusMod.MOD_LOGGER::info);
                VersusMod.MOD_LOGGER.info("[pvwg] bench written to {}", out.toAbsolutePath());
            } catch (Throwable throwable) {
                VersusMod.MOD_LOGGER.error("[pvwg] bench failed", throwable);
            } finally {
                server.halt(false);
            }
        });
    }

    static int runCommand(CommandSourceStack source, int radius) {
        ChunkPos center = ChunkPos.containing(BlockPos.containing(source.getPosition()));
        try {
            Path out = run(source.getLevel(), center, radius, message -> source.sendSuccess(() -> Component.literal(message), false));
            source.sendSuccess(() -> Component.literal("[pvwg] bench written to " + out.toAbsolutePath()), false);
            return 1;
        } catch (IOException exception) {
            source.sendFailure(Component.literal("[pvwg] bench failed: " + exception.getMessage()));
            return 0;
        }
    }

    public static Path run(ServerLevel world, ChunkPos center, int radius, Consumer<String> log) throws IOException {
        ServerChunkCache chunkManager = world.getChunkSource();
        String settings = describeGenerator(chunkManager.getGenerator());
        String generator = encodeGenerator(world, chunkManager.getGenerator());
        String gate = chunkManager.getGenerator() instanceof NoiseBasedChunkGenerator noiseGenerator
                ? "players_versus_aquifer_and_ore_veins=" + PvWorldgen.isPvGenerator(noiseGenerator.generatorSettings().value())
                : "not a noise generator";
        int minChunkX = center.x() - radius;
        int minChunkZ = center.z() - radius;
        int chunksPerSide = radius * 2 + 1;
        int chunkCount = chunksPerSide * chunksPerSide;
        log.accept(String.format(Locale.ROOT, "[pvwg] bench: %d chunks around chunk %d,%d, settings %s",
                chunkCount, center.x(), center.z(), settings));
        log.accept("[pvwg] generator " + generator);

        Region region = new Region(world, minChunkX * 16, minChunkZ * 16, chunksPerSide * 16);
        Map<String, Long> stageNanos = new LinkedHashMap<>();
        for (ChunkStatus status : STAGES) {
            long start = System.nanoTime();
            for (int chunkX = minChunkX; chunkX < minChunkX + chunksPerSide; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ < minChunkZ + chunksPerSide; chunkZ++) {
                    chunkManager.getChunk(chunkX, chunkZ, status, true);
                }
            }
            long elapsed = System.nanoTime() - start;
            stageNanos.put(status.getName(), elapsed);
            log.accept(String.format(Locale.ROOT, "[pvwg] %-9s %9.1f ms  %7.2f ms/chunk",
                    status.getName(), elapsed / 1e6, elapsed / 1e6 / chunkCount));

            if (status == ChunkStatus.TERRAIN) {
                for (int chunkX = minChunkX; chunkX < minChunkX + chunksPerSide; chunkX++) {
                    for (int chunkZ = minChunkZ; chunkZ < minChunkZ + chunksPerSide; chunkZ++) {
                        ChunkAccess chunk = chunkManager.getChunk(chunkX, chunkZ, ChunkStatus.TERRAIN, true);
                        region.capture(chunk);
                        region.hashBlocks("terrain", chunk);
                    }
                }
            }
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT));
        Path dir = FabricLoader.getInstance().getGameDir().resolve("pvwg")
                .resolve("bench-" + timestamp + "-" + settings.replaceAll("[^A-Za-z0-9_.-]", "_"));
        Files.createDirectories(dir);
        region.writeImages(dir);
        region.basinLevelMetric = basinLevels(world, region);
        List<String> report = region.report(settings, generator, gate, world.getSeed(), center, radius, stageNanos, chunkCount);
        Files.write(dir.resolve("report.txt"), report);
        for (String line : report) {
            if (line.startsWith("metric")) log.accept("[pvwg] " + line);
        }
        return dir;
    }

    /** The generator, encoded the way level.dat stores it. */
    static String encodeGenerator(ServerLevel world, ChunkGenerator generator) {
        return ChunkGenerator.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, world.registryAccess()), generator)
                .result().map(JsonElement::toString).orElse("unencodable " + generator.getClass().getName());
    }

    static String describeGenerator(ChunkGenerator generator) {
        if (generator instanceof NoiseBasedChunkGenerator noiseGenerator) {
            return noiseGenerator.generatorSettings().unwrapKey().map(key -> key.identifier().toString()).orElse("inline-settings");
        }
        return generator.getClass().getSimpleName();
    }

    /**
     * The basins' level over the region (the refactor plan, Section 6.2, 2d revised again), every
     * {@link #BASIN_LEVEL_STEP} blocks: the share of columns on each of its steps (no basins, then lakes up to each of
     * the step levels) and between them; {@code null} without a Players Versus aquifer.
     */
    private static String basinLevels(ServerLevel world, Region region) {
        if (!(world.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator noiseGenerator)) return null;
        NoiseGeneratorSettings settings = noiseGenerator.generatorSettings().value();
        if (!PvWorldgen.isPvGenerator(settings) || settings.aquifers().isEmpty()) return null;
        if (!(PvWorldgen.unwrap(settings.aquifers().get().fluidLevelSpreadNoise()) instanceof AquiferSpread spread)) return null;
        RandomState noise = world.getChunkSource().randomState();
        int[] steps = {PvWorldgenConstants.BASIN_LEVEL_DRY, PvWorldgenConstants.BASIN_LEVEL_LOW, PvWorldgenConstants.BASIN_LEVEL_MID,
                PvWorldgenConstants.BASIN_LEVEL_FULL};
        long[] onStep = new long[steps.length];
        long all = 0;
        for (int dx = 0; dx < region.size; dx += BASIN_LEVEL_STEP) {
            for (int dz = 0; dz < region.size; dz += BASIN_LEVEL_STEP) {
                double level = noise.sampleBlockValueUncached(spread.level(), region.minX + dx, 0, region.minZ + dz);
                all++;
                for (int i = 0; i < steps.length; i++) {
                    if (Math.abs(level - steps[i]) < 0.5) onStep[i]++;
                }
            }
        }
        long between = all;
        for (long count : onStep) between -= count;
        return String.format(Locale.ROOT, "metric basin_level_columns no basins %.1f%%, lakes up to y %d %.1f%%, y %d %.1f%%, y %d %.1f%%;"
                        + " between steps %.1f%%", 100.0 * onStep[0] / all, steps[1], 100.0 * onStep[1] / all, steps[2],
                100.0 * onStep[2] / all, steps[3], 100.0 * onStep[3] / all, 100.0 * between / all);
    }

    private static ChunkPos parseCenter(String value) {
        String[] parts = value.split(",");
        return new ChunkPos(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
    }

    /** Column data captured after the carvers ran, for images and metrics. */
    private static final class Region {
        private static final byte AIR = 0, WATER = 1, LAVA = 2, STONE = 3, DEEPSLATE = 4, OTHER = 5;

        private final int minX, minZ, size, bottomY, topY, seaLevel;
        private final int[] surfaceY, floorY, surfaceColor;
        private final char[] surfaceClass;
        private final int[] surfaceBiome;
        private final int[][] layerBiome = new int[BIOME_LAYER_YS.length][];
        private final byte[][] slices = new byte[SLICE_YS.length][];
        private final BitSet basinWater;
        private final List<String> biomeIds = new ArrayList<>();
        private final Map<String, Integer> biomeIndex = new HashMap<>();
        private long waterAtOrAboveCeiling;
        private long fluidTicksQueued, fluidTicksQueuedInBasinLayers;
        private int protoChunks;
        /** Water blocks beside or above air, and those among them with a fluid tick queued. */
        private long leakingWater, leakingWaterTicking;
        private final long[] leakingWaterByBand = new long[LEAK_BAND_TOPS.length];
        /** Water blocks by {@code WATER_BAND_TOPS}, and below them. */
        private final long[] waterByBand = new long[WATER_BAND_TOPS.length];
        private long waterBelowSeaBand;
        /**
         * The high river's water by layer ({@link #HIGH_RIVER_LAYERS}, the only water above sea level before features): at
         * its surface and in its bed; open faces beside the surface's water, where it spills, and the columns they're in;
         * surface water with open air under it; bed water with open air beside or under it, which the walls should leave
         * none of.
         */
        private final long[] riverSurfaceWater = new long[HIGH_RIVER_LAYERS.length], riverBedWater = new long[HIGH_RIVER_LAYERS.length],
                riverSpillFaces = new long[HIGH_RIVER_LAYERS.length], riverSpillColumns = new long[HIGH_RIVER_LAYERS.length],
                riverSurfaceOverAir = new long[HIGH_RIVER_LAYERS.length], riverBedBesideAir = new long[HIGH_RIVER_LAYERS.length];
        /**
         * Columns of the high river's water within {@link #RIVER_HOLLOW_REACH} blocks above air, and its bed water's
         * walls one block thick with air behind them, by layer: where it runs over open ground instead of in it.
         */
        private final long[] riverOverHollow = new long[HIGH_RIVER_LAYERS.length], riverThinWalls = new long[HIGH_RIVER_LAYERS.length];
        /** Blocks of y SECTION_MIN_Y..SECTION_MAX_Y ({@link #AIR}, {@link #WATER}, or {@link #STONE} for any other), by {@link #sectionIndex}. */
        private final byte[] sectionBlocks;
        /** Blocks of y CAVE_SECTION_MIN_Y..CAVE_SECTION_MAX_Y, like {@link #sectionBlocks}, by {@link #caveIndex}. */
        private final byte[] caveBlocks;
        /** The column with the most water in the basin layers, and the top column of the biggest dry way down; -1 if none. */
        private int lakeColumn = -1, lakeColumnWater, largestDescentColumn = -1;
        private String basinLevelMetric;
        /** Air in the dry caves' heights, by {@link #descentIndex}, and by {@link #DESCENT_BAND_TOPS} and by y. */
        private final BitSet descentAir;
        private final long[] descentAirByBand = new long[DESCENT_BAND_TOPS.length], descentAirByY = new long[DESCENT_LAYERS];
        /** Block hashes by status: [chunk, by {@link #chunkIndex}][16-block section from the bottom]. */
        private final Map<String, long[][]> sectionHashes = new LinkedHashMap<>();
        private final Registry<Structure> structures;
        private final List<String> structureStarts = new ArrayList<>();

        Region(ServerLevel world, int minX, int minZ, int size) {
            this.minX = minX;
            this.minZ = minZ;
            this.size = size;
            this.bottomY = world.getMinY();
            this.topY = world.getMinY() + world.getHeight() - 1;
            this.seaLevel = world.getSeaLevel();
            int columns = size * size;
            this.surfaceY = new int[columns];
            this.floorY = new int[columns];
            this.surfaceColor = new int[columns];
            this.surfaceClass = new char[columns];
            this.surfaceBiome = new int[columns];
            for (int i = 0; i < BIOME_LAYER_YS.length; i++) this.layerBiome[i] = new int[columns];
            for (int i = 0; i < SLICE_YS.length; i++) this.slices[i] = new byte[columns];
            this.basinWater = new BitSet(columns * (BASIN_SEAM_MAX_Y - BASIN_SEAM_MIN_Y));
            this.sectionBlocks = size <= SECTION_MAX_SIZE ? new byte[columns * (SECTION_MAX_Y - SECTION_MIN_Y + 1)] : null;
            this.descentAir = new BitSet(columns * DESCENT_LAYERS);
            this.caveBlocks = size <= SECTION_MAX_SIZE ? new byte[columns * CAVE_SECTION_LAYERS] : null;
            this.structures = world.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        }

        private int chunksPerSide() {
            return this.size / 16;
        }

        private int chunkIndex(ChunkPos pos) {
            return (pos.x() - (this.minX >> 4)) + (pos.z() - (this.minZ >> 4)) * chunksPerSide();
        }

        /** FNV-1a over the raw ids of every block of each 16-block section, for {@link #appendHashes}. */
        void hashBlocks(String status, ChunkAccess chunk) {
            long[][] byChunk = this.sectionHashes.computeIfAbsent(status, key -> new long[chunksPerSide() * chunksPerSide()][]);
            LevelChunkSection[] sections = chunk.getSections();
            long[] hashes = new long[sections.length];
            for (int i = 0; i < sections.length; i++) {
                LevelChunkSection section = sections[i];
                long hash = 0xcbf29ce484222325L;
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            hash = (hash ^ Block.getId(section.getBlockState(x, y, z))) * 0x100000001b3L;
                        }
                    }
                }
                hashes[i] = hash;
            }
            byChunk[chunkIndex(chunk.getPos())] = hashes;
        }

        private int column(int x, int z) {
            return (x - this.minX) + (z - this.minZ) * this.size;
        }

        private static int sectionIndex(int column, int y) {
            return column * (SECTION_MAX_Y - SECTION_MIN_Y + 1) + (y - SECTION_MIN_Y);
        }

        private static int descentIndex(int column, int y) {
            return column * DESCENT_LAYERS + (y - DESCENT_BOTTOM_Y);
        }

        private static int caveIndex(int column, int y) {
            return column * CAVE_SECTION_LAYERS + (y - CAVE_SECTION_MIN_Y);
        }

        void capture(ChunkAccess chunk) {
            ChunkPos chunkPos = chunk.getPos();
            // An ImposterProtoChunk wraps a chunk that is already full: its post-processing lists are gone.
            ProtoChunk proto = chunk instanceof ProtoChunk protoChunk && !(chunk instanceof ImposterProtoChunk) ? protoChunk : null;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    int x = chunkPos.getMinBlockX() + localX;
                    int z = chunkPos.getMinBlockZ() + localZ;
                    int column = column(x, z);

                    int surface = this.topY;
                    while (surface >= this.bottomY && chunk.getBlockState(pos.set(x, surface, z)).isAir()) surface--;
                    int floor = surface;
                    while (floor >= this.bottomY && chunk.getBlockState(pos.set(x, floor, z)).is(Blocks.WATER)) floor--;
                    this.surfaceY[column] = surface;
                    this.floorY[column] = floor;
                    this.surfaceColor[column] = surface >= this.bottomY ? baseColor(chunk.getBlockState(pos.set(x, floor, z))) : 0;
                    this.surfaceClass[column] = surface > floor ? '~' : surface >= this.bottomY ? mapChar(chunk.getBlockState(pos.set(x, floor, z))) : ' ';
                    this.surfaceBiome[column] = biomeIndex(chunk, x, Math.max(floor, this.bottomY), z);
                    for (int i = 0; i < BIOME_LAYER_YS.length; i++) {
                        this.layerBiome[i][column] = biomeIndex(chunk, x, BIOME_LAYER_YS[i], z);
                    }
                    for (int i = 0; i < SLICE_YS.length; i++) {
                        int y = SLICE_YS[i];
                        this.slices[i][column] = y < this.bottomY || y > this.topY ? AIR : classify(chunk.getBlockState(pos.set(x, y, z)));
                    }
                    for (int y = BASIN_SEAM_MIN_Y; y < BASIN_SEAM_MAX_Y; y++) {
                        if (chunk.getBlockState(pos.set(x, y, z)).is(Blocks.WATER)) {
                            this.basinWater.set(column * (BASIN_SEAM_MAX_Y - BASIN_SEAM_MIN_Y) + (y - BASIN_SEAM_MIN_Y));
                        }
                    }
                    for (int y = WATER_CEILING_Y; y <= surface; y++) {
                        if (chunk.getBlockState(pos.set(x, y, z)).is(Blocks.WATER)) this.waterAtOrAboveCeiling++;
                    }
                    for (int y = DESCENT_BOTTOM_Y; y <= DESCENT_TOP_Y; y++) {
                        if (!chunk.getBlockState(pos.set(x, y, z)).isAir()) continue;
                        this.descentAir.set(descentIndex(column, y));
                        this.descentAirByY[y - DESCENT_BOTTOM_Y]++;
                        int band = 0;
                        while (y >= DESCENT_BAND_TOPS[band]) band++;
                        this.descentAirByBand[band]++;
                    }
                    if (this.caveBlocks != null) {
                        int basinWater = 0;
                        for (int y = CAVE_SECTION_MIN_Y; y <= CAVE_SECTION_MAX_Y; y++) {
                            BlockState state = chunk.getBlockState(pos.set(x, y, z));
                            boolean water = state.is(Blocks.WATER);
                            this.caveBlocks[caveIndex(column, y)] = state.isAir() ? AIR : water ? WATER : STONE;
                            if (water && y > PvWorldgenConstants.BASIN_MIN_Y && y < PvWorldgenConstants.CORRIDOR_MAX_Y) basinWater++;
                        }
                        if (basinWater > this.lakeColumnWater) {
                            this.lakeColumnWater = basinWater;
                            this.lakeColumn = column;
                        }
                    }
                    if (this.sectionBlocks != null) {
                        for (int y = SECTION_MIN_Y; y <= SECTION_MAX_Y; y++) {
                            BlockState state = chunk.getBlockState(pos.set(x, y, z));
                            this.sectionBlocks[sectionIndex(column, y)] = state.isAir() ? AIR : state.is(Blocks.WATER) ? WATER : STONE;
                        }
                    }
                }
            }
            BitSet ticking = proto != null ? countQueuedFluidTicks(proto) : new BitSet();
            countLeaks(chunk, ticking);
            countHighRiver(chunk);
            for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
                StructureStart start = entry.getValue();
                if (start.getPieces().isEmpty()) continue; // 26.3's isValid()
                BoundingBox box = start.getBoundingBox();
                this.structureStarts.add(String.format(Locale.ROOT, "structure %s start chunk %d,%d box %d,%d,%d..%d,%d,%d",
                        this.structures.getKey(entry.getKey()), start.getChunkPos().x(), start.getChunkPos().z(), box.minX(), box.minY(),
                        box.minZ(), box.maxX(), box.maxY(), box.maxZ()));
            }
        }

        /** The high river's water and where it can flow, inside the chunk ({@link #riverSurfaceWater} and the rest). */
        private void countHighRiver(ChunkAccess chunk) {
            ChunkPos chunkPos = chunk.getPos();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int layer = 0; layer < HIGH_RIVER_LAYERS.length; layer++) {
                int surfaceY = HIGH_RIVER_LAYERS[layer][0], minY = HIGH_RIVER_LAYERS[layer][1];
                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        int x = chunkPos.getMinBlockX() + localX, z = chunkPos.getMinBlockZ() + localZ;
                        int lowest = Integer.MAX_VALUE;
                        for (int y = minY; y <= surfaceY; y++) {
                            if (!chunk.getBlockState(pos.set(x, y, z)).is(Blocks.WATER)) continue;
                            lowest = Math.min(lowest, y);
                            boolean surface = y == surfaceY;
                            if (!surface) this.countThinWalls(chunk, layer, localX, y, localZ);
                            if (surface) this.riverSurfaceWater[layer]++;
                            else this.riverBedWater[layer]++;
                            boolean spilling = false;
                            for (int[] offset : SIDES_AND_BELOW) {
                                int nx = localX + offset[0], nz = localZ + offset[2];
                                if (nx < 0 || nx > 15 || nz < 0 || nz > 15) continue;
                                if (!chunk.getBlockState(pos.set(chunkPos.getMinBlockX() + nx, y + offset[1], chunkPos.getMinBlockZ() + nz)).isAir()) continue;
                                if (!surface) this.riverBedBesideAir[layer]++;
                                else if (offset[1] < 0) this.riverSurfaceOverAir[layer]++;
                                else {
                                    this.riverSpillFaces[layer]++;
                                    spilling = true;
                                }
                            }
                            if (spilling) this.riverSpillColumns[layer]++;
                        }
                        if (lowest == Integer.MAX_VALUE) continue;
                        for (int y = lowest - 1; y >= lowest - RIVER_HOLLOW_REACH; y--) {
                            if (chunk.getBlockState(pos.set(x, y, z)).isAir()) {
                                this.riverOverHollow[layer]++;
                                break;
                            }
                        }
                    }
                }
            }
        }

        /** Bed water's sides where the block beside it is solid and the one past that is air: a wall one block thick. */
        private void countThinWalls(ChunkAccess chunk, int layer, int localX, int y, int localZ) {
            ChunkPos chunkPos = chunk.getPos();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int[] offset : SIDES_AND_BELOW) {
                if (offset[1] != 0) continue;
                int wallX = localX + offset[0], wallZ = localZ + offset[2], pastX = wallX + offset[0], pastZ = wallZ + offset[2];
                if (pastX < 0 || pastX > 15 || pastZ < 0 || pastZ > 15) continue;
                BlockState wall = chunk.getBlockState(pos.set(chunkPos.getMinBlockX() + wallX, y, chunkPos.getMinBlockZ() + wallZ));
                if (wall.isAir() || wall.is(Blocks.WATER)) continue;
                if (chunk.getBlockState(pos.set(chunkPos.getMinBlockX() + pastX, y, chunkPos.getMinBlockZ() + pastZ)).isAir()) {
                    this.riverThinWalls[layer]++;
                }
            }
        }

        /**
         * Water blocks by height, and those with air beside or below them, inside the chunk: the water spills there once
         * it's updated, or right away if it has a fluid tick queued ({@code ticking}, by {@link #localIndex}).
         */
        private void countLeaks(ChunkAccess chunk, BitSet ticking) {
            ChunkPos chunkPos = chunk.getPos();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    for (int y = this.bottomY; y < LEAK_MIN_Y; y++) {
                        if (chunk.getBlockState(pos.set(chunkPos.getMinBlockX() + localX, y, chunkPos.getMinBlockZ() + localZ)).is(Blocks.WATER)) {
                            this.waterBelowSeaBand++;
                        }
                    }
                    for (int y = LEAK_MIN_Y; y < LEAK_MAX_Y; y++) {
                        BlockState state = chunk.getBlockState(pos.set(chunkPos.getMinBlockX() + localX, y, chunkPos.getMinBlockZ() + localZ));
                        if (!state.is(Blocks.WATER)) continue;
                        int waterBand = 0;
                        while (y >= WATER_BAND_TOPS[waterBand]) waterBand++;
                        this.waterByBand[waterBand]++;
                        if (!state.getFluidState().isSource()) continue;
                        boolean leaks = false;
                        for (int[] offset : SIDES_AND_BELOW) {
                            int nx = localX + offset[0], ny = y + offset[1], nz = localZ + offset[2];
                            if (nx < 0 || nx > 15 || nz < 0 || nz > 15) continue;
                            if (!chunk.getBlockState(pos.set(chunkPos.getMinBlockX() + nx, ny, chunkPos.getMinBlockZ() + nz)).isAir()) continue;
                            leaks = true;
                        }
                        if (!leaks) continue;
                        this.leakingWater++;
                        if (ticking.get(this.localIndex(localX, y, localZ))) this.leakingWaterTicking++;
                        int band = 0;
                        while (y >= LEAK_BAND_TOPS[band]) band++;
                        this.leakingWaterByBand[band]++;
                    }
                }
            }
        }

        private int localIndex(int localX, int y, int localZ) {
            return ((y - this.bottomY) * 16 + localZ) * 16 + localX;
        }

        /**
         * Counts the fluid blocks the terrain and the carvers marked for a fluid update (the chunk's post-processing lists).
         *
         * @return the marked positions, by {@link #localIndex}
         */
        private BitSet countQueuedFluidTicks(ProtoChunk chunk) {
            this.protoChunks++;
            BitSet marked = new BitSet();
            ShortList[] lists = chunk.getPostProcessing();
            for (int index = 0; index < lists.length; index++) {
                ShortList packed = lists[index];
                if (packed == null) continue;
                int sectionY = chunk.getSectionYFromSectionIndex(index);
                for (int i = 0; i < packed.size(); i++) {
                    BlockPos pos = ProtoChunk.unpackOffsetCoordinates(packed.getShort(i), sectionY, chunk.getPos());
                    int y = pos.getY();
                    this.fluidTicksQueued++;
                    if (y >= BASIN_SEAM_MIN_Y && y < BASIN_SEAM_MAX_Y) this.fluidTicksQueuedInBasinLayers++;
                    if (y >= this.bottomY && y <= this.topY) marked.set(this.localIndex(pos.getX() & 15, y, pos.getZ() & 15));
                }
            }
            return marked;
        }

        private int biomeIndex(ChunkAccess chunk, int x, int y, int z) {
            String id = chunk.getBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)).getRegisteredName();
            return this.biomeIndex.computeIfAbsent(id, key -> {
                this.biomeIds.add(key);
                return this.biomeIds.size() - 1;
            });
        }

        private static byte classify(BlockState state) {
            if (state.isAir()) return AIR;
            if (state.is(Blocks.WATER)) return WATER;
            if (state.is(Blocks.LAVA)) return LAVA;
            if (state.is(Blocks.STONE)) return STONE;
            if (state.is(Blocks.DEEPSLATE)) return DEEPSLATE;
            return OTHER;
        }

        /** Rough map colors for the top solid block, so beaches, snow, badlands and mud stand out. */
        private static int baseColor(BlockState state) {
            Block block = state.getBlock();
            if (block == Blocks.SAND || block == Blocks.SANDSTONE || block == Blocks.SUSPICIOUS_SAND) return 0xDACD96;
            if (block == Blocks.RED_SAND || block == Blocks.RED_SANDSTONE) return 0xBE6E3C;
            if (block == Blocks.GRAVEL || block == Blocks.SUSPICIOUS_GRAVEL) return 0x8C8782;
            if (block == Blocks.GRASS_BLOCK) return 0x5F9646;
            if (block == Blocks.SNOW_BLOCK || block == Blocks.SNOW || block == Blocks.POWDER_SNOW) return 0xF0F5FA;
            if (block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE) return 0xAAC8F0;
            if (block == Blocks.MUD || block == Blocks.MUDDY_MANGROVE_ROOTS) return 0x5A463C;
            if (block == Blocks.DIRT || block == Blocks.COARSE_DIRT || block == Blocks.PODZOL || block == Blocks.ROOTED_DIRT) return 0x785A3C;
            if (block == Blocks.MYCELIUM) return 0x826E82;
            if (block == Blocks.CALCITE) return 0xDDDDD5;
            if (BuiltInRegistries.BLOCK.getKey(block).getPath().contains("terracotta")) return 0xAA6446;
            return 0x808080;
        }

        void writeImages(Path dir) throws IOException {
            int minHeight = Integer.MAX_VALUE, maxHeight = Integer.MIN_VALUE;
            for (int y : this.floorY) {
                minHeight = Math.min(minHeight, y);
                maxHeight = Math.max(maxHeight, y);
            }
            float heightRange = Math.max(1, maxHeight - minHeight);

            BufferedImage surface = image();
            for (int column = 0; column < this.surfaceY.length; column++) {
                int waterDepth = this.surfaceY[column] - this.floorY[column];
                int rgb;
                if (waterDepth > 0) {
                    float shade = 1.0F - Math.min(waterDepth, 40) / 40.0F * 0.75F;
                    rgb = scale(0x3F76E4, shade);
                } else {
                    float shade = 0.55F + 0.45F * (this.floorY[column] - minHeight) / heightRange;
                    rgb = scale(this.surfaceColor[column], shade);
                }
                set(surface, column, rgb);
            }
            ImageIO.write(surface, "png", dir.resolve("surface.png").toFile());

            writeBiomeImage(dir.resolve("biomes-surface.png"), this.surfaceBiome);
            for (int i = 0; i < BIOME_LAYER_YS.length; i++) {
                writeBiomeImage(dir.resolve("biomes-y" + BIOME_LAYER_YS[i] + ".png"), this.layerBiome[i]);
            }

            int[] sliceColors = {0x000000, 0x2F6FD6, 0xFF7A00, 0xA0A0A0, 0x505058, 0x8A6A4A};
            for (int i = 0; i < SLICE_YS.length; i++) {
                BufferedImage slice = image();
                for (int column = 0; column < this.slices[i].length; column++) {
                    set(slice, column, sliceColors[this.slices[i][column]]);
                }
                ImageIO.write(slice, "png", dir.resolve("slice-y" + SLICE_YS[i] + ".png").toFile());
            }

            List<String> legend = new ArrayList<>();
            for (int i = 0; i < this.biomeIds.size(); i++) {
                legend.add(String.format(Locale.ROOT, "#%06X %s", biomeColor(this.biomeIds.get(i)), this.biomeIds.get(i)));
            }
            Files.write(dir.resolve("biomes-legend.txt"), legend);
        }

        private void writeBiomeImage(Path file, int[] biomes) throws IOException {
            BufferedImage image = image();
            for (int column = 0; column < biomes.length; column++) {
                set(image, column, biomeColor(this.biomeIds.get(biomes[column])));
            }
            ImageIO.write(image, "png", file.toFile());
        }

        private BufferedImage image() {
            return new BufferedImage(this.size, this.size, BufferedImage.TYPE_INT_RGB);
        }

        private void set(BufferedImage image, int column, int rgb) {
            image.setRGB(column % this.size, column / this.size, rgb);
        }

        private static int biomeColor(String id) {
            int hash = id.hashCode() * 0x9E3779B1;
            float hue = ((hash >>> 8) & 0xFFFF) / 65535.0F;
            float saturation = 0.45F + ((hash >>> 24) & 0xFF) / 255.0F * 0.45F;
            float brightness = 0.6F + (hash & 0xFF) / 255.0F * 0.35F;
            return Color.HSBtoRGB(hue, saturation, brightness) & 0xFFFFFF;
        }

        private static int scale(int rgb, float factor) {
            int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * factor));
            int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * factor));
            int b = Math.min(255, Math.round((rgb & 0xFF) * factor));
            return (r << 16) | (g << 8) | b;
        }

        List<String> report(String settings, String generator, String gate, long seed, ChunkPos center, int radius, Map<String, Long> stageNanos, int chunkCount) {
            List<String> lines = new ArrayList<>();
            lines.add("settings " + settings);
            lines.add("generator " + generator);
            lines.add("gate " + gate);
            lines.add("final_density data, compiled by the game");
            lines.add("seed " + seed);
            lines.add(String.format(Locale.ROOT, "region center chunk %d,%d radius %d (%d chunks), heights %d..%d",
                    center.x(), center.z(), radius, chunkCount, this.bottomY, this.topY));
            lines.add("cpus " + Runtime.getRuntime().availableProcessors() + ", java " + System.getProperty("java.version"));
            lines.add("mods " + FabricLoader.getInstance().getAllMods().stream()
                    .map(mod -> mod.getMetadata().getId() + " " + mod.getMetadata().getVersion().getFriendlyString())
                    .filter(mod -> !mod.startsWith("fabric") && !mod.startsWith("java ") && !mod.startsWith("minecraft ")
                            && !mod.startsWith("mixinextras "))
                    .sorted()
                    .collect(java.util.stream.Collectors.joining(", ")));
            long total = 0;
            for (Map.Entry<String, Long> stage : stageNanos.entrySet()) {
                total += stage.getValue();
                lines.add(String.format(Locale.ROOT, "time %-9s %10.1f ms %8.2f ms/chunk",
                        stage.getKey(), stage.getValue() / 1e6, stage.getValue() / 1e6 / chunkCount));
            }
            lines.add(String.format(Locale.ROOT, "time %-9s %10.1f ms %8.2f ms/chunk", "total", total / 1e6, total / 1e6 / chunkCount));

            lines.add("metric water_at_or_above_y" + WATER_CEILING_Y + " " + this.waterAtOrAboveCeiling);
            double chunks = this.size * this.size / 256.0;
            lines.add(String.format(Locale.ROOT, "metric water_beside_or_above_air_y%d..%d_per_chunk %.2f (%.2f with a fluid tick);"
                            + " by height: y %d..%d %.2f, %d..%d %.2f, %d..%d %.2f, %d..%d %.2f",
                    LEAK_MIN_Y, LEAK_MAX_Y - 1, this.leakingWater / chunks, this.leakingWaterTicking / chunks,
                    LEAK_MIN_Y, LEAK_BAND_TOPS[0] - 1, this.leakingWaterByBand[0] / chunks,
                    LEAK_BAND_TOPS[0], LEAK_BAND_TOPS[1] - 1, this.leakingWaterByBand[1] / chunks,
                    LEAK_BAND_TOPS[1], LEAK_BAND_TOPS[2] - 1, this.leakingWaterByBand[2] / chunks,
                    LEAK_BAND_TOPS[2], LEAK_BAND_TOPS[3] - 1, this.leakingWaterByBand[3] / chunks));
            lines.add(String.format(Locale.ROOT, "metric water_by_height_per_chunk y %d..%d %.2f, %d..%d %.2f, %d..%d %.2f, %d..%d %.2f,"
                            + " %d..%d %.2f, %d..%d %.2f",
                    this.bottomY, LEAK_MIN_Y - 1, this.waterBelowSeaBand / chunks,
                    LEAK_MIN_Y, WATER_BAND_TOPS[0] - 1, this.waterByBand[0] / chunks,
                    WATER_BAND_TOPS[0], WATER_BAND_TOPS[1] - 1, this.waterByBand[1] / chunks,
                    WATER_BAND_TOPS[1], WATER_BAND_TOPS[2] - 1, this.waterByBand[2] / chunks,
                    WATER_BAND_TOPS[2], WATER_BAND_TOPS[3] - 1, this.waterByBand[3] / chunks,
                    WATER_BAND_TOPS[3], WATER_BAND_TOPS[4] - 1, this.waterByBand[4] / chunks));
            lines.add(String.format(Locale.ROOT, "metric basin_seam_ratio_x %.2f", seamRatio(true)));
            lines.add(String.format(Locale.ROOT, "metric basin_seam_ratio_z %.2f", seamRatio(false)));
            appendWaterByChunkOffset(lines, true);
            appendWaterByChunkOffset(lines, false);
            lines.add(String.format(Locale.ROOT, "metric fluid_ticks_queued_per_chunk %.1f, in y %d..%d %.1f (water blocks there %.1f)",
                    perChunk(this.fluidTicksQueued), BASIN_SEAM_MIN_Y, BASIN_SEAM_MAX_Y - 1, perChunk(this.fluidTicksQueuedInBasinLayers),
                    this.basinWater.cardinality() / (this.size * this.size / 256.0)));

            appendLowlands(lines);
            for (int layer = 0; layer < HIGH_RIVER_LAYERS.length; layer++) {
                lines.add(String.format(Locale.ROOT, "metric %s water at y %d %.2f, in the bed %.2f; open faces beside the"
                                + " surface's water %.2f (in %.2f columns), under it %.2f; bed water beside or over open air %.2f",
                        HIGH_RIVER_METRICS[layer], HIGH_RIVER_LAYERS[layer][0], this.riverSurfaceWater[layer] / chunks,
                        this.riverBedWater[layer] / chunks, this.riverSpillFaces[layer] / chunks, this.riverSpillColumns[layer] / chunks,
                        this.riverSurfaceOverAir[layer] / chunks, this.riverBedBesideAir[layer] / chunks));
                lines.add(String.format(Locale.ROOT, "metric %s columns of water within %d blocks above air %.2f; bed walls one block"
                                + " thick with air behind %.2f", HIGH_RIVER_METRICS[layer].replace("_per_chunk", "_over_open_ground_per_chunk"),
                        RIVER_HOLLOW_REACH, this.riverOverHollow[layer] / chunks, this.riverThinWalls[layer] / chunks));
            }
            appendDescents(lines);
            if (this.basinLevelMetric != null) lines.add(this.basinLevelMetric);
            lines.add("biomes at surface:");
            appendHistogram(lines, this.surfaceBiome);
            for (int i = 0; i < BIOME_LAYER_YS.length; i++) {
                lines.add("biomes at y " + BIOME_LAYER_YS[i] + ":");
                appendHistogram(lines, this.layerBiome[i]);
            }

            lines.add("map surface (" + MAP_CELL + "x" + MAP_CELL + " blocks per character, north up):"
                    + " ~ water  : sand  , gravel  \" grass  * snow/ice  m mud  d dirt  t terracotta  c calcite  # stone  ? other");
            appendMap(lines, column -> this.surfaceClass[column]);
            List<String> biomeLegend = new ArrayList<>();
            lines.add("map surface biomes (legend below):");
            appendMap(lines, column -> biomeChar(this.surfaceBiome[column]));
            for (int i = 0; i < this.biomeIds.size(); i++) biomeLegend.add(biomeChar(i) + " " + this.biomeIds.get(i));
            lines.add("legend " + String.join(", ", biomeLegend));
            appendRiverSections(lines);
            appendCaveSections(lines);
            appendHashes(lines);
            this.structureStarts.stream().sorted().forEach(lines::add);
            return lines;
        }

        /**
         * The air of the dry caves' heights, by band and by y, and the bodies of air (blocks joined through their sides)
         * that reach from the top of those heights to the bottom: how many, their size, and their columns at the top, the
         * ways in from above.
         */
        private void appendDescents(List<String> lines) {
            double chunks = this.size * this.size / 256.0;
            int columns = this.size * this.size, top = DESCENT_LAYERS - 1, zStep = DESCENT_LAYERS * this.size;
            BitSet seen = new BitSet(columns * DESCENT_LAYERS);
            IntArrayList body = new IntArrayList();
            long descents = 0, descentBlocks = 0, descentTops = 0, bodiesFromTop = 0;
            int largest = 0;
            for (int column = 0; column < columns; column++) {
                int start = column * DESCENT_LAYERS + top;
                if (!this.descentAir.get(start) || seen.get(start)) continue;
                bodiesFromTop++;
                body.clear();
                body.add(start);
                seen.set(start);
                boolean reachesBottom = false;
                long tops = 0;
                for (int next = 0; next < body.size(); next++) {
                    int index = body.getInt(next), level = index % DESCENT_LAYERS, at = index / DESCENT_LAYERS;
                    int localX = at % this.size, localZ = at / this.size;
                    if (level == 0) reachesBottom = true;
                    if (level == top) tops++;
                    if (level > 0) visit(index - 1, seen, body);
                    if (level < top) visit(index + 1, seen, body);
                    if (localX > 0) visit(index - DESCENT_LAYERS, seen, body);
                    if (localX < this.size - 1) visit(index + DESCENT_LAYERS, seen, body);
                    if (localZ > 0) visit(index - zStep, seen, body);
                    if (localZ < this.size - 1) visit(index + zStep, seen, body);
                }
                if (!reachesBottom) continue;
                if (body.size() > largest) {
                    largest = body.size();
                    this.largestDescentColumn = column;
                }
                descents++;
                descentBlocks += body.size();
                descentTops += tops;
            }
            lines.add(String.format(Locale.ROOT, "metric dry_caves_air_per_chunk y %d..%d %.1f, %d..%d %.1f, %d..%d %.1f",
                    DESCENT_BOTTOM_Y, DESCENT_BAND_TOPS[0] - 1, this.descentAirByBand[0] / chunks, DESCENT_BAND_TOPS[0], DESCENT_BAND_TOPS[1] - 1,
                    this.descentAirByBand[1] / chunks, DESCENT_BAND_TOPS[1], DESCENT_BAND_TOPS[2] - 1, this.descentAirByBand[2] / chunks));
            StringBuilder byY = new StringBuilder("metric dry_caves_air_by_y_per_chunk from y " + DESCENT_BOTTOM_Y + ":");
            for (long count : this.descentAirByY) byY.append(String.format(Locale.ROOT, " %.1f", count / chunks));
            lines.add(byY.toString());
            lines.add(String.format(Locale.ROOT, "metric dry_caves_descents from y %d to y %d: %d bodies of air (of %d open at y %d),"
                            + " %.1f blocks per chunk, %.2f of their columns at y %d per chunk",
                    DESCENT_TOP_Y, DESCENT_BOTTOM_Y, descents, bodiesFromTop, DESCENT_TOP_Y, descentBlocks / chunks, descentTops / chunks, DESCENT_TOP_Y));
        }

        /** Adds air not yet seen to the body. */
        private void visit(int index, BitSet seen, IntArrayList body) {
            if (seen.get(index) || !this.descentAir.get(index)) return;
            seen.set(index);
            body.add(index);
        }

        /**
         * Cross sections of the high river, y {@link #SECTION_MAX_Y} down to {@link #SECTION_MIN_Y}: up to
         * {@link #SECTIONS_PER_AXIS} along x and as many along z, each through the middle of the first stretch of the
         * river's surface water (3 blocks or more) on its row, rows at least {@link #SECTION_SPACING} blocks apart.
         */
        private void appendRiverSections(List<String> lines) {
            if (this.sectionBlocks == null) return;
            for (boolean alongX : new boolean[]{true, false}) {
                int found = 0, lastRow = -SECTION_SPACING;
                for (int row = 0; row < this.size && found < SECTIONS_PER_AXIS; row++) {
                    if (row - lastRow < SECTION_SPACING) continue;
                    int runStart = -1, first = -1, last = -1;
                    for (int i = 0; i <= this.size && first < 0; i++) {
                        boolean water = i < this.size && this.sectionBlock(alongX, row, i, PvWorldgenConstants.HIGH_RIVER_Y) == WATER;
                        if (water && runStart < 0) runStart = i;
                        if (!water && runStart >= 0) {
                            if (i - runStart >= 3) {
                                first = runStart;
                                last = i - 1;
                            }
                            runStart = -1;
                        }
                    }
                    if (first < 0) continue;
                    found++;
                    lastRow = row;
                    int middle = (first + last) / 2, from = Math.max(0, middle - SECTION_REACH), to = Math.min(this.size - 1, middle + SECTION_REACH);
                    int rowBase = alongX ? this.minZ : this.minX, base = alongX ? this.minX : this.minZ;
                    lines.add(String.format(Locale.ROOT, "section along %s at %s %d, %s %d..%d, river surface %d..%d (# ground, ~ water, . air):",
                            alongX ? "x" : "z", alongX ? "z" : "x", rowBase + row, alongX ? "x" : "z", base + from, base + to, base + first, base + last));
                    for (int y = SECTION_MAX_Y; y >= SECTION_MIN_Y; y--) {
                        StringBuilder line = new StringBuilder(String.format(Locale.ROOT, "  %4d ", y));
                        for (int i = from; i <= to; i++) {
                            byte block = this.sectionBlock(alongX, row, i, y);
                            line.append(block == AIR ? '.' : block == WATER ? '~' : '#');
                        }
                        lines.add(line.toString());
                    }
                }
            }
        }

        /**
         * Cross sections of the caves, y {@link #CAVE_SECTION_MAX_Y} down to {@link #CAVE_SECTION_MIN_Y}: along x and z
         * through the top of the biggest body of air that reaches from the dry caves' top to their bottom, and along x
         * through the column with the most water in the basin layers.
         */
        private void appendCaveSections(List<String> lines) {
            if (this.caveBlocks == null) return;
            if (this.largestDescentColumn >= 0) {
                appendCaveSection(lines, this.largestDescentColumn, true, "through the top of the biggest dry way down");
                appendCaveSection(lines, this.largestDescentColumn, false, "through the top of the biggest dry way down");
            }
            if (this.lakeColumn >= 0) appendCaveSection(lines, this.lakeColumn, true, "through the most basin water");
        }

        private void appendCaveSection(List<String> lines, int column, boolean alongX, String what) {
            int localX = column % this.size, localZ = column / this.size, at = alongX ? localX : localZ;
            int from = Math.max(0, at - CAVE_SECTION_REACH), to = Math.min(this.size - 1, at + CAVE_SECTION_REACH);
            lines.add(String.format(Locale.ROOT, "cave section %s, along %s at %s %d, %s %d..%d (# ground, ~ water, . air):", what,
                    alongX ? "x" : "z", alongX ? "z" : "x", alongX ? this.minZ + localZ : this.minX + localX, alongX ? "x" : "z",
                    (alongX ? this.minX : this.minZ) + from, (alongX ? this.minX : this.minZ) + to));
            for (int y = CAVE_SECTION_MAX_Y; y >= CAVE_SECTION_MIN_Y; y--) {
                StringBuilder line = new StringBuilder(String.format(Locale.ROOT, "  %4d ", y));
                for (int i = from; i <= to; i++) {
                    int c = alongX ? i + localZ * this.size : localX + i * this.size;
                    byte block = this.caveBlocks[caveIndex(c, y)];
                    line.append(block == AIR ? '.' : block == WATER ? '~' : '#');
                }
                lines.add(line.toString());
            }
        }

        private byte sectionBlock(boolean alongX, int row, int i, int y) {
            int column = alongX ? i + row * this.size : row + i * this.size;
            return this.sectionBlocks[sectionIndex(column, y)];
        }

        private static final String HASH_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ+/";

        /**
         * Per status: one hash per 16-block layer over the whole region, from the bottom up, then a map with one character
         * per chunk (north up) hashing all of the chunk's blocks.
         */
        private void appendHashes(List<String> lines) {
            for (Map.Entry<String, long[][]> entry : this.sectionHashes.entrySet()) {
                long[][] byChunk = entry.getValue();
                int sections = 0;
                for (long[] hashes : byChunk) if (hashes != null) sections = Math.max(sections, hashes.length);
                StringBuilder layers = new StringBuilder("hash " + entry.getKey() + " layers from y " + this.bottomY + ", 16 blocks each:");
                for (int section = 0; section < sections; section++) {
                    long layer = 0;
                    for (long[] hashes : byChunk) layer = mix(layer, hashes != null && section < hashes.length ? hashes[section] : 0L);
                    layers.append(String.format(Locale.ROOT, " %08x", layer >>> 32));
                }
                lines.add(layers.toString());
                lines.add("hash " + entry.getKey() + " chunks (one character per chunk, north up):");
                int side = chunksPerSide();
                for (int chunkZ = 0; chunkZ < side; chunkZ++) {
                    StringBuilder row = new StringBuilder("  ");
                    for (int chunkX = 0; chunkX < side; chunkX++) {
                        long[] hashes = byChunk[chunkX + chunkZ * side];
                        long chunk = 0;
                        if (hashes != null) for (long hash : hashes) chunk = mix(chunk, hash);
                        row.append(hashes == null ? ' ' : HASH_CHARS.charAt((int) (chunk >>> 58)));
                    }
                    lines.add(row.toString());
                }
            }
        }

        private static long mix(long hash, long value) {
            return (Long.rotateLeft(hash, 23) ^ value) * 0x9e3779b97f4a7c15L;
        }

        /**
         * Columns whose highest block that isn't air or water lies below the sea surface, with water over it or dry. A
         * renderer that estimates water as "up to sea level wherever the ground is lower" (Distant Horizons' distant
         * generator) draws water over the dry ones.
         */
        private void appendLowlands(List<String> lines) {
            long wet = 0, dry = 0;
            for (int column = 0; column < this.size * this.size; column++) {
                if (this.floorY[column] < this.bottomY || this.floorY[column] >= this.seaLevel - 1) continue;
                if (this.surfaceY[column] > this.floorY[column]) wet++;
                else dry++;
            }
            double chunks = this.size * this.size / 256.0;
            lines.add(String.format(Locale.ROOT, "metric ground_below_sea_surface_columns_per_chunk %.1f under water, %.1f dry (%.1f%% dry)",
                    wet / chunks, dry / chunks, wet + dry == 0 ? 0.0 : 100.0 * dry / (wet + dry)));
        }

        /** Water blocks in y 0..31 per block offset inside the chunk along one axis, relative to the mean of the 16 offsets. */
        private void appendWaterByChunkOffset(List<String> lines, boolean alongX) {
            int layers = BASIN_SEAM_MAX_Y - BASIN_SEAM_MIN_Y;
            long[] counts = new long[16];
            for (int column = 0; column < this.size * this.size; column++) {
                int world = alongX ? this.minX + column % this.size : this.minZ + column / this.size;
                for (int layer = 0; layer < layers; layer++) {
                    if (this.basinWater.get(column * layers + layer)) counts[world & 15]++;
                }
            }
            double mean = 0;
            for (long count : counts) mean += count / 16.0;
            StringBuilder values = new StringBuilder();
            double low = 0, high = 0;
            for (int offset = 0; offset < 16; offset++) {
                double relative = mean == 0 ? 0 : counts[offset] / mean;
                values.append(String.format(Locale.ROOT, " %.2f", relative));
                if (offset < 8) low += relative / 8;
                else high += relative / 8;
            }
            String axis = alongX ? "x" : "z";
            lines.add("metric water_y" + BASIN_SEAM_MIN_Y + ".." + (BASIN_SEAM_MAX_Y - 1) + "_by_" + axis + "_in_chunk" + values);
            lines.add(String.format(Locale.ROOT, "metric water_y%d..%d_%s0..7_vs_%s8..15 %.3f",
                    BASIN_SEAM_MIN_Y, BASIN_SEAM_MAX_Y - 1, axis, axis, high == 0 ? 0 : low / high));
        }

        /** One character per MAP_CELL x MAP_CELL blocks: the most common value in that square. */
        private void appendMap(List<String> lines, java.util.function.IntFunction<Character> charOf) {
            for (int cellZ = 0; cellZ < this.size; cellZ += MAP_CELL) {
                StringBuilder row = new StringBuilder("  ");
                for (int cellX = 0; cellX < this.size; cellX += MAP_CELL) {
                    Map<Character, Integer> votes = new HashMap<>();
                    for (int z = cellZ; z < Math.min(cellZ + MAP_CELL, this.size); z++) {
                        for (int x = cellX; x < Math.min(cellX + MAP_CELL, this.size); x++) {
                            votes.merge(charOf.apply(x + z * this.size), 1, Integer::sum);
                        }
                    }
                    row.append(votes.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(' '));
                }
                lines.add(row.toString());
            }
        }

        private static final String BIOME_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

        private static char biomeChar(int index) {
            return index < BIOME_CHARS.length() ? BIOME_CHARS.charAt(index) : '+';
        }

        private static char mapChar(BlockState state) {
            Block block = state.getBlock();
            if (block == Blocks.SAND || block == Blocks.SANDSTONE || block == Blocks.SUSPICIOUS_SAND
                    || block == Blocks.RED_SAND || block == Blocks.RED_SANDSTONE) return ':';
            if (block == Blocks.GRAVEL || block == Blocks.SUSPICIOUS_GRAVEL) return ',';
            if (block == Blocks.GRASS_BLOCK) return '"';
            if (block == Blocks.SNOW_BLOCK || block == Blocks.SNOW || block == Blocks.POWDER_SNOW
                    || block == Blocks.ICE || block == Blocks.PACKED_ICE || block == Blocks.BLUE_ICE) return '*';
            if (block == Blocks.MUD || block == Blocks.MUDDY_MANGROVE_ROOTS) return 'm';
            if (block == Blocks.DIRT || block == Blocks.COARSE_DIRT || block == Blocks.PODZOL || block == Blocks.ROOTED_DIRT
                    || block == Blocks.MYCELIUM) return 'd';
            if (BuiltInRegistries.BLOCK.getKey(block).getPath().contains("terracotta")) return 't';
            if (block == Blocks.CALCITE) return 'c';
            if (block == Blocks.STONE || block == Blocks.DEEPSLATE || block == Blocks.ANDESITE || block == Blocks.DIORITE
                    || block == Blocks.GRANITE || block == Blocks.TUFF) return '#';
            return '?';
        }

        private double perChunk(long count) {
            return this.protoChunks == 0 ? 0.0 : (double) count / this.protoChunks;
        }

        private void appendHistogram(List<String> lines, int[] biomes) {
            Map<String, Integer> counts = new TreeMap<>();
            for (int biome : biomes) counts.merge(this.biomeIds.get(biome), 1, Integer::sum);
            counts.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .forEach(entry -> lines.add(String.format(Locale.ROOT, "  %6.2f%% %s",
                            100.0 * entry.getValue() / biomes.length, entry.getKey())));
        }

        /** Water/non-water changes between neighbouring columns across chunk borders, divided by those across chunk middles. */
        private double seamRatio(boolean alongX) {
            int layers = BASIN_SEAM_MAX_Y - BASIN_SEAM_MIN_Y;
            long border = 0, middle = 0;
            for (int a = 0; a < this.size; a++) {
                for (int b = 0; b + 1 < this.size; b++) {
                    int columnA = alongX ? b + a * this.size : a + b * this.size;
                    int columnB = alongX ? columnA + 1 : columnA + this.size;
                    int worldCoordinate = (alongX ? this.minX : this.minZ) + b;
                    int local = worldCoordinate & 15;
                    if (local != 15 && local != 7) continue;
                    for (int layer = 0; layer < layers; layer++) {
                        boolean waterA = this.basinWater.get(columnA * layers + layer);
                        boolean waterB = this.basinWater.get(columnB * layers + layer);
                        if (waterA == waterB) continue;
                        if (local == 15) border++;
                        else middle++;
                    }
                }
            }
            return (border + 1.0) / (middle + 1.0);
        }
    }
}
