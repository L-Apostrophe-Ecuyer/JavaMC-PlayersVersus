package frootloops.versus.mod.environment.worldgen.rocks;

import com.mojang.serialization.MapCodec;
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
 * Placement filter {@code players-versus:rock_ground}, no settings: keeps a position where rock may stand
 * ({@link RockGround}): in the Players Versus world type only (vanilla world types keep vanilla's worldgen), in a
 * climate for rocks, on level ground with no water at its middle.
 */
public record RockGroundFilter() implements PlacementFilter {

    public static final MapCodec<RockGroundFilter> CODEC = MapCodec.unit(new RockGroundFilter());

    @Override
    public boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos origin) {
        if (!(context.generator().getBiomeSource() instanceof PvBiomeSource)) return false;
        // A sampler without caches: nothing kept between the generator's threads
        Climate.TargetPoint climate = context.level().getLevel().getChunkSource().randomState().createClimateSampler(SamplerContext.EMPTY_UNCACHED)
                .sample(QuartPos.fromBlock(origin.getX()), QuartPos.fromBlock(origin.getY()), QuartPos.fromBlock(origin.getZ()));
        return climate(climate) && ground(context.level(), origin.getX(), origin.getZ());
    }

    /** Whether rock may stand in this climate. */
    static boolean climate(Climate.TargetPoint climate) {
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
