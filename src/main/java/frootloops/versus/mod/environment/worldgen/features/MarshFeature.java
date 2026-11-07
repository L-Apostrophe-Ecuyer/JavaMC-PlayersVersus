package frootloops.versus.mod.environment.worldgen.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import frootloops.versus.mod.environment.CustomBlocks;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderSet;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.synth.Noise;
import net.minecraft.world.level.levelgen.synth.NoiseStack;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

/**
 * Feature type {@code players-versus:marsh}: the ground of a wetland drawn from one smooth noise, its dryness, instead of
 * scattered patches. Run once a chunk (no placement), it goes over the chunk's columns of its biomes:
 * <ul>
 *   <li>near the water line (the floor at most {@link #BAND_BELOW} below it, or {@link #BAND_ABOVE} above), when
 *   {@code reshape}, the dryness shapes the land: water channels where it is low (two deep where it is lowest), land
 *   flush with the water above {@link #LAND}, hummocks a block higher from {@link #HUMMOCK}: grass islands with water
 *   between;</li>
 *   <li>the top of the ground follows the same dryness in rings from the water: gray mud, brown mud, the edge block
 *   (rooted dirt, podzol where it's cold), then grass ({@link #ground}); under the water, gray mud near the land and
 *   brown mud further out;</li>
 *   <li>higher ground, and ground that isn't soil (stone, logs), is left as it is.</li>
 * </ul>
 * Water is only laid where nothing can run out: a channel next to open air at the water line is mud instead.
 *
 * @param biomes   the biomes whose columns it shapes (a chunk can hold others)
 * @param dryness  added to the noise: higher is drier (fewer channels, thinner mud)
 * @param edge     the block between the mud and the grass
 * @param reshape  whether it raises islands and digs channels, or only sets the ground's blocks
 */
public record MarshFeature(HolderSet<Biome> biomes, float dryness, BlockState edge, boolean reshape) implements Feature {

