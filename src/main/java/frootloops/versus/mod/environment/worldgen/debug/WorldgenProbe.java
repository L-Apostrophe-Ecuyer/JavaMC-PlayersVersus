package frootloops.versus.mod.environment.worldgen.debug;

import frootloops.versus.mod.environment.worldgen.PvWorldgen;
import frootloops.versus.mod.environment.worldgen.aquifer.PvAquiferDecision;
import frootloops.versus.mod.environment.worldgen.aquifer.PvAquiferRules;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.noise.NoiseRouter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * {@code /pvwg probe}: what the generator thinks about the block you're standing on.
 *
 * <p>Density functions are sampled raw at the block (no cell interpolation, no structure terrain adaptation), so
 * values near a boundary can differ slightly from what chunk generation produced.
 */
public final class WorldgenProbe {

    /** Mirrors vanilla's {@code NoiseChunkGenerator.createFluidLevelSampler}: lava below this y. */
    private static final int VANILLA_LAVA_LEVEL = -54;

    private WorldgenProbe() {
    }

    static int run(ServerCommandSource source) {
        BlockPos pos = BlockPos.ofFloored(source.getPosition());
        for (String line : describe(source.getWorld(), pos)) {
            source.sendFeedback(() -> Text.literal(line), false);
        }
        return 1;
    }

    public static List<String> describe(ServerWorld world, BlockPos pos) {
        List<String> lines = new ArrayList<>();
        ServerChunkManager chunkManager = world.getChunkManager();
        ChunkGenerator generator = chunkManager.getChunkGenerator();
        NoiseConfig noiseConfig = chunkManager.getNoiseConfig();
        MultiNoiseUtil.MultiNoiseSampler climate = noiseConfig.getMultiNoiseSampler();

        lines.add(String.format(Locale.ROOT, "[pvwg] %d %d %d, settings %s",
                pos.getX(), pos.getY(), pos.getZ(), WorldgenBench.describeGenerator(generator)));

        int quartX = BiomeCoords.fromBlock(pos.getX());
        int quartY = BiomeCoords.fromBlock(pos.getY());
        int quartZ = BiomeCoords.fromBlock(pos.getZ());
        RegistryEntry<Biome> sourceBiome = generator.getBiomeSource().getBiome(quartX, quartY, quartZ, climate);
        lines.add("biome " + sourceBiome.getIdAsString() + " (stored in chunk: " + world.getBiome(pos).getIdAsString() + ")");

        MultiNoiseUtil.NoiseValuePoint point = climate.sample(quartX, quartY, quartZ);
        lines.add(String.format(Locale.ROOT, "climate T %.3f  H %.3f  C %.3f  E %.3f  D %.3f  W %.3f",
                MultiNoiseUtil.toFloat(point.temperatureNoise()),
                MultiNoiseUtil.toFloat(point.humidityNoise()),
                MultiNoiseUtil.toFloat(point.continentalnessNoise()),
                MultiNoiseUtil.toFloat(point.erosionNoise()),
                MultiNoiseUtil.toFloat(point.depth()),
                MultiNoiseUtil.toFloat(point.weirdnessNoise())));

        NoiseRouter router = noiseConfig.getNoiseRouter();
        DensityFunction.NoisePos noisePos = new DensityFunction.UnblendedNoisePos(pos.getX(), pos.getY(), pos.getZ());
        double finalDensity = router.finalDensity().sample(noisePos);
        lines.add(String.format(Locale.ROOT, "density final %.4f  depth %.4f  continents %.4f  erosion %.4f  ridges %.4f",
                finalDensity,
                router.depth().sample(noisePos),
                router.continents().sample(noisePos),
                router.erosion().sample(noisePos),
                router.ridges().sample(noisePos)));

        if (generator instanceof NoiseChunkGenerator noiseGenerator) {
            ChunkGeneratorSettings settings = noiseGenerator.getSettings().value();
            if (PvWorldgen.isPvGenerator(settings)) {
                DensityFunction floodedness = router.fluidLevelFloodednessNoise();
                DensityFunction spread = router.fluidLevelSpreadNoise();
                boolean lavaLevel = pos.getY() < Math.min(VANILLA_LAVA_LEVEL, settings.seaLevel());
                PvAquiferDecision decision = PvAquiferRules.decide(noisePos, finalDensity, lavaLevel, floodedness, spread);
                lines.add(String.format(Locale.ROOT, "aquifer floodedness %.4f  spread %.4f  ->  %s",
                        floodedness.sample(noisePos), spread.sample(noisePos), decision));
            } else {
                lines.add("aquifer: vanilla (not a Players Versus generator)");
            }
        }
        return lines;
    }
}
