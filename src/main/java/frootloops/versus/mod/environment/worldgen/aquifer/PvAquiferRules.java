package frootloops.versus.mod.environment.worldgen.aquifer;

import java.util.ArrayList;
import java.util.List;

import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.*;

/**
 * The Players Versus aquifer rules, shared by {@link PvAquifer} and {@code /pvwg probe}.
 *
 * <p>Each position first gets what its own floodedness says ({@link #atPosition}): sea water, basin water, a barrier
 * band (floodedness between its barrier and water thresholds), or dry; and in the basin layers, basin water where the
 * flooded corridors' noodle opens the block. That's the water the aquifer places, and it depends on nothing else. The walls come from the neighbourhood ({@link #decide}): for a non-solid position, in order:
 * <ol>
 *   <li>below the lava level: lava;</li>
 *   <li>at or above {@link frootloops.versus.mod.environment.worldgen.PvWorldgenConstants#SEA_LEVEL}: the high river's
 *   water and its walls ({@link #aboveSea}), else air;</li>
 *   <li>water where its floodedness says so;</li>
 *   <li>a wall where water could flow in: from one of the four sides, or from above;</li>
 *   <li>a barrier band's stone within {@link frootloops.versus.mod.environment.worldgen.PvWorldgenConstants#BAND_REACH}
 *   steps of water, so water keeps a wall of that thickness where the bands gave it one, and from
 *   {@link frootloops.versus.mod.environment.worldgen.PvWorldgenConstants#BANDS_KEPT_FROM_Y} up, where the sea's band
 *   fills coastal hollows;</li>
 *   <li>otherwise air.</li>
 * </ol>
 * So water never touches open air, whatever the terrain or a carver opens (walls don't depend on density), and the
 * bands' stone away from water, which only filled caves, is gone. A position's walls only depend on its neighbours'
 * floodedness, which the neighbouring chunk computes the same way ({@link Lattice}), so walls hold across chunk borders.
 */
public final class PvAquiferRules {

    /** Where water flows into a position from: the four sides, then above. */
    private static final int[][] INFLOW = {{-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}, {0, 1, 0}};
    /** The positions within {@code BAND_REACH} steps, nearest first. */
    private static final int[][] BAND_NEIGHBOURHOOD = neighbourhood(BAND_REACH);

    private PvAquiferRules() {
    }

    /** A value the rules read at a block: F, S or the corridors' noodle. */
    @FunctionalInterface
    public interface Field {
        double at(int x, int y, int z);
    }

    /** For {@link #atPosition} without the flooded corridors: no corridor anywhere. */
    public static final Field NO_CORRIDORS = (x, y, z) -> Double.POSITIVE_INFINITY;

    /** What {@link #atPosition} says at a block, for the neighbours {@link #decide} looks at. */
    @FunctionalInterface
    public interface Positions {
        PvAquiferDecision at(int x, int y, int z);
    }

    /**
     * What a position's own floodedness says, below sea level: water ({@code SEA_WATER}, {@code BASIN_WATER} and
     * their ticking forms), a barrier band ({@code SEA_BARRIER}, {@code BASIN_BARRIER}), or {@code AIR}. Sea water
     * starts at {@link frootloops.versus.mod.environment.worldgen.PvWorldgenConstants#SEA_WATER_MIN_Y}; below it, sea
     * floodedness above the water threshold is a band.
     *
     * @param floodedness sea/river floodedness F (router slot {@code fluid_level_floodedness}): the aquifer's
     *                    {@link Lattice}-based F, or the function itself for exact values
     * @param spread      cave-basin floodedness S (router slot {@code fluid_level_spread}), likewise
     */
    public static PvAquiferDecision atPosition(int x, int y, int z, Field floodedness, Field spread) {
        return atPosition(x, y, z, floodedness, spread, NO_CORRIDORS);
    }

    /**
     * {@link #atPosition}, with the flooded corridors: in their layers, a block that isn't basin water is when the
     * corridors' noodle opens it (the refactor plan, Section 10, question 7).
     *
     * @param corridors the noodle with the corridors' bias at a block ({@code players-versus:overworld/caves/corridor_noodle}),
     *                  the value the final density takes the minimum with there: at most 0 where it opens the block
     */
    public static PvAquiferDecision atPosition(int x, int y, int z, Field floodedness, Field spread, Field corridors) {
        if (y >= SEA_LEVEL) return PvAquiferDecision.AIR;

        if (y > SEA_BAND_MIN_Y) {
            double seaFloodedness = floodedness.at(x, y, z);
            if (seaFloodedness > SEA_WATER_THRESHOLD && y >= SEA_WATER_MIN_Y) {
                return seaFloodedness < SEA_WATER_THRESHOLD + FLUID_TICK_MARGIN
                        ? PvAquiferDecision.SEA_WATER_TICKING
                        : PvAquiferDecision.SEA_WATER;
            }
            if (seaFloodedness > seaBarrierThreshold(y)) return PvAquiferDecision.SEA_BARRIER;
        }

        if (y > BASIN_MIN_Y && y < BASIN_MAX_Y) {
            double basinFloodedness = spread.at(x, y, z);
            double waterThreshold = basinWaterThreshold(y);
            if (basinFloodedness > waterThreshold) {
                return basinFloodedness < waterThreshold + FLUID_TICK_MARGIN
                        ? PvAquiferDecision.BASIN_WATER_TICKING
                        : PvAquiferDecision.BASIN_WATER;
            }
            if (y < CORRIDOR_MAX_Y && corridors.at(x, y, z) <= 0.0) return PvAquiferDecision.BASIN_WATER;
            if (basinFloodedness > basinBarrierThreshold(y) && y < BASIN_BARRIER_MAX_Y) {
                return PvAquiferDecision.BASIN_BARRIER;
            }
        }

        return PvAquiferDecision.AIR;
    }

