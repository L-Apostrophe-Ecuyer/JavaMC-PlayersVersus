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
 * the point where it would tip back, and it stops lying down, at a right angle. A tree cut through its trunk drops onto
 * the stump as it starts to tip (it settles by the height of the cut, falling as the tree turns), so it ends lying on
 * the ground, not over it.
 */
public final class Topple {

    /** What pulls it down, in blocks a tick per tick: more than a falling block's 0.04, so a tree comes down briskly. */
    public static final double GRAVITY = 0.1;
    /** How fast the cut starts a tree turning at the least, in radians a tick. */
    public static final double PUSH = 0.06;
    /** Lying down. */
    public static final double DOWN = Math.PI / 2.0;
    /** A block's own moment round its centre, for its weight: a unit cube's, turning on an axis through it. */
    private static final double OWN_INERTIA = 1.0 / 6.0;

    /** A block of the tree: where its centre starts, and what it weighs. */
    public record Mass(Vec3 centre, double weight) {}

    private final Vec3 hinge;
    private final Direction way;
    private final double along, up, inertia, drop;
    /** How far it has turned and how fast; how far it has settled onto its stump and how fast; each before the last step too. */
    private double angle, speed, settled, settling;
    private double angleBefore, speedBefore, settledBefore, settlingBefore;

    public Topple(Vec3 hinge, Direction way, double along, double up, double inertia, double drop, double angle, double speed,
                  double settled, double settling) {
        this.hinge = hinge;
        this.way = way;
        this.along = along;
        this.up = up;
        this.inertia = inertia;
        this.drop = drop;
        this.angle = this.angleBefore = angle;
        this.speed = this.speedBefore = speed;
        this.settled = this.settledBefore = settled;
        this.settling = this.settlingBefore = settling;
    }

    /**
     * These blocks turning over this way on this hinge, from standing, pushed just hard enough to go over, settling this
     * far down onto the hinge's level as they go (the hinge is where they turn once settled).
     */
    public static Topple of(Vec3 hinge, Direction way, Collection<Mass> masses, double drop) {
        double along = 0.0, up = 0.0, inertia = 0.0;
        for (Mass mass : masses) {
            double a = along(hinge, way, mass.centre()), u = mass.centre().y - drop - hinge.y;
            along += mass.weight() * a;
            up += mass.weight() * u;
            inertia += mass.weight() * (a * a + u * u + OWN_INERTIA);
        }
        // Weight behind the hinge must be lifted over it first: as much speed as climbing that takes, and a quarter more
        double climb = along < 0.0 && up > 0.0 ? GRAVITY * (Math.hypot(along, up) - up) : 0.0;
        double push = inertia > 0.0 ? Math.max(PUSH, 1.25 * Math.sqrt(2.0 * climb / inertia)) : PUSH;
        return new Topple(hinge, way, along, up, Math.max(inertia, OWN_INERTIA), drop, 0.0, push, 0.0, 0.0);
    }

    /** A tick of turning, gravity first, and of settling; it stops lying down, settled. */
    public void step() {
        this.angleBefore = this.angle;
        this.speedBefore = this.speed;
        this.settledBefore = this.settled;
        this.settlingBefore = this.settling;
        if (this.angle < DOWN) {
            this.speed += GRAVITY * (this.up * Math.sin(this.angle) + this.along * Math.cos(this.angle)) / this.inertia;
            this.angle = Math.min(this.angle + this.speed, DOWN);
        }
        if (this.settled < this.drop) {
            this.settling += GRAVITY;
            this.settled = Math.min(this.settled + this.settling, this.drop);
        } else {
            this.settling = 0.0;
        }
    }

    /** Back to where the last step started, as when it hit something on the way. */
    public void undo() {
        this.angle = this.angleBefore;
        this.speed = this.speedBefore;
        this.settled = this.settledBefore;
        this.settling = this.settlingBefore;
    }

    /** Back to this far through the last step (0 its start, 1 its end), as when it hit something partway, keeping its speed. */
    public void backTo(double through) {
        this.angle = this.angleBefore + (this.angle - this.angleBefore) * through;
        this.settled = this.settledBefore + (this.settled - this.settledBefore) * through;
    }

    /** Stopped turned and settled this far, as the server said. */
    public void stopAt(double angle, double settled) {
        this.angleBefore = this.angle;
        this.settledBefore = this.settled;
        this.angle = angle;
        this.settled = settled;
    }

    public boolean isDown() {
        return this.angle >= DOWN && this.settled >= this.drop;
    }

    /** Where the centre of the block that started here is now. */
    public Vec3 centre(Vec3 start) {
        return this.centre(start, this.angle, this.settled);
    }

    /** Where the centre of the block that started here was before the last step. */
    public Vec3 centreBefore(Vec3 start) {
        return this.centre(start, this.angleBefore, this.settledBefore);
    }

    /** Where the centre of the block that started here is this far through the last step (0 its start, 1 its end). */
    public Vec3 centreThrough(Vec3 start, double through) {
        return this.centre(start, this.angleBefore + (this.angle - this.angleBefore) * through,
                this.settledBefore + (this.settled - this.settledBefore) * through);
    }

    /** Where the centre of the block that started here is, turned and settled this far. */
    public Vec3 centre(Vec3 start, double angle, double settled) {
        double a = along(this.hinge, this.way, start), u = start.y - this.drop - this.hinge.y;
        double cos = Math.cos(angle), sin = Math.sin(angle);
        double turnedAlong = a * cos + u * sin, turnedUp = u * cos - a * sin;
        return new Vec3(start.x + this.way.getStepX() * (turnedAlong - a), this.hinge.y + turnedUp + this.drop - settled,
                start.z + this.way.getStepZ() * (turnedAlong - a));
    }

    /** How fast the block that started here moves now, in blocks a tick: round the hinge, and down while settling. */
    public Vec3 velocity(Vec3 start) {
        double a = along(this.hinge, this.way, start), u = start.y - this.drop - this.hinge.y;
        double cos = Math.cos(this.angle), sin = Math.sin(this.angle);
        double alongSpeed = this.speed * (u * cos - a * sin), upSpeed = -this.speed * (u * sin + a * cos);
        if (this.settled < this.drop) upSpeed -= this.settling;
        return new Vec3(this.way.getStepX() * alongSpeed, upSpeed, this.way.getStepZ() * alongSpeed);
    }

    /** How far from the hinge, once settled, the block that started here is: how far it moves for a radian of turn. */
    public double reach(Vec3 start) {
        return Math.hypot(along(this.hinge, this.way, start), start.y - this.drop - this.hinge.y);
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

    public double drop() {
        return this.drop;
    }

    public double angle() {
        return this.angle;
    }

    /** The angle before the last step (or stop), for drawing between ticks. */
    public double angleBefore() {
        return this.angleBefore;
    }

    /** How much of the last step's turn and settling there was, as a block this far from the hinge moved through it. */
    public double moved(double reach) {
        return (this.angle - this.angleBefore) * reach + (this.settled - this.settledBefore);
    }

    public double speed() {
        return this.speed;
    }

    public double settled() {
        return this.settled;
    }

    public double settledBefore() {
        return this.settledBefore;
    }

    public double settling() {
        return this.settling;
    }
}
