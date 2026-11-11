package frootloops.versus.mod.environment.worldgen.rocks;

import frootloops.versus.mod.environment.worldgen.debug.WorldgenCensus;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import frootloops.versus.mod.environment.worldgen.biome.PvBiomeSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;

/**
 * Placement filter {@code players-versus:rock_ground}: keeps a position where rock may stand ({@link RockGround}): in
 * the Players Versus world type only (vanilla world types keep vanilla's worldgen), on level ground with no water at its
 * middle, and in a climate for rocks unless {@code "climate": false} (a meadow's cluster: meadows are cool, high and
 * little eroded, yet a few rocks belong there).
 */
public record RockGroundFilter(boolean climate) implements PlacementFilter {

    public static final MapCodec<RockGroundFilter> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("climate", true).forGetter(RockGroundFilter::climate)
    ).apply(instance, RockGroundFilter::new));

    @Override
    public boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos origin) {
        if (!(context.generator().getBiomeSource() instanceof PvBiomeSource)) return false;
        if (this.climate) {
            // A sampler without caches: nothing kept between the generator's threads
            Climate.TargetPoint climate = context.level().getLevel().getChunkSource().randomState().createClimateSampler(SamplerContext.EMPTY_UNCACHED)
                    .sample(QuartPos.fromBlock(origin.getX()), QuartPos.fromBlock(origin.getY()), QuartPos.fromBlock(origin.getZ()));
            if (!suits(climate)) {
                WorldgenCensus.rockGround(why(climate));
                return false;
            }
        }
        boolean ground = ground(context.level(), origin.getX(), origin.getZ());
        WorldgenCensus.rockGround(ground ? "kept" : "not level, or wet");
        return ground;
    }

    /** Which of the climate's rules refuses rock here, for the census. */
    private static String why(Climate.TargetPoint climate) {
        float temperature = Climate.unquantizeCoord(climate.temperature()), erosion = Climate.unquantizeCoord(climate.erosion());
        if (temperature < -0.15F || temperature > 0.55F) return "temperature";
        return erosion <= -0.3F ? "erosion" : "3D-noise climate";
    }

    /** Whether rock may stand in this climate. */
    static boolean suits(Climate.TargetPoint climate) {
        return RockGround.climate(Climate.unquantizeCoord(climate.temperature()), Climate.unquantizeCoord(climate.humidity()), Climate.unquantizeCoord(climate.erosion()));
    }

    /** Whether the ground at (x, z) is level ({@link RockGround#level}) and has no water over its middle. */
    public static boolean ground(WorldGenLevel level, int x, int z) {
        int[] heights = new int[RockGround.AROUND.length];
        for (int index = 0; index < heights.length; index++) {
            heights[index] = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x + RockGround.AROUND[index][0], z + RockGround.AROUND[index][1]);
        }
        if (level.getFluidState(new BlockPos(x, heights[0], z)).is(FluidTags.WATER)) return false;
        return RockGround.level(heights);
    }

    @Override
    public MapCodec<RockGroundFilter> codec() {
        return CODEC;
    }
}
