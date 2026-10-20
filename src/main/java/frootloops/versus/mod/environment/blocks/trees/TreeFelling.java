package frootloops.versus.mod.environment.blocks.trees;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.blocks.leaves.LeafLitter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Trees coming down, on the server, as Felling works them out. A tree block going (cut by a player, blown up, burnt,
 * pushed) that leaves anything with nothing holding it up creaks, and FELLING_DELAY ticks later, if nothing holds it up
 * by then, it falls away from whoever cut it: each tree block a falling block of its wood's vanilla log turned to lie
 * along the fall, each leaf of its crown a falling leaf, thrown so that a block h above the cut lands about h out. The
 * trunk lands as a fallen log one block thick, the rest of its wood as log items, the leaves burst as if they had
 * decayed where they land; a falling log hurts what it lands on as an anvil does, but never kills a player or a pet.
 * What the tree carries breaks as if mined: the creaking heart in its trunk, the bee nest on it, its bees coming out
 * angry at whoever cut it; vines and cocoa drop off as vanilla's do when their support goes.
 */
public final class TreeFelling {

    /** How a felled tree's falling blocks land (entity tags, so they keep through a save): as a fallen log, as an item, as a burst leaf. */
    public static final String TRUNK = "players-versus.felled_trunk";
    public static final String BRANCH = "players-versus.felled_branch";
    public static final String LEAF = "players-versus.felled_leaf";

    /** A cut waiting to bring its tree down: where, when, and who made it (if anyone). */
    private record Pending(BlockPos cut, long due, @Nullable Player cutter, @Nullable Vec3 from) {}

    /** The player breaking a tree block right now: who made the cut that follows. */
    private record Cutting(ServerLevel level, BlockPos pos, Player player) {}

    private static final Map<ServerLevel, List<Pending>> PENDING = new WeakHashMap<>();
    private static @Nullable Cutting cutting;
    /** While a tree comes down, its own blocks going are not cuts. */
    private static boolean felling;

