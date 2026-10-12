package frootloops.versus.mod.items_and_effects.recipes;

import org.junit.jupiter.api.Test;

import static frootloops.versus.mod.items_and_effects.recipes.RecipeGroupHover.Action.CLOSE;
import static frootloops.versus.mod.items_and_effects.recipes.RecipeGroupHover.Action.NONE;
import static frootloops.versus.mod.items_and_effects.recipes.RecipeGroupHover.Action.OPEN;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RecipeGroupHoverTest {
    private static final long DWELL = RecipeGroupHover.DWELL_MILLIS;

    @Test
    void aGroupOpensOnceTheMouseHasStayedOnItForTheDwell() {
        RecipeGroupHover<String> hover = new RecipeGroupHover<>();
        assertEquals(NONE, hover.update("oak", false, false, 1000));
        assertEquals(NONE, hover.update("oak", false, false, 1000 + DWELL - 1));
        assertEquals(OPEN, hover.update("oak", false, false, 1000 + DWELL));
    }

    @Test
    void movingToAnotherGroupStartsTheDwellAgain() {
        RecipeGroupHover<String> hover = new RecipeGroupHover<>();
        hover.update("oak", false, false, 0);
        assertEquals(NONE, hover.update("birch", false, false, DWELL));
        assertEquals(NONE, hover.update("birch", false, false, DWELL + DWELL - 1));
        assertEquals(OPEN, hover.update("birch", false, false, DWELL * 2));
    }

    @Test
    void leavingBeforeTheDwellOpensNothing() {
        RecipeGroupHover<String> hover = new RecipeGroupHover<>();
        hover.update("oak", false, false, 0);
        assertEquals(NONE, hover.update(null, false, false, DWELL - 1));
        assertEquals(NONE, hover.update("oak", false, false, DWELL));
        assertEquals(OPEN, hover.update("oak", false, false, DWELL * 2));
    }

    @Test
    void anOpenPopupStaysWhileTheMouseIsOverItsIconOrItself() {
        RecipeGroupHover<String> hover = new RecipeGroupHover<>();
        assertEquals(NONE, hover.update("oak", true, true, 0));
        assertEquals(NONE, hover.update(null, true, true, 10_000));
    }

    @Test
    void anOpenPopupRetractsWhenTheMouseIsOutsideBoth() {
        RecipeGroupHover<String> hover = new RecipeGroupHover<>();
        assertEquals(CLOSE, hover.update(null, true, false, 0));
        assertEquals(CLOSE, hover.update("birch", true, false, 1));
    }

    @Test
    void afterRetractingAnotherGroupUnderTheMouseOpensAfterItsDwell() {
        RecipeGroupHover<String> hover = new RecipeGroupHover<>();
        assertEquals(CLOSE, hover.update("birch", true, false, 0));
        assertEquals(NONE, hover.update("birch", false, false, 1));
        assertEquals(OPEN, hover.update("birch", false, false, 1 + DWELL));
    }

    @Test
    void aClickedGroupStaysClosedUntilTheMouseLeavesIt() {
        RecipeGroupHover<String> hover = new RecipeGroupHover<>();
        hover.suppress("oak");
        assertEquals(NONE, hover.update("oak", false, false, 0));
        assertEquals(NONE, hover.update("oak", false, false, DWELL * 10));
        assertEquals(NONE, hover.update(null, false, false, DWELL * 11));
        assertEquals(NONE, hover.update("oak", false, false, DWELL * 12));
        assertEquals(OPEN, hover.update("oak", false, false, DWELL * 13));
    }

    @Test
    void aClickedGroupDoesNotStopAnotherFromOpening() {
        RecipeGroupHover<String> hover = new RecipeGroupHover<>();
        hover.suppress("oak");
        hover.update("birch", false, false, 0);
        assertEquals(OPEN, hover.update("birch", false, false, DWELL));
    }

    @Test
    void theDwellIsTwoToFourTicks() {
        assertEquals(true, RecipeGroupHover.DWELL_MILLIS >= 2 * 50 && RecipeGroupHover.DWELL_MILLIS <= 4 * 50);
    }
}
