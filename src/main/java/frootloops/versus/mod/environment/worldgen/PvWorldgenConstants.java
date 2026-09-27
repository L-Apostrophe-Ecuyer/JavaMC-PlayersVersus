package frootloops.versus.mod.environment.worldgen;

/**
 * Numbers that more than one part of the Players Versus world type depends on.
 *
 * <p>Keep every value that the aquifer, the density functions and the surface rules must agree on here, so a
 * change in one place can't silently break another. Until the density functions move to Java (plan phases 2–3),
 * some of these values are duplicated in {@code data/players-versus/worldgen/**}; those spots are named below.
 */
public final class PvWorldgenConstants {

    private PvWorldgenConstants() {
    }

    // ------------------------------------------------------------------------------------------------------------
    // Sea level
    // ------------------------------------------------------------------------------------------------------------

    /**
     * The aquifer fills water strictly below this y, so the ocean surface is at y 63.
     *
     * <p>Note: the noise settings still declare {@code sea_level: 63} (vanilla meaning: water up to y 62). Aligning
     * the two is quirk Q5 in {@code docs/worldgen-refactor-plan.md}; the JSON {@code range_choice} bound in
     * {@code aquifer_floodedness_oceans_and_rivers_y64.json} also uses 64.
     */
    public static final int SEA_LEVEL = 64;

    // ------------------------------------------------------------------------------------------------------------
    // Aquifer: sea-level water (oceans, rivers)
    // ------------------------------------------------------------------------------------------------------------

    /** Sea-level flooding is only evaluated strictly above this y. Below it, open space stays dry (or lava). */
    public static final int SEA_BAND_MIN_Y = -32;

    /**
     * Floodedness above this is water. Also the upper bound of the {@code range_choice} in
     * {@code aquifer_fluid_level_floodedness.json}.
     */
    public static final double SEA_WATER_THRESHOLD = 0.34;

    /**
     * Floodedness above this (and below {@link #SEA_WATER_THRESHOLD}) is a stone barrier. Also the lower bound of
     * the {@code range_choice} in {@code aquifer_fluid_level_floodedness.json}.
     */
    public static final double SEA_BARRIER_THRESHOLD = 0.0001;

    /** From this y upward, the barrier threshold rises so that barriers thin out near the sea surface. */
    public static final int SEA_BARRIER_RAMP_START_Y = 60;

    /** How much the barrier threshold rises per block above {@link #SEA_BARRIER_RAMP_START_Y}. */
    public static final double SEA_BARRIER_RAMP_PER_BLOCK = 0.015;

    // ------------------------------------------------------------------------------------------------------------
    // Aquifer: cave basins
    // ------------------------------------------------------------------------------------------------------------

    /** Cave basins are evaluated strictly between these two y levels. */
    public static final int BASIN_MIN_Y = -4;
    public static final int BASIN_MAX_Y = 32;

    /** Basin barriers only form strictly below this y. */
    public static final int BASIN_BARRIER_MAX_Y = 23;

    /** Spread above this is basin water (above {@link #BASIN_WATER_RAMP_Y}). */
    public static final double BASIN_WATER_THRESHOLD = 0.5;

    /** Below this y, the basin water threshold drops by {@link #BASIN_WATER_RAMP_PER_BLOCK} per block. */
    public static final int BASIN_WATER_RAMP_Y = 8;
    public static final double BASIN_WATER_RAMP_PER_BLOCK = 0.08;

    /** Below this y the basin barrier threshold is {@link #SEA_BARRIER_THRESHOLD}; above it, it rises per block. */
    public static final int BASIN_BARRIER_RAMP_Y = 12;
    public static final double BASIN_BARRIER_RAMP_PER_BLOCK = 0.06;

    // ------------------------------------------------------------------------------------------------------------
    // Aquifer: fluid ticks
    // ------------------------------------------------------------------------------------------------------------

    /** Water within this margin above its threshold schedules a fluid tick, so it can flow into nearby openings. */
    public static final double FLUID_TICK_MARGIN = 0.2;

    /** Basin water also schedules a tick when the terrain density is below this (close to a cave wall). */
    public static final double BASIN_TICK_DENSITY = 0.08;
}
