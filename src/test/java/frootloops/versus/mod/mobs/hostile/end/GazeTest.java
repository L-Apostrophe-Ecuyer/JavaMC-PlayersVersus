package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusSettings;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How well a gaze sees a warped thing: fully when stared at, less and less as it turns aside, never quite not at all. */
class GazeTest {
    private static final Vec3 AHEAD = new Vec3(0.0, 0.0, 1.0);

    /** A thing this many degrees aside of straight ahead, this far off. */
    private static Vec3 aside(double degrees, double distance) {
        double radians = Math.toRadians(degrees);
        return new Vec3(Math.sin(radians) * distance, 0.0, Math.cos(radians) * distance);
    }

    @Test
    void staredAtItShowsFully() {
        assertEquals(1.0F, Gaze.visibility(AHEAD, aside(0.0, 20.0), 1.0), 1.0E-6F);
    }

    @Test
    void farAsideItsLeastSeen() {
        assertEquals(0.0F, Gaze.visibility(AHEAD, aside(60.0, 20.0), 1.0), 1.0E-6F);
        assertEquals(0.0F, Gaze.visibility(AHEAD, aside(180.0, 20.0), 1.0), 1.0E-6F);
    }

    @Test
    void itFadesInGraduallyAsTheGazeNears() {
        float far = Gaze.visibility(AHEAD, aside(30.0, 20.0), 1.0);
        float middle = Gaze.visibility(AHEAD, aside(20.0, 20.0), 1.0);
        float near = Gaze.visibility(AHEAD, aside(12.0, 20.0), 1.0);
        assertTrue(0.0F < far && far < middle && middle < near && near < 1.0F, far + ", " + middle + ", " + near);
    }

    @Test
    void lookingAnywhereOnABigCloseBodyIsStaring() {
        // A watchling's 2.9 blocks, 4 blocks off: its edge is about 20 degrees from its middle
        assertEquals(1.0F, Gaze.visibility(AHEAD, aside(18.0, 4.0), 1.45), 1.0E-6F);
    }

    @Test
    void itsNeverQuiteInvisible() {
        assertEquals(VersusSettings.Gaze.LEAST_OPACITY, Gaze.opacity(0.0F), 1.0E-6F);
        assertTrue(Gaze.opacity(0.0F) > 0.0F);
        assertEquals(1.0F, Gaze.opacity(1.0F), 1.0E-6F);
        assertTrue(Gaze.opacity(0.3F) < Gaze.opacity(0.6F));
    }

    @Test
    void aGlanceIsntAStare() {
        assertTrue(Gaze.watched(Gaze.visibility(AHEAD, aside(2.0, 20.0), 1.0)));
        assertFalse(Gaze.watched(Gaze.visibility(AHEAD, aside(25.0, 20.0), 1.0)));
    }
}
