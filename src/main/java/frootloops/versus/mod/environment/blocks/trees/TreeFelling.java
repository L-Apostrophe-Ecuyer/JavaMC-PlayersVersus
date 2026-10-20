package frootloops.versus.mod.environment.blocks.trees;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.blocks.leaves.LeafLitter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Trees coming down, on the server, as Felling works them out. A cut (a tree block going, or what its foot rests on:
 * broken by a player, blown up, burnt, pushed, dug out) that leaves anything with nothing holding it up creaks, and
 * FELLING_DELAY ticks later, if nothing holds it up by then, it comes down away from whoever cut it. Each of its blocks
 * becomes a falling block: its tree blocks as themselves, the hives on it with their bees inside, the leaves of its
 * crown. A tree standing over its cut turns over on its hinge as one body (Topple), faster as it goes, its clients
 * turning it alike (TopplePayload); what it sweeps through it hurts by how fast it moves, and it is let go lying down,
 * or where it hits something, each block flying on with the speed it had. Anything else drops. The trunk lands as a
 * fallen log one block thick, the rest of its wood as log items, the leaves burst as if they had decayed where they
 * land, a hive lands whole and its bees come out angry at whoever cut the tree; nothing a felled tree does kills a
 * player or a pet. The creaking heart in the trunk breaks as if mined; vines and cocoa drop off as vanilla's do when
 * their support goes.
 */
public final class TreeFelling {

    /** How a felled tree's falling blocks land (entity tags, so they keep through a save): as a fallen log, as an item, as a burst leaf, as a hive. */
    public static final String TRUNK = "players-versus.felled_trunk";
    public static final String BRANCH = "players-versus.felled_branch";
    public static final String LEAF = "players-versus.felled_leaf";
    public static final String HIVE = "players-versus.felled_hive";
    /** A felled tree's block that turned over far enough along x, or z, to land lying that way. */
    public static final String TURNED_X = "players-versus.felled_turned_x";
    public static final String TURNED_Z = "players-versus.felled_turned_z";

    /** A falling block's gravity, blocks a tick per tick: what a swept creature's damage is measured by, as a falling log's. */
    private static final double FALLING_BLOCK_GRAVITY = 0.04;
    /** How long a tree may take turning over before it's let go, in ticks. */
    private static final int MOST_SWING_TICKS = 200;
    /** How far from a felled hive its bees look for whoever to go after, when nobody cut the tree, in blocks. */
    private static final double BEE_REACH = 16.0;

    /**
     * A cut waiting to bring its tree down: where (more spots, when later cuts would bring down only what this one will),
     * when, who made it (if anyone), and the tree blocks it would bring down when it was made.
     */
    private record Pending(List<BlockPos> cuts, long due, @Nullable Player cutter, @Nullable Vec3 from, Set<BlockPos> blocks) {}

    /** The player breaking a tree block, or what one rests on, right now: who made the cut that follows. */
    private record Cutting(ServerLevel level, BlockPos pos, Player player) {}

    /** A falling block of a felled tree turning over with it, where its centre started, and whether it's wood or a hive (not a leaf). */
    private record Piece(FallingBlockEntity block, Vec3 start, boolean solid) {}

    /** A felled tree's hive that landed whole, its bees to come out once it's in place. */
    private record Landed(BlockPos pos, @Nullable Player cutter) {}

    /** A felled group turning over as one: its pieces, the creatures it has hit, and who cut it. */
    private static final class Swing {
        final int id;
        final Topple topple;
        final List<Piece> pieces;
        final @Nullable Player cutter;
        final Set<LivingEntity> hit = new HashSet<>();
        /** The players told it started, who are told when it stops. */
        final Set<ServerPlayer> told = new HashSet<>();
        /** How far from the hinge its furthest block is, for how far a step moves it. */
        final double reach;
        int ticks;

        Swing(int id, Topple topple, List<Piece> pieces, @Nullable Player cutter) {
            this.id = id;
            this.topple = topple;
            this.pieces = pieces;
            this.cutter = cutter;
            this.reach = pieces.stream().mapToDouble(piece -> topple.reach(piece.start())).max().orElse(1.0);
        }
    }

