package frootloops.versus.mod.environment.blocks.trees;

import java.util.Collection;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * A felled tree turning over on its hinge as one body, stepped a tick at a time the same way on the server
 * (TreeFelling) and on its clients, so they agree without being told each step. The hinge is a level line across the
 * way the tree falls, at the foot of its front; gravity pulls its weight round it as a rigid body's would:
 * angle'' = g (up sin angle + along cos angle) / inertia, along and up the weighted sums of how far each block's centre
 * sits along the way and up from the hinge, inertia their moment round it. The cut gives it a push that sees it past
 * the point where it would tip back, and it stops lying down, at a right angle.
 */
public final class Topple {

    /** What pulls it down: a falling block's gravity, in blocks a tick per tick. */
    public static final double GRAVITY = 0.04;
    /** How fast the cut starts a tree turning at the least, in radians a tick. */
    public static final double PUSH = 0.02;
    /** Lying down. */
    public static final double DOWN = Math.PI / 2.0;
    /** A block's own moment round its centre, for its weight: a unit cube's, turning on an axis through it. */
    private static final double OWN_INERTIA = 1.0 / 6.0;

    /** A block of the tree: where its centre starts, and what it weighs. */
    public record Mass(Vec3 centre, double weight) {}

    private final Vec3 hinge;
    private final Direction way;
    private final double along, up, inertia;
    private double angle, speed, angleBefore, speedBefore;

    public Topple(Vec3 hinge, Direction way, double along, double up, double inertia, double angle, double speed) {
        this.hinge = hinge;
        this.way = way;
        this.along = along;
        this.up = up;
        this.inertia = inertia;
        this.angle = this.angleBefore = angle;
        this.speed = this.speedBefore = speed;
    }

    /** These blocks turning over this way on this hinge, from standing, pushed just hard enough to go over. */
    public static Topple of(Vec3 hinge, Direction way, Collection<Mass> masses) {
        double along = 0.0, up = 0.0, inertia = 0.0;
        for (Mass mass : masses) {
            double a = along(hinge, way, mass.centre()), u = mass.centre().y - hinge.y;
            along += mass.weight() * a;
            up += mass.weight() * u;
            inertia += mass.weight() * (a * a + u * u + OWN_INERTIA);
        }
        // Weight behind the hinge must be lifted over it first: as much speed as climbing that takes, and a quarter more
        double climb = along < 0.0 && up > 0.0 ? GRAVITY * (Math.hypot(along, up) - up) : 0.0;
        double push = inertia > 0.0 ? Math.max(PUSH, 1.25 * Math.sqrt(2.0 * climb / inertia)) : PUSH;
        return new Topple(hinge, way, along, up, Math.max(inertia, OWN_INERTIA), 0.0, push);
    }

    /** A tick of turning, gravity first; it stops lying down. */
    public void step() {
        this.angleBefore = this.angle;
        this.speedBefore = this.speed;
        this.speed += GRAVITY * (this.up * Math.sin(this.angle) + this.along * Math.cos(this.angle)) / this.inertia;
        this.angle = Math.min(this.angle + this.speed, DOWN);
    }

    /** Back to where the last step started, as when it hit something on the way. */
    public void undo() {
        this.angle = this.angleBefore;
        this.speed = this.speedBefore;
    }

    /** Stopped at this angle, as the server said. */
    public void stopAt(double angle) {
        this.angleBefore = this.angle;
        this.angle = angle;
    }

    public boolean isDown() {
        return this.angle >= DOWN;
    }

    /** Where the centre of the block that started here is now. */
    public Vec3 centre(Vec3 start) {
        return this.centre(start, this.angle);
    }

    /** Where the centre of the block that started here is, turned this far. */
    public Vec3 centre(Vec3 start, double angle) {
        double a = along(this.hinge, this.way, start), u = start.y - this.hinge.y;
        double cos = Math.cos(angle), sin = Math.sin(angle);
        double turnedAlong = a * cos + u * sin, turnedUp = u * cos - a * sin;
        return new Vec3(start.x + this.way.getStepX() * (turnedAlong - a), this.hinge.y + turnedUp, start.z + this.way.getStepZ() * (turnedAlong - a));
    }

    /** How fast the block that started here moves now, in blocks a tick. */
    public Vec3 velocity(Vec3 start) {
        double a = along(this.hinge, this.way, start), u = start.y - this.hinge.y;
        double cos = Math.cos(this.angle), sin = Math.sin(this.angle);
        double alongSpeed = this.speed * (u * cos - a * sin), upSpeed = -this.speed * (u * sin + a * cos);
        return new Vec3(this.way.getStepX() * alongSpeed, upSpeed, this.way.getStepZ() * alongSpeed);
    }

    /** How far this point is from the hinge, along the way it falls. */
    private static double along(Vec3 hinge, Direction way, Vec3 point) {
        return (point.x - hinge.x) * way.getStepX() + (point.z - hinge.z) * way.getStepZ();
    }

    public Vec3 hinge() {
        return this.hinge;
    }

    public Direction way() {
        return this.way;
    }

    public double along() {
        return this.along;
    }

    public double up() {
        return this.up;
    }

    public double inertia() {
        return this.inertia;
    }

    public double angle() {
        return this.angle;
    }

    /** The angle before the last step (or stop), for drawing between ticks. */
    public double angleBefore() {
        return this.angleBefore;
    }

    public double speed() {
        return this.speed;
    }
}
