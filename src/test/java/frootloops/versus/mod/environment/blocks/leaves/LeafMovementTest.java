package frootloops.versus.mod.environment.blocks.leaves;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeafMovementTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    /** The closed form matches a fall stepped tick by tick as vanilla does: move, then gravity, then drag. */
    @Test
    void theEquivalentFallHeightIsVanillasFreeFall() {
        double speed = 0.0, fallen = 0.0;
        for (int tick = 1; tick <= 80; tick++) {
            speed = (speed + 0.08) * 0.98;
            fallen += speed;
            assertEquals(fallen, LeafMovement.equivalentFallHeight(speed), 1.0e-6, "after " + tick + " ticks");
        }
        assertEquals(0.0, LeafMovement.equivalentFallHeight(0.0));
        assertTrue(LeafMovement.equivalentFallHeight(3.92) > 1.0e6, "terminal speed is never reached");
    }

    /** About 0.62 blocks per tick is the speed of a 3-block fall, the safe fall distance. */
    @Test
    void aSafeFallsSpeed() {
        double height = LeafMovement.equivalentFallHeight(0.62);
        assertTrue(height > 2.8 && height < 3.3, "0.62 blocks per tick is a fall of " + height);
    }

    /** Under the cap nothing is slowed; above it the speed goes down to the cap, losing at most its share a tick. */
    @Test
    void theSpeedCapTightensTheDeeperIn() {
        double inside = VersusSettings.Leaves.SPEED_CAP_INSIDE, brushing = VersusSettings.Leaves.SPEED_CAP_BRUSHING;
        double mostLost = VersusSettings.Leaves.MOST_SPEED_LOST_PER_TICK;
        assertEquals(1.0, LeafMovement.horizontalKeep(0.065, 1.0), "sneaking isn't slowed, even fully inside");
        assertEquals(1.0, LeafMovement.horizontalKeep(0.28, 0.0), "sprinting barely touching leaves isn't slowed");
        assertEquals(inside / 0.15, LeafMovement.horizontalKeep(0.15, 1.0), 1.0e-9, "just over the cap: down to it");
        assertEquals(1.0 - mostLost, LeafMovement.horizontalKeep(1.0, 1.0), 1.0e-9, "far over: the most a tick");
        double walkingInABush = 0.216 * LeafMovement.horizontalKeep(0.216, 0.78);
        assertTrue(walkingInABush < 0.19 && walkingInABush > 0.14, "walking through a waist-high bush: " + walkingInABush);
        for (double immersion = 0.0; immersion < 1.0; immersion += 0.1) {
            assertTrue(LeafMovement.horizontalKeep(0.28, immersion + 0.1) <= LeafMovement.horizontalKeep(0.28, immersion),
                    "deeper is never faster");
        }
        assertTrue(brushing > 0.28, "the cap barely touching leaves is above sprinting");
    }

    /** Settling into leaves at a steady walk: the speed a tick ends at is the cap, whatever it came in at. */
    @Test
    void walkingSettlesAtTheCap() {
        double speed = 0.28, cap = VersusSettings.Leaves.SPEED_CAP_INSIDE;
        for (int tick = 0; tick < 40; tick++) {
            double moved = speed * LeafMovement.horizontalKeep(speed, 1.0);
            speed = moved * 0.546 + 0.098; // ground friction, then a walking step's input
        }
        assertEquals(cap, speed * LeafMovement.horizontalKeep(speed, 1.0), 1.0e-6);
    }

    /** Leaves shed nothing to something standing in them, a little to a walk, a lot to a crash; never above the most. */
    @Test
    void sheddingGrowsWithSpeed() {
        assertEquals(0.0, LeafLitter.shedChance(0.08), "gravity's tug on something standing still");
        double walking = LeafLitter.shedChance(0.2), crashing = LeafLitter.shedChance(1.0);
        assertTrue(walking > 0.0 && walking < 0.005, "walking: " + walking);
        assertTrue(crashing > 0.05, "crashing: " + crashing);
        assertEquals(VersusSettings.Leaves.MOST_SHED_CHANCE, LeafLitter.shedChance(4.0));
    }
}
