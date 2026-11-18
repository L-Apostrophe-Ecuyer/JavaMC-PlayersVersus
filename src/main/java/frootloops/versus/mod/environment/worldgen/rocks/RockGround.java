package frootloops.versus.mod.environment.worldgen.rocks;

import frootloops.versus.mod.environment.worldgen.PvWorldgenConstants;

/**
 * Where rock may stand in the world: above the mountainsides, out of the 3D-noise terrain, on level ground; and for rocks
 * placed by a filter that asks for it, in the middle temperatures (plains, forests, savannas, jungles, not the cold biomes,
 * nor deserts and badlands). Formations go where their biomes' tags put them, cold places too ({@link RockGroundFilter},
 * {@link RockFormationStructure}).
 */
public final class RockGround {

    private static final float COLDEST = -0.15F, WARMEST = 0.55F;
    /** Lower erosion is mountainous: the mountainsides start at -0.385. */
    private static final float LEAST_EROSION = -0.3F;
    /** The 3D-noise terrain's climate (part 2's zone): rugged, overhanging ground. */
    private static final float NOISE_HUMIDITY_MIN = (float) PvWorldgenConstants.NOISE_ZONE_HUMIDITY_MIN, NOISE_HUMIDITY_MAX = (float) PvWorldgenConstants.NOISE_ZONE_HUMIDITY_MAX;
    private static final float NOISE_EROSION_MIN = (float) PvWorldgenConstants.NOISE_ZONE_EROSION_MIN, NOISE_EROSION_MAX = (float) PvWorldgenConstants.NOISE_ZONE_EROSION_MAX;
    /**
     * How far the ground may rise or fall under a rock and around it, in blocks, over the 16 blocks read: a gentle slope
     * at most. Most of the ground in the middle biomes rises and falls 6 blocks or more over that: hills, not stretches.
     */
    public static final int LEVEL = 3;
    /** Where the ground is read, from the rock's middle: there, then eight points 4 blocks out and eight 8 blocks out. */
    public static final int[][] AROUND = around();

    private RockGround() {
    }

    private static int[][] around() {
        int[][] around = new int[17][];
        around[0] = new int[]{0, 0};
        int index = 1;
        for (int far : new int[]{4, 8}) {
            for (int step = 0; step < 8; step++) {
                double angle = step * Math.PI / 4;
                around[index++] = new int[]{(int) Math.round(far * Math.cos(angle)), (int) Math.round(far * Math.sin(angle))};
            }
        }
        return around;
    }

    /** Whether rock may stand in this climate: its temperature, and its ground ({@link #terrain}). */
    public static boolean climate(float temperature, float humidity, float erosion) {
        return temperature >= COLDEST && temperature <= WARMEST && terrain(humidity, erosion);
    }

    /** Whether the climate's ground takes rock, whatever its temperature: above the mountainsides, out of the 3D-noise terrain. */
    public static boolean terrain(float humidity, float erosion) {
        if (erosion <= LEAST_EROSION) return false;
        boolean noise = humidity >= NOISE_HUMIDITY_MIN && humidity < NOISE_HUMIDITY_MAX && erosion >= NOISE_EROSION_MIN && erosion < NOISE_EROSION_MAX;
        return !noise;
    }

    /** Whether the ground read at {@link #AROUND} is level enough for a rock: its heights within {@link #LEVEL} of each other. */
    public static boolean level(int[] heights) {
        return level(heights, LEVEL);
    }

    /** Whether the ground read at {@link #AROUND} has its heights within {@code tolerance} of each other. */
    public static boolean level(int[] heights, int tolerance) {
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        for (int height : heights) {
            low = Math.min(low, height);
            high = Math.max(high, height);
        }
        return high - low <= tolerance;
    }
}
