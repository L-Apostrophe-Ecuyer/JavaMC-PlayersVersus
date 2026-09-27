package frootloops.versus.mod.environment.worldgen.aquifer;

import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.function.ToDoubleFunction;

import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.*;

/**
 * The Players Versus aquifer rules, shared by {@link PvAquifer} and {@code /pvwg probe}.
 *
 * <p>For a non-solid position, in order:
 * <ol>
 *   <li>below the lava level: lava;</li>
 *   <li>at or above {@link frootloops.versus.mod.environment.worldgen.PvWorldgenConstants#SEA_LEVEL}: air;</li>
 *   <li>sea band: floodedness decides water, a barrier (solid), or falls through;</li>
 *   <li>basin band: spread decides basin water, a barrier (solid), or falls through;</li>
 *   <li>otherwise air.</li>
 * </ol>
 * Noise is only sampled when a rule needs it.
 */
public final class PvAquiferRules {

    private PvAquiferRules() {
    }

    /**
     * @param pos         position to decide for; also used to sample {@code floodedness} and {@code spread}
     * @param density     terrain density at {@code pos} (carvers pass 0)
     * @param lavaLevel   whether {@code pos} is below the generator's lava level
     * @param floodedness sea/river floodedness (router slot {@code fluid_level_floodedness}): the aquifer's
     *                    {@link Lattice}, or the function itself for exact values
     * @param spread      cave-basin floodedness (router slot {@code fluid_level_spread}), likewise
     */
    public static PvAquiferDecision decide(DensityFunction.NoisePos pos, double density, boolean lavaLevel,
                                           ToDoubleFunction<DensityFunction.NoisePos> floodedness,
                                           ToDoubleFunction<DensityFunction.NoisePos> spread) {
        if (density > 0.0) return PvAquiferDecision.SOLID;
        if (lavaLevel) return PvAquiferDecision.LAVA;

        int y = pos.blockY();
        if (y >= SEA_LEVEL) return PvAquiferDecision.AIR_ABOVE_SEA;

        if (y > SEA_BAND_MIN_Y) {
            double seaFloodedness = floodedness.applyAsDouble(pos);
            if (seaFloodedness > SEA_WATER_THRESHOLD) {
                return seaFloodedness < SEA_WATER_THRESHOLD + FLUID_TICK_MARGIN
                        ? PvAquiferDecision.SEA_WATER_TICKING
                        : PvAquiferDecision.SEA_WATER;
            }
            if (seaFloodedness > seaBarrierThreshold(y)) return PvAquiferDecision.SEA_BARRIER;
        }

        if (y > BASIN_MIN_Y && y < BASIN_MAX_Y) {
            double basinFloodedness = spread.applyAsDouble(pos);
            double waterThreshold = basinWaterThreshold(y);
            if (basinFloodedness > waterThreshold) {
                return basinFloodedness < waterThreshold + FLUID_TICK_MARGIN
                        ? PvAquiferDecision.BASIN_WATER_TICKING
                        : PvAquiferDecision.BASIN_WATER;
            }
            if (basinFloodedness > basinBarrierThreshold(y) && y < BASIN_BARRIER_MAX_Y) {
                return PvAquiferDecision.BASIN_BARRIER;
            }
        }

        return PvAquiferDecision.AIR;
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
}