    private static final Map<ServerLevel, List<Pending>> PENDING = new WeakHashMap<>();
    private static final Map<ServerLevel, List<Swing>> SWINGS = new WeakHashMap<>();
    private static final Map<ServerLevel, List<Landed>> LANDED = new WeakHashMap<>();
    private static final Map<FallingBlockEntity, Swing> SWINGING = new WeakHashMap<>();
    private static final Map<FallingBlockEntity, Player> HIVE_CUTTERS = new WeakHashMap<>();
    private static @Nullable Cutting cutting;
    /** While a tree comes down, its own blocks going are not cuts: the spots are checked once it's down. */
    private static boolean felling;
    private static final Set<BlockPos> CHECK_AFTER = new LinkedHashSet<>();
    private static int swings;

    private TreeFelling() {
    }

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(TreeFelling::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> forgetAll());
        // A player who starts seeing a turning tree's blocks partway through is told how it's turning by then
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
            Swing swing = entity instanceof FallingBlockEntity block ? SWINGING.get(block) : null;
            if (swing != null) tell(swing, player);
        });
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (level instanceof ServerLevel server && (Felling.isPart(state) || Felling.isPart(level.getBlockState(pos.above())))) cutBy(server, pos, player);
            return true;
        });
    }

    /** A player is about to break this tree block, or the block a tree rests on: they are the one cutting it. */
    public static void cutBy(ServerLevel level, BlockPos pos, Player player) {
        cutting = new Cutting(level, pos.immutable(), player);
    }

    /** A tree block went from this spot, whatever took it: what it held up may come down. */
    public static void removed(ServerLevel level, BlockPos pos) {
        check(level, pos);
    }

    /** What a tree block rested on at this spot is gone or no longer holds it (TreeBlock.updateShape): what it held up may come down. */
    public static void unheld(ServerLevel level, BlockPos pos) {
        check(level, pos);
    }

    /**
     * A cut at this spot: if that leaves anything with nothing holding it up, it creaks, and comes down soon. Twice at
     * one spot (a tree block cut is also what the block above it rested on) is one cut, whoever made it.
     */
    private static void check(ServerLevel level, BlockPos cut) {
        Player cutter = null;
        if (cutting != null && cutting.level() == level && cutting.pos().equals(cut)) {
            cutter = cutting.player();
            cutting = null;
        }
        if (!VersusSettings.Trees.DO_FELLING) return;
        if (felling) {
            CHECK_AFTER.add(cut.immutable());
            return;
        }
        Vec3 from = cutter == null ? null : cutter.position();
        List<Felling.Fall> falls = Felling.unheld(level, cut, from, level.getRandom());
        if (falls.isEmpty()) return;
        Set<BlockPos> blocks = new HashSet<>();
        for (Felling.Fall fall : falls) blocks.addAll(fall.blocks());
        BlockPos at = cut.immutable();
        // A cut already waiting that brings all this down takes this spot too, and this cutter if it had none: one creak
        List<Pending> pending = PENDING.computeIfAbsent(level, key -> new ArrayList<>());
        for (int i = 0; i < pending.size(); i++) {
            Pending other = pending.get(i);
            if (!other.cuts().contains(at) && !other.blocks().containsAll(blocks)) continue;
            List<BlockPos> cuts = new ArrayList<>(other.cuts());
            if (!cuts.contains(at)) cuts.add(at);
            boolean cutBy = other.cutter() == null && cutter != null;
            pending.set(i, new Pending(cuts, other.due(), cutBy ? cutter : other.cutter(), cutBy ? from : other.from(), other.blocks()));
            return;
        }
        level.playSound(null, at, SoundEvents.CREAKING_HEART_HURT, SoundSource.BLOCKS, 1.0f, 0.5f);
        pending.add(new Pending(List.of(at), level.getGameTime() + VersusSettings.Trees.FELLING_DELAY, cutter, from, blocks));
    }

    /** A world closing: nothing it was waiting on or turning over is kept, so nothing holds on to it. */
    private static void forgetAll() {
        PENDING.clear();
        SWINGS.clear();
        LANDED.clear();
        SWINGING.clear();
        HIVE_CUTTERS.clear();
        CHECK_AFTER.clear();
        cutting = null;
    }

    private static void tick(ServerLevel level) {
        cutting = null;
        List<Landed> landed = LANDED.remove(level);
        if (landed != null) for (Landed hive : landed) beesOut(level, hive);
        List<Swing> swinging = SWINGS.get(level);
        if (swinging != null) swinging.removeIf(swing -> swing(level, swing));
        List<Pending> pending = PENDING.get(level);
        if (pending == null || pending.isEmpty()) return;
        List<Pending> due = new ArrayList<>();
        pending.removeIf(cut -> cut.due() <= level.getGameTime() && due.add(cut));
        // What comes down is what nothing holds up now: a cut filled in the meantime holds its tree up again
        for (Pending cut : due) {
            for (BlockPos at : cut.cuts()) {
                for (Felling.Fall fall : Felling.unheld(level, at, cut.from(), level.getRandom())) fell(level, fall, at, cut.cutter());
            }
        }
    }

    private static void fell(ServerLevel level, Felling.Fall fall, BlockPos at, @Nullable Player cutter) {
        Direction way = fall.direction();
        BlockPos below = at.below();
        Set<BlockPos> trunk = trunk(fall.blocks(), at);
        // A tree standing over its cut turns over on it; anything else (a branch cut off) just drops
        boolean topples = fall.blocks().contains(at.above()) && Felling.holds(level, below, level.getBlockState(below));
        // Cut through its trunk with room in front, it drops onto what's under the cut as it tips, and lies on the ground
        double drop = topples && settles(level, fall.blocks(), at, way) ? 1.0 : 0.0;
        List<Piece> pieces = new ArrayList<>();
        List<BlockPos> after;
        felling = true;
        try {
            for (BlockPos pos : fall.blocks()) {
                if (level.getBlockState(pos).is(Blocks.CREAKING_HEART)) level.destroyBlock(pos, true, cutter);
            }
            for (BlockPos pos : hives(level, fall)) pieces.add(new Piece(launchHive(level, pos, cutter), Vec3.atCenterOf(pos), true));
            int flying = 0;
            List<BlockPos> blocks = new ArrayList<>(fall.blocks());
            blocks.sort(Comparator.comparingInt(BlockPos::getY));
            for (BlockPos pos : blocks) {
                BlockState state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof TreeBlock)) continue;
                if (flying++ >= VersusSettings.Trees.MOST_FALLING_BLOCKS) {
                    level.destroyBlock(pos, true);
                    continue;
                }
                FallingBlockEntity log = launch(level, pos, state, trunk.contains(pos) ? TRUNK : BRANCH);
                log.setHurtsEntities(VersusSettings.Trees.LOG_DAMAGE_PER_BLOCK, VersusSettings.Trees.MOST_LOG_DAMAGE);
                pieces.add(new Piece(log, Vec3.atCenterOf(pos), true));
            }
            for (BlockPos pos : fall.crown()) {
                BlockState leaf = level.getBlockState(pos);
                if (!(leaf.getBlock() instanceof LeavesBlock)) continue;
                if (flying++ >= VersusSettings.Trees.MOST_FALLING_BLOCKS) {
                    burst(level, pos, leaf);
                    level.removeBlock(pos, false);
                    continue;
                }
                pieces.add(new Piece(launch(level, pos, leaf, LEAF), Vec3.atCenterOf(pos), false));
            }
        } finally {
            felling = false;
            after = new ArrayList<>(CHECK_AFTER);
            CHECK_AFTER.clear();
        }
        if (topples && !pieces.isEmpty()) swing(level, hinge(fall.blocks(), at, way).subtract(0.0, drop, 0.0), way, pieces, drop, cutter);
        // What the fallen tree held up may come down too: another tree's branch resting on it
        for (BlockPos pos : after) check(level, pos);
    }

    /** The trunk: each column of the group going straight up from just above the cut, within a block of it (a mega tree's four). */
    private static Set<BlockPos> trunk(Set<BlockPos> group, BlockPos cut) {
        Set<BlockPos> trunk = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos.MutableBlockPos pos = cut.offset(dx, 1, dz).mutable();
                while (group.contains(pos)) {
                    trunk.add(pos.immutable());
                    pos.move(Direction.UP);
                }
            }
        }
        return trunk;
    }

    /** Where a group falling this way turns over: the front edge of its lowest layer, level with that layer's bottom. */
    private static Vec3 hinge(Set<BlockPos> group, BlockPos cut, Direction way) {
        int bottom = Integer.MAX_VALUE;
        for (BlockPos pos : group) bottom = Math.min(bottom, pos.getY());
        double front = Double.NEGATIVE_INFINITY;
        for (BlockPos pos : group) {
            if (pos.getY() == bottom) front = Math.max(front, (pos.getX() - cut.getX()) * way.getStepX() + (pos.getZ() - cut.getZ()) * way.getStepZ());
        }
        double reach = front + 0.5;
        return new Vec3(cut.getX() + 0.5 + way.getStepX() * reach, bottom, cut.getZ() + 0.5 + way.getStepZ() * reach);
    }

    /**
     * Whether a group cut right under its lowest layer drops onto what's under the cut as it tips: when the space in
     * front of the cut, at the cut's level, is clear for it to lie in (not when it was dug out from under, the ground all
     * round the hole, which it tips over the edge of instead).
     */
    private static boolean settles(ServerLevel level, Set<BlockPos> group, BlockPos cut, Direction way) {
        int bottom = Integer.MAX_VALUE;
        for (BlockPos pos : group) bottom = Math.min(bottom, pos.getY());
        BlockPos front = cut.relative(way);
        if (bottom != cut.getY() + 1 || !level.hasChunkAt(front)) return false;
        BlockState state = level.getBlockState(front);
        return state.is(BlockTags.LEAVES) || state.getCollisionShape(level, front).isEmpty();
    }

    /** The hives on the felled tree: any next to one of its tree blocks or the leaves going with it. */
    private static Set<BlockPos> hives(ServerLevel level, Felling.Fall fall) {
        Set<BlockPos> hives = new LinkedHashSet<>();
        for (Set<BlockPos> part : List.of(fall.blocks(), fall.crown())) {
            for (BlockPos pos : part) {
                for (Direction side : Direction.values()) {
                    BlockPos next = pos.relative(side);
                    if (level.hasChunkAt(next) && level.getBlockState(next).is(BlockTags.BEEHIVES)) hives.add(next.immutable());
                }
            }
        }
        return hives;
    }

    /** This block of a felled tree as a falling block, marked with how it lands. */
    private static FallingBlockEntity launch(ServerLevel level, BlockPos pos, BlockState state, String role) {
        FallingBlockEntity block = FallingBlockEntity.fall(level, pos, state);
        block.addTag(role);
        return block;
    }

    /** A felled tree's hive as a falling block, its bees and honey inside it. */
    private static FallingBlockEntity launchHive(ServerLevel level, BlockPos pos, @Nullable Player cutter) {
        BlockEntity hive = level.getBlockEntity(pos);
        CompoundTag data = hive == null ? null : hive.saveWithoutMetadata(level.registryAccess());
        FallingBlockEntity block = launch(level, pos, level.getBlockState(pos), HIVE);
        block.blockData = data;
        if (cutter != null) HIVE_CUTTERS.put(block, cutter);
        return block;
    }

    /** This felled group starting to turn over on its hinge, settling this far, its clients told so they turn it alike. */
    private static void swing(ServerLevel level, Vec3 hinge, Direction way, List<Piece> pieces, double drop, @Nullable Player cutter) {
        List<Topple.Mass> masses = new ArrayList<>();
        for (Piece piece : pieces) masses.add(new Topple.Mass(piece.start(), piece.solid() ? 1.0 : Felling.LEAF_WEIGHT));
        Swing swing = new Swing(swings++, Topple.of(hinge, way, masses, drop), new ArrayList<>(pieces), cutter);
        SWINGS.computeIfAbsent(level, key -> new ArrayList<>()).add(swing);
        for (Piece piece : pieces) SWINGING.put(piece.block(), swing);
        for (ServerPlayer player : PlayerLookup.tracking(level, BlockPos.containing(hinge))) tell(swing, player);
    }

    /** Tells this player how this felled group is turning, as it is now, once. */
    private static void tell(Swing swing, ServerPlayer player) {
        if (!swing.told.add(player) || !ServerPlayNetworking.canSend(player, TopplePayload.TYPE.id())) return;
        int[] blocks = new int[swing.pieces.size()];
        long[] starts = new long[swing.pieces.size()];
        for (int i = 0; i < swing.pieces.size(); i++) {
            Piece piece = swing.pieces.get(i);
            blocks[i] = piece.block().getId();
            starts[i] = BlockPos.containing(piece.start()).asLong();
        }
        ServerPlayNetworking.send(player, TopplePayload.start(swing.id, swing.topple, blocks, starts));
    }

    /**
     * A tick of a felled group turning over: its blocks move round the hinge, its leaves burst against the ground, its
     * wood hurts what it sweeps through. Lying down, hitting something solid on the way, or tipping back, it's let go.
     *
     * @return whether it's done
     */
    private static boolean swing(ServerLevel level, Swing swing) {
        swing.pieces.removeIf(piece -> !piece.block().isAlive());
        Topple topple = swing.topple;
        if (swing.pieces.isEmpty()) {
            stop(level, swing);
            return true;
        }
        topple.step();
        double free = freeThrough(level, swing);
        boolean blocked = free < 1.0;
        if (blocked) topple.backTo(free);
        swing.pieces.removeIf(piece -> !piece.solid() && burstAgainst(level, piece, topple.centre(piece.start())));
        for (Piece piece : swing.pieces) {
            Vec3 centre = topple.centre(piece.start());
            piece.block().setPos(centre.x, centre.y - 0.5, centre.z);
            piece.block().setDeltaMovement(topple.velocity(piece.start()));
            if (piece.solid()) sweep(level, swing, piece, centre);
        }
        if (!blocked && !topple.isDown() && topple.speed() > 0.0 && ++swing.ticks < MOST_SWING_TICKS) return false;
        release(level, swing);
        return true;
    }

    /**
     * How far through its last step a felled group got before its wood ran into anything solid, or any of it into a
     * chunk that isn't loaded (1 all the way): looked for along the step half a block at a time, so a fast top can't
     * pass through a wall.
     */
    private static double freeThrough(ServerLevel level, Swing swing) {
        int samples = Math.max(1, (int) Math.ceil(swing.topple.moved(swing.reach) / 0.5));
        for (int k = 1; k <= samples; k++) {
            double through = (double) k / samples;
            for (Piece piece : swing.pieces) {
                Vec3 centre = swing.topple.centreThrough(piece.start(), through);
                if (!level.hasChunkAt(BlockPos.containing(centre)) || piece.solid() && obstructed(level, centre)) return (double) (k - 1) / samples;
            }
        }
        return 1.0;
    }

    /** Whether a block's room round this centre runs into anything solid that isn't leaves. */
    private static boolean obstructed(ServerLevel level, Vec3 centre) {
        AABB box = AABB.ofSize(centre, 0.9, 0.9, 0.9);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (!level.hasChunkAt(pos)) return true;
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.is(BlockTags.LEAVES)) continue;
            VoxelShape shape = state.getCollisionShape(level, pos);
            for (AABB part : shape.toAabbs()) if (part.move(pos).intersects(box)) return true;
        }
        return false;
    }

    /** A turning leaf about to go into the ground bursts where it is. */
    private static boolean burstAgainst(ServerLevel level, Piece leaf, Vec3 centre) {
        BlockPos next = BlockPos.containing(centre);
        BlockState there = level.getBlockState(next);
        if (there.is(BlockTags.LEAVES) || there.getCollisionShape(level, next).isEmpty()) return false;
        burst(level, leaf.block().blockPosition(), leaf.block().getBlockState());
        leaf.block().discard();
        return true;
    }

    /** A turning log hits each creature in its way once, as hard as an anvil reaching its speed would. */
    private static void sweep(ServerLevel level, Swing swing, Piece piece, Vec3 centre) {
        double speed = swing.topple.velocity(piece.start()).length();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(centre, 1.0, 1.0, 1.0),
                target -> target.isAlive() && !target.isSpectator() && !swing.hit.contains(target))) {
            swing.hit.add(target);
            float damage = sweepDamage(speed);
            if (damage > 0.0f) target.hurtServer(level, level.damageSources().fallingBlock(piece.block()), damage);
        }
    }

    /**
     * What a felled tree's log hits a creature with at this speed (blocks a tick): what a falling log does landing from
     * as high as it would fall to reach that speed, LOG_DAMAGE_PER_BLOCK a block after the first, at most MOST_LOG_DAMAGE.
     */
    public static float sweepDamage(double speed) {
        double height = speed * speed / (2.0 * FALLING_BLOCK_GRAVITY);
        int blocks = Math.max(0, (int) Math.ceil(height - 1.0));
        return Math.min(VersusSettings.Trees.MOST_LOG_DAMAGE, VersusSettings.Trees.LOG_DAMAGE_PER_BLOCK * blocks);
    }

    /** A felled group let go: each block flies on as fast as it moved, a trunk turned far enough over to lie along the way it fell. */
    private static void release(ServerLevel level, Swing swing) {
        Topple topple = swing.topple;
        String turned = topple.angle() < Topple.DOWN / 2.0 ? null : topple.way().getAxis() == Direction.Axis.X ? TURNED_X : TURNED_Z;
        for (Piece piece : swing.pieces) {
            SWINGING.remove(piece.block());
            piece.block().setDeltaMovement(topple.velocity(piece.start()));
            piece.block().needsSync = true;
            if (turned != null) piece.block().addTag(turned);
        }
        stop(level, swing);
    }

    /** Tells every player told it started, and any now watching it, that a felled group stopped turning. */
    private static void stop(ServerLevel level, Swing swing) {
        Set<ServerPlayer> players = new HashSet<>(swing.told);
        players.addAll(PlayerLookup.tracking(level, BlockPos.containing(swing.topple.hinge())));
        TopplePayload payload = TopplePayload.release(swing.id, swing.topple.angle(), swing.topple.settled());
        for (ServerPlayer player : players) {
            if (!player.hasDisconnected() && ServerPlayNetworking.canSend(player, TopplePayload.TYPE.id())) ServerPlayNetworking.send(player, payload);
        }
    }

    /** A log's axis once its tree has turned over along this axis: standing up, it lies along the fall; lying along the fall, it stands. */
    private static Direction.Axis turned(Direction.Axis axis, Direction.Axis fall) {
        if (axis == Direction.Axis.Y) return fall;
        return axis == fall ? Direction.Axis.Y : axis;
    }

    /** A leaf bursting as if it decayed here: its drops fall, and its shed settles on the ground below by chance. */
    private static void burst(ServerLevel level, BlockPos pos, BlockState leaf) {
        Block.dropResources(leaf, level, pos);
        LeafLitter.decay(level, pos.above(), leaf, level.getRandom());
    }

    /** Whether this falling block is part of a felled tree. */
    public static boolean isFelled(FallingBlockEntity block) {
        Set<String> tags = block.entityTags();
        return tags.contains(TRUNK) || tags.contains(BRANCH) || tags.contains(LEAF) || tags.contains(HIVE);
    }

    /** Whether this falling block is turning over with its tree, moved by it and not by its own fall. */
    public static boolean isSwinging(FallingBlockEntity block) {
        return SWINGING.containsKey(block);
    }

    /**
     * What a felled tree's falling block lands as at this spot, or null when it breaks there instead (breakOnLanding): a
     * trunk block as its wood's log, lying along the way its tree turned over if it did, unless it would lie on another
     * log (the fallen log is one block thick) or a creature is there; a hive as itself.
     */
    public static @Nullable BlockState landing(ServerLevel level, FallingBlockEntity block, BlockPos pos, BlockState state) {
        Set<String> tags = block.entityTags();
        if (tags.contains(HIVE)) return state;
        if (tags.contains(TRUNK) && !level.getBlockState(pos.below()).is(BlockTags.LOGS)
                && level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) return lying(state, tags);
        breakOnLanding(level, block, pos);
        return null;
    }

    /** A trunk's block landed: its wood's log, turned as its tree turned over. */
    private static BlockState lying(BlockState state, Set<String> tags) {
        BlockState log = state.getBlock() instanceof TreeBlock tree ? tree.log().defaultBlockState() : state;
        if (!state.hasProperty(RotatedPillarBlock.AXIS) || !log.hasProperty(RotatedPillarBlock.AXIS)) return log;
        Direction.Axis axis = state.getValue(RotatedPillarBlock.AXIS);
        if (tags.contains(TURNED_X)) axis = turned(axis, Direction.Axis.X);
        else if (tags.contains(TURNED_Z)) axis = turned(axis, Direction.Axis.Z);
        return log.setValue(RotatedPillarBlock.AXIS, axis);
    }

    /** A felled tree's falling block landed whole here: a hive's bees come out once its block is in place. */
    public static void landed(ServerLevel level, FallingBlockEntity block, BlockPos pos) {
        if (block.entityTags().contains(HIVE)) LANDED.computeIfAbsent(level, key -> new ArrayList<>()).add(new Landed(pos.immutable(), HIVE_CUTTERS.get(block)));
    }

    /** A felled tree's falling block that can't land whole: a log drops as its wood's log, a leaf bursts, a hive breaks and lets its bees out. */
    public static void breakOnLanding(ServerLevel level, FallingBlockEntity block, BlockPos pos) {
        Set<String> tags = block.entityTags();
        BlockState state = block.getBlockState();
        if (tags.contains(LEAF)) burst(level, pos, state);
        else if (tags.contains(HIVE)) {
            releaseBees(level, block, Vec3.atCenterOf(pos));
            Block.dropResources(state, level, pos);
        } else block.spawnAtLocation(level, state.getBlock() instanceof TreeBlock tree ? tree.log() : state.getBlock());
    }

    /** A landed hive's bees come out, angry. */
    private static void beesOut(ServerLevel level, Landed hive) {
        BlockState state = level.getBlockState(hive.pos());
        if (level.getBlockEntity(hive.pos()) instanceof BeehiveBlockEntity bees) bees.emptyAllLivingFromHive(null, state, BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
        anger(level, Vec3.atCenterOf(hive.pos()), hive.cutter());
    }

    /** The bees a broken felled hive carried come out where it broke, angry. */
    private static void releaseBees(ServerLevel level, FallingBlockEntity hive, Vec3 at) {
        if (hive.blockData != null && hive.blockData.get("bees") != null) {
            List<BeehiveBlockEntity.Occupant> bees = BeehiveBlockEntity.Occupant.LIST_CODEC
                    .parse(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), hive.blockData.get("bees")).result().orElse(List.of());
            for (BeehiveBlockEntity.Occupant occupant : bees) {
                Entity bee = occupant.createEntity(level, BlockPos.containing(at));
                if (bee == null) continue;
                bee.snapTo(at.x, at.y, at.z, level.getRandom().nextFloat() * 360.0f, 0.0f);
                level.addFreshEntity(bee);
            }
        }
        anger(level, at, HIVE_CUTTERS.get(hive));
    }

    /** The bees round a felled hive go after whoever cut its tree, or the nearest player when nobody did. */
    private static void anger(ServerLevel level, Vec3 at, @Nullable Player cutter) {
        Player target = cutter != null && cutter.isAlive() && cutter.level() == level ? cutter : level.getNearestPlayer(at.x, at.y, at.z, BEE_REACH, false);
        if (target == null) return;
        for (Bee bee : level.getEntitiesOfClass(Bee.class, AABB.ofSize(at, 16.0, 12.0, 16.0), bee -> bee.getTarget() == null)) bee.setTarget(target);
    }

    /** What a felled tree does to this creature: as much as ever, but never its last half heart, a player's or a pet's. */
    public static float spare(LivingEntity entity, DamageSource source, float amount) {
        if (!(source.getDirectEntity() instanceof FallingBlockEntity block) || !isFelled(block)) return amount;
        if (!(entity instanceof Player) && !(entity instanceof OwnableEntity owned && owned.getOwnerReference() != null)) return amount;
        return Math.min(amount, Math.max(0.0f, entity.getHealth() - 1.0f));
    }
}
