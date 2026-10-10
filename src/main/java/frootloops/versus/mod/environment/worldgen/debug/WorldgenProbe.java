package frootloops.versus.mod.environment.worldgen.debug;

import frootloops.versus.mod.environment.worldgen.PvWorldgen;
import frootloops.versus.mod.environment.worldgen.aquifer.PvAquifer;
import frootloops.versus.mod.environment.worldgen.aquifer.PvAquiferDecision;
import frootloops.versus.mod.environment.worldgen.biome.PvBiomeSource;
import frootloops.versus.mod.environment.worldgen.density.AquiferFloodedness;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityBufferPool;
import net.minecraft.world.level.levelgen.densityfunction.DensitySamplerSet;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

/**
 * {@code /pvwg probe}: what the generator thinks about the block you're standing on.
 *
 * <p>Density functions are sampled exactly at the block (no structure terrain adaptation, no blending), so values near
 * a boundary can differ slightly from what chunk generation produced. The aquifer line shows the values the aquifer
 * uses (smooth inputs from its lattice) next to the exact ones.
 */
public final class WorldgenProbe {

    /** Mirrors vanilla's {@code NoiseBasedChunkGenerator.createFluidPicker}: lava below this y. */
    private static final int VANILLA_LAVA_LEVEL = -54;

    private WorldgenProbe() {
    }

    static int run(CommandSourceStack source) {
        BlockPos pos = BlockPos.containing(source.getPosition());
        for (String line : describe(source.getLevel(), pos)) {
            source.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    public static List<String> describe(ServerLevel world, BlockPos pos) {
        RandomState noiseConfig = world.getChunkSource().randomState();
        DensityBufferPool buffers = noiseConfig.acquireDensityBufferPool();
        try {
            return describe(world, pos, noiseConfig, SamplerContext.builder().useBufferArena(buffers).enableCaches().build());
        } finally {
            noiseConfig.releaseDensityBufferPool(buffers);
        }
    }

    private static List<String> describe(ServerLevel world, BlockPos pos, RandomState noiseConfig, SamplerContext context) {
        List<String> lines = new ArrayList<>();
        ServerChunkCache chunkManager = world.getChunkSource();
        ChunkGenerator generator = chunkManager.getGenerator();
        Climate.Sampler climate = noiseConfig.createClimateSampler(context);
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        lines.add(String.format(Locale.ROOT, "[pvwg] %d %d %d, settings %s", x, y, z, WorldgenBench.describeGenerator(generator)));

        int quartX = QuartPos.fromBlock(x);
        int quartY = QuartPos.fromBlock(y);
        int quartZ = QuartPos.fromBlock(z);
        Holder<Biome> sourceBiome = generator.getBiomeSource().createResolver(climate).getNoiseBiome(quartX, quartY, quartZ);
        lines.add("biome " + sourceBiome.getRegisteredName() + " (stored in chunk: " + world.getBiome(pos).getRegisteredName() + ")");

        Climate.TargetPoint point = climate.sample(quartX, quartY, quartZ);
        if (generator.getBiomeSource() instanceof PvBiomeSource pvBiomeSource) {
            lines.add("biome rule " + pvBiomeSource.ruleAt(point));
        }
        lines.add(String.format(Locale.ROOT, "climate T %.3f  H %.3f  C %.3f  E %.3f  D %.3f  W %.3f",
                Climate.unquantizeCoord(point.temperature()),
                Climate.unquantizeCoord(point.humidity()),
                Climate.unquantizeCoord(point.continentalness()),
                Climate.unquantizeCoord(point.erosion()),
                Climate.unquantizeCoord(point.depth()),
                Climate.unquantizeCoord(point.weirdness())));

        if (!(generator instanceof NoiseBasedChunkGenerator noiseGenerator)) return lines;
        NoiseGeneratorSettings settings = noiseGenerator.generatorSettings().value();
        NoiseRouter router = settings.noiseRouter();
        float finalDensity = noiseConfig.sampleBlockValueUncached(router.finalDensity(), x, y, z);
        lines.add(String.format(Locale.ROOT, "density final %.4f  depth %.4f  continents %.4f  erosion %.4f  ridges %.4f",
                finalDensity,
                noiseConfig.sampleBlockValueUncached(router.depth(), x, y, z),
                noiseConfig.sampleBlockValueUncached(router.continents(), x, y, z),
                noiseConfig.sampleBlockValueUncached(router.erosion(), x, y, z),
                noiseConfig.sampleBlockValueUncached(router.ridges(), x, y, z)));

        if (!PvWorldgen.isPvGenerator(settings)) {
            lines.add("aquifer: vanilla (not a Players Versus generator)");
            return lines;
        }
        Aquifer.Config config = settings.aquifers().orElseThrow();
        ChunkPos chunk = ChunkPos.containing(pos);
        DensityVolume volume = new DensityVolume(16, world.getHeight(), 16, chunk.getMinBlockX(), world.getMinY(), chunk.getMinBlockZ(), 1, 1, 1);
        DensitySamplerSet samplers = noiseConfig.samplersWithContext(context);
        boolean lavaLevel = y < Math.min(VANILLA_LAVA_LEVEL, settings.seaLevel());
        PvAquifer aquifer = PvAquifer.create(config, samplers, volume,
                (fx, fy, fz) -> new Aquifer.FluidStatus(settings.seaLevel(), Blocks.WATER.defaultBlockState()));
        PvAquiferDecision decision = aquifer.decide(x, y, z, finalDensity, lavaLevel);
        // its own floodedness says water, a barrier band or nothing; the walls come from its neighbours
        lines.add(String.format(Locale.ROOT, "aquifer floodedness %.4f (exact %.4f)  spread %.4f (exact %.4f)  ->  own %s, placed %s",
                aquifer.floodedness(x, y, z), noiseConfig.sampleBlockValueUncached(config.fluidLevelFloodednessNoise(), x, y, z),
                aquifer.spread(x, y, z), noiseConfig.sampleBlockValueUncached(config.fluidLevelSpreadNoise(), x, y, z),
                aquifer.atPosition(x, y, z), decision));
        if (PvWorldgen.unwrap(config.fluidLevelFloodednessNoise()) instanceof AquiferFloodedness floodedness) {
            // the valleys the final density takes the minimum with: negative where they open the block
            lines.add(String.format(Locale.ROOT, "high river valley %.4f, upper layer %.4f, flooded corridors %.4f, dry paths %.4f",
                    noiseConfig.sampleBlockValueUncached(floodedness.highRiver(), x, y, z),
                    noiseConfig.sampleBlockValueUncached(floodedness.upperHighRiver(), x, y, z), aquifer.corridor(x, y, z),
                    aquifer.dryPath(x, y, z)));
        }
        return lines;
    }
}
