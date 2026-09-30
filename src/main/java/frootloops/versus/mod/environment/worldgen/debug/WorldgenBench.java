package frootloops.versus.mod.environment.worldgen.debug;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.PvWorldgen;
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
 *   <li>water at or above y 64: the Players Versus aquifer never places any there but the high river's, at y 77..80;</li>
 *   <li>basin seam ratio: water/non-water changes across chunk borders divided by the same count across chunk
 *   middles, for y 0..31. About 1 means no seams;</li>
 *   <li>water in y 0..31 by x (and z) offset inside the chunk, relative to the mean; z is the control;</li>
 *   <li>fluid ticks queued: fluid blocks the aquifer marked for a fluid update, per chunk, overall and for y 0..31 next to
 *   the water blocks there. Each one runs when its chunk becomes a full chunk;</li>
 *   <li>water beside or above air in y -31..63 (neighbours inside the chunk): where water spills, or stands as a wall of
 *   water until something updates it. Split by height, and by whether the water has a fluid tick queued;</li>
 *   <li>water by height, per chunk: below y -31, where the aquifer places none; y -31..-9, which should stay dry
 *   (Section 10, question 7 of the refactor plan); y -8..-1, 0..23, 24..47 and 48..63;</li>
 *   <li>the high river's water and where it spills.</li>
 * </ul>
 * The report also holds text maps (one character per 8x8 blocks, north up) of the surface and its biomes, so results
 * can be compared without the images; hashes of every block after {@code TERRAIN}, by 16-block layer and by chunk, so
 * two runs that should agree can be checked block for block and their differences located; and the structure starts
 * in the region.
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
    /** The high river's water surface; above sea level, only its water is there before features run. */
    private static final int HIGH_RIVER_Y = frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_Y;
    private static final int HIGH_RIVER_MIN_Y = frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_MIN_Y;
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
         * The high river's water (y 77..80, the only water above sea level before features): at its surface and in its
         * bed; open faces beside the surface's water, where it spills, and the columns they're in; surface water with
         * open air under it; bed water with open air beside or under it, which the walls should leave none of.
         */
        private long riverSurfaceWater, riverBedWater, riverSpillFaces, riverSpillColumns, riverSurfaceOverAir, riverBedBesideAir;
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
                }
            }
            BitSet ticking = proto != null ? countQueuedFluidTicks(proto) : new BitSet();
            countLeaks(chunk, ticking);
            countHighRiver(chunk);
            for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
                StructureStart start = entry.getValue();
                if (!start.isValid()) continue;
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
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    int x = chunkPos.getMinBlockX() + localX, z = chunkPos.getMinBlockZ() + localZ;
                    for (int y = HIGH_RIVER_MIN_Y; y <= HIGH_RIVER_Y; y++) {
                        if (!chunk.getBlockState(pos.set(x, y, z)).is(Blocks.WATER)) continue;
                        boolean surface = y == HIGH_RIVER_Y;
                        if (surface) this.riverSurfaceWater++;
                        else this.riverBedWater++;
                        boolean spilling = false;
                        for (int[] offset : SIDES_AND_BELOW) {
                            int nx = localX + offset[0], nz = localZ + offset[2];
                            if (nx < 0 || nx > 15 || nz < 0 || nz > 15) continue;
                            if (!chunk.getBlockState(pos.set(chunkPos.getMinBlockX() + nx, y + offset[1], chunkPos.getMinBlockZ() + nz)).isAir()) continue;
                            if (!surface) this.riverBedBesideAir++;
                            else if (offset[1] < 0) this.riverSurfaceOverAir++;
                            else {
                                this.riverSpillFaces++;
                                spilling = true;
                            }
                        }
                        if (spilling) this.riverSpillColumns++;
                    }
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
            String id = chunk.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z)).getRegisteredName();
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
            lines.add("final_density " + (DensityCompilerCompat.ACTIVE ? "vanilla types, for C2ME's compiler" : "Java kernel"));
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
            lines.add(String.format(Locale.ROOT, "metric high_river_per_chunk water at y %d %.2f, in the bed %.2f; open faces beside the"
                            + " surface's water %.2f (in %.2f columns), under it %.2f; bed water beside or over open air %.2f",
                    HIGH_RIVER_Y, this.riverSurfaceWater / chunks, this.riverBedWater / chunks, this.riverSpillFaces / chunks,
                    this.riverSpillColumns / chunks, this.riverSurfaceOverAir / chunks, this.riverBedBesideAir / chunks));
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
            appendHashes(lines);
            this.structureStarts.stream().sorted().forEach(lines::add);
            return lines;
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
