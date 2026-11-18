package frootloops.versus.mod.items_and_effects.containers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The peek takes the wheel and the use only while it is drawn and wanted. */
class PeekRulesTest {

    @Test
    void thePeekTakesInputWhileItShows() {
        assertTrue(PeekRules.takesInput(true, true, false, false, false, true));
    }

    @Test
    void anUnseenOrUnwantedPeekTakesNothing() {
        assertFalse(PeekRules.takesInput(false, true, false, false, false, true), "the peek off");
        assertFalse(PeekRules.takesInput(true, false, false, false, false, true), "nothing to show");
        assertFalse(PeekRules.takesInput(true, true, true, false, false, true), "a screen open");
        assertFalse(PeekRules.takesInput(true, true, false, true, false, true), "sneaking");
    }

    @Test
    void withTheHudHiddenThePeekTakesNothing() {
        // F1: the peek isn't drawn, so the wheel and the use stay the game's
        assertFalse(PeekRules.takesInput(true, true, false, false, true, true));
    }

    @Test
    void aPeekTakesNothingWithSomethingInTheHand() {
        // The wheel and the use stay the game's (the hotbar's wheel, placing and using what is held)
        assertFalse(PeekRules.takesInput(true, true, false, false, false, false));
    }
}
