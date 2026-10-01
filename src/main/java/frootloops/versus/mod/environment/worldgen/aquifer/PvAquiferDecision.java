package frootloops.versus.mod.environment.worldgen.aquifer;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
    LAVA(Blocks.LAVA.defaultBlockState(), false),
    /** Oceans and rivers: floodedness above the water threshold. */
    SEA_WATER(Blocks.WATER.defaultBlockState(), false),
    /** Same, but close to the threshold, so the water gets a fluid tick and can spill into openings. */
    SEA_WATER_TICKING(Blocks.WATER.defaultBlockState(), true),
    /**
     * A wall that keeps sea water out of open space: where sea water could flow in, or the sea's barrier band
     * (floodedness between the barrier and water thresholds) near water. {@link PvAquiferRules#atPosition} names the
     * band itself with it. Solid, like vanilla's barriers: the terrain pass fills it with an ore vein or the default
     * block, and carvers leave it alone.
     */
    SEA_BARRIER(null, false),
    /** Water pooled in a low cave basin. */
    BASIN_WATER(Blocks.WATER.defaultBlockState(), false),
    BASIN_WATER_TICKING(Blocks.WATER.defaultBlockState(), true),
    /** The same for basin water, and the basins' barrier band; solid like {@link #SEA_BARRIER}. */
    BASIN_BARRIER(null, false),
    /**
     * The high river's water at its surface (y 80). It gets a fluid tick and no wall beside it, so where the ground next
     * to it is open it spills over the edge.
     */
    HIGH_RIVER_WATER(Blocks.WATER.defaultBlockState(), true),
    /** The high river's water under its surface, in its bed. */
    HIGH_RIVER_BED_WATER(Blocks.WATER.defaultBlockState(), false),
    /**
     * A wall where the high river's water could flow into open space: beside or under the bed's water, or under the
     * surface's. Solid like {@link #SEA_BARRIER}.
     */
    HIGH_RIVER_BARRIER(null, false),
    /** At or above sea level: open space stays air. */
    AIR_ABOVE_SEA(Blocks.AIR.defaultBlockState(), false),
    /** No rule placed a fluid or barrier; from {@link PvAquiferRules#atPosition}, no water or band there. */
    AIR(Blocks.AIR.defaultBlockState(), false);

    /** The block to place, or {@code null} for solid: whatever the terrain pass or the existing block puts there. */
    @Nullable
    public final BlockState state;
    public final boolean needsFluidTick;

    PvAquiferDecision(@Nullable BlockState state, boolean needsFluidTick) {
        this.state = state;
        this.needsFluidTick = needsFluidTick;
    }
}