    public static final MapCodec<MarshFeature> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            RegistryCodecs.holderSet(Registries.BIOME).fieldOf("biomes").forGetter(MarshFeature::biomes),
            Codec.FLOAT.optionalFieldOf("dryness", 0.0F).forGetter(MarshFeature::dryness),
            BlockState.CODEC.fieldOf("edge").forGetter(MarshFeature::edge),
            Codec.BOOL.optionalFieldOf("reshape", true).forGetter(MarshFeature::reshape)
    ).apply(instance, MarshFeature::new));

    /** Land from this dryness; water channels below it. */
    public static final float LAND = -0.05F;
    /** Channels two deep below this. */
    public static final float DEEP = LAND - 0.3F;
    /** Hummocks a block above the water from this dryness. */
    public static final float HUMMOCK = 0.45F;
    /** The rings' widths, in dryness, from the water out: gray mud, brown mud, the edge block; grass beyond. */
    public static final float SHORE_WIDTH = 0.08F, MUD_WIDTH = 0.1F, EDGE_WIDTH = 0.08F;
    /** Under the water, gray mud this close (in dryness) to land, brown mud further out. */
    public static final float SHALLOWS = 0.15F;
    /** The band of columns reshaped: the floor from this far below the water line to this far above it. */
    public static final int BAND_BELOW = 3, BAND_ABOVE = 1;
    /** Ground higher than this above the water line keeps its blocks. */
    public static final int GROUND_ABOVE = 2;

    /** The ground's rings, wettest first. */
    public enum Ground { CHANNEL, SHORE, MUD, EDGE, GRASS }

    /** Which ring this dryness falls in. */
    public static Ground ground(float dryness) {
        if (dryness < LAND) return Ground.CHANNEL;
        if (dryness < LAND + SHORE_WIDTH) return Ground.SHORE;
        if (dryness < LAND + SHORE_WIDTH + MUD_WIDTH) return Ground.MUD;
        if (dryness < LAND + SHORE_WIDTH + MUD_WIDTH + EDGE_WIDTH) return Ground.EDGE;
        return Ground.GRASS;
    }

    /** The top of the ground's height for this dryness, against the water line (whose water is at {@code waterLine}). */
    public static int surface(float dryness, int waterLine) {
        if (dryness < DEEP) return waterLine - 2;
        if (dryness < LAND) return waterLine - 1;
        return dryness >= HUMMOCK ? waterLine + 1 : waterLine;
    }

    private static final Map<Long, Noise> NOISES = new ConcurrentHashMap<>();

    /** The dryness noise of a world: islands some twenty blocks across, with smaller lobes. */
    public static float noise(long seed, int x, int z) {
        Noise noise = NOISES.computeIfAbsent(seed, s -> {
            WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(s ^ 0x6D617273684C616EL));
            return NoiseStack.builder()
                    .add(new SimplexNoise(random, true), 1.0 / 28.0, 0.7F)
                    .add(new SimplexNoise(random, true), 1.0 / 11.0, 0.3F)
                    .build();
        });
        return noise.get(x, z);
    }

    @Override
    public MapCodec<MarshFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int waterLine = level.getSeaLevel() - 1;
        int chunkX = SectionPos.blockToSectionCoord(origin.getX()) << 4, chunkZ = SectionPos.blockToSectionCoord(origin.getZ()) << 4;
        long seed = level.getSeed();
        boolean changed = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunkX; x < chunkX + 16; x++) {
            for (int z = chunkZ; z < chunkZ + 16; z++) {
                int floor = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                if (floor > waterLine + GROUND_ABOVE) continue;
                pos.set(x, floor, z);
                if (!this.biomes.contains(level.getBiome(pos)) || !isSoil(level.getBlockState(pos))) continue;
                float dryness = noise(seed, x, z) + this.dryness;
                Ground ground = ground(dryness);
                if (this.reshape && floor >= waterLine - BAND_BELOW && floor <= waterLine + BAND_ABOVE) {
                    changed |= this.reshapeColumn(level, x, z, floor, waterLine, dryness, ground);
                } else {
                    boolean underWater = !level.getFluidState(pos.above()).isEmpty();
                    BlockState top = underWater ? floorUnderWater(dryness) : this.ring(ground == Ground.CHANNEL ? Ground.SHORE : ground);
                    changed |= set(level, pos, top);
                }
            }
        }
        return changed;
    }

    /** Raises or lowers this column to its dryness's height, and sets its top. */
    private boolean reshapeColumn(WorldGenLevel level, int x, int z, int floor, int waterLine, float dryness, Ground ground) {
        int target = surface(dryness, waterLine);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, floor, z);
        boolean channel = ground == Ground.CHANNEL;
        // A channel must hold its water: where open air touches the water line beside it, it stays mud at the line
        if (channel && opensToAir(level, x, waterLine, z)) {
            target = waterLine;
            channel = false;
            ground = Ground.SHORE;
        }
        // What grew on the old top goes (the trees come after)
        BlockPos above = new BlockPos(x, floor + 1, z);
        if (!level.getBlockState(above).isAir() && level.getFluidState(above).isEmpty() && level.getBlockState(above).canBeReplaced()) {
            level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (int y = Math.min(floor, target) + 1; y <= Math.max(floor, target); y++) {
            pos.setY(y);
            if (y < target) set(level, pos, Blocks.DIRT.defaultBlockState()); // raised
            else if (y > target) set(level, pos, y <= waterLine ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState()); // lowered
        }
        pos.setY(target);
        BlockState top = channel ? floorUnderWater(dryness) : this.ring(ground);
        set(level, pos, top);
        return true;
    }

    private static boolean opensToAir(WorldGenLevel level, int x, int y, int z) {
        BlockPos center = new BlockPos(x, y, z);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(center.relative(direction)).isAir()) return true;
        }
        return false;
    }

    private BlockState ring(Ground ground) {
        return switch (ground) {
            case CHANNEL, SHORE -> CustomBlocks.GRAY_MUD.defaultBlockState();
            case MUD -> CustomBlocks.BROWN_MUD.defaultBlockState();
            case EDGE -> this.edge;
            case GRASS -> Blocks.GRASS_BLOCK.defaultBlockState();
        };
    }

    private static BlockState floorUnderWater(float dryness) {
        return dryness >= LAND - SHALLOWS ? CustomBlocks.GRAY_MUD.defaultBlockState() : CustomBlocks.BROWN_MUD.defaultBlockState();
    }

    private static boolean set(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos) == state) return false;
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        return true;
    }

    /** Ground a marsh may reshape: soil, sand and gravel, mud and clay; not stone, nor what grows on it. */
    private static boolean isSoil(BlockState state) {
        Block block = state.getBlock();
        return block == Blocks.GRASS_BLOCK || block == Blocks.DIRT || block == Blocks.COARSE_DIRT || block == Blocks.ROOTED_DIRT
                || block == Blocks.PODZOL || block == Blocks.MOSS_BLOCK || block == Blocks.SAND || block == Blocks.GRAVEL
                || block == Blocks.CLAY || block == Blocks.MUD || block == CustomBlocks.BROWN_MUD || block == CustomBlocks.GRAY_MUD
                || block == CustomBlocks.BROWN_CLAY || block == CustomBlocks.GRAY_CLAY;
    }
}
