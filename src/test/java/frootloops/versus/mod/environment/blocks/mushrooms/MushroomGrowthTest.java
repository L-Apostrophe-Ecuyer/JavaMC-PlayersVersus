package frootloops.versus.mod.environment.blocks.mushrooms;

import frootloops.versus.mod.environment.blocks.mushrooms.MushroomGrowth.Action;
import frootloops.versus.mod.environment.blocks.mushrooms.MushroomGrowth.Conditions;
import org.junit.jupiter.api.Test;

import static frootloops.versus.mod.environment.blocks.mushrooms.MushroomGrowth.Kind.BROWN;
import static frootloops.versus.mod.environment.blocks.mushrooms.MushroomGrowth.Kind.PALE;
import static frootloops.versus.mod.environment.blocks.mushrooms.MushroomGrowth.Kind.RED;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** What a small mushroom does on a random tick, by its stage and what's around it. */
class MushroomGrowthTest {
    /** Dark, on stone, beside another of its kind (not grown), above y 0. */
    private static final Conditions GOOD = new Conditions(true, true, true, false, false);

    @Test
    void aMushroomInTheDarkBesideAnotherGrowsToTwo() {
        assertEquals(Action.GROW, MushroomGrowth.next(BROWN, 1, GOOD));
        assertEquals(Action.GROW, MushroomGrowth.next(RED, 1, GOOD));
    }

    @Test
    void aMushroomAloneInTheLightOrOnTheWrongGroundStaysAsItIs() {
        assertEquals(Action.NONE, MushroomGrowth.next(BROWN, 1, new Conditions(false, true, true, false, false)));
        assertEquals(Action.NONE, MushroomGrowth.next(BROWN, 1, new Conditions(true, false, true, false, false)));
        assertEquals(Action.NONE, MushroomGrowth.next(BROWN, 1, new Conditions(true, true, false, false, false)));
    }

    @Test
    void stageTwoGrowsOnlyBesideAGrownOneAndShrinksWhenItCantGrow() {
        assertEquals(Action.NONE, MushroomGrowth.next(BROWN, 2, GOOD));
        assertEquals(Action.GROW, MushroomGrowth.next(BROWN, 2, new Conditions(true, true, true, true, false)));
        assertEquals(Action.SHRINK, MushroomGrowth.next(BROWN, 2, new Conditions(false, true, true, true, false)));
    }

    @Test
    void stageThreeSpreadsWhileItCanGrowAndShrinksWhenNot() {
        assertEquals(Action.SPREAD, MushroomGrowth.next(RED, 3, GOOD));
        assertEquals(Action.SHRINK, MushroomGrowth.next(RED, 3, new Conditions(true, true, false, false, false)));
    }

    @Test
    void belowZeroMushroomsGrowIntoPaleShroomsWhichOnlyGrowThere() {
        Conditions deep = new Conditions(true, true, true, false, true);
        assertEquals(Action.TURN_PALE, MushroomGrowth.next(BROWN, 1, deep));
        assertEquals(Action.TURN_PALE, MushroomGrowth.next(RED, 1, deep));
        assertEquals(Action.GROW, MushroomGrowth.next(PALE, 1, deep));
        assertEquals(Action.NONE, MushroomGrowth.next(PALE, 1, GOOD));
        assertEquals(Action.SHRINK, MushroomGrowth.next(BROWN, 2, deep));
        assertEquals(Action.SHRINK, MushroomGrowth.next(PALE, 3, GOOD));
    }

    @Test
    void theLightGrowsWithTheStage() {
        assertEquals(2, MushroomGrowth.light(2));
        assertEquals(4, MushroomGrowth.light(3));
    }
}
