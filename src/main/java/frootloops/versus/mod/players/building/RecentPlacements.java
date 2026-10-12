package frootloops.versus.mod.players.building;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The last blocks each player placed, which they break much faster for a moment, though never at once: a misplaced
 * block is quick to take back, with the tool it needs to drop. The server keeps them a little longer than the client, so
 * that it doesn't refuse a break the client already finished.
 */
public final class RecentPlacements {
    private RecentPlacements() {}

    private static final int CLIENT_TICKS = 40, SERVER_TICKS = 60, KEPT = 8;

    private record Placement(ResourceKey<Level> dimension, long pos, Block block, long time) {}

    // One for each side (the client's and, in single player, the server's thread), so neither needs a lock
    private static final Map<UUID, Deque<Placement>> CLIENT = new HashMap<>(), SERVER = new HashMap<>();

    public static void record(Player player, BlockPos pos, BlockState state) {
        Level level = player.level();
        Deque<Placement> placements = side(level).computeIfAbsent(player.getUUID(), uuid -> new ArrayDeque<>());
        placements.addFirst(new Placement(level.dimension(), pos.asLong(), state.getBlock(), level.getGameTime()));
        while (placements.size() > KEPT) placements.removeLast();
    }

    /** Whether the player placed the block at pos just now. */
    public static boolean recent(Player player, BlockPos pos, BlockState state) {
        Level level = player.level();
        Deque<Placement> placements = side(level).get(player.getUUID());
        if (placements == null) return false;
        long now = level.getGameTime(), lifetime = level.isClientSide() ? CLIENT_TICKS : SERVER_TICKS;
        placements.removeIf(placement -> now - placement.time > lifetime || now < placement.time);
        for (Placement placement : placements) {
            if (placement.pos == pos.asLong() && placement.block == state.getBlock() && placement.dimension.equals(level.dimension())) return true;
        }
        return false;
    }

    /**
     * The mining progress per tick of a block just placed, from its usual progress: four times as fast, done in five
     * ticks at the slowest and two at the fastest; a block mined in one tick already (or not at all) stays as it is.
     */
    public static float faster(float progress) {
        if (progress <= 0.0F || progress >= 1.0F) return progress;
        return Math.max(progress, Math.min(Math.max(progress * 4.0F, 0.2F), 0.5F));
    }

    private static Map<UUID, Deque<Placement>> side(Level level) {
        return level.isClientSide() ? CLIENT : SERVER;
    }
}
