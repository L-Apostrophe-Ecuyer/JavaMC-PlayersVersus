package frootloops.versus.mod.mobs.hostile.end;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How well a gaze sees a warped thing: fully when stared at, not at all a little aside. */
class GazeTest {
    private static final Vec3 AHEAD = new Vec3(0.0, 0.0, 1.0);

    /** A thing straight ahead, at this many degrees aside, this far off. */
    private static Vec3 aside(double degrees, double distance) {
        double radians = Math.toRadians(degrees);
        return new Vec3(Math.sin(radians) * distance, 0.0, Math.cos(radians) * distance);
    }

    @Test
    void staredAtItShowsFully() {
        assertEquals(1.0F, Gaze.visibility(AHEAD, aside(0.0, 20.0), 1.0), 1.0E-6F);
    }

    @Test
    void aLittleAsideItsGone() {
        // A body 1 block across at 20 blocks spans about 3 degrees each way: 25 degrees aside is well out of it
        assertEquals(0.0F, Gaze.visibility(AHEAD, aside(25.0, 20.0), 1.0), 1.0E-6F);
        assertEquals(0.0F, Gaze.visibility(AHEAD, aside(180.0, 20.0), 1.0), 1.0E-6F);
    }

    @Test
    void itFadesInAsTheGazeNears() {
        float far = Gaze.visibility(AHEAD, aside(10.0, 20.0), 1.0);
        float near = Gaze.visibility(AHEAD, aside(5.0, 20.0), 1.0);
        assertTrue(far < near && near < 1.0F && far > 0.0F, far + " then " + near);
    }

    @Test
    void lookingAnywhereOnABigCloseBodyIsStaring() {
        // A watchling's 2.9 blocks, 4 blocks off: its edge is about 20 degrees from its middle
        assertEquals(1.0F, Gaze.visibility(AHEAD, aside(18.0, 4.0), 1.45), 1.0E-6F);
    }

    @Test
    void halfSeenIsWatched() {
        assertTrue(Gaze.watched(Gaze.visibility(AHEAD, aside(2.0, 20.0), 1.0)));
        assertTrue(!Gaze.watched(Gaze.visibility(AHEAD, aside(25.0, 20.0), 1.0)));
    }
}
