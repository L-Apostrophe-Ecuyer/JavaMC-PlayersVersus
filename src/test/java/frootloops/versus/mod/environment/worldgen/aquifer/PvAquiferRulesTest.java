package frootloops.versus.mod.environment.worldgen.aquifer;

import frootloops.versus.mod.environment.worldgen.TestGame;
import net.minecraft.block.Blocks;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.SEA_LEVEL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PvAquiferRulesTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    private static PvAquiferDecision decide(int y, double density, double floodedness, double spread) {
        return PvAquiferRules.decide(new DensityFunction.UnblendedNoisePos(0, y, 0), density, false, pos -> floodedness, pos -> spread);
    }

    @Test
    void barriersAreSolidSoCarversLeaveThem() {
        assertEquals(PvAquiferDecision.SEA_BARRIER, decide(40, 0.0, 0.2, 0.0));
        assertEquals(PvAquiferDecision.BASIN_BARRIER, decide(10, 0.0, 0.0, 0.3));
        assertNull(PvAquiferDecision.SEA_BARRIER.state);
        assertNull(PvAquiferDecision.BASIN_BARRIER.state);
    }

    @Test
    void waterTicksOnlyNearItsThreshold() {
        assertEquals(PvAquiferDecision.SEA_WATER_TICKING, decide(40, 0.0, 0.4, 0.0));
        assertEquals(PvAquiferDecision.SEA_WATER, decide(40, 0.0, 0.6, 0.0));
        // basin water threshold at y 10 is 0.5; the old rule ticked all basin water because density is at most 0 here (Q8)
        assertEquals(PvAquiferDecision.BASIN_WATER_TICKING, decide(10, -0.5, 0.0, 0.55));
        assertEquals(PvAquiferDecision.BASIN_WATER, decide(10, -0.5, 0.0, 0.8));
    }

    @Test
    void theRestOfTheRules() {
        assertEquals(PvAquiferDecision.SOLID, decide(10, 0.1, 1.0, 1.0));
        assertEquals(PvAquiferDecision.AIR_ABOVE_SEA, decide(SEA_LEVEL, 0.0, 1.0, 1.0));
        assertEquals(PvAquiferDecision.SEA_WATER, decide(SEA_LEVEL - 1, 0.0, 1.0, 0.0));
        assertEquals(PvAquiferDecision.AIR, decide(40, 0.0, 0.0, 1.0));
        assertEquals(PvAquiferDecision.LAVA, PvAquiferRules.decide(new DensityFunction.UnblendedNoisePos(0, -60, 0), 0.0, true, pos -> 1.0, pos -> 1.0));
        assertEquals(Blocks.WATER.getDefaultState(), PvAquiferDecision.BASIN_WATER.state);
    }
}
