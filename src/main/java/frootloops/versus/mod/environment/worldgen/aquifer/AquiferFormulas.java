package frootloops.versus.mod.environment.worldgen.aquifer;

import net.minecraft.util.math.MathHelper;

import java.util.function.DoubleSupplier;

import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.SEA_BARRIER_THRESHOLD;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.SEA_WATER_THRESHOLD;

/**
 * The Players Versus aquifer's two inputs, sea floodedness (F) and basin floodedness (S), computed from their leaf
 * values. These are the formulas of the density-function JSON they replace ({@code aquifer_fluid_level_floodedness},
 * {@code aquifer_floodedness_oceans_and_rivers_y64}, {@code river_carver_aquifer}, {@code ramen_cave_aquifer},
 * {@code aquifer_fluid_level_spread}, {@code aquifer_floodedness_cave_basins_y24}), written so they give the same
 * doubles: the same operations in the same order, vanilla's {@code mul} (0 times anything is 0) where the JSON
 * multiplies two functions, and band checks against vanilla's {@code minecraft:y}, which is 31.9999999999995 at y 32.
 * {@code AquiferFormulaTest} compares them with the JSON.
 */
public final class AquiferFormulas {

    private AquiferFormulas() {
    }

    /**
     * Vanilla's {@code minecraft:y} function at a block: a {@code y_clamped_gradient} from -4064 to 4062, which rounds
     * to just below the block's y at a few heights (y 32 among them). The JSON's {@code range_choice} bands test this
     * value, not the integer.
     */
    public static double yValue(int y) {
        return MathHelper.clampedMap((double) y, -4064.0, 4062.0, -4064.0, 4062.0);
    }

    /** Vanilla's {@code y_clamped_gradient}. */
    private static double gradient(int y, int fromY, int toY, double fromValue, double toValue) {
        return MathHelper.clampedMap((double) y, (double) fromY, (double) toY, fromValue, toValue);
    }

    /** Vanilla's {@code mul} of two functions: 0 when the first is 0, without looking at the second. */
    private static double mul(double first, double second) {
        return first == 0.0 ? 0.0 : first * second;
    }

    /**
     * F: sea-level floodedness, plus the ramen-cave term in y -4..31 where F alone would make a barrier.
     *
     * @param seaFloodedness F', from {@link #seaFloodedness}
     * @param ramenNoise     {@code noise(minecraft:noodle, xz 3, y 3)} at the block, only asked for when needed
     */
    public static double floodedness(int y, double seaFloodedness, DoubleSupplier ramenNoise) {
        double yValue = yValue(y);
        if (yValue >= -4.0 && yValue < 32.0 && seaFloodedness >= SEA_BARRIER_THRESHOLD && seaFloodedness < SEA_WATER_THRESHOLD) {
            return seaFloodedness + ramen(y, ramenNoise.getAsDouble());
        }
        return seaFloodedness;
    }

    /**
     * F': water below the surface of oceans and lakes, in river channels, and in cave entrances near coasts.
     *
     * @param depth           the {@code depth} router function
     * @param continentalness {@code shifted_noise(minecraft:continentalness, xz 0.25, y 0.1)}
     * @param entrances       {@code players-versus:overworld/caves/entrances}
     * @param ridge           {@code shifted_noise(minecraft:ridge, xz 0.25, y 0)}
     * @param surface         {@code noise(minecraft:surface, xz 2, y 1)}
     */
    public static double seaFloodedness(int y, double depth, double continentalness, double entrances, double ridge, double surface) {
        double yValue = yValue(y);
        if (!(yValue >= -32.0 && yValue < 64.0)) return 0.0;
        double roughness = Math.abs(surface);
        double ocean = Math.min(0.0, (roughness * -0.02 + -0.06) + depth) * -8.0;
        double oceanOrRiver = Math.max(ocean, river(y, yValue, entrances, ridge));
        double coastCondition = gradient(y, 16, 48, 0.0, -0.34) + continentalness;
        double coastDepth = Math.min(0.0, (Math.min(-0.08, continentalness + -0.1) + roughness * -0.08) + depth) * -64.0;
        double coastEntrances = Math.min(0.0, gradient(y, -32, 8, 0.32, -0.18) + entrances);
        return Math.max(oceanOrRiver, mul(coastCondition, mul(coastDepth, coastEntrances)));
    }

    /** River channels: y 48..63 where the ridge noise is within 0.3 of 0. */
    private static double river(int y, double yValue, double entrances, double ridge) {
        if (!(yValue >= 48.0 && yValue < 64.0)) return 0.0;
        if (!(ridge >= -0.3 && ridge < 0.3)) return 0.0;
        double entrance = mul(gradient(y, 48, 54, -0.2, 3.0), Math.min(0.0, entrances + -0.2));
        double scaledRidge = ridge * 6.25;
        double channel = gradient(y, 48, 64, 0.25, -1.0) + scaledRidge * scaledRidge;
        return Math.min(0.0, entrance + channel) * -2.0;
    }

    /** The ramen-cave term: extra floodedness in y 0..39 along the noodle noise. */
    private static double ramen(int y, double ramenNoise) {
        double yValue = yValue(y);
        if (!(yValue >= 0.0 && yValue < 40.0)) return 0.0;
        double shape = mul(mul(gradient(y, -4, 8, 0.0, 1.0), gradient(y, 8, 16, 1.4, 1.0)), gradient(y, 16, 40, 1.2, 0.0));
        return Math.max(0.0, (mul(shape, Math.abs(ramenNoise) + -0.24) + 0.1) * -6.0);
    }

    /**
     * The part of S that the JSON interpolates: cave walls from the entrance and noodle caves, zero above y 16.
     *
     * @param entrances {@code players-versus:overworld/caves/entrances}
     * @param noodle    {@code players-versus:overworld/caves/noodle}
     * @param surface   {@code noise(minecraft:surface, xz 4, y 2)}
     */
    public static double basinInner(int y, double entrances, double noodle, double surface) {
        double walls = (Math.abs(surface) * -0.06 + -0.12)
                + (entrances + Math.min(0.0, mul(entrances + -0.24, Math.min(0.0, noodle + -0.08) * -4.0)));
        return Math.min(1.0, mul(gradient(y, 0, 16, 0.0, -16.0), Math.min(0.0, walls)));
    }

    /** S: basin floodedness in y -2..47 from {@link #basinInner}; its factor turns negative from y 24 up. */
    public static double spread(int y, double inner) {
        double yValue = yValue(y);
        if (!(yValue >= -2.0 && yValue < 48.0)) return 0.0;
        double band = yValue >= 24.0 && yValue < 48.0 ? gradient(y, 24, 48, -0.2, 0.0) : 1.0;
        return mul(band, Math.max(0.0, gradient(y, 48, 24, -3.0, -0.1) + inner)) + 0.0;
    }
}
