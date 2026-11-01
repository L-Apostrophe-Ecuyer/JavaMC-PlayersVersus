package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.worldgen.TestGame;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A watchling's static: how it builds while looked at and clears while not, and what counts as looking at it. */
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

    @Test
    void lookingMeansNearTheMiddleOfTheView() {
        Vec3 ahead = new Vec3(0.0, 0.0, 1.0);
        assertTrue(WatchlingStare.looking(ahead, ahead));
        // A little off the middle (about 15 degrees) still counts; well aside (about 45) doesn't
        assertTrue(WatchlingStare.looking(ahead, new Vec3(0.26, 0.0, 0.966)));
        assertFalse(WatchlingStare.looking(ahead, new Vec3(0.707, 0.0, 0.707)));
        assertFalse(WatchlingStare.looking(ahead, new Vec3(0.0, 0.0, -1.0)));
    }
}
