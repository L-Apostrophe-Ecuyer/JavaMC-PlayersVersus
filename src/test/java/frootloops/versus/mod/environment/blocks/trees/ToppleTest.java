package frootloops.versus.mod.environment.blocks.trees;

import frootloops.versus.mod.environment.worldgen.TestGame;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToppleTest {

    @BeforeAll
    static void start() {
        TestGame.start();
    }

    /** The centres of a column of blocks standing on (0, 1, 0)'s block, this many high. */
    private static List<Vec3> column(int height) {
        List<Vec3> centres = new ArrayList<>();
        for (int y = 1; y <= height; y++) centres.add(new Vec3(0.5, y + 0.5, 0.5));
        return centres;
    }

    /** That column falling east on the east edge of its foot. */
    private static Topple fallingEast(List<Vec3> centres) {
        return Topple.of(new Vec3(1.0, 1.0, 0.5), Direction.EAST, centres.stream().map(centre -> new Topple.Mass(centre, 1.0)).toList());
    }

    private static int stepsDown(Topple topple) {
        int steps = 0;
        while (!topple.isDown() && steps < 1000) {
            topple.step();
            steps++;
        }
        return steps;
    }

    /** Before its first step a topple stands where its blocks stood. */
    @Test
    void itStartsWhereTheTreeStood() {
        List<Vec3> column = column(6);
        Topple topple = fallingEast(column);
        for (Vec3 centre : column) assertEquals(0.0, topple.centre(centre).distanceTo(centre), 1.0E-12);
    }

    /**
     * A six-block trunk comes down in about two seconds, never stopping on the way and far faster at the end than the
     * push it started with, and lies along the way it fell on the ground beyond its foot.
     */
    @Test
    void aTrunkComesDownFasterAndFasterAndLiesAlongItsWay() {
        List<Vec3> column = column(6);
        Topple topple = fallingEast(column);
        double push = topple.speed();
        int steps = 0;
        while (!topple.isDown()) {
            topple.step();
            steps++;
            assertTrue(topple.speed() > 0.0, "it stopped at step " + steps);
        }
        assertTrue(steps >= 20 && steps <= 80, "a six-block trunk took " + steps + " ticks to come down");
        assertTrue(topple.speed() > 3.0 * push, "it came down at " + topple.speed() + " a tick, pushed at " + push);
        for (int i = 0; i < column.size(); i++) {
            Vec3 lying = topple.centre(column.get(i));
            assertEquals(1.5 + i, lying.x, 1.0E-9, "block " + i + " lies at x " + lying.x);
            assertEquals(1.5, lying.y, 1.0E-9, "block " + i + " lies at y " + lying.y);
            assertEquals(0.5, lying.z, 1.0E-9, "block " + i + " moved across, to z " + lying.z);
        }
        Vec3 top = topple.velocity(column.getLast());
        assertTrue(top.y < -0.5, "its top comes down at " + top);
    }

    /** Each tick a block moves about as far as its speed says, so a block let go flies on as it moved. */
    @Test
    void itsBlocksMoveAsFastAsTheirSpeedSays() {
        List<Vec3> column = column(8);
        Topple topple = fallingEast(column);
        for (int i = 0; i < 15; i++) topple.step();
        Vec3 top = column.getLast(), before = topple.centre(top);
        topple.step();
        Vec3 moved = topple.centre(top).subtract(before), speed = topple.velocity(top);
        assertEquals(0.0, moved.subtract(speed).length(), 0.1 * speed.length(), "it moved " + moved + " with a speed of " + speed);
    }

    /** A tree whose weight sits a little behind its hinge still goes over: the cut's push sees it past the top. */
    @Test
    void aTreeLeaningBackALittleStillGoesOver() {
        List<Topple.Mass> masses = new ArrayList<>();
        for (Vec3 centre : column(6)) masses.add(new Topple.Mass(centre, 1.0));
        masses.add(new Topple.Mass(new Vec3(-0.5, 6.5, 0.5), 1.0));
        masses.add(new Topple.Mass(new Vec3(-1.5, 6.5, 0.5), 1.0));
        Topple topple = Topple.of(new Vec3(1.0, 1.0, 0.5), Direction.EAST, masses);
        int steps = 0;
        while (!topple.isDown() && steps < 400) {
            topple.step();
            steps++;
            assertTrue(topple.speed() > 0.0, "it tipped back at step " + steps);
        }
        assertTrue(topple.isDown(), "it never came down");
    }

    /** Undoing a step puts it back where the step started. */
    @Test
    void undoingAStepPutsItBack() {
        Topple topple = fallingEast(column(6));
        for (int i = 0; i < 10; i++) topple.step();
        double angle = topple.angle(), speed = topple.speed();
        topple.step();
        topple.undo();
        assertEquals(angle, topple.angle(), 0.0);
        assertEquals(speed, topple.speed(), 0.0);
    }

    /** A log sweeping into a creature hits as an anvil reaching its speed would: nothing slow, two a block after the first, ten at most. */
    @Test
    void aSweepHitsAsHardAsAFallToItsSpeed() {
        assertEquals(0.0f, TreeFelling.sweepDamage(0.2), 1.0E-6);
        assertEquals(8.0f, TreeFelling.sweepDamage(0.6), 1.0E-6);
        assertEquals(10.0f, TreeFelling.sweepDamage(2.0), 1.0E-6);
    }

    /** Started from the numbers the server sends, a client's topple turns exactly as the server's. */
    @Test
    void aClientTurnsItExactlyAsTheServer() {
        Topple server = fallingEast(column(7));
        Topple client = new Topple(server.hinge(), server.way(), server.along(), server.up(), server.inertia(), server.angle(), server.speed());
        while (!server.isDown()) {
            server.step();
            client.step();
            assertEquals(server.angle(), client.angle(), 0.0);
        }
    }
}
