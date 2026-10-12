package frootloops.versus.mod.players.building;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The state each player's client last asked for its next placement ({@link PlacementHintPayload}). The client sends it
 * just before the use it goes with, so the next placement takes it; any placement clears it, so one that never reached
 * the server can't leave its hint to a later one.
 */
public final class PlacementHints {
    private PlacementHints() {}

    /** A hint older than this (in milliseconds) is from a placement that didn't happen. */
    private static final long LIFETIME = 2000;

    private record Hint(long pos, int state, long time) {}

    // Filled from the network thread or the server's, read on the server's
    private static final Map<UUID, Hint> HINTS = new ConcurrentHashMap<>();

    public static void store(Player player, BlockPos pos, int state) {
        HINTS.put(player.getUUID(), new Hint(pos.asLong(), state, System.currentTimeMillis()));
    }

    /** The state the player asked for at pos, if it's of block; the player's hint is gone after this either way. */
    @Nullable
    public static BlockState take(Player player, BlockPos pos, Block block) {
        Hint hint = HINTS.remove(player.getUUID());
        if (hint == null || hint.pos != pos.asLong() || System.currentTimeMillis() - hint.time > LIFETIME) return null;
        BlockState state = Block.stateById(hint.state);
        return state.getBlock() == block ? state : null;
    }
}
