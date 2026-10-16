package frootloops.versus.mod.items_and_effects.inventory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HotbarRefillRulesTest {

    @Test
    void aStackThatRanOutIsRefilled() {
        assertTrue(HotbarRefillRules.usedUp(true, true, true, false));
    }

    @Test
    void nothingIsRefilledUnlessItWasAStackableItemThatIsGoneFromTheSameHand() {
        assertFalse(HotbarRefillRules.usedUp(false, true, true, false), "a hand that held nothing, or a tool");
        assertFalse(HotbarRefillRules.usedUp(true, false, true, false), "still holding something");
        assertFalse(HotbarRefillRules.usedUp(true, true, false, false), "the player changed slot");
    }

    @Test
    void droppingOrSwappingHandsIsNotRunningOut() {
        assertFalse(HotbarRefillRules.usedUp(true, true, true, true));
    }

    @Test
    void theBiggestStackRefills() {
        assertEquals(2, HotbarRefillRules.largest(new int[]{0, 12, 40, 7}));
    }

    @Test
    void equalStacksRefillFromTheFirst() {
        assertEquals(1, HotbarRefillRules.largest(new int[]{0, 16, 16, 3}));
    }

    @Test
    void noMatchingStackMeansNoRefill() {
        assertEquals(-1, HotbarRefillRules.largest(new int[]{0, 0, 0}));
        assertEquals(-1, HotbarRefillRules.largest(new int[0]));
    }
}
