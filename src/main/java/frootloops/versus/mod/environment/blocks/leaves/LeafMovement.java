package frootloops.versus.mod.environment.blocks.leaves;

import frootloops.versus.VersusSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;

/**
 * Moving through leaves, like scaffolding you can push through ({@code docs/superpowers/specs/2026-10-18-leaf-overhaul-design.md}).
 * Living things other than animals and spiders go through leaves, slowed the deeper they are in them; with leaves at
 * their feet they climb holding jump and sink holding sneak; a leaf they're above holds them, unless they're sneaking,
 * gliding or falling onto it from higher than 2.5 blocks; and a fall through leaves counts only as far as the speed it
 * still has.
 *
 * <p>Whether a leaf is solid only reads what client and server both know of the entity (its position, sneaking, gliding,
 * fall distance), never its velocity, and changes nothing: collision shapes are asked many times a tick, on both sides.
 * The movement itself ({@link #adjust}) runs where the entity is simulated, the client for its player and the server for
 * mobs; the fall cushion ({@link #cushionFall}) runs where vanilla counts the fall, on both.
 */
public final class LeafMovement {

    /** Vanilla's free fall: 0.08 of gravity a tick, then 2% drag, tends to 3.92 blocks per tick. */
    private static final double TERMINAL_FALL_SPEED = 3.92;
    private static final double AIR_DRAG = 0.98;
    /** How much the lower half of the body counts in the horizontal slowdown; the upper half counts the rest. */
    private static final double LEGS_WEIGHT = 0.75;

    private LeafMovement() {
    }

    /** Leaves let this entity through: living things, but not animals or spiders, which walk on them and climb them. */
    public static boolean passes(Entity entity) {
        return entity instanceof LivingEntity && !(entity instanceof Animal) && !(entity instanceof Spider);
    }

    /**
     * Whether a leaf at this position is solid for this collision context: always for placing blocks (leaves can't be
     * placed into anyone) and for things that don't pass. For one that passes, only while its feet are above the leaf
     * and it isn't sneaking, gliding or falling hard. On the server a player's leaves are always open: its client
     * simulates its movement and decides, and the server mustn't pull it back for going through.
     */
    public static boolean isSolid(BlockPos pos, CollisionContext context) {
        if (context.isPlacement()) return true;
        if (!(context instanceof EntityCollisionContext entityContext) || !passes(entityContext.getEntity())) return true;
        if (!VersusSettings.Leaves.DO_LEAF_MOVEMENT) return true;
        LivingEntity entity = (LivingEntity) entityContext.getEntity();
        if (entity.isFallFlying()) return false;
        if (entity instanceof Player && !entity.level().isClientSide()) return false;
        return context.isAbove(Shapes.block(), pos, true) && !context.isDescending()
                && entity.fallDistance <= VersusSettings.Leaves.BREAK_THROUGH_FALL_DISTANCE;
    }

    /**
     * How much of an entity is in the leaves it goes through, from 0 to 1, however many leaf blocks that takes.
     *
     * @param body     the share of its bounding box in them, the lower half counting three times the upper half (the
     *                 horizontal slowdown: brushing leaves with the head is mild, wading through a bush is not)
     * @param swept    the plain share of its box swept through the tick's motion (the vertical drag, so a fast fall
     *                 can't skip a thin layer between two ticks)
     * @param foothold leaves at its feet, which it can climb and sink through
     * @param densest  the leaf block holding the most of its body, for the rustling, or null
     */
    public record Immersion(double body, double swept, boolean foothold, BlockPos densest) {
        public static final Immersion NONE = new Immersion(0.0, 0.0, false, null);

        public boolean any() {
            return body > 0.0 || swept > 0.0;
        }
    }

