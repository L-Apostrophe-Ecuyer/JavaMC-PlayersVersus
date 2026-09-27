package frootloops.versus.mod.environment.worldgen.debug;

import frootloops.versus.VersusMod;
import it.unimi.dsi.fastutil.shorts.ShortList;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.WrapperProtoChunk;
import net.minecraft.world.gen.carver.CarvingMask;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;

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
 * <p>Metrics, measured right after the carvers ran:
 * <ul>
 *   <li>water at or above y 64: the Players Versus aquifer never places any there;</li>
 *   <li>stone inside carved space below y -8: stone there is an aquifer barrier written over deepslate by a carver
 *   (quirk Q6);</li>
 *   <li>basin seam ratio: water/non-water changes across chunk borders divided by the same count across chunk
 *   middles, for y 0..31. About 1 means no seams; much more than 1 along x is quirk Q1.</li>
 *   <li>fluid ticks queued: fluid blocks the aquifer marked for a fluid update during NOISE and CARVERS, per chunk,
 *   overall and for y 0..31 next to the water blocks there. Each one runs when its chunk becomes a full chunk. Almost as
 *   many ticks as water blocks in y 0..31 is quirk Q8.</li>
 * </ul>
 */
public final class WorldgenBench {

    public static final int MAX_RADIUS = 32;

