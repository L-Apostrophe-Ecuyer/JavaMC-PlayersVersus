package frootloops.versus.mod.mobs;

import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Night, for the pale wolves' forest spawns: dusk to dawn, any day. */
class MobSpawningTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    @Test
    void nightIsFromDuskToDawn() {
        assertTrue(MobSpawning.isNight(13000L));
        assertTrue(MobSpawning.isNight(18000L));
        assertTrue(MobSpawning.isNight(23000L));
        assertFalse(MobSpawning.isNight(12999L));
        assertFalse(MobSpawning.isNight(6000L));
        assertFalse(MobSpawning.isNight(23001L));
    }

    @Test
    void nightComesEveryDay() {
        assertTrue(MobSpawning.isNight(24000L * 7 + 18000L));
        assertFalse(MobSpawning.isNight(24000L * 7 + 6000L));
    }
}
