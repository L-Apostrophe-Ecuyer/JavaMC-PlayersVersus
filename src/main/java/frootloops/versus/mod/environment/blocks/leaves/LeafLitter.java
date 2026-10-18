package frootloops.versus.mod.environment.blocks.leaves;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SegmentableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;

/**
 * Leaf litter comes from the leaves above it: natural leaves something pushes through shed a segment now and then, the
 * more the faster it goes (a crash through a canopy knocks a handful down), and dying leaves leave one by chance, so a
 * felled tree carpets the ground under it. Cherry leaves shed pink petals. The litter falls straight down through the
 * canopy to the first ground it can stay on, and a pile of it softens a landing.
 */
public final class LeafLitter {

    /** Leaves that shed leaf litter: the broad-leaved trees. */
    public static final TagKey<Block> SHEDS_LEAF_LITTER = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "sheds_leaf_litter"));
    /** Leaves that shed pink petals instead. */
    public static final TagKey<Block> SHEDS_PINK_PETALS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "sheds_pink_petals"));

    /** Below this speed (blocks per tick) moving through leaves sheds nothing: standing in them, or gravity's tug. */
    private static final double STILL_SPEED = 0.1;
    private static final Direction[] FACINGS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private LeafLitter() {
    }

    /** What these leaves shed, as one segment, or null for leaves that shed nothing (spruce, pale oak). */
    public static BlockState shedState(BlockState leaves) {
        if (leaves.is(SHEDS_LEAF_LITTER)) return Blocks.LEAF_LITTER.defaultBlockState();
        if (leaves.is(SHEDS_PINK_PETALS)) return Blocks.PINK_PETALS.defaultBlockState();
        return null;
    }

    /** The chance per tick that a leaf something moves through this fast (blocks per tick) sheds. */
    public static double shedChance(double speed) {
        double over = Math.max(0.0, speed - STILL_SPEED);
        return Math.min(VersusSettings.Leaves.MOST_SHED_CHANCE, VersusSettings.Leaves.SHED_RATE * over * over);
    }

    /**
     * A leaf something passes through, on the server: natural leaves shed by chance, the more the faster it goes. Its
     * speed is measured as vanilla's sweet berry bush measures it: a player's is the movement its client sent, anything
     * else's how far it moved this tick. (A player is inside blocks during its own tick, after the server has caught its
     * old position up with where its client put it, so how far it moved would always be nothing.)
     */
    public static void disturb(ServerLevel level, BlockPos pos, BlockState leaves, Entity entity) {
        if (!VersusSettings.Leaves.DO_LEAF_LITTER_SHEDDING || leaves.getValue(LeavesBlock.PERSISTENT)) return;
        Vec3 movement = entity.isClientAuthoritative() ? entity.getKnownMovement() : entity.position().subtract(entity.oldPosition());
        double chance = shedChance(movement.length());
        if (chance > 0.0 && level.getRandom().nextDouble() < chance) shed(level, pos, leaves, level.getRandom());
    }

    /** A decaying leaf, just before it goes: it leaves a segment by chance. */
    public static void decay(ServerLevel level, BlockPos pos, BlockState leaves, RandomSource random) {
        if (!VersusSettings.Leaves.DO_LEAF_LITTER_SHEDDING) return;
        if (random.nextDouble() < VersusSettings.Leaves.DECAY_LITTER_CHANCE) shed(level, pos, leaves, random);
    }

    /**
     * Drops one segment of what the leaves shed straight down, through any leaves below, to the first air above ground
     * it can stay on, within {@code LITTER_DROP_RANGE} blocks: a new segment there, or one more on the same litter (up to
     * four). It stops at anything else, so it never replaces a plant, lands in water or rests on leaves.
     *
     * @return whether a segment landed
     */
    public static boolean shed(ServerLevel level, BlockPos leavesPos, BlockState leaves, RandomSource random) {
        BlockState litter = shedState(leaves);
        if (litter == null) return false;
        BlockPos.MutableBlockPos pos = leavesPos.mutable();
        for (int i = 0; i < VersusSettings.Leaves.LITTER_DROP_RANGE; i++) {
            pos.move(Direction.DOWN);
            BlockState here = level.getBlockState(pos);
            if (here.is(litter.getBlock())) return addSegment(level, pos, here);
            if (here.getBlock() instanceof LeavesBlock) continue;
            if (!here.isAir()) return false;
            if (litter.canSurvive(level, pos)) {
                BlockState placed = litter.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                        ? litter.setValue(BlockStateProperties.HORIZONTAL_FACING, FACINGS[random.nextInt(FACINGS.length)])
                        : litter;
                level.setBlockAndUpdate(pos, placed);
                return true;
            }
        }
        return false;
    }

    private static boolean addSegment(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof SegmentableBlock segmentable)) return false;
        IntegerProperty amount = segmentable.getSegmentAmountProperty();
        int segments = state.getValue(amount);
        if (segments >= SegmentableBlock.MAX_SEGMENT) return false;
        level.setBlockAndUpdate(pos, state.setValue(amount, segments + 1));
        return true;
    }

    /** Landing in leaf litter takes {@code LITTER_CUSHION_PER_SEGMENT} off the fall per segment; at the head of checkFallDamage. */
    public static void cushionLanding(LivingEntity entity, boolean onGround) {
        if (!onGround || entity.fallDistance <= 0.0) return;
        BlockState feet = entity.level().getBlockState(entity.blockPosition());
        if (!feet.is(Blocks.LEAF_LITTER) || !(feet.getBlock() instanceof SegmentableBlock segmentable)) return;
        int segments = feet.getValue(segmentable.getSegmentAmountProperty());
        entity.fallDistance *= Math.clamp(1.0 - VersusSettings.Leaves.LITTER_CUSHION_PER_SEGMENT * segments, 0.0, 1.0);
    }
}