    /**
     * What the aquifer places at a position.
     *
     * @param density   terrain density at the position (carvers pass 0)
     * @param lavaLevel whether the position is below the generator's lava level
     * @param positions {@link #atPosition} at any block, for the position itself and its neighbours
     */
    public static PvAquiferDecision decide(int x, int y, int z, double density, boolean lavaLevel, Positions positions) {
        if (density > 0.0) return PvAquiferDecision.SOLID;
        if (lavaLevel) return PvAquiferDecision.LAVA;

        if (y >= SEA_LEVEL) return aboveSea(x, y, z, positions);

        PvAquiferDecision here = positions.at(x, y, z);
        if (isWater(here)) return here;
        PvAquiferDecision wall = wall(x, y, z, positions);
        if (wall != PvAquiferDecision.AIR) return wall;
        if (here != PvAquiferDecision.AIR && (y >= BANDS_KEPT_FROM_Y || waterWithinReach(x, y, z, positions))) return here;
        return PvAquiferDecision.AIR;
    }

    /**
     * Above sea level only the high river places water, at y 77..80 ({@code PvAquifer.highRiverAt}, which the positions
     * give there). A wall keeps it in wherever it could flow into open space: from the bed's water beside, or from any of
     * its water above. The surface's water gets no wall beside it, so where the ground next to it is open, it spills.
     */
    private static PvAquiferDecision aboveSea(int x, int y, int z, Positions positions) {
        if (y < HIGH_RIVER_MIN_Y - 1 || y > HIGH_RIVER_Y) return PvAquiferDecision.AIR_ABOVE_SEA;
        PvAquiferDecision here = positions.at(x, y, z);
        if (here != PvAquiferDecision.AIR) return here;
        for (int[] offset : INFLOW) {
            PvAquiferDecision neighbour = positions.at(x + offset[0], y + offset[1], z + offset[2]);
            if (neighbour == PvAquiferDecision.HIGH_RIVER_BED_WATER || offset[1] == 1 && neighbour == PvAquiferDecision.HIGH_RIVER_WATER) {
                return PvAquiferDecision.HIGH_RIVER_BARRIER;
            }
        }
        return PvAquiferDecision.AIR_ABOVE_SEA;
    }

    /**
     * A wall where water could flow into a position: {@code SEA_BARRIER} if sea water is beside or above it, else
     * {@code BASIN_BARRIER} if basin water is, else {@code AIR}.
     */
    private static PvAquiferDecision wall(int x, int y, int z, Positions positions) {
        PvAquiferDecision wall = PvAquiferDecision.AIR;
        for (int[] offset : INFLOW) {
            PvAquiferDecision neighbour = positions.at(x + offset[0], y + offset[1], z + offset[2]);
            if (neighbour == PvAquiferDecision.SEA_WATER || neighbour == PvAquiferDecision.SEA_WATER_TICKING) {
                return PvAquiferDecision.SEA_BARRIER;
            }
            if (isWater(neighbour)) wall = PvAquiferDecision.BASIN_BARRIER;
        }
        return wall;
    }

    private static boolean waterWithinReach(int x, int y, int z, Positions positions) {
        for (int[] offset : BAND_NEIGHBOURHOOD) {
            if (isWater(positions.at(x + offset[0], y + offset[1], z + offset[2]))) return true;
        }
        return false;
    }

    public static boolean isWater(PvAquiferDecision decision) {
        return decision == PvAquiferDecision.SEA_WATER || decision == PvAquiferDecision.SEA_WATER_TICKING
                || decision == PvAquiferDecision.BASIN_WATER || decision == PvAquiferDecision.BASIN_WATER_TICKING
                || decision == PvAquiferDecision.HIGH_RIVER_WATER || decision == PvAquiferDecision.HIGH_RIVER_BED_WATER;
    }

    /** Barriers need slightly more floodedness near the sea surface, so they thin out there. */
    public static double seaBarrierThreshold(int y) {
        return SEA_BARRIER_THRESHOLD + (y < SEA_BARRIER_RAMP_START_Y ? 0.0 : (double) (y - SEA_BARRIER_RAMP_START_Y) * SEA_BARRIER_RAMP_PER_BLOCK);
    }

    /** Basins flood more easily near the bottom of the band. */
    public static double basinWaterThreshold(int y) {
        return y > BASIN_WATER_RAMP_Y ? BASIN_WATER_THRESHOLD : BASIN_WATER_THRESHOLD - (double) (BASIN_WATER_RAMP_Y - y) * BASIN_WATER_RAMP_PER_BLOCK;
    }

    /** Basin barriers get rarer higher up in the band. */
    public static double basinBarrierThreshold(int y) {
        return y < BASIN_BARRIER_RAMP_Y ? SEA_BARRIER_THRESHOLD : (double) (y - BASIN_BARRIER_RAMP_Y) * BASIN_BARRIER_RAMP_PER_BLOCK;
    }

    /** The offsets within {@code reach} steps along the axes (a Manhattan distance), the position itself left out, nearest first. */
    private static int[][] neighbourhood(int reach) {
        List<int[]> offsets = new ArrayList<>();
        for (int distance = 1; distance <= reach; distance++) {
            for (int dx = -distance; dx <= distance; dx++) {
                for (int dy = -distance + Math.abs(dx); dy <= distance - Math.abs(dx); dy++) {
                    int dz = distance - Math.abs(dx) - Math.abs(dy);
                    offsets.add(new int[]{dx, dy, dz});
                    if (dz != 0) offsets.add(new int[]{dx, dy, -dz});
                }
            }
        }
        return offsets.toArray(int[][]::new);
    }
}
