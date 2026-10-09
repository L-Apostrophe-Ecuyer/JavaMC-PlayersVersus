package frootloops.versus.mod.mobs.hostile.overworld.warden;

import frootloops.versus.mod.environment.worldgen.TestGame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.phys.Vec3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TrailTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    /** A warden heads where its prey was 40 ticks ago; until then, where it first saw it. */
    @Test
    void theTrailLagsFortyTicksBehind() {
        Trail trail = new Trail(40);
        Object prey = new Object();
        assertNull(trail.oldest());
        for (int tick = 0; tick < 10; tick++) trail.record(prey, new Vec3(tick, 0.0, 0.0));
        assertEquals(new Vec3(0.0, 0.0, 0.0), trail.oldest());
        for (int tick = 10; tick < 100; tick++) trail.record(prey, new Vec3(tick, 0.0, 0.0));
        assertEquals(new Vec3(60.0, 0.0, 0.0), trail.oldest(), "40 ticks behind tick 99");
    }

    /** Chasing someone else starts the trail over. */
    @Test
    void aNewPreyStartsANewTrail() {
        Trail trail = new Trail(40);
        for (int tick = 0; tick < 50; tick++) trail.record("first", new Vec3(tick, 0.0, 0.0));
        trail.record("second", new Vec3(0.0, 5.0, 0.0));
        assertEquals(new Vec3(0.0, 5.0, 0.0), trail.oldest());
        trail.clear();
        assertNull(trail.oldest());
    }
}
