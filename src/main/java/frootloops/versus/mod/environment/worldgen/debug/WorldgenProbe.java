package frootloops.versus.mod.environment.worldgen.debug;

import frootloops.versus.mod.environment.worldgen.PvWorldgen;
import frootloops.versus.mod.environment.worldgen.aquifer.Lattice;
import frootloops.versus.mod.environment.worldgen.aquifer.PvAquiferDecision;
import frootloops.versus.mod.environment.worldgen.aquifer.PvAquiferRules;
import frootloops.versus.mod.environment.worldgen.biome.PvBiomeSource;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
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

import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.BASIN_MAX_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.BASIN_MIN_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.SEA_BAND_MIN_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.SEA_LEVEL;

/**
 * {@code /pvwg probe}: what the generator thinks about the block you're standing on.
 *
 * <p>Density functions are sampled raw at the block (no cell interpolation, no structure terrain adaptation), so
 * values near a boundary can differ slightly from what chunk generation produced. The aquifer line shows the lattice
 * values the aquifer uses, and the exact values next to them.
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
        if (generator.getBiomeSource() instanceof PvBiomeSource pvBiomeSource) {
            lines.add("biome rule " + pvBiomeSource.ruleAt(point));
        }
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
                ChunkPos chunkPos = new ChunkPos(pos);
                Lattice floodednessLattice = new Lattice(floodedness, chunkPos, SEA_BAND_MIN_Y, SEA_LEVEL);
                Lattice spreadLattice = new Lattice(spread, chunkPos, BASIN_MIN_Y, BASIN_MAX_Y);
                boolean lavaLevel = pos.getY() < Math.min(VANILLA_LAVA_LEVEL, settings.seaLevel());
                PvAquiferDecision decision = PvAquiferRules.decide(noisePos, finalDensity, lavaLevel, floodednessLattice, spreadLattice);
                PvAquiferDecision exact = PvAquiferRules.decide(noisePos, finalDensity, lavaLevel, floodedness::sample, spread::sample);
                lines.add(String.format(Locale.ROOT, "aquifer floodedness %.4f (exact %.4f)  spread %.4f (exact %.4f)  ->  %s%s",
                        floodednessLattice.applyAsDouble(noisePos), floodedness.sample(noisePos),
                        spreadLattice.applyAsDouble(noisePos), spread.sample(noisePos),
                        decision, decision == exact ? "" : " (exact values would give " + exact + ")"));
            } else {
                lines.add("aquifer: vanilla (not a Players Versus generator)");
            }
        }
        return lines;
    }
}
