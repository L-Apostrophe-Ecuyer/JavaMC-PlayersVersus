package frootloops.versus.mod.environment;

import frootloops.versus.mixin.client.environment.visuals.UntintedParticleLeavesAccessor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Leaves rustle and shed when a player pushes through them (leaves let living things through, {@code LeavesMixin}): a
 * burst of leaves on the way in, then leaves falling all around and a rustle every so often while they move, more and
 * louder the faster they go, half as much sneaking. The particles are the leaves' own falling leaves (tinted by the
 * biome where vanilla tints them), or bits of the block for leaves that shed none.
 *
 * <p>Client side only, for every player the client sees, so everyone nearby sees and hears the others too.
 */
@Environment(EnvType.CLIENT)
public final class LeavesRustling {

    /** How far a player moves inside leaves between two rustles, in blocks. */
    private static final double RUSTLE_DISTANCE = 0.9;
    /** Below this speed, in blocks per tick, a player standing in leaves stays quiet. */
    private static final double MIN_SPEED = 0.03;
    /** Leaves per tick per block-per-tick of speed while moving, and the most per tick. */
    private static final double LEAVES_PER_SPEED = 30.0;
    private static final int MAX_LEAVES_PER_TICK = 10;
    /** The shower on the way into the leaves. */
    private static final int BURST_LEAVES = 24;

    private static final Map<UUID, Rustle> PLAYERS = new HashMap<>();

    private LeavesRustling() {
    }

    /** What the rustling remembers of a player between ticks. */
    private static final class Rustle {
        boolean inside;
        double distance;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(LeavesRustling::tick);
    }

    private static void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            PLAYERS.clear();
            return;
        }
        Set<UUID> seen = new HashSet<>();
        for (Player player : level.players()) {
            if (player.isSpectator()) continue;
            seen.add(player.getUUID());
            rustle(level, player, PLAYERS.computeIfAbsent(player.getUUID(), uuid -> new Rustle()));
        }
        PLAYERS.keySet().retainAll(seen);
    }

    private static void rustle(ClientLevel level, Player player, Rustle rustle) {
        BlockPos leavesPos = leavesAround(level, player);
        if (leavesPos == null) {
            rustle.inside = false;
            rustle.distance = 0.0;
            return;
        }
        BlockState leaves = level.getBlockState(leavesPos);
        double dx = player.getX() - player.xo, dy = player.getY() - player.yo, dz = player.getZ() - player.zo;
        double speed = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float quiet = player.isShiftKeyDown() ? 0.5F : 1.0F;
        RandomSource random = level.getRandom();

        if (!rustle.inside) {
            rustle.inside = true;
            rustle.distance = 0.0;
            if (speed >= MIN_SPEED) {
                shed(level, player, leavesPos, leaves, Math.round(BURST_LEAVES * quiet), speed);
                playRustle(level, player, speed, quiet * 1.2F, random);
            }
            return;
        }
        if (speed < MIN_SPEED) return;

        // A fraction left over becomes a leaf now and then, so slow movement still sheds a few.
        double leavesNow = Math.min(MAX_LEAVES_PER_TICK, speed * LEAVES_PER_SPEED) * quiet;
        int count = (int) leavesNow + (random.nextDouble() < leavesNow - Math.floor(leavesNow) ? 1 : 0);
        shed(level, player, leavesPos, leaves, count, speed);

        rustle.distance += speed;
        if (rustle.distance >= RUSTLE_DISTANCE) {
            rustle.distance = 0.0;
            playRustle(level, player, speed, quiet, random);
        }
    }

    /** The leaves the player is in, from the head down (the head is in them first when falling into a tree). */
    private static BlockPos leavesAround(ClientLevel level, Player player) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int x = (int) Math.floor(player.getX()), z = (int) Math.floor(player.getZ());
        for (int y = (int) Math.floor(player.getY() + player.getBbHeight() - 0.01); y >= (int) Math.floor(player.getY()); y--) {
            if (level.getBlockState(pos.set(x, y, z)).getBlock() instanceof LeavesBlock) return pos.immutable();
        }
        return null;
    }

    private static void shed(ClientLevel level, Player player, BlockPos pos, BlockState leaves, int count, double speed) {
        if (count <= 0) return;
        ParticleOptions particle = leafParticle(level, pos, leaves);
        RandomSource random = level.getRandom();
        // Around the player, a little wider than they are.
        double width = player.getBbWidth() + 0.7, height = player.getBbHeight() + 0.2;
        double spread = 0.04 + speed * 0.25;
        for (int i = 0; i < count; i++) {
            double x = player.getX() + (random.nextDouble() - 0.5) * width;
            double y = player.getY() - 0.1 + random.nextDouble() * height;
            double z = player.getZ() + (random.nextDouble() - 0.5) * width;
            level.addParticle(particle, x, y, z,
                    (random.nextDouble() - 0.5) * spread, random.nextDouble() * spread * 0.5, (random.nextDouble() - 0.5) * spread);
        }
    }

    private static ParticleOptions leafParticle(ClientLevel level, BlockPos pos, BlockState leaves) {
        if (leaves.getBlock() instanceof UntintedParticleLeavesBlock untinted) {
            return ((UntintedParticleLeavesAccessor) untinted).versus$getLeafParticle();
        }
        if (leaves.getBlock() instanceof TintedParticleLeavesBlock) {
            return ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, level.getClientLeafTintColor(pos));
        }
        return new BlockParticleOption(ParticleTypes.BLOCK, leaves);
    }

    private static void playRustle(ClientLevel level, Player player, double speed, float loudness, RandomSource random) {
        float volume = (float) Math.min(1.0, 0.35 + speed * 1.5) * loudness;
        float pitch = 0.9F + random.nextFloat() * 0.25F;
        level.playLocalSound(player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
                CustomSpecialEffects.LEAVES_RUSTLE, SoundSource.BLOCKS, volume, pitch, false);
    }
}
