package frootloops.versus.mod.environment.worldgen;

/**
 * Numbers that more than one part of the Players Versus world type depends on.
 *
 * <p>Keep every value that the aquifer, the density functions and the surface rules must agree on here, so a
 * change in one place can't silently break another. Where a value is still written out elsewhere, the spot is named
 * below. The aquifer's and the terrain's formulas are Java since plan phases 2b and 3 ({@code aquifer/AquiferFormulas},
 * {@code density/*}); the JSON they replaced is kept for the tests only ({@code src/test/resources/reference}).
 */
public final class PvWorldgenConstants {

    private PvWorldgenConstants() {
    }

    // ------------------------------------------------------------------------------------------------------------
    // Sea level
    // ------------------------------------------------------------------------------------------------------------

    /**
     * The aquifer fills water strictly below this y, so the ocean surface is at y 63. Same meaning as the noise
     * settings' {@code sea_level} (vanilla: water below it), which must hold the same number, since spawning,
     * icebergs, ocean structures and the snow line read that one. Also written out as the top of F's band and of
     * the river term in {@code AquiferFormulas} (y below 64), as in the JSON it replaced.
     */
    public static final int SEA_LEVEL = 64;

    // ------------------------------------------------------------------------------------------------------------
    // Aquifer: sea-level water (oceans, rivers)
    // ------------------------------------------------------------------------------------------------------------

    /** Sea-level flooding is only evaluated strictly above this y. Below it, open space stays dry (or lava). */
    public static final int SEA_BAND_MIN_Y = -32;

    /**
     * Floodedness above this is water. {@code AquiferFormulas.floodedness} adds the ramen-cave term below it, as the
     * {@code range_choice} in {@code aquifer_fluid_level_floodedness.json} did.
     */
    public static final double SEA_WATER_THRESHOLD = 0.34;

    /**
     * Floodedness above this (and below {@link #SEA_WATER_THRESHOLD}) is a barrier, which the terrain pass fills with
     * ore veins or the default block. {@code AquiferFormulas.floodedness} adds the ramen-cave term from here up, as
     * the {@code range_choice} in {@code aquifer_fluid_level_floodedness.json} did.
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
    // Aquifer: walls
    // ------------------------------------------------------------------------------------------------------------

    /**
     * How far, in steps along the axes, a barrier band's stone is kept around water. Stone also stands wherever water
     * could flow in (one step, from a side or from above), whatever the bands say ({@code aquifer/PvAquiferRules}).
     */
    public static final int BAND_REACH = 2;

    /**
     * From this height up, a barrier band's stone stays wherever it is. Near the sea surface the sea's band fills the dry
     * hollows next to coasts up to about sea level, which shapes the coast: without it they'd open into pits.
     */
    public static final int BANDS_KEPT_FROM_Y = 56;

    // ------------------------------------------------------------------------------------------------------------
    // Aquifer: fluid ticks
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Water within this margin above its threshold schedules a fluid tick, so it can flow into nearby openings. Water
     * further inside a body is surrounded by water and doesn't need one.
     */
    public static final double FLUID_TICK_MARGIN = 0.2;
}
