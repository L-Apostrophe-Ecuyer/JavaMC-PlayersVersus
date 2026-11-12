package frootloops.versus.mod.environment.worldgen.terrain;

import frootloops.versus.mod.environment.worldgen.WorldgenTestData;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;

/**
 * Random columns of the Players Versus world (seed 1234) with the climate the biomes and the terrain read there, and
 * the 3D-noise zone's window, for the terrain tests; the game's own evaluation of this mod's density functions.
 */
final class TerrainSamples {

    static final long SEED = 1234L;
    static final int RANGE = 30_000;

    /** A column: where, and the router's humidity, erosion, continentalness and ridges (weirdness) there. */
    record Column(int x, int z, double humidity, double erosion, double continentalness, double ridges, double zone) {
    }

    private static final RandomState STATE = WorldgenTestData.randomState(SEED);
    private static final NoiseRouter ROUTER = WorldgenTestData.router();
    private static final DensityFunction ZONE = WorldgenTestData.density("players-versus:overworld/noise_zone");

    private TerrainSamples() {
    }

    static RandomState state() {
        return STATE;
    }

    /** The density function {@code id}'s value in the column (y 0: the 2D functions don't read it). */
    static double at(DensityFunction function, int x, int z) {
        return STATE.sampleBlockValueUncached(function, x, 0, z);
    }

    /**
     * Columns inside the zone, well inland (the window 1, continentalness 0.3 or more): {@code patches} found at random
     * places, each with up to {@code perPatch} more within 40 blocks of it (the same patch, a different column).
     */
    static List<Column> inTheZone(long seed, int patches, int perPatch) {
        Random random = new Random(seed);
        List<Column> columns = new ArrayList<>();
        int found = 0;
        for (int tries = 0; found < patches && tries < 5_000_000; tries++) {
            int x = random.nextInt(2 * RANGE) - RANGE, z = random.nextInt(2 * RANGE) - RANGE;
            Column column = column(x, z);
            if (!inside(column)) continue;
            found++;
            columns.add(column);
            for (int i = 0; i < perPatch; i++) {
                Column near = column(x + random.nextInt(81) - 40, z + random.nextInt(81) - 40);
                if (inside(near)) columns.add(near);
            }
        }
        return columns;
    }

    private static boolean inside(Column column) {
        return column.zone() >= 0.999 && column.continentalness() >= 0.3;
    }

    static Column column(int x, int z) {
        return new Column(x, z, at(ROUTER.vegetation(), x, z), at(ROUTER.erosion(), x, z), at(ROUTER.continents(), x, z), at(ROUTER.ridges(), x, z), at(ZONE, x, z));
    }

    /** {@code count} columns at random places within {@link #RANGE} blocks of the origin. */
    static List<Column> random(long seed, int count) {
        Random random = new Random(seed);
        List<Column> columns = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int x = random.nextInt(2 * RANGE) - RANGE, z = random.nextInt(2 * RANGE) - RANGE;
            columns.add(column(x, z));
        }
        return columns;
    }
}
