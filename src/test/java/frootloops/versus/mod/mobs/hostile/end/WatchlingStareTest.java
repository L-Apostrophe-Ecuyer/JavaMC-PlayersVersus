package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A watchling's static: how it builds while looked at and clears while not (what counts as looking: GazeTest). */
class WatchlingStareTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    @Test
    void theStaticFillsTheScreenAfterStareTicksOfLooking() {
        float stare = 0.0F;
        for (int tick = 0; tick < VersusSettings.Watchlings.STARE_TICKS - 1; tick++) stare = WatchlingStare.next(stare, true);
        assertTrue(stare < 1.0F, "full before its time: " + stare);
        assertTrue(WatchlingStare.next(stare, true) >= 0.999F, "not full after its time");
    }

    @Test
    void lookingAwayClearsItAndNeverBelowNothing() {
        assertEquals(0.5F - 1.0F / VersusSettings.Watchlings.CLEAR_TICKS, WatchlingStare.next(0.5F, false), 1.0E-6F);
        assertEquals(0.0F, WatchlingStare.next(0.0F, false));
    }
}