    private static final List<ChunkStatus> STAGES = List.of(
            ChunkStatus.BIOMES, ChunkStatus.NOISE, ChunkStatus.SURFACE, ChunkStatus.CARVERS, ChunkStatus.FEATURES, ChunkStatus.FULL);
    private static final int[] SLICE_YS = {-40, -20, 0, 16, 28, 40, 56, 62};
    private static final int[] BIOME_LAYER_YS = {-40, 0, 32};
    private static final int BASIN_SEAM_MIN_Y = 0;
    private static final int BASIN_SEAM_MAX_Y = 32;
    private static final int WATER_CEILING_Y = 64;
    private static final int DEEPSLATE_ONLY_BELOW_Y = -8;

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
                Path out = run(server.getOverworld(), center, radius, VersusMod.MOD_LOGGER::info);
                VersusMod.MOD_LOGGER.info("[pvwg] bench written to {}", out.toAbsolutePath());
            } catch (Throwable throwable) {
                VersusMod.MOD_LOGGER.error("[pvwg] bench failed", throwable);
            } finally {
                server.stop(false);
            }
        });
    }

    static int runCommand(ServerCommandSource source, int radius) {
        ChunkPos center = new ChunkPos(BlockPos.ofFloored(source.getPosition()));
        try {
            Path out = run(source.getWorld(), center, radius, message -> source.sendFeedback(() -> Text.literal(message), false));
            source.sendFeedback(() -> Text.literal("[pvwg] bench written to " + out.toAbsolutePath()), false);
            return 1;
        } catch (IOException exception) {
            source.sendError(Text.literal("[pvwg] bench failed: " + exception.getMessage()));
            return 0;
        }
    }

    public static Path run(ServerWorld world, ChunkPos center, int radius, Consumer<String> log) throws IOException {
        ServerChunkManager chunkManager = world.getChunkManager();
        String settings = describeGenerator(chunkManager.getChunkGenerator());
        int minChunkX = center.x - radius;
        int minChunkZ = center.z - radius;
        int chunksPerSide = radius * 2 + 1;
        int chunkCount = chunksPerSide * chunksPerSide;
        log.accept(String.format(Locale.ROOT, "[pvwg] bench: %d chunks around chunk %d,%d, settings %s",
                chunkCount, center.x, center.z, settings));

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
            stageNanos.put(status.getId(), elapsed);
            log.accept(String.format(Locale.ROOT, "[pvwg] %-9s %9.1f ms  %7.2f ms/chunk",
                    status.getId(), elapsed / 1e6, elapsed / 1e6 / chunkCount));

            if (status == ChunkStatus.CARVERS) {
                for (int chunkX = minChunkX; chunkX < minChunkX + chunksPerSide; chunkX++) {
                    for (int chunkZ = minChunkZ; chunkZ < minChunkZ + chunksPerSide; chunkZ++) {
                        region.capture(chunkManager.getChunk(chunkX, chunkZ, ChunkStatus.CARVERS, true));
                    }
                }
            }
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT));
        Path dir = FabricLoader.getInstance().getGameDir().resolve("pvwg")
                .resolve("bench-" + timestamp + "-" + settings.replaceAll("[^A-Za-z0-9_.-]", "_"));
        Files.createDirectories(dir);
        region.writeImages(dir);
        List<String> report = region.report(settings, world.getSeed(), center, radius, stageNanos, chunkCount);
        Files.write(dir.resolve("report.txt"), report);
        for (String line : report) {
            if (line.startsWith("metric")) log.accept("[pvwg] " + line);
        }
        return dir;
    }

    static String describeGenerator(ChunkGenerator generator) {
        if (generator instanceof NoiseChunkGenerator noiseGenerator) {
            return noiseGenerator.getSettings().getKey().map(key -> key.getValue().toString()).orElse("inline-settings");
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

        private final int minX, minZ, size, bottomY, topY;
        private final int[] surfaceY, floorY, surfaceColor;
        private final int[] surfaceBiome;
        private final int[][] layerBiome = new int[BIOME_LAYER_YS.length][];
        private final byte[][] slices = new byte[SLICE_YS.length][];
        private final BitSet basinWater;
        private final List<String> biomeIds = new ArrayList<>();
        private final Map<String, Integer> biomeIndex = new HashMap<>();
        private long waterAtOrAboveCeiling, stoneInCarvedDeepslate, carvedDeepslateZone;
        private long fluidTicksQueued, fluidTicksQueuedInBasinLayers;
        private int protoChunks;

        Region(ServerWorld world, int minX, int minZ, int size) {
            this.minX = minX;
            this.minZ = minZ;
            this.size = size;
            this.bottomY = world.getBottomY();
            this.topY = world.getBottomY() + world.getHeight() - 1;
            int columns = size * size;
            this.surfaceY = new int[columns];
            this.floorY = new int[columns];
            this.surfaceColor = new int[columns];
            this.surfaceBiome = new int[columns];
            for (int i = 0; i < BIOME_LAYER_YS.length; i++) this.layerBiome[i] = new int[columns];
            for (int i = 0; i < SLICE_YS.length; i++) this.slices[i] = new byte[columns];
            this.basinWater = new BitSet(columns * (BASIN_SEAM_MAX_Y - BASIN_SEAM_MIN_Y));
        }

        void capture(Chunk chunk) {
            ChunkPos chunkPos = chunk.getPos();
            // A WrapperProtoChunk wraps a chunk that is already full: its carving mask and post-processing lists are gone.
            ProtoChunk proto = chunk instanceof ProtoChunk protoChunk && !(chunk instanceof WrapperProtoChunk) ? protoChunk : null;
            CarvingMask carvingMask = proto != null ? proto.getCarvingMask() : null;
            BlockPos.Mutable pos = new BlockPos.Mutable();
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    int x = chunkPos.getStartX() + localX;
                    int z = chunkPos.getStartZ() + localZ;
                    int column = (x - this.minX) + (z - this.minZ) * this.size;

                    int surface = this.topY;
                    while (surface >= this.bottomY && chunk.getBlockState(pos.set(x, surface, z)).isAir()) surface--;
                    int floor = surface;
                    while (floor >= this.bottomY && chunk.getBlockState(pos.set(x, floor, z)).isOf(Blocks.WATER)) floor--;
                    this.surfaceY[column] = surface;
                    this.floorY[column] = floor;
                    this.surfaceColor[column] = surface >= this.bottomY ? baseColor(chunk.getBlockState(pos.set(x, floor, z))) : 0;
                    this.surfaceBiome[column] = biomeIndex(chunk, x, Math.max(floor, this.bottomY), z);
                    for (int i = 0; i < BIOME_LAYER_YS.length; i++) {
                        this.layerBiome[i][column] = biomeIndex(chunk, x, BIOME_LAYER_YS[i], z);
                    }
                    for (int i = 0; i < SLICE_YS.length; i++) {
                        int y = SLICE_YS[i];
                        this.slices[i][column] = y < this.bottomY || y > this.topY ? AIR : classify(chunk.getBlockState(pos.set(x, y, z)));
                    }
                    for (int y = BASIN_SEAM_MIN_Y; y < BASIN_SEAM_MAX_Y; y++) {
                        if (chunk.getBlockState(pos.set(x, y, z)).isOf(Blocks.WATER)) {
                            this.basinWater.set(column * (BASIN_SEAM_MAX_Y - BASIN_SEAM_MIN_Y) + (y - BASIN_SEAM_MIN_Y));
                        }
                    }
                    for (int y = WATER_CEILING_Y; y <= surface; y++) {
                        if (chunk.getBlockState(pos.set(x, y, z)).isOf(Blocks.WATER)) this.waterAtOrAboveCeiling++;
                    }
                    if (carvingMask != null) {
                        for (int y = this.bottomY; y < DEEPSLATE_ONLY_BELOW_Y; y++) {
                            if (!carvingMask.get(localX, y, localZ)) continue;
                            this.carvedDeepslateZone++;
                            if (chunk.getBlockState(pos.set(x, y, z)).isOf(Blocks.STONE)) this.stoneInCarvedDeepslate++;
                        }
                    }
                }
            }
            if (proto != null) countQueuedFluidTicks(proto);
        }

        /** Counts the fluid blocks NOISE and CARVERS marked for a fluid update (the chunk's post-processing lists). */
        private void countQueuedFluidTicks(ProtoChunk chunk) {
            this.protoChunks++;
            ShortList[] lists = chunk.getPostProcessingLists();
            for (int index = 0; index < lists.length; index++) {
                ShortList packed = lists[index];
                if (packed == null) continue;
                int sectionY = chunk.sectionIndexToCoord(index);
                for (int i = 0; i < packed.size(); i++) {
                    int y = ProtoChunk.joinBlockPos(packed.getShort(i), sectionY, chunk.getPos()).getY();
                    this.fluidTicksQueued++;
                    if (y >= BASIN_SEAM_MIN_Y && y < BASIN_SEAM_MAX_Y) this.fluidTicksQueuedInBasinLayers++;
                }
            }
        }

        private int biomeIndex(Chunk chunk, int x, int y, int z) {
            String id = chunk.getBiomeForNoiseGen(BiomeCoords.fromBlock(x), BiomeCoords.fromBlock(y), BiomeCoords.fromBlock(z)).getIdAsString();
            return this.biomeIndex.computeIfAbsent(id, key -> {
                this.biomeIds.add(key);
                return this.biomeIds.size() - 1;
            });
        }

        private static byte classify(BlockState state) {
            if (state.isAir()) return AIR;
            if (state.isOf(Blocks.WATER)) return WATER;
            if (state.isOf(Blocks.LAVA)) return LAVA;
            if (state.isOf(Blocks.STONE)) return STONE;
            if (state.isOf(Blocks.DEEPSLATE)) return DEEPSLATE;
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
            if (Registries.BLOCK.getId(block).getPath().contains("terracotta")) return 0xAA6446;
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

        List<String> report(String settings, long seed, ChunkPos center, int radius, Map<String, Long> stageNanos, int chunkCount) {
            List<String> lines = new ArrayList<>();
            lines.add("settings " + settings);
            lines.add("seed " + seed);
            lines.add(String.format(Locale.ROOT, "region center chunk %d,%d radius %d (%d chunks), heights %d..%d",
                    center.x, center.z, radius, chunkCount, this.bottomY, this.topY));
            lines.add("cpus " + Runtime.getRuntime().availableProcessors() + ", java " + System.getProperty("java.version"));
            long total = 0;
            for (Map.Entry<String, Long> stage : stageNanos.entrySet()) {
                total += stage.getValue();
                lines.add(String.format(Locale.ROOT, "time %-9s %10.1f ms %8.2f ms/chunk",
                        stage.getKey(), stage.getValue() / 1e6, stage.getValue() / 1e6 / chunkCount));
            }
            lines.add(String.format(Locale.ROOT, "time %-9s %10.1f ms %8.2f ms/chunk", "total", total / 1e6, total / 1e6 / chunkCount));

            lines.add("metric water_at_or_above_y" + WATER_CEILING_Y + " " + this.waterAtOrAboveCeiling);
            lines.add(String.format(Locale.ROOT, "metric stone_in_carved_space_below_y%d %d of %d carved positions",
                    DEEPSLATE_ONLY_BELOW_Y, this.stoneInCarvedDeepslate, this.carvedDeepslateZone));
            lines.add(String.format(Locale.ROOT, "metric basin_seam_ratio_x %.2f", seamRatio(true)));
            lines.add(String.format(Locale.ROOT, "metric basin_seam_ratio_z %.2f", seamRatio(false)));
            lines.add(String.format(Locale.ROOT, "metric fluid_ticks_queued_per_chunk %.1f, in y %d..%d %.1f (water blocks there %.1f)",
                    perChunk(this.fluidTicksQueued), BASIN_SEAM_MIN_Y, BASIN_SEAM_MAX_Y - 1, perChunk(this.fluidTicksQueuedInBasinLayers),
                    this.basinWater.cardinality() / (this.size * this.size / 256.0)));

            lines.add("biomes at surface:");
            appendHistogram(lines, this.surfaceBiome);
            for (int i = 0; i < BIOME_LAYER_YS.length; i++) {
                lines.add("biomes at y " + BIOME_LAYER_YS[i] + ":");
                appendHistogram(lines, this.layerBiome[i]);
            }
            return lines;
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
