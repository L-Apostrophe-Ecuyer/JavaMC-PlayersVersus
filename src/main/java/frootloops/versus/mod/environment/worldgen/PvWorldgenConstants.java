package frootloops.versus.mod.environment.worldgen;

/**
 * Numbers that more than one part of the Players Versus world type depends on.
 *
 * <p>Keep every value that the aquifer, the density functions and the surface rules must agree on here, so a
 * change in one place can't silently break another. The aquifer's formulas are Java ({@code aquifer/AquiferFormulas},
 * {@code aquifer/PvAquiferRules}); the terrain's density functions are data, which {@code tools/port-26.3/pv_density.py}
 * writes with the values here (the corridors' and the high river's), so change them here and run it.
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
     * Sea water only from this y up; everything below stays dry (the owner's design, refactor plan Section 10,
     * question 7). Below it, floodedness that would be water makes the sea's barrier band instead, so water just above
     * keeps a floor as thick as the bands give ({@link #BAND_REACH}), and caves further down are dry.
     */
    public static final int SEA_WATER_MIN_Y = -8;

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
    // Aquifer: flooded corridors (the refactor plan, Section 10, question 7)
    // ------------------------------------------------------------------------------------------------------------

    /**
     * Noodle caves come back to the basin layers (above {@link #BASIN_MIN_Y}, below this y) as flooded corridors near
     * the flooded caves. Elsewhere in these layers they're the dry noodles ({@link #DRY_NOODLE_BIAS}): the noodle's
     * height bias ({@code caves/corridor_noodle} in the data) lets fewer through, and the aquifer leaves them dry.
     */
    public static final int CORRIDOR_MAX_Y = 24;

    /**
     * The corridors' zone: where the entrance value ({@code players-versus:overworld/caves/entrances}, negative in the
     * entrance caves) is below this, the noodle's bias moves to {@link #CORRIDOR_BIAS}, fully once it's
     * {@link #CORRIDOR_ZONE_TAPER} lower. {@code FloodedNoodleSurveyTest} compared zones up to 0.2, 0.3 and 0.4.
     */
    public static final double CORRIDOR_ENTRANCES = 0.4;
    public static final double CORRIDOR_ZONE_TAPER = 0.05;
    public static final double CORRIDOR_ZONE_SCALE = 1.0 / CORRIDOR_ZONE_TAPER;

    /** The noodle's height bias in the corridors' zone; vanilla's noodles have none (0). */
    public static final double CORRIDOR_BIAS = 0.0;

    /**
     * Nearer the caves, from this entrance value down to 0, the bias falls on to {@link #CORRIDOR_FLARE_BIAS}, so the
     * corridors widen into the caves they reach instead of passing a block or two by them. In the survey's lake-rich
     * areas, 14% of the corridors' water reached a lake without it (a zone up to 0.3), 59 to 65% with it.
     */
    public static final double CORRIDOR_FLARE_FROM = 0.15;
    public static final double CORRIDOR_FLARE_SCALE = 1.0 / CORRIDOR_FLARE_FROM;
    public static final double CORRIDOR_FLARE_BIAS = -0.06;

    /**
     * Dry noodles: from {@link #DRY_NOODLE_MIN_Y} to {@link #DRY_NOODLE_MAX_Y}, where the noodle's height bias otherwise
     * keeps noodles out (0.08 in y -8..20), it's at most this away from the entrance caves (an entrance value from
     * {@link #CORRIDOR_ENTRANCES} up, fading in over {@link #CORRIDOR_ZONE_TAPER}), so some tunnels lead down dry from
     * y 32 to y -16 and rarely meet an entrance cave. Nearer them the bias is as before: in the corridors' layers the
     * corridors keep their own, and the aquifer floods only what opens there ({@code caves/flooded_corridors}).
     */
    public static final double DRY_NOODLE_BIAS = 0.02;
    public static final int DRY_NOODLE_MIN_Y = -16;
    public static final int DRY_NOODLE_MAX_Y = 32;

    // ------------------------------------------------------------------------------------------------------------
    // The high river (the refactor plan, Section 10, question 7; Section 6.2, 2f): water at y 80, and a thinner upper
    // layer at y 96 on the same path, which falls into it where the ground climbs from one to the other
    // ------------------------------------------------------------------------------------------------------------

    /** The high river's water surface. Its water there spills wherever the ground beside it is open. */
    public static final int HIGH_RIVER_Y = 80;
    /** Its bed: water down to this many blocks under the surface, narrowing to nothing at the bottom. */
    public static final int HIGH_RIVER_BED = 3;
    /** The bed's bottom: the lowest block the river opens and fills. */
    public static final int HIGH_RIVER_MIN_Y = HIGH_RIVER_Y - HIGH_RIVER_BED;
    /**
     * Half the river's width at its surface, in the units of its noise ({@code players-versus:overworld/high_river},
     * which changes by about 0.0028 per block): about 10 blocks.
     */
    public static final double HIGH_RIVER_HALF_WIDTH = 0.03;
    /** How much wider the valley gets per block above the surface, so ground over the water is cut back into banks. */
    public static final double HIGH_RIVER_WIDENING = 0.006;

    /** The upper layer's water surface, which spills like the high river's. */
    public static final int HIGH_RIVER_UPPER_Y = 96;
    /** Its bed, shallower than the high river's. */
    public static final int HIGH_RIVER_UPPER_BED = 2;
    public static final int HIGH_RIVER_UPPER_MIN_Y = HIGH_RIVER_UPPER_Y - HIGH_RIVER_UPPER_BED;
    /** Its half width at its surface (about 5 blocks) and its banks' widening: half the high river's. */
    public static final double HIGH_RIVER_UPPER_HALF_WIDTH = 0.015;
    public static final double HIGH_RIVER_UPPER_WIDENING = 0.003;

    /** How much vanilla's depth falls per block up (1.5 to -1.5 over y -64..320): 0.01 is about 1.3 blocks. */
    public static final double DEPTH_PER_BLOCK = 3.0 / 384;
    /**
     * Each layer runs at full width where the depth at its surface's height is up to this, the ground's nominal surface
     * up to about 6 blocks above its water. On the low side it runs on wherever that depth is above 0, over caves and
     * dips, and past that while the terrain at its surface is solid, to the ground's real edge, where it spills.
     */
    public static final double HIGH_RIVER_FULL_DEPTH = 0.05;
    /**
     * Into higher ground the high river cuts a gorge: from {@link #HIGH_RIVER_FULL_DEPTH} it narrows to this share of
     * its width (as wide as the upper layer), and keeps it up to the gorge's head.
     */
    public static final double HIGH_RIVER_GORGE_WIDTH = 0.5;
    /**
     * The gorge's head, a wall where the depth at the high river's surface reaches this: past the depth where the upper
     * layer's surface meets the ground's nominal surface (0.125), so the upper layer runs on above the wall and its
     * water falls over it into the river. About 20 blocks of ground above the water, before the terrain's own bumps.
     */
    public static final double HIGH_RIVER_CLOSED = (HIGH_RIVER_UPPER_Y - HIGH_RIVER_Y) * DEPTH_PER_BLOCK + 0.03;
    /** The upper layer narrows from {@link #HIGH_RIVER_FULL_DEPTH} and is closed at this depth at its surface. */
    public static final double HIGH_RIVER_UPPER_CLOSED = 0.1;
    /** The valleys' top, exclusive: the final density only looks for the river from a bed's bottom up to here. */
    public static final int HIGH_RIVER_VALLEY_MAX_Y = 128;

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
