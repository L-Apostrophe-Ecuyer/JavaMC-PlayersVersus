package frootloops.versus.mod.mobs.hostile.overworld.crawling;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A spider's grip on its surfaces. The server keeps the sides it touched last tick, for its move control; clients keep
 * the way out of its surface and the way it faces along it, turned toward the synced face a little each tick so the
 * model swings smoothly from floor to wall to ceiling.
 */
public final class GripState {
    /** How much of the way to the new surface, and the new heading, a client turns each tick. */
    static final float NORMAL_RATE = 0.35F, FORWARD_RATE = 0.4F;
    /** How far the heading must turn before the server syncs it again: cos 5°. */
    static final float RESYNC_DOT = 0.996F;

    // Server.
    int touching;

    // Clients.
    private final Vector3f normal = new Vector3f(0.0F, 1.0F, 0.0F);
    private final Vector3f normalO = new Vector3f(0.0F, 1.0F, 0.0F);
    private final Vector3f forward = new Vector3f(0.0F, 0.0F, 1.0F);
    private final Vector3f forwardO = new Vector3f(0.0F, 0.0F, 1.0F);

    public int touching() {
        return this.touching;
    }

    public void setTouching(int touching) {
        this.touching = touching;
    }

    /**
     * Server: the way a spider clinging by {@code face} faces along its surface, from how it moved this tick, or else
     * toward its target; null keeps the heading it has.
     */
    @Nullable
    public static Vector3f heading(Direction face, Vec3 motion, @Nullable Vec3 toTarget) {
        Vec3 normal = SurfaceGrip.normal(face);
        Vec3 along = motion.subtract(normal.scale(motion.dot(normal)));
        if (along.lengthSqr() < 1.0E-4 && toTarget != null) along = toTarget.subtract(normal.scale(toTarget.dot(normal)));
        return along.lengthSqr() < 1.0E-4 ? null : along.normalize().toVector3f();
    }

    /** Server: whether a new heading has turned far enough from the synced one to sync it. */
    public static boolean worthSyncing(Vector3fc synced, Vector3fc heading) {
        return synced.dot(heading) < RESYNC_DOT;
    }

    /** Clients, every tick: turn toward the synced face and heading; on the floor the heading is the body's yaw. */
    public void turnToward(Direction face, Vector3fc syncedHeading, float bodyYaw) {
        this.normalO.set(this.normal);
        this.forwardO.set(this.forward);
        approach(this.normal, SurfaceGrip.normal(face).toVector3f(), NORMAL_RATE);
        approach(this.forward, face == Direction.DOWN ? yawHeading(bodyYaw) : new Vector3f(syncedHeading), FORWARD_RATE);
    }

    public Vector3f normal(float partialTick) {
        return interpolate(this.normalO, this.normal, partialTick);
    }

    public Vector3f forward(float partialTick) {
        return interpolate(this.forwardO, this.forward, partialTick);
    }

    /** The way a body at {@code yaw} degrees faces on level ground. */
    public static Vector3f yawHeading(float yaw) {
        double radians = Math.toRadians(yaw);
        return new Vector3f(-Mth.sin(radians), 0.0F, Mth.cos(radians));
    }

    /** Turns a unit vector part of the way to another; turning right round, it snaps rather than pass through nothing. */
    static void approach(Vector3f vector, Vector3fc target, float rate) {
        if (vector.dot(target) < -0.5F) vector.set(target);
        else vector.lerp(target, rate).normalize();
    }

    private static Vector3f interpolate(Vector3fc from, Vector3fc to, float partialTick) {
        if (from.dot(to) < 0.0F) return new Vector3f(to);
        return new Vector3f(from).lerp(to, partialTick).normalize();
    }
}
