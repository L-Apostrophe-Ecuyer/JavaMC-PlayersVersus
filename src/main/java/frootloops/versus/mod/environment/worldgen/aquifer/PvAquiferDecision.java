package frootloops.versus.mod.environment.worldgen.aquifer;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import org.jetbrains.annotations.Nullable;

/**
 * What the Players Versus aquifer decided for one position, and why.
 *
 * <p>Each value says which rule fired, so {@code /pvwg probe} can explain a decision and the aquifer itself doesn't
 * allocate anything per block.
 */
public enum PvAquiferDecision {
    /** Terrain density is positive: leave the block to the terrain (stone, ore veins, ...). */
    SOLID(null, false),
    /** Below the lava level of the chunk generator's fluid sampler. */
    LAVA(Blocks.LAVA.getDefaultState(), false),
    /** Oceans and rivers: floodedness above the water threshold. */
    SEA_WATER(Blocks.WATER.getDefaultState(), false),
    /** Same, but close to the threshold, so the water gets a fluid tick and can spill into openings. */
    SEA_WATER_TICKING(Blocks.WATER.getDefaultState(), true),
    /**
     * Between the barrier and water thresholds: a wall that keeps sea water out of caves. Solid, like vanilla's
     * barriers: the terrain pass fills it with an ore vein or the default block, and carvers leave it alone.
     */
    SEA_BARRIER(null, false),
    /** Water pooled in a low cave basin. */
    BASIN_WATER(Blocks.WATER.getDefaultState(), false),
    BASIN_WATER_TICKING(Blocks.WATER.getDefaultState(), true),
    /** Wall around a cave basin; solid like {@link #SEA_BARRIER}. */
    BASIN_BARRIER(null, false),
    /** At or above sea level: open space stays air. */
    AIR_ABOVE_SEA(Blocks.AIR.getDefaultState(), false),
    /** No rule placed a fluid or barrier. */
    AIR(Blocks.AIR.getDefaultState(), false);

    /** The block to place, or {@code null} for solid: whatever the terrain pass or the existing block puts there. */
    @Nullable
    public final BlockState state;
    public final boolean needsFluidTick;

    PvAquiferDecision(@Nullable BlockState state, boolean needsFluidTick) {
        this.state = state;
        this.needsFluidTick = needsFluidTick;
    }
}
