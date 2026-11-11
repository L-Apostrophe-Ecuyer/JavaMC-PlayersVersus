package frootloops.versus.mod.environment.worldgen.debug;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.blocks.BlockTagAudit;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityBufferPool;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import net.fabricmc.loader.api.FabricLoader;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * What a biome's land is made of and what grows on it, counted on a generated world: the headless run's way of asking why
 * something doesn't generate (mangrove trees, say) of a world type, to compare with another's.
 *
 * <p>Set the system property {@code pv.worldgen.census=<biome ids>} (comma separated; {@code runWorldgenSmoke} takes
 * {@code -Ppv.census=...}) to run it once the server has started, instead of the bench, then stop the server. It logs lines
 * starting with {@code [pvwg] census}:
 * <ul>
 *   <li>whether a few plants (a mangrove propagule, grass, a sapling) survive on each of a few floor blocks, with the tags the
 *   block is in;</li>
 *   <li>for each biome: the nearest patch of it (searched in {@code pv.worldgen.census.range} blocks, default 8000), then
 *   the full chunks {@code pv.worldgen.census.radius} chunks around its middle (default 4): per biome found there, its
 *   columns, how many a propagule could be planted on, the blocks under them, and the trees' blocks above them;</li>
 *   <li>the rocks features placed meanwhile, by the biome at their middle.</li>
 * </ul>
 */
public final class WorldgenCensus {

    /** Blocks between the samples the search for a biome takes, at sea level. */
    private static final int SEARCH_STEP = 32;
    /** How far past the first sample of a biome its patch is looked at, in steps, to find the patch's middle. */
    private static final int PATCH_REACH = 6;
    private static final int TREE_HEIGHT = 40;
    /** Every this many columns of the biome asked for, one is written out as it is. */
    private static final int COLUMN_SAMPLE_SPACING = 1500;
    private static final List<String> FLOORS = List.of("minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:podzol",
            "minecraft:farmland", "minecraft:mud", "minecraft:clay", "minecraft:moss_block", "minecraft:sand", "minecraft:muddy_mangrove_roots",
            "players-versus:gray_mud", "players-versus:brown_mud", "players-versus:gray_clay", "players-versus:brown_clay");
    /** Plants whose survival on a floor block is tried; the mod's own ones too. */
    private static final List<String> PLANTS = List.of("minecraft:mangrove_propagule", "minecraft:oak_sapling", "minecraft:short_grass", "minecraft:fern",
            "minecraft:dandelion", "players-versus:wheat_grass", "players-versus:clovers");
    private static final List<String> TAGS = List.of("supports_mangrove_propagule", "supports_vegetation", "substrate_overworld", "dirt", "mud");

    /** The rocks placed by features while the census runs, by the biome at their middle. */
    private static final Map<String, AtomicInteger> ROCKS = new ConcurrentHashMap<>();
    private static final boolean REQUESTED = isRequested();

    private WorldgenCensus() {
    }

    /** Why rock ground was refused or kept (RockGroundFilter), while the census runs. */
    private static final Map<String, AtomicInteger> ROCK_GROUND = new ConcurrentHashMap<>();

    /** Counts what the rock ground filter said of a place: kept, or why not. */
    public static void rockGround(String outcome) {
        if (REQUESTED) ROCK_GROUND.computeIfAbsent(outcome, key -> new AtomicInteger()).incrementAndGet();
    }

    /** Counts a rock a feature placed (RockFeature), while the census runs. */
    public static void rockPlaced(WorldGenLevel level, BlockPos at) {
        if (!REQUESTED) return;
        String biome = level.getBiome(at).unwrapKey().map(key -> key.identifier().toString()).orElse("?");
        ROCKS.computeIfAbsent(biome, key -> new AtomicInteger()).incrementAndGet();
    }