    private TreeFelling() {
    }

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(TreeFelling::tick);
    }

    /** A player is about to break this tree block: they are the one cutting it. */
    public static void cutBy(ServerLevel level, BlockPos pos, Player player) {
        cutting = new Cutting(level, pos.immutable(), player);
    }

    /**
     * A tree block went from this spot: if that leaves anything with nothing holding it up, it creaks, and comes down
     * soon. Not when what took its place is part of a tree itself (a creaking heart set in a trunk): that's no cut.
     */
    public static void removed(ServerLevel level, BlockPos pos) {
        Player cutter = cutting != null && cutting.level() == level && cutting.pos().equals(pos) ? cutting.player() : null;
        cutting = null;
        if (felling || !VersusSettings.Trees.DO_FELLING || Felling.isPart(level.getBlockState(pos))) return;
        Vec3 from = cutter == null ? null : cutter.position();
        if (Felling.afterCut(level, pos, from, level.getRandom()).isEmpty()) return;
        level.playSound(null, pos, SoundEvents.CREAKING_HEART_HURT, SoundSource.BLOCKS, 1.0f, 0.5f);
        PENDING.computeIfAbsent(level, key -> new ArrayList<>())
                .add(new Pending(pos.immutable(), level.getGameTime() + VersusSettings.Trees.FELLING_DELAY, cutter, from));
    }

    private static void tick(ServerLevel level) {
        List<Pending> pending = PENDING.get(level);
        if (pending == null || pending.isEmpty()) return;
        List<Pending> due = new ArrayList<>();
        pending.removeIf(cut -> cut.due() <= level.getGameTime() && due.add(cut));
        for (Pending cut : due) {
            // Filled in the meantime with part of a tree, the cut holds it up again
            if (Felling.isPart(level.getBlockState(cut.cut()))) continue;
            for (Felling.Fall fall : Felling.afterCut(level, cut.cut(), cut.from(), level.getRandom())) fell(level, fall, cut);
        }
    }

    private static void fell(ServerLevel level, Felling.Fall fall, Pending cut) {
        Direction way = fall.direction();
        Set<BlockPos> trunk = trunk(fall.blocks(), cut.cut());
        felling = true;
        try {
            carried(level, fall, cut.cutter());
            int flying = 0;
            List<BlockPos> blocks = new ArrayList<>(fall.blocks());
            blocks.sort(Comparator.comparingInt(BlockPos::getY));
            for (BlockPos pos : blocks) {
                BlockState state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof TreeBlock tree)) continue;
                if (flying++ >= VersusSettings.Trees.MOST_FALLING_BLOCKS) {
                    level.destroyBlock(pos, true);
                    continue;
                }
                Direction.Axis axis = turned(state.getValue(RotatedPillarBlock.AXIS), way.getAxis());
                FallingBlockEntity log = launch(level, pos, tree.log().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis), cut.cut(), way,
                        trunk.contains(pos) ? TRUNK : BRANCH);
                log.setHurtsEntities(VersusSettings.Trees.LOG_DAMAGE_PER_BLOCK, VersusSettings.Trees.MOST_LOG_DAMAGE);
            }
            for (BlockPos pos : fall.crown()) {
                BlockState leaf = level.getBlockState(pos);
                if (!(leaf.getBlock() instanceof LeavesBlock)) continue;
                if (flying++ >= VersusSettings.Trees.MOST_FALLING_BLOCKS) {
                    burst(level, pos, leaf);
                    level.removeBlock(pos, false);
                    continue;
                }
                launch(level, pos, leaf, cut.cut(), way, LEAF);
            }
        } finally {
            felling = false;
        }
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

    /** A log's axis once its tree has fallen over along this axis: standing up, it lies along the fall; lying along the fall, it stands. */
    private static Direction.Axis turned(Direction.Axis axis, Direction.Axis fall) {
        if (axis == Direction.Axis.Y) return fall;
        return axis == fall ? Direction.Axis.Y : axis;
    }

    /** This block of a falling tree, as a falling block thrown that way, fast enough that from h above the cut it lands about h out. */
    private static FallingBlockEntity launch(ServerLevel level, BlockPos pos, BlockState state, BlockPos cut, Direction way, String role) {
        FallingBlockEntity block = FallingBlockEntity.fall(level, pos, state);
        double speed = throwSpeed(pos.getY() - cut.getY());
        block.setDeltaMovement(way.getStepX() * speed, 0.0, way.getStepZ() * speed);
        block.addTag(role);
        return block;
    }

    /**
     * How fast across to throw a block h above the cut so it lands about h out, falling as vanilla's falling blocks do:
     * 0.04 a tick faster down, and both ways 2% slower, each tick.
     */
    static double throwSpeed(double height) {
        if (height <= 0.0) return 0.0;
        // A tick of a falling block: gravity, then the move, then both speeds lose 2%
        double y = height, down = 0.0, across = 0.0, keep = 1.0;
        for (int tick = 0; tick < 200 && y > 0.0; tick++) {
            down -= 0.04;
            y += down;
            across += keep;
            down *= 0.98;
            keep *= 0.98;
        }
        return height / across;
    }

    /** What the felled tree carries breaks as if mined: the creaking heart in its trunk, the bee nests on it (their bees out after the cutter). */
    private static void carried(ServerLevel level, Felling.Fall fall, @Nullable Player cutter) {
        Set<BlockPos> nests = new HashSet<>();
        for (BlockPos pos : fall.blocks()) {
            if (level.getBlockState(pos).is(Blocks.CREAKING_HEART)) level.destroyBlock(pos, true, cutter);
            for (Direction side : Direction.values()) {
                BlockPos next = pos.relative(side);
                if (level.getBlockState(next).is(Blocks.BEE_NEST)) nests.add(next.immutable());
            }
        }
        for (BlockPos nest : nests) {
            if (cutter != null && level.getBlockEntity(nest) instanceof BeehiveBlockEntity bees) {
                bees.emptyAllLivingFromHive(cutter, level.getBlockState(nest), BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
            }
            level.destroyBlock(nest, true, cutter);
        }
    }

    /** A leaf bursting as if it decayed here: its drops fall, and its shed settles on the ground below by chance. */
    private static void burst(ServerLevel level, BlockPos pos, BlockState leaf) {
        Block.dropResources(leaf, level, pos);
        LeafLitter.decay(level, pos.above(), leaf, level.getRandom());
    }

    /** Whether this falling block is part of a felled tree. */
    public static boolean isFelled(FallingBlockEntity block) {
        Set<String> tags = block.entityTags();
        return tags.contains(TRUNK) || tags.contains(BRANCH) || tags.contains(LEAF);
    }

    /**
     * A felled tree's falling block landing at this spot: a trunk log lands as a block, unless it would lie on another
     * log (the fallen log is one block thick) or a creature is there; else it breaks: a log into its item, a leaf as if it
     * decayed there.
     *
     * @return whether to place it as vanilla would
     */
    public static boolean land(ServerLevel level, FallingBlockEntity block, BlockPos pos) {
        Set<String> tags = block.entityTags();
        if (tags.contains(TRUNK) && !level.getBlockState(pos.below()).is(BlockTags.LOGS)
                && level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty()) return true;
        breakOnLanding(level, block, pos);
        return false;
    }

    /** A felled tree's falling block that can't land whole: a log drops as its item, a leaf bursts. */
    public static void breakOnLanding(ServerLevel level, FallingBlockEntity block, BlockPos pos) {
        if (block.entityTags().contains(LEAF)) burst(level, pos, block.getBlockState());
        else block.spawnAtLocation(level, block.getBlockState().getBlock());
    }

    /** What a falling tree's log does to this creature: as an anvil would, but never its last half heart, a player's or a pet's. */
    public static float spare(LivingEntity entity, DamageSource source, float amount) {
        if (!(source.getDirectEntity() instanceof FallingBlockEntity block) || !isFelled(block)) return amount;
        if (!(entity instanceof Player) && !(entity instanceof OwnableEntity owned && owned.getOwnerReference() != null)) return amount;
        return Math.min(amount, Math.max(0.0f, entity.getHealth() - 1.0f));
    }

    /** While a felled tree's own blocks go, so no removal of them counts as a cut. */
    public static boolean isFelling() {
        return felling;
    }
}