    /** How much of the entity is in leaves open to it, its box swept through this motion. */
    public static Immersion measure(LivingEntity entity, Vec3 motion) {
        Level level = entity.level();
        CollisionContext context = CollisionContext.of(entity);
        AABB box = entity.getBoundingBox();
        AABB swept = box.expandTowards(motion);
        double middle = (box.minY + box.maxY) * 0.5;
        double legs = 0.0, upper = 0.0, inSwept = 0.0, most = 0.0;
        BlockPos densest = null;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = Mth.floor(swept.minX); x <= Mth.floor(swept.maxX); x++) {
            for (int y = Mth.floor(swept.minY); y <= Mth.floor(swept.maxY); y++) {
                for (int z = Mth.floor(swept.minZ); z <= Mth.floor(swept.maxZ); z++) {
                    pos.set(x, y, z);
                    if (!(level.getBlockState(pos).getBlock() instanceof LeavesBlock) || isSolid(pos, context)) continue;
                    inSwept += overlap(swept.minX, swept.maxX, x) * overlap(swept.minY, swept.maxY, y) * overlap(swept.minZ, swept.maxZ, z);
                    double area = overlap(box.minX, box.maxX, x) * overlap(box.minZ, box.maxZ, z);
                    double inLegs = area * overlap(box.minY, middle, y), inUpper = area * overlap(middle, box.maxY, y);
                    legs += inLegs;
                    upper += inUpper;
                    if (inLegs + inUpper > most) {
                        most = inLegs + inUpper;
                        densest = pos.immutable();
                    }
                }
            }
        }
        if (inSwept <= 0.0) return Immersion.NONE;
        double half = box.getXsize() * box.getZsize() * box.getYsize() * 0.5;
        double body = half > 0.0 ? Math.min(1.0, (LEGS_WEIGHT * legs + (1.0 - LEGS_WEIGHT) * upper) / half) : 0.0;
        double sweptVolume = swept.getXsize() * swept.getYsize() * swept.getZsize();
        double sweptShare = sweptVolume > 0.0 ? Math.min(1.0, inSwept / sweptVolume) : 0.0;
        BlockPos feet = entity.blockPosition();
        boolean foothold = level.getBlockState(feet).getBlock() instanceof LeavesBlock && !isSolid(feet, context);
        return new Immersion(body, sweptShare, foothold, densest);
    }

    /** How much of [min, max] lies in the block [cell, cell + 1]. */
    private static double overlap(double min, double max, int cell) {
        return Math.max(0.0, Math.min(max, cell + 1.0) - Math.max(min, cell));
    }

    /**
     * The motion vanilla is about to move a living thing with this tick (after its input, from {@code handleOnClimbable}),
     * through the leaves it's in: horizontal speed capped by how deep it is, vertical speed dragged; and with leaves at its
     * feet and no faster a fall than {@code CATCH_SPEED}, holding jump climbs and holding sneak sinks no faster than
     * scaffolding. A faster fall isn't caught at once, holding jump or not: the drag slows it first.
     */
    public static Vec3 adjust(LivingEntity entity, Vec3 motion) {
        if (!movesThroughLeaves(entity)) return motion;
        Immersion immersion = measure(entity, motion);
        if (!immersion.any()) return motion;
        double keep = horizontalKeep(Math.sqrt(motion.x * motion.x + motion.z * motion.z), immersion.body());
        double y = motion.y * (1.0 - VersusSettings.Leaves.VERTICAL_DRAG * immersion.swept());
        if (immersion.foothold() && y >= -VersusSettings.Leaves.CATCH_SPEED) {
            if (entity.isJumping()) {
                y = Math.max(y, VersusSettings.Leaves.CLIMB_SPEED);
                entity.resetFallDistance();
            } else if (entity.isDescending()) {
                y = Math.max(y, -VersusSettings.Leaves.DESCEND_SPEED);
            }
        }
        return new Vec3(motion.x * keep, y, motion.z * keep);
    }

    /**
     * A swimmer's motion this tick, before it moves: leaves underwater drag it, across the more of its body is in them,
     * up and down the more of its path is. The drag stays in its speed, as water's own does, so a swimmer fully in
     * leaves settles at about half its speed, as a walker does.
     */
    public static Vec3 swim(LivingEntity entity, Vec3 motion) {
        if (!movesThroughLeaves(entity)) return motion;
        Immersion immersion = measure(entity, motion);
        if (!immersion.any()) return motion;
        double across = 1.0 - VersusSettings.Leaves.SWIM_DRAG * immersion.body();
        return new Vec3(motion.x * across, motion.y * (1.0 - VersusSettings.Leaves.SWIM_DRAG * immersion.swept()), motion.z * across);
    }

    /** Whether leaves move this entity now: it passes them, and isn't a spectator, flying, riding or clipping through. */
    private static boolean movesThroughLeaves(LivingEntity entity) {
        return VersusSettings.Leaves.DO_LEAF_MOVEMENT && passes(entity) && !entity.isSpectator() && !entity.noPhysics
                && !entity.isPassenger() && !(entity instanceof Player player && player.getAbilities().flying);
    }

    /** A glider through leaves loses some of its speed on every axis, the more the deeper it is in them. */
    public static Vec3 glide(LivingEntity entity, Vec3 motion) {
        if (!VersusSettings.Leaves.DO_LEAF_MOVEMENT || !passes(entity)) return motion;
        double swept = measure(entity, motion).swept();
        return swept > 0.0 ? motion.scale(1.0 - VersusSettings.Leaves.GLIDE_DRAG * swept) : motion;
    }

    /**
     * A fall through leaves counts only as far as the speed it still has: while the entity is in leaves and still
     * falling, its fall distance can't be more than the free-fall height that gives this tick's speed. At the head of
     * {@code checkFallDamage}, before vanilla adds the tick's fall, with the tick's vertical movement. Not on the tick it
     * lands, whose movement the ground cut short: a bush on the ground doesn't erase a fast fall.
     */
    public static void cushionFall(LivingEntity entity, double dy, boolean onGround) {
        if (!VersusSettings.Leaves.DO_LEAF_MOVEMENT || dy >= 0.0 || onGround || entity.fallDistance <= 0.0 || !passes(entity)) return;
        // The box swept back up to where it was before this tick's fall.
        if (measure(entity, new Vec3(0.0, -dy, 0.0)).swept() <= 0.0) return;
        double fall = -dy;
        entity.fallDistance = Math.min(entity.fallDistance, Math.max(0.0, equivalentFallHeight(fall) - fall));
    }

    /**
     * The share of its horizontal speed something moving this fast keeps this tick at this immersion: the speed cap
     * tightens from {@code SPEED_CAP_BRUSHING} barely in leaves to {@code SPEED_CAP_INSIDE} fully in them, and above it
     * at most {@code MOST_SPEED_LOST_PER_TICK} goes in a tick, so pushing in slows over a few ticks.
     */
    public static double horizontalKeep(double speed, double immersion) {
        double cap = Mth.lerp(Math.clamp(immersion, 0.0, 1.0), VersusSettings.Leaves.SPEED_CAP_BRUSHING, VersusSettings.Leaves.SPEED_CAP_INSIDE);
        if (speed <= cap) return 1.0;
        return Math.max(cap / speed, 1.0 - VersusSettings.Leaves.MOST_SPEED_LOST_PER_TICK);
    }

    /**
     * The height a vanilla free fall from rest drops before it moves this fast (blocks per tick). Its speed after n ticks
     * is 3.92 (1 - 0.98^n), so it has fallen 3.92 n - 49 v by then.
     */
    public static double equivalentFallHeight(double speed) {
        if (speed <= 0.0) return 0.0;
        if (speed >= TERMINAL_FALL_SPEED) return Double.MAX_VALUE;
        double ticks = Math.log(1.0 - speed / TERMINAL_FALL_SPEED) / Math.log(AIR_DRAG);
        return Math.max(0.0, TERMINAL_FALL_SPEED * ticks - AIR_DRAG / (1.0 - AIR_DRAG) * speed);
    }
}
