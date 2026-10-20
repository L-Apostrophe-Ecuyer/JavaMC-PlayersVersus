package frootloops.versus.mod.environment.blocks.leaves;

import frootloops.versus.VersusSettings;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Warnings in the log that follow players through leaves, to debug in game how leaves move them
 * ({@code VersusSettings.Leaves.LOG_LEAVES}): what the client did with each move, how each side saw the leaves around,
 * and every time the server pulled a player back or found nothing under them. Lines start with the side and the
 * player's tick, so a client line and the server's answer to it can be put side by side.
 */
public final class LeafDebug {

    private static final Logger LOGGER = LoggerFactory.getLogger("players-versus/leaves");

    private LeafDebug() {
    }

    /** Whether to log this entity: players only, while the setting is on. */
    public static boolean on(Entity entity) {
        return VersusSettings.Leaves.LOG_LEAVES && entity instanceof Player;
    }

    /** A move a player's client made in or on leaves, before and after the leaves had their say. */
    public static void moved(LivingEntity entity, Vec3 before, Vec3 after, LeafMovement.Immersion immersion) {
        BlockPos below = BlockPos.containing(entity.getX(), entity.getY() - 0.01, entity.getZ());
        if (!immersion.any() && !(entity.level().getBlockState(below).getBlock() instanceof LeavesBlock)) return;
        log(entity, "move {} -> {} at {} | in leaves body {} swept {} foothold {} | onGround {} fall {} jumping {} sneaking {} | {}",
                vec(before), vec(after), vec(entity.position()), num(immersion.body()), num(immersion.swept()), immersion.foothold(),
                entity.onGround(), num(entity.fallDistance), entity.isJumping(), entity.isDescending(), around(entity, CollisionContext.of(entity)));
    }

    /** A fall through leaves, cushioned. */
    public static void cushioned(LivingEntity entity, double dy, double before, double after) {
        log(entity, "fall cushioned in leaves: dy {} fall {} -> {} at {}", num(dy), num(before), num(after), vec(entity.position()));
    }

    /** A landing on top of a leaf. */
    public static void landed(Entity entity, BlockPos pos, double fallDistance, double counted) {
        log(entity, "landed on the leaf at {} after falling {}, counted as {}", pos.toShortString(), num(fallDistance), num(counted));
    }

    /** The server pulled a player back: where its client said it went, and where it was sent back to. */
    public static void pulledBack(Player player, Vec3 claimed, Vec3 back) {
        AABB box = player.getBoundingBox().move(claimed.subtract(player.position()));
        log(player, "SERVER PULLED THE PLAYER BACK: client said {}, sent back to {} | leaves at the claimed spot, as the move check sees them: {} | as the player sees them: {}",
                vec(claimed), vec(back), leaves(player.level(), box, CollisionContext.withPosition(player, player.getY())),
                leaves(player.level(), box, CollisionContext.of(player)));
    }

    /** The server found nothing holding up a player who says they're on the ground. */
    public static void standingOnAir(Player player) {
        BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.01, player.getZ());
        BlockState state = player.level().getBlockState(below);
        log(player, "server found nothing under the player at {}: below is {} (solid to the player: {}) | fall {} onGround {}",
                vec(player.position()), state.getBlock().getName().getString(), state.getBlock() instanceof LeavesBlock
                        ? LeafMovement.isSolid(below, CollisionContext.of(player)) : "not leaves", num(player.fallDistance), player.onGround());
    }

    private static void log(Entity entity, String message, Object... args) {
        Object[] all = new Object[args.length + 3];
        all[0] = entity.level().isClientSide() ? "client" : "server";
        all[1] = entity.tickCount;
        all[2] = entity.getName().getString();
        System.arraycopy(args, 0, all, 3, args.length);
        LOGGER.warn("[{} tick {}] {}: " + message, all);
    }

    /** The leaves at the entity's feet, head and just below, each solid or open to it. */
    private static String around(LivingEntity entity, CollisionContext context) {
        AABB box = entity.getBoundingBox();
        return leaves(entity.level(), new AABB(box.minX, box.minY - 0.5, box.minZ, box.maxX, box.maxY, box.maxZ), context);
    }

    private static String leaves(Level level, AABB box, CollisionContext context) {
        List<String> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
                Mth.floor(box.maxX - 1.0E-7), Mth.floor(box.maxY - 1.0E-7), Mth.floor(box.maxZ - 1.0E-7))) {
            if (level.getBlockState(pos).getBlock() instanceof LeavesBlock) {
                found.add(pos.toShortString() + (LeafMovement.isSolid(pos, context) ? " solid" : " open"));
            }
        }
        return found.isEmpty() ? "no leaves" : "leaves " + String.join("; ", found);
    }

    private static String vec(Vec3 vec) {
        return String.format(Locale.ROOT, "(%.3f, %.3f, %.3f)", vec.x, vec.y, vec.z);
    }

    private static String num(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
