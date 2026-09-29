package frootloops.versus.mod.environment.worldgen.debug;

import frootloops.versus.mod.environment.worldgen.PvWorldgen;
import frootloops.versus.mod.environment.worldgen.aquifer.AquiferInputs;
import frootloops.versus.mod.environment.worldgen.aquifer.PvAquifer;
import frootloops.versus.mod.environment.worldgen.aquifer.PvAquiferDecision;
import frootloops.versus.mod.environment.worldgen.biome.PvBiomeSource;
import frootloops.versus.mod.environment.worldgen.density.PvHighRiver;
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
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * {@code /pvwg probe}: what the generator thinks about the block you're standing on.
 *
 * <p>Density functions are sampled raw at the block (no cell interpolation, no structure terrain adaptation), so
 * values near a boundary can differ slightly from what chunk generation produced. The aquifer line shows the values
 * the aquifer uses (smooth inputs from its lattice) next to the exact ones.
 */
public final class WorldgenProbe {

    /** Mirrors vanilla's {@code NoiseChunkGenerator.createFluidLevelSampler}: lava below this y. */
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
        List<String> lines = new ArrayList<>();
        ServerChunkCache chunkManager = world.getChunkSource();
        ChunkGenerator generator = chunkManager.getGenerator();
        RandomState noiseConfig = chunkManager.randomState();
        Climate.Sampler climate = noiseConfig.sampler();

        lines.add(String.format(Locale.ROOT, "[pvwg] %d %d %d, settings %s",
                pos.getX(), pos.getY(), pos.getZ(), WorldgenBench.describeGenerator(generator)));

        int quartX = QuartPos.fromBlock(pos.getX());
        int quartY = QuartPos.fromBlock(pos.getY());
        int quartZ = QuartPos.fromBlock(pos.getZ());
        Holder<Biome> sourceBiome = generator.getBiomeSource().getNoiseBiome(quartX, quartY, quartZ, climate);
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

        NoiseRouter router = noiseConfig.router();
        DensityFunction.FunctionContext noisePos = new DensityFunction.SinglePointContext(pos.getX(), pos.getY(), pos.getZ());
        double finalDensity = router.finalDensity().compute(noisePos);
        lines.add(String.format(Locale.ROOT, "density final %.4f  depth %.4f  continents %.4f  erosion %.4f  ridges %.4f",
                finalDensity,
                router.depth().compute(noisePos),
                router.continents().compute(noisePos),
                router.erosion().compute(noisePos),
                router.ridges().compute(noisePos)));

        if (generator instanceof NoiseBasedChunkGenerator noiseGenerator) {
            NoiseGeneratorSettings settings = noiseGenerator.generatorSettings().value();
            if (PvWorldgen.isPvGenerator(settings)) {
                DensityFunction floodedness = router.fluidLevelFloodednessNoise();
                DensityFunction spread = router.fluidLevelSpreadNoise();
                boolean lavaLevel = pos.getY() < Math.min(VANILLA_LAVA_LEVEL, settings.seaLevel());
                PvAquifer aquifer = new PvAquifer(AquiferInputs.of(noiseConfig, settings), router.depth(), new ChunkPos(pos),
                        (x, y, z) -> { throw new UnsupportedOperationException("the probe passes lavaLevel itself"); });
                PvAquiferDecision decision = aquifer.decide(noisePos, finalDensity, lavaLevel);
                // its own floodedness says water, a barrier band or nothing; the walls come from its neighbours
                lines.add(String.format(Locale.ROOT, "aquifer floodedness %.4f (exact %.4f)  spread %.4f (exact %.4f)  ->  own %s, placed %s",
                        aquifer.floodedness(noisePos), floodedness.compute(noisePos),
                        aquifer.spread(noisePos), spread.compute(noisePos), aquifer.atPosition(pos.getX(), pos.getY(), pos.getZ()), decision));
                PvHighRiver river = AquiferInputs.of(noiseConfig, settings).highRiver();
                if (river != null) {
                    // exact values here; the terrain pass and the aquifer interpolate them on the cell grid
                    double depth = river.depth().compute(noisePos);
                    lines.add(String.format(Locale.ROOT, "high river channel %.4f  depth at y 80 %.4f  terrain at y 80 %.4f  ->  half width"
                                    + " here %.4f, valley %.4f", river.channel().compute(noisePos), depth, river.terrain().compute(noisePos),
                            PvHighRiver.activity(depth) * PvHighRiver.fullHalfWidth(pos.getY()), river.compute(noisePos)));
                }
            } else {
                lines.add("aquifer: vanilla (not a Players Versus generator)");
            }
        }
        return lines;
    }
}
