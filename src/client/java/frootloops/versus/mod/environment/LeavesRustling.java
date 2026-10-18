package frootloops.versus.mod.environment;

import frootloops.versus.mixin.client.environment.visuals.UntintedParticleLeavesAccessor;
import frootloops.versus.mod.environment.blocks.leaves.LeafMovement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.TintedParticleLeavesBlock;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Leaves rustle and shed when something pushes through them ({@link LeafMovement}: living things other than animals and
 * spiders): a burst of leaves on the way in, the bigger and louder the faster it comes (a fall crashing into a canopy
 * most of all), then leaves falling around it and a rustle every so often while it moves, more the faster it goes and
 * the deeper it is, half as much sneaking. The particles are the leaves' own falling leaves (tinted by the biome where
 * vanilla tints them), or bits of the block for leaves that shed none.
 *
 * <p>Client side only, for everything within {@code RANGE} of the player, so you hear what moves through the bushes
 * around you and others see and hear you.
 */
@Environment(EnvType.CLIENT)
public final class LeavesRustling {

    /** How far from the player leaves rustle, in blocks. */
    private static final double RANGE = 32.0;
    /** How much of the body must be in leaves to count as in them. */
    private static final double IN_LEAVES = 0.05;
    /** How far something moves inside leaves between two rustles, in blocks. */
    private static final double RUSTLE_DISTANCE = 0.9;
    /** Below this speed, in blocks per tick, something standing in leaves stays quiet. */
    private static final double MIN_SPEED = 0.03;
    /** Leaves per tick per block-per-tick of speed while moving fully inside, and the most per tick. */
    private static final double LEAVES_PER_SPEED = 30.0;
    private static final int MAX_LEAVES_PER_TICK = 10;
    /** The shower on the way in at walking speed, and how much bigger a fast entry makes it, at most. */
    private static final int BURST_LEAVES = 24;
    private static final double MOST_BURST_SCALE = 3.0;
    /** Falling faster than this into leaves crashes into them: the burst's rustle is louder and deeper. */
    private static final double CRASH_SPEED = 0.5;

    private static final Map<Integer, Rustle> MOVERS = new HashMap<>();

    private LeavesRustling() {
    }

    /** What the rustling remembers of something between ticks. */
    private static final class Rustle {
        boolean inside;
        double distance;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(LeavesRustling::tick);
    }

    private static void tick(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        Player viewer = minecraft.player;
        if (level == null || viewer == null || minecraft.isPaused()) {
            MOVERS.clear();
            return;
        }
        Set<Integer> seen = new HashSet<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, viewer.getBoundingBox().inflate(RANGE),
                entity -> entity.isAlive() && !entity.isSpectator() && LeafMovement.passes(entity))) {
            seen.add(entity.getId());
            rustle(level, entity, MOVERS.computeIfAbsent(entity.getId(), id -> new Rustle()));
        }
        MOVERS.keySet().retainAll(seen);
    }

    private static void rustle(ClientLevel level, LivingEntity entity, Rustle rustle) {
        LeafMovement.Immersion immersion = LeafMovement.measure(entity, Vec3.ZERO);
        if (immersion.body() < IN_LEAVES || immersion.densest() == null) {
            rustle.inside = false;
            rustle.distance = 0.0;
            return;
        }
        BlockPos leavesPos = immersion.densest();
        BlockState leaves = level.getBlockState(leavesPos);
        double dx = entity.getX() - entity.xo, dy = entity.getY() - entity.yo, dz = entity.getZ() - entity.zo;
        double speed = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float quiet = entity.isShiftKeyDown() ? 0.5F : 1.0F;
        RandomSource random = level.getRandom();

        if (!rustle.inside) {
            rustle.inside = true;
            rustle.distance = 0.0;
            if (speed >= MIN_SPEED) {
                double scale = Math.clamp(speed / 0.25, 0.5, MOST_BURST_SCALE);
                shed(level, entity, leavesPos, leaves, (int) Math.round(BURST_LEAVES * scale * quiet), speed);
                boolean crash = -dy > CRASH_SPEED;
                playRustle(level, entity, speed, quiet * (crash ? 1.6F : 1.2F), crash ? 0.75F : 1.0F, random);
            }
            return;
        }
        if (speed < MIN_SPEED) return;

        // A fraction left over becomes a leaf now and then, so slow movement still sheds a few.
        double leavesNow = Math.min(MAX_LEAVES_PER_TICK, speed * LEAVES_PER_SPEED * immersion.body()) * quiet;
        int count = (int) leavesNow + (random.nextDouble() < leavesNow - Math.floor(leavesNow) ? 1 : 0);
        shed(level, entity, leavesPos, leaves, count, speed);

        rustle.distance += speed;
        if (rustle.distance >= RUSTLE_DISTANCE) {
            rustle.distance = 0.0;
            playRustle(level, entity, speed, quiet * (float) (0.5 + 0.5 * immersion.body()), 1.0F, random);
        }
    }

    private static void shed(ClientLevel level, LivingEntity entity, BlockPos pos, BlockState leaves, int count, double speed) {
        if (count <= 0) return;
        ParticleOptions particle = leafParticle(level, pos, leaves);
        RandomSource random = level.getRandom();
        // Around the entity, a little wider than it is.
        double width = entity.getBbWidth() + 0.7, height = entity.getBbHeight() + 0.2;
        double spread = 0.04 + speed * 0.25;
        for (int i = 0; i < count; i++) {
            double x = entity.getX() + (random.nextDouble() - 0.5) * width;
            double y = entity.getY() - 0.1 + random.nextDouble() * height;
            double z = entity.getZ() + (random.nextDouble() - 0.5) * width;
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

    private static void playRustle(ClientLevel level, LivingEntity entity, double speed, float loudness, float pitchScale, RandomSource random) {
        float volume = (float) Math.min(1.0, 0.35 + speed * 1.5) * loudness;
        float pitch = (0.9F + random.nextFloat() * 0.25F) * pitchScale;
        level.playLocalSound(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                CustomSpecialEffects.LEAVES_RUSTLE, SoundSource.BLOCKS, volume, pitch, false);
    }
}
