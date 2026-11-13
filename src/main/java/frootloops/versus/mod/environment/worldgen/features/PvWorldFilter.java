package frootloops.versus.mod.environment.worldgen.features;

import com.mojang.serialization.MapCodec;
import frootloops.versus.mod.environment.worldgen.biome.PvBiomeSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;

/**
 * Placement filter {@code players-versus:players_versus_world}: keeps a position in the Players Versus world type only
 * (its biome source, {@link PvBiomeSource}), so that what the mod adds to vanilla's biomes (the river banks) stays out of
 * vanilla's world types, which keep vanilla's worldgen.
 */
public final class PvWorldFilter implements PlacementFilter {

    public static final PvWorldFilter INSTANCE = new PvWorldFilter();
    public static final MapCodec<PvWorldFilter> CODEC = MapCodec.unit(INSTANCE);

    private PvWorldFilter() {
    }

    @Override
    public boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos origin) {
        return context.generator().getBiomeSource() instanceof PvBiomeSource;
    }

    @Override
    public MapCodec<PvWorldFilter> codec() {
        return CODEC;
    }
}
