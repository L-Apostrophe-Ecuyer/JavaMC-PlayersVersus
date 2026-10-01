package frootloops.versus.mod.environment.worldgen.aquifer;

import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.block.Blocks;

import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.BANDS_KEPT_FROM_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.BASIN_MIN_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.CORRIDOR_MAX_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.HIGH_RIVER_Y;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.SEA_LEVEL;
import static frootloops.versus.mod.environment.worldgen.PvWorldgenConstants.SEA_WATER_MIN_Y;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PvAquiferRulesTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    private static PvAquiferDecision atPosition(int y, double floodedness, double spread) {
        return PvAquiferRules.atPosition(0, y, 0, (x, py, z) -> floodedness, (x, py, z) -> spread);
    }

    private static PvAquiferDecision atPosition(int y, double floodedness, double spread, double corridors) {
        return PvAquiferRules.atPosition(0, y, 0, (x, py, z) -> floodedness, (x, py, z) -> spread, (x, py, z) -> corridors);
    }

    /** A made-up neighbourhood: the given decisions at some blocks, dry everywhere else. */
    private static final class Neighbourhood implements PvAquiferRules.Positions {
        private final Map<Long, PvAquiferDecision> decisions = new HashMap<>();

        Neighbourhood put(int x, int y, int z, PvAquiferDecision decision) {
            this.decisions.put(key(x, y, z), decision);
            return this;
        }

        @Override
        public PvAquiferDecision at(int x, int y, int z) {
            return this.decisions.getOrDefault(key(x, y, z), PvAquiferDecision.AIR);
        }

        PvAquiferDecision decide(int x, int y, int z) {
            return PvAquiferRules.decide(x, y, z, 0.0, false, this);
        }

        private static long key(int x, int y, int z) {
            return ((long) x & 0xFFFFF) << 40 | ((long) y & 0xFFFFF) << 20 | (long) z & 0xFFFFF;
        }
    }

    @Test
    void positionsGetWaterOrABandOrNothing() {
        assertEquals(PvAquiferDecision.SEA_BARRIER, atPosition(40, 0.2, 0.0));
        assertEquals(PvAquiferDecision.BASIN_BARRIER, atPosition(10, 0.0, 0.3));
        assertEquals(PvAquiferDecision.SEA_WATER, atPosition(SEA_LEVEL - 1, 1.0, 0.0));
        assertEquals(PvAquiferDecision.AIR, atPosition(40, 0.0, 1.0));
        // sea level and up: nothing, whatever the floodedness
        assertEquals(PvAquiferDecision.AIR, atPosition(SEA_LEVEL, 1.0, 1.0));
        // a sea band takes the place of basin water
        assertEquals(PvAquiferDecision.SEA_BARRIER, atPosition(10, 0.2, 1.0));
        // sea water starts at y -8; below it, what would be water is a band, kept only near water above it
        assertEquals(PvAquiferDecision.SEA_WATER, atPosition(SEA_WATER_MIN_Y, 1.0, 0.0));
        assertEquals(PvAquiferDecision.SEA_BARRIER, atPosition(SEA_WATER_MIN_Y - 1, 1.0, 0.0));
        assertEquals(PvAquiferDecision.AIR, new Neighbourhood().put(0, -20, 0, PvAquiferDecision.SEA_BARRIER).decide(0, -20, 0));
    }

    @Test
    void waterTicksOnlyNearItsThreshold() {
        assertEquals(PvAquiferDecision.SEA_WATER_TICKING, atPosition(40, 0.4, 0.0));
        assertEquals(PvAquiferDecision.SEA_WATER, atPosition(40, 0.6, 0.0));
        // basin water threshold at y 10 is 0.5; the old rule ticked all basin water because density is at most 0 here (Q8)
        assertEquals(PvAquiferDecision.BASIN_WATER_TICKING, atPosition(10, 0.0, 0.55));
        assertEquals(PvAquiferDecision.BASIN_WATER, atPosition(10, 0.0, 0.8));
    }

    @Test
    void wallsStandWhereWaterWouldFlowIn() {
        // water beside a dry block, or above it: a wall
        assertEquals(PvAquiferDecision.SEA_BARRIER, new Neighbourhood().put(1, 40, 0, PvAquiferDecision.SEA_WATER).decide(0, 40, 0));
        assertEquals(PvAquiferDecision.SEA_BARRIER, new Neighbourhood().put(0, 41, 0, PvAquiferDecision.SEA_WATER).decide(0, 40, 0));
        assertEquals(PvAquiferDecision.BASIN_BARRIER, new Neighbourhood().put(0, 10, -1, PvAquiferDecision.BASIN_WATER_TICKING).decide(0, 10, 0));
        // sea water wins the wall's name
        assertEquals(PvAquiferDecision.SEA_BARRIER, new Neighbourhood().put(0, 10, -1, PvAquiferDecision.BASIN_WATER)
                .put(0, 11, 0, PvAquiferDecision.SEA_WATER).decide(0, 10, 0));
        // water below a dry block doesn't flow up into it; water diagonally doesn't reach it either
        assertEquals(PvAquiferDecision.AIR, new Neighbourhood().put(0, 39, 0, PvAquiferDecision.SEA_WATER).decide(0, 40, 0));
        assertEquals(PvAquiferDecision.AIR, new Neighbourhood().put(1, 40, 1, PvAquiferDecision.SEA_WATER).decide(0, 40, 0));
        // the water itself stays water
        assertEquals(PvAquiferDecision.SEA_WATER, new Neighbourhood().put(0, 40, 0, PvAquiferDecision.SEA_WATER).decide(0, 40, 0));
    }

    @Test
    void bandsStayStoneNearWaterOrTheSeaSurface() {
        // a band with water within two steps, below it included: stone
        assertEquals(PvAquiferDecision.SEA_BARRIER, new Neighbourhood().put(0, 40, 0, PvAquiferDecision.SEA_BARRIER)
                .put(0, 38, 0, PvAquiferDecision.SEA_WATER).decide(0, 40, 0));
        assertEquals(PvAquiferDecision.BASIN_BARRIER, new Neighbourhood().put(0, 10, 0, PvAquiferDecision.BASIN_BARRIER)
                .put(1, 10, 1, PvAquiferDecision.BASIN_WATER).decide(0, 10, 0));
        // three steps away: open
        assertEquals(PvAquiferDecision.AIR, new Neighbourhood().put(0, 40, 0, PvAquiferDecision.SEA_BARRIER)
                .put(1, 38, 1, PvAquiferDecision.SEA_WATER).decide(0, 40, 0));
        assertEquals(PvAquiferDecision.AIR, new Neighbourhood().put(0, 40, 0, PvAquiferDecision.SEA_BARRIER).decide(0, 40, 0));
        // near the sea surface a band stays stone, water or not: it fills the hollows next to coasts
        assertEquals(PvAquiferDecision.SEA_BARRIER, new Neighbourhood().put(0, BANDS_KEPT_FROM_Y, 0, PvAquiferDecision.SEA_BARRIER)
                .decide(0, BANDS_KEPT_FROM_Y, 0));
        assertEquals(PvAquiferDecision.AIR, new Neighbourhood().put(0, BANDS_KEPT_FROM_Y - 1, 0, PvAquiferDecision.SEA_BARRIER)
                .decide(0, BANDS_KEPT_FROM_Y - 1, 0));
    }

    @Test
    void corridorsAreBasinWaterWhereTheirNoodleOpens() {
        // S below the basin water threshold, the corridors' noodle open (at most 0): basin water, no tick
        assertEquals(PvAquiferDecision.BASIN_WATER, atPosition(10, 0.0, 0.0, -0.01));
        assertEquals(PvAquiferDecision.BASIN_WATER, atPosition(10, 0.0, 0.0, 0.0));
        // closed: nothing (and without corridors, nothing either)
        assertEquals(PvAquiferDecision.AIR, atPosition(10, 0.0, 0.0, 0.01));
        assertEquals(PvAquiferDecision.AIR, atPosition(10, 0.0, 0.0));
        // only in the corridors' layers, y -3..23
        assertEquals(PvAquiferDecision.AIR, atPosition(CORRIDOR_MAX_Y, 0.0, 0.0, -1.0));
        assertEquals(PvAquiferDecision.AIR, atPosition(BASIN_MIN_Y, 0.0, 0.0, -1.0));
        // the sea's water and band come first
        assertEquals(PvAquiferDecision.SEA_BARRIER, atPosition(10, 0.2, 0.0, -1.0));
    }

    @Test
    void theRestOfTheRules() {
        Neighbourhood flooded = new Neighbourhood().put(0, 10, 0, PvAquiferDecision.SEA_WATER).put(0, SEA_LEVEL, 0, PvAquiferDecision.SEA_WATER);
        assertEquals(PvAquiferDecision.SOLID, PvAquiferRules.decide(0, 10, 0, 0.1, false, flooded));
        assertEquals(PvAquiferDecision.AIR_ABOVE_SEA, flooded.decide(0, SEA_LEVEL, 0));
        assertEquals(PvAquiferDecision.LAVA, PvAquiferRules.decide(0, -60, 0, 0.0, true, flooded));
        assertEquals(Blocks.WATER.defaultBlockState(), PvAquiferDecision.BASIN_WATER.state);
    }

    @Test
    void barriersAreSolidSoCarversLeaveThem() {
        assertNull(PvAquiferDecision.SEA_BARRIER.state);
        assertNull(PvAquiferDecision.BASIN_BARRIER.state);
        assertNull(PvAquiferDecision.HIGH_RIVER_BARRIER.state);
    }

    /** The high river's water: walled in its bed and under it, but open beside its surface, where it spills. */
    @Test
    void theHighRiverSpillsFromItsSurfaceOnly() {
        int y = HIGH_RIVER_Y;
        Neighbourhood river = new Neighbourhood()
                .put(0, y, 0, PvAquiferDecision.HIGH_RIVER_WATER)
                .put(0, y - 1, 0, PvAquiferDecision.HIGH_RIVER_BED_WATER);
        assertEquals(PvAquiferDecision.HIGH_RIVER_WATER, river.decide(0, y, 0));
        assertEquals(PvAquiferDecision.HIGH_RIVER_BED_WATER, river.decide(0, y - 1, 0));
        // open space beside the surface's water stays open, so the water spills into it
        assertEquals(PvAquiferDecision.AIR_ABOVE_SEA, river.decide(1, y, 0));
        assertEquals(PvAquiferDecision.AIR_ABOVE_SEA, river.decide(0, y, -1));
        // a wall beside and under the bed's water
        assertEquals(PvAquiferDecision.HIGH_RIVER_BARRIER, river.decide(1, y - 1, 0));
        assertEquals(PvAquiferDecision.HIGH_RIVER_BARRIER, river.decide(0, y - 2, 0));
        // and under the surface's water where there's no bed under it
        Neighbourhood shallow = new Neighbourhood().put(0, y, 0, PvAquiferDecision.HIGH_RIVER_WATER);
        assertEquals(PvAquiferDecision.HIGH_RIVER_BARRIER, shallow.decide(0, y - 1, 0));
        // nothing above the surface, nor diagonally below it
        assertEquals(PvAquiferDecision.AIR_ABOVE_SEA, river.decide(0, y + 1, 0));
        assertEquals(PvAquiferDecision.AIR_ABOVE_SEA, shallow.decide(1, y - 1, 0));
        // solid stays solid
        assertEquals(PvAquiferDecision.SOLID, PvAquiferRules.decide(0, y, 0, 0.1, false, river));
        assertTrue(PvAquiferDecision.HIGH_RIVER_WATER.needsFluidTick, "the surface's water must tick to spill");
        assertTrue(PvAquiferRules.isWater(PvAquiferDecision.HIGH_RIVER_WATER) && PvAquiferRules.isWater(PvAquiferDecision.HIGH_RIVER_BED_WATER));
    }
}