    /** Headless mode for {@code runWorldgenSmoke}: run once when the server has started, then stop it. */
    public static void registerHeadlessRun() {
        String biomes = System.getProperty("pv.worldgen.census");
        if (biomes == null || biomes.isBlank()) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                int radius = Integer.getInteger("pv.worldgen.census.radius", 4);
                int range = Integer.getInteger("pv.worldgen.census.range", 8000);
                run(server.overworld(), List.of(biomes.trim().split("\\s*,\\s*")), radius, range, VersusMod.MOD_LOGGER::info);
            } catch (Throwable throwable) {
                VersusMod.MOD_LOGGER.error("[pvwg] census failed", throwable);
            } finally {
                server.halt(false);
            }
        });
    }

    /** Whether the census is the headless run's job: the bench then stays out of its way. */
    public static boolean isRequested() {
        String biomes = System.getProperty("pv.worldgen.census");
        return biomes != null && !biomes.isBlank();
    }

    public static void run(ServerLevel world, List<String> biomes, int radius, int range, Consumer<String> log) {
        log.accept("[pvwg] census: generator " + WorldgenBench.describeGenerator(world.getChunkSource().getGenerator()));
        survival(world, log);
        log.accept("[pvwg] census: solid blocks of the mod outside #blocks_motion_no_leaves: " + BlockTagAudit.solidBlocksNotBlockingMotion());
        for (String biome : biomes) {
            String id = biome.contains(":") ? biome : "minecraft:" + biome;
            BlockPos middle = find(world, id, range, log);
            if (middle != null) count(world, id, middle, radius, log);
        }
        // Against the columns of each biome counted above
        log.accept("[pvwg] census rocks placed, by biome: " + new TreeMap<>(ROCKS));
        log.accept("[pvwg] census rock ground, places tried: " + new TreeMap<>(ROCK_GROUND));
    }

    // Survival

    /** Whether plants survive on a block, set in the sky over the spawn chunk, and which tags the block is in. */
    private static void survival(ServerLevel world, Consumer<String> log) {
        world.getChunk(0, 0);
        BlockPos floor = new BlockPos(8, 200, 8);
        for (String name : FLOORS) {
            Block block = block(name);
            if (block == null) {
                log.accept("[pvwg] census survival: " + name + " is not a block");
                continue;
            }
            world.setBlock(floor, block.defaultBlockState(), 2);
            StringBuilder plants = new StringBuilder(), tags = new StringBuilder();
            for (String plant : PLANTS) {
                Block planted = block(plant);
                if (planted == null) continue;
                plants.append(' ').append(planted.defaultBlockState().canSurvive(world, floor.above()) ? '+' : '-').append(plant.substring(plant.indexOf(':') + 1));
            }
            for (String tag : TAGS) {
                if (block.defaultBlockState().is(TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("minecraft", tag)))) tags.append(" #").append(tag);
            }
            BlockState state = block.defaultBlockState();
            log.accept(String.format(Locale.ROOT, "[pvwg] census survival: on %-32s%s  in:%s  heightmaps: ocean floor %b, motion blocking %b, world surface %b", name, plants, tags,
                    Heightmap.Types.OCEAN_FLOOR.isOpaque().test(state), Heightmap.Types.MOTION_BLOCKING.isOpaque().test(state), Heightmap.Types.WORLD_SURFACE.isOpaque().test(state)));
        }
        world.setBlock(floor, Blocks.AIR.defaultBlockState(), 2);
    }

    @Nullable
    private static Block block(String name) {
        Identifier id = Identifier.tryParse(name);
        return id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
    }

    // Finding a biome

    /** The middle of the nearest patch of a biome that the biome source places at sea level, or null. */
    @Nullable
    public static BlockPos find(ServerLevel world, String biome, int range, Consumer<String> log) {
        RandomState noise = world.getChunkSource().randomState();
        DensityBufferPool buffers = noise.acquireDensityBufferPool();
        try {
            Climate.Sampler climate = noise.createClimateSampler(SamplerContext.builder().useBufferArena(buffers).enableCaches().build());
            var resolver = world.getChunkSource().getGenerator().getBiomeSource().createResolver(climate);
            int quartY = QuartPos.fromBlock(world.getSeaLevel());
            int rings = range / SEARCH_STEP;
            for (int ring = 0; ring <= rings; ring++) {
                for (int dx = -ring; dx <= ring; dx++) {
                    for (int dz = -ring; dz <= ring; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                        if (!resolver.getNoiseBiome(QuartPos.fromBlock(dx * SEARCH_STEP), quartY, QuartPos.fromBlock(dz * SEARCH_STEP)).getRegisteredName().equals(biome)) continue;
                        // Found: the sample nearest the middle of the patch around it
                        List<int[]> patch = new ArrayList<>();
                        long sumX = 0, sumZ = 0;
                        for (int px = dx - PATCH_REACH; px <= dx + PATCH_REACH; px++) {
                            for (int pz = dz - PATCH_REACH; pz <= dz + PATCH_REACH; pz++) {
                                if (!resolver.getNoiseBiome(QuartPos.fromBlock(px * SEARCH_STEP), quartY, QuartPos.fromBlock(pz * SEARCH_STEP)).getRegisteredName().equals(biome)) continue;
                                patch.add(new int[]{px, pz});
                                sumX += px;
                                sumZ += pz;
                            }
                        }
                        double meanX = (double) sumX / patch.size(), meanZ = (double) sumZ / patch.size();
                        int[] best = patch.getFirst();
                        for (int[] cell : patch) {
                            if (Math.hypot(cell[0] - meanX, cell[1] - meanZ) < Math.hypot(best[0] - meanX, best[1] - meanZ)) best = cell;
                        }
                        BlockPos middle = new BlockPos(best[0] * SEARCH_STEP, world.getSeaLevel(), best[1] * SEARCH_STEP);
                        log.accept(String.format(Locale.ROOT, "[pvwg] census %s: nearest sample %d,%d, its patch %d samples (every %d blocks) around %d,%d",
                                biome, dx * SEARCH_STEP, dz * SEARCH_STEP, patch.size(), SEARCH_STEP, middle.getX(), middle.getZ()));
                        return middle;
                    }
                }
            }
            log.accept("[pvwg] census " + biome + ": none within " + range + " blocks");
            return null;
        } finally {
            noise.releaseDensityBufferPool(buffers);
        }
    }

    // Counting

    private static final class Tally {
        int columns, plantable;
        final Map<String, Integer> floors = new TreeMap<>(), blocks = new TreeMap<>(), onGround = new TreeMap<>(), floating = new TreeMap<>();
        final List<BlockPos> floatingAt = new ArrayList<>();
    }

    private static void count(ServerLevel world, String biome, BlockPos middle, int radius, Consumer<String> log) {
        ServerChunkCache chunks = world.getChunkSource();
        ChunkPos center = ChunkPos.containing(middle);
        BlockState propagule = Blocks.MANGROVE_PROPAGULE.defaultBlockState();
        Map<String, Tally> tallies = new TreeMap<>();
        List<BlockPos> samples = new ArrayList<>();
        int inBiome = 0;
        int side = (2 * radius + 1) * 16;
        BufferedImage map = new BufferedImage(side, side, BufferedImage.TYPE_INT_RGB);
        int originX = (center.x() - radius) * 16, originZ = (center.z() - radius) * 16;
        for (int chunkX = center.x() - radius; chunkX <= center.x() + radius; chunkX++) {
            for (int chunkZ = center.z() - radius; chunkZ <= center.z() + radius; chunkZ++) {
                chunks.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
                for (int x = chunkX * 16; x < chunkX * 16 + 16; x++) {
                    for (int z = chunkZ * 16; z < chunkZ * 16 + 16; z++) {
                        int top = world.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
                        // The ground is the first block under the trees (their roots, logs, leaves and what hangs from them) that has a collision shape
                        BlockPos ground = new BlockPos(x, top - 1, z);
                        while (ground.getY() > world.getMinY() && !isGround(world, ground)) ground = ground.below();
                        String name = world.getBiome(ground).getRegisteredName();
                        map.setRGB(x - originX, z - originZ, groundColor(world, ground, top));
                        Tally tally = tallies.computeIfAbsent(name, key -> new Tally());
                        tally.columns++;
                        tally.floors.merge(path(world.getBlockState(ground)), 1, Integer::sum);
                        if (propagule.canSurvive(world, ground.above())) tally.plantable++;
                        if (name.equals(biome) && inBiome++ % COLUMN_SAMPLE_SPACING == 0) samples.add(new BlockPos(x, top, z));
                        String plant = path(world.getBlockState(ground.above()));
                        if (!isTree(plant)) tally.onGround.merge(plant, 1, Integer::sum);
                        floating(world, x, z, tally);
                        for (int y = ground.getY() + 1; y <= ground.getY() + TREE_HEIGHT; y++) {
                            String block = path(world.getBlockState(new BlockPos(x, y, z)));
                            if (isTree(block)) tally.blocks.merge(block, 1, Integer::sum);
                        }
                    }
                }
            }
        }
        log.accept(String.format(Locale.ROOT, "[pvwg] census %s: %d chunks around chunk %d,%d, full", biome, (2 * radius + 1) * (2 * radius + 1), center.x(), center.z()));
        tallies.forEach((name, tally) -> {
            log.accept(String.format(Locale.ROOT, "[pvwg] census   %s%s: %d columns, a propagule survives on %d (%.0f%%)",
                    name, name.equals(biome) ? " (the biome asked for)" : "", tally.columns, tally.plantable, 100.0 * tally.plantable / tally.columns));
            log.accept("[pvwg] census     ground: " + tally.floors);
            log.accept("[pvwg] census     on the ground: " + tally.onGround);
            log.accept("[pvwg] census     trees: " + tally.blocks);
            if (!tally.floating.isEmpty()) log.accept("[pvwg] census     floating: " + tally.floating + " at " + tally.floatingAt);
        });
        writeMap(map, biome, log);
        for (BlockPos sample : samples) column(world, sample, log);
        Tally asked = tallies.get(biome.contains(":") ? biome : "minecraft:" + biome);
        if (asked != null) for (int i = 0; i < Math.min(3, asked.floatingAt.size()); i++) column(world, asked.floatingAt.get(i * asked.floatingAt.size() / 3).above(3), log);
    }

    /**
     * The ground as it is once the chunks are full (the bench's maps are of the terrain, before the features): each
     * column's ground block in the bench's colours, shaded by height, and water by its depth.
     */
    private static int groundColor(ServerLevel world, BlockPos ground, int top) {
        int depth = world.getHeight(Heightmap.Types.WORLD_SURFACE, ground.getX(), ground.getZ()) - 1 - ground.getY();
        if (depth > 0 && !world.getFluidState(ground.above()).isEmpty()) {
            return WorldgenBench.Region.scale(0x3F76E4, 1.0F - Math.min(depth, 12) / 12.0F * 0.6F);
        }
        float shade = 0.7F + 0.3F * Math.clamp((ground.getY() - 56) / 24.0F, 0.0F, 1.0F);
        return WorldgenBench.Region.scale(WorldgenBench.Region.baseColor(world.getBlockState(ground)), shade);
    }

    private static void writeMap(BufferedImage map, String biome, Consumer<String> log) {
        try {
            Path dir = FabricLoader.getInstance().getGameDir().resolve("pvwg");
            Files.createDirectories(dir);
            Path file = dir.resolve("census-" + biome.replace(':', '_').replace('/', '_') + ".png");
            ImageIO.write(map, "png", file.toFile());
            log.accept("[pvwg] census map " + file);
        } catch (IOException exception) {
            log.accept("[pvwg] census map not written: " + exception);
        }
    }

    /** How many floating positions are kept to show where, per biome. */
    private static final int FLOATING_SAMPLES = 12;

    /**
     * Ground that floats in this column: a block of the ground (not a tree's, nor a lily pad) over air or water, from the
     * top of the column down to sea level, as a feature that places ground where there is none leaves it (the small
     * swamp mangrove's alter_ground did: its provider answered at any height, so it laid disks two blocks above the
     * ground).
     */
    private static void floating(ServerLevel world, int x, int z, Tally tally) {
        int top = world.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        for (int y = top - 1; y > world.getSeaLevel(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!isGround(world, pos) || world.getBlockState(pos).is(Blocks.LILY_PAD)) continue;
            BlockPos below = pos.below();
            if (!world.getBlockState(below).getCollisionShape(world, below).isEmpty()) continue;
            tally.floating.merge(path(world.getBlockState(pos)), 1, Integer::sum);
            if (tally.floatingAt.size() < FLOATING_SAMPLES) tally.floatingAt.add(pos);
        }
    }

    /** A column as it is: the heightmaps' heights, and the blocks from three above the ocean floor's down to six under it. */
    private static void column(ServerLevel world, BlockPos top, Consumer<String> log) {
        StringBuilder blocks = new StringBuilder();
        for (int y = top.getY() + 3; y >= top.getY() - 6; y--) {
            BlockPos pos = new BlockPos(top.getX(), y, top.getZ());
            BlockState state = world.getBlockState(pos);
            blocks.append(String.format(Locale.ROOT, " %d:%s%s", y, path(state), state.getCollisionShape(world, pos).isEmpty() ? "~" : ""));
        }
        log.accept(String.format(Locale.ROOT, "[pvwg] census column %d,%d: ocean floor %d, world surface %d, motion blocking %d, without leaves %d;%s",
                top.getX(), top.getZ(), world.getHeight(Heightmap.Types.OCEAN_FLOOR, top.getX(), top.getZ()),
                world.getHeight(Heightmap.Types.WORLD_SURFACE, top.getX(), top.getZ()), world.getHeight(Heightmap.Types.MOTION_BLOCKING, top.getX(), top.getZ()),
                world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, top.getX(), top.getZ()), blocks));
    }

    /** Ground, as opposed to a tree and what is above or around it: a block with a collision shape that isn't a tree's. */
    private static boolean isGround(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return !state.getCollisionShape(world, pos).isEmpty() && !isTree(path(state));
    }

    private static String path(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
    }

    /** A tree's blocks (and what grows on them), as opposed to the ground and the air. */
    private static boolean isTree(String path) {
        return path.endsWith("_log") || path.endsWith("_leaves") || path.equals("mangrove_roots") || path.equals("mangrove_propagule")
                || path.equals("vine") || path.equals("moss_carpet") || path.equals("bee_nest") || path.endsWith("_wood");
    }
}
