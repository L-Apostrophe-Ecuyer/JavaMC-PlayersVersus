package frootloops.versus.mod.mobs.hostile.overworld.climbing;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Spiders on any surface, as in Nyf's Spiders: they path along floors, walls and ceilings ({@link SurfaceNavigation}),
 * crawl along them without falling ({@link ClimbingMoveControl}), and clients turn their model to the surface they
 * cling to, facing the way they crawl.
 *
 * <p>A face is the side of the spider's box that its surface touches: DOWN for the floor, UP for a ceiling, a
 * horizontal direction for a wall. The server picks one every tick and syncs it to clients.
 */
public final class SurfaceClimbing {
    /** How close a block must be to a side of the spider's box to hold it. */
    static final double GRIP = 0.1;
    /** How far below a path node ground may be for the spider to walk to it rather than crawl. */
    static final double FLOOR_REACH = 0.6;
    /** The sides of a box are probed a little narrower than they are, so a block at a neighbouring side doesn't count. */
    private static final double INSET = 0.05;

    private SurfaceClimbing() {
    }

    /** The sides of the mob's box that touch a block, as a mask of {@link #bit} values. */
    public static int touching(Mob mob) {
        AABB box = mob.getBoundingBox();
        int mask = 0;
        for (Direction face : Direction.values()) {
            if (!mob.level().noCollision(mob, beyond(box, face, GRIP))) mask |= bit(face);
        }
        return mask;
    }

    public static int bit(Direction face) {
        return 1 << face.get3DDataValue();
    }

    /** Whether the mask has a wall or a ceiling in it: something to cling to off the ground. */
    public static boolean clingsOffTheGround(int touching) {
        return (touching & ~bit(Direction.DOWN)) != 0;
    }

    /** The thin slab {@code depth} deep just beyond one side of a box. */
    static AABB beyond(AABB box, Direction face, double depth) {
        return switch (face) {
            case DOWN -> new AABB(box.minX + INSET, box.minY - depth, box.minZ + INSET, box.maxX - INSET, box.minY, box.maxZ - INSET);
            case UP -> new AABB(box.minX + INSET, box.maxY, box.minZ + INSET, box.maxX - INSET, box.maxY + depth, box.maxZ - INSET);
            case NORTH -> new AABB(box.minX + INSET, box.minY + INSET, box.minZ - depth, box.maxX - INSET, box.maxY - INSET, box.minZ);
            case SOUTH -> new AABB(box.minX + INSET, box.minY + INSET, box.maxZ, box.maxX - INSET, box.maxY - INSET, box.maxZ + depth);
            case WEST -> new AABB(box.minX - depth, box.minY + INSET, box.minZ + INSET, box.minX, box.maxY - INSET, box.maxZ - INSET);
            case EAST -> new AABB(box.maxX, box.minY + INSET, box.minZ + INSET, box.maxX + depth, box.maxY - INSET, box.maxZ - INSET);
        };
    }

    /**
     * The face the spider clings by, from the sides it touches, how it moved this tick and the face it clung by before:
     * climbing up or down a wall takes the wall it runs along, standing on the ground takes the floor, a ceiling takes
     * over once the spider is under one, and otherwise it keeps to a wall it touches. Touching nothing, it's the floor.
     */
    public static Direction chooseFace(int touching, double dx, double dy, double dz, Direction previous, boolean onGround) {
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        boolean moving = length > 0.01;
        Direction wall = null;
        double across = Double.MAX_VALUE;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if ((touching & bit(side)) == 0) continue;
            double into = moving ? Math.abs(dx * side.getStepX() + dz * side.getStepZ()) / length : 1.0;
            if (side == previous) into -= 0.01;
            if (into < across) {
                across = into;
                wall = side;
            }
        }
        if (wall != null && moving && Math.abs(dy) > 0.5 * length) return wall;
        if (onGround && (touching & bit(Direction.DOWN)) != 0) return Direction.DOWN;
        if ((touching & bit(Direction.UP)) != 0) return Direction.UP;
        return wall != null ? wall : Direction.DOWN;
    }

    /** The way out of the surface the spider clings to by {@code face}: up off the floor, down off a ceiling, out of a wall. */
    public static Vec3 normal(Direction face) {
        Direction out = face.getOpposite();
        return new Vec3(out.getStepX(), out.getStepY(), out.getStepZ());
    }

    /**
     * The speed a mob walks level ground at, in blocks per tick, at its movement speed times {@code modifier}: each tick
     * adds speed² × 0.216 / f³ ahead, and the ground's friction f (0.6 × 0.91) keeps f of what it had.
     */
    public static double walkingSpeed(Mob mob, double modifier) {
        double speed = modifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double friction = 0.6 * 0.91;
        return speed * speed * 0.21600002 / (friction * friction * friction) * friction / (1.0 - friction);
    }

    /** A small pull into each wall and ceiling the mob touches, which keeps it on them. */
    static Vec3 grip(int touching, double pull) {
        double x = 0.0, y = 0.0, z = 0.0;
        for (Direction face : Direction.values()) {
            if (face == Direction.DOWN || (touching & bit(face)) == 0) continue;
            x += face.getStepX() * pull;
            y += face.getStepY() * pull;
            z += face.getStepZ() * pull;
        }
        return new Vec3(x, y, z);
    }
}
