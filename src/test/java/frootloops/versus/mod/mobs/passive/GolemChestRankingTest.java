package frootloops.versus.mod.mobs.passive;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GolemChestRankingTest {

    private static final double FAR = 32 * 32 * 3;

    @Test
    void aChestWithNoRoomIsNeverChosen() {
        assertEquals(Double.POSITIVE_INFINITY, GolemChestRanking.cost(0, 500, false, 1));
        assertEquals(Double.POSITIVE_INFINITY, GolemChestRanking.cost(0, 0, true, 1));
    }

    @Test
    void aChestAlreadyHoldingTheItemBeatsAnEmptyOneAtAnyDistance() {
        double holding = GolemChestRanking.cost(8, 1, false, FAR);
        double empty = GolemChestRanking.cost(1728, 0, true, 0);
        assertTrue(holding < empty, "a single cobblestone across the base still beats the empty chest next door");
    }

    @Test
    void theChestHoldingMostOfTheItemWinsEvenWhenFurther() {
        double storage = GolemChestRanking.cost(64, 1500, false, FAR);
        double scrap = GolemChestRanking.cost(64, 3, false, 4);
        assertTrue(storage < scrap);
    }

    @Test
    void theNearerChestBreaksATie() {
        assertTrue(GolemChestRanking.cost(64, 100, false, 4) < GolemChestRanking.cost(64, 100, false, 100));
        assertTrue(GolemChestRanking.cost(1728, 0, true, 4) < GolemChestRanking.cost(1728, 0, true, 100));
    }

    @Test
    void aChestHoldingOnlyOtherThingsIsTheLastResortAndNeverBeatsAnEmptyOne() {
        double other = GolemChestRanking.cost(40, 0, false, 0);
        assertTrue(other < Double.POSITIVE_INFINITY, "better than holding the item for good");
        assertTrue(other > GolemChestRanking.cost(1728, 0, true, FAR), "an empty chest at the edge of range still comes first");
        assertTrue(other > GolemChestRanking.cost(1, Integer.MAX_VALUE, false, FAR));
    }

    @Test
    void theLastResortIsTheFurthestChestWithRoom() {
        assertTrue(GolemChestRanking.cost(40, 0, false, FAR) < GolemChestRanking.cost(40, 0, false, 4));
        assertEquals(Double.POSITIVE_INFINITY, GolemChestRanking.cost(0, 0, false, FAR), "but not one that is full");
    }

    @Test
    void aHugeStockStillRanksAheadOfEveryEmptyChest() {
        double stocked = GolemChestRanking.cost(1, Integer.MAX_VALUE, false, FAR);
        assertTrue(stocked < GolemChestRanking.cost(1728, 0, true, 0));
    }
}
