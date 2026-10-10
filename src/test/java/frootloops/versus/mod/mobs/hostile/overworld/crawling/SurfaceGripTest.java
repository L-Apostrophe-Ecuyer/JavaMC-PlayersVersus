package frootloops.versus.mod.mobs.hostile.overworld.crawling;

import frootloops.versus.mod.environment.worldgen.TestGame;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurfaceGripTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    private static int touching(Direction... faces) {
        int mask = 0;
        for (Direction face : faces) mask |= SurfaceGrip.bit(face);
        return mask;
    }

    /** Standing or walking on the ground is the floor, even beside a wall; crawling up or down the wall takes the wall. */
    @Test
    void theFloorUntilItCrawlsUp() {
        assertEquals(Direction.DOWN, SurfaceGrip.chooseFace(touching(Direction.DOWN), 0.0, 0.0, 0.0, Direction.DOWN, true));
        assertEquals(Direction.DOWN, SurfaceGrip.chooseFace(touching(Direction.DOWN, Direction.NORTH), 0.1, 0.0, 0.0, Direction.DOWN, true));
        assertEquals(Direction.NORTH, SurfaceGrip.chooseFace(touching(Direction.DOWN, Direction.NORTH), 0.0, 0.15, -0.01, Direction.DOWN, true));
        assertEquals(Direction.NORTH, SurfaceGrip.chooseFace(touching(Direction.NORTH), 0.0, -0.15, 0.0, Direction.NORTH, false));
    }

    /** On a wall it keeps its grip standing still, and walking along it sideways; in a corner it takes the wall it runs along. */
    @Test
    void wallsHoldOn() {
        assertEquals(Direction.EAST, SurfaceGrip.chooseFace(touching(Direction.EAST), 0.0, 0.0, 0.0, Direction.EAST, false));
        assertEquals(Direction.EAST, SurfaceGrip.chooseFace(touching(Direction.EAST), 0.0, 0.0, 0.12, Direction.EAST, false));
        assertEquals(Direction.EAST, SurfaceGrip.chooseFace(touching(Direction.EAST, Direction.SOUTH), 0.0, 0.14, 0.0, Direction.EAST, false), "straight up a corner: the wall it held");
        assertEquals(Direction.SOUTH, SurfaceGrip.chooseFace(touching(Direction.EAST, Direction.SOUTH), -0.05, 0.14, 0.0, Direction.EAST, false), "up and away from the east wall: along the south one");
        assertEquals(Direction.EAST, SurfaceGrip.chooseFace(touching(Direction.EAST, Direction.SOUTH), 0.0, 0.0, 0.0, Direction.EAST, false));
    }

    /** Under a ceiling, crawling across it, it's the ceiling; going down a wall from it, the wall. Touching nothing, the floor. */
    @Test
    void ceilingsAndLettingGo() {
        assertEquals(Direction.UP, SurfaceGrip.chooseFace(touching(Direction.UP, Direction.WEST), 0.12, 0.0, 0.0, Direction.WEST, false));
        assertEquals(Direction.UP, SurfaceGrip.chooseFace(touching(Direction.UP), 0.0, 0.0, 0.0, Direction.UP, false));
        assertEquals(Direction.WEST, SurfaceGrip.chooseFace(touching(Direction.UP, Direction.WEST), 0.0, -0.14, 0.0, Direction.UP, false));
        assertEquals(Direction.DOWN, SurfaceGrip.chooseFace(0, 0.0, -0.3, 0.0, Direction.UP, false));
    }

    @Test
    void normalsPointOutOfTheSurface() {
        assertEquals(new Vec3(0.0, 1.0, 0.0), SurfaceGrip.normal(Direction.DOWN));
        assertEquals(new Vec3(0.0, -1.0, 0.0), SurfaceGrip.normal(Direction.UP));
        assertEquals(new Vec3(0.0, 0.0, 1.0), SurfaceGrip.normal(Direction.NORTH));
        assertEquals(new Vec3(-1.0, 0.0, 0.0), SurfaceGrip.normal(Direction.EAST));
    }

    /** On a wall a spider heads the way it crawls along the wall, or toward its target when still; straight out of the wall is no heading. */
    @Test
    void headingsLieAlongTheSurface() {
        Vector3f up = GripState.heading(Direction.NORTH, new Vec3(0.0, 0.2, -0.05), null);
        assertEquals(1.0F, up.y, 1.0E-6F);
        Vector3f sideways = GripState.heading(Direction.NORTH, Vec3.ZERO, new Vec3(3.0, 0.0, 2.0));
        assertEquals(1.0F, sideways.x, 1.0E-6F);
        assertNull(GripState.heading(Direction.NORTH, new Vec3(0.0, 0.0, 0.3), null));
        assertTrue(GripState.worthSyncing(new Vector3f(0.0F, 1.0F, 0.0F), new Vector3f(1.0F, 0.0F, 0.0F)));
    }

    /** Clients swing the model to a new surface over a few ticks, and flip straight over when it falls off a ceiling. */
    @Test
    void theModelTurnsSmoothly() {
        GripState state = new GripState();
        state.turnToward(Direction.NORTH, new Vector3f(0.0F, 1.0F, 0.0F), 0.0F);
        Vector3f halfway = state.normal(1.0F);
        assertTrue(halfway.y > 0.1F && halfway.z > 0.1F, "turning toward the wall: " + halfway);
        for (int tick = 0; tick < 20; tick++) state.turnToward(Direction.NORTH, new Vector3f(0.0F, 1.0F, 0.0F), 0.0F);
        assertEquals(1.0F, state.normal(1.0F).z, 1.0E-3F);
        for (int tick = 0; tick < 20; tick++) state.turnToward(Direction.UP, new Vector3f(1.0F, 0.0F, 0.0F), 0.0F);
        state.turnToward(Direction.DOWN, new Vector3f(1.0F, 0.0F, 0.0F), 0.0F);
        assertEquals(1.0F, state.normal(1.0F).y, 1.0E-6F, "off the ceiling, straight onto its feet");
    }

    /** The spider mixin applies on 26.3: spiders crawl, and their legs move with the height they crawl. */
    @Test
    void theGripMixinApplies() throws NoSuchMethodException {
        assertTrue(SurfaceCrawler.class.isAssignableFrom(Spider.class), "spiders don't crawl");
        Spider.class.getDeclaredMethod("calculateEntityAnimation", boolean.class);
    }
}
