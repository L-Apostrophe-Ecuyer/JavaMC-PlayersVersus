package frootloops.versus.mod.environment.blocks.leaves;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
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
 * felled tree strews some over the ground where its crown lands. The green broad-leaved trees shed fallen leaves (which
 * brown into leaf litter or blow away with time), poplars leaf litter itself and cherry pink petals. It falls straight down through the canopy to the first
 * ground it can stay on.
 */
public final class LeafLitter {

    /** Leaves that shed fallen leaves: the green broad-leaved trees. */
    public static final TagKey<Block> SHEDS_FALLEN_LEAVES = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "sheds_fallen_leaves"));
    /** Leaves that shed leaf litter itself: the poplars, already turned. */
    public static final TagKey<Block> SHEDS_LEAF_LITTER = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "sheds_leaf_litter"));
    /** Leaves that shed pink petals instead. */
    public static final TagKey<Block> SHEDS_PINK_PETALS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "sheds_pink_petals"));
    /**
     * Ground cover that breaks at a touch without costing the attack (the player's client): leaf litter and fallen
     * leaves, so a felled tree's mess clears as fast as the attack key held sweeps over it.
     */
    public static final TagKey<Block> BREAKS_WITHOUT_COOLDOWN = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "breaks_without_cooldown"));

    /** Below this speed (blocks per tick) moving through leaves sheds nothing: standing in them, or gravity's tug. */
    private static final double STILL_SPEED = 0.1;
    private static final Direction[] FACINGS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private LeafLitter() {
    }

    /** What these leaves shed, as one segment, or null for leaves that shed nothing (spruce, pale oak). */
    public static BlockState shedState(BlockState leaves) {
        if (leaves.is(SHEDS_FALLEN_LEAVES)) return CustomBlocks.FALLEN_LEAVES.defaultBlockState();
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
        shedByChance(level, pos, leaves, random, VersusSettings.Leaves.DECAY_LITTER_CHANCE);
    }

    /** A felled tree's leaf bursting against the ground: it leaves a segment by a smaller chance than a decaying one. */
    public static void felled(ServerLevel level, BlockPos pos, BlockState leaves, RandomSource random) {
        shedByChance(level, pos, leaves, random, VersusSettings.Leaves.FELLED_LITTER_CHANCE);
    }

    private static void shedByChance(ServerLevel level, BlockPos pos, BlockState leaves, RandomSource random, double chance) {
        if (VersusSettings.Leaves.DO_LEAF_LITTER_SHEDDING && random.nextDouble() < chance) shed(level, pos, leaves, random);
    }

    /**
     * Drops one segment of what the leaves shed straight down, through any leaves below, to the first air above ground
     * it can stay on, within {@code LITTER_DROP_RANGE} blocks: a new segment there, or one more on a pile it joins (up to
     * four). A leaf joins any pile of leaves, fallen or browned, a petal only petals. It stops at anything else, so it
     * never replaces a plant, lands in water or rests on leaves.
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
            if (joins(litter, here)) return addSegment(level, pos, here);
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

    private static boolean joins(BlockState litter, BlockState pile) {
        if (litter.is(Blocks.PINK_PETALS)) return pile.is(Blocks.PINK_PETALS);
        return pile.is(Blocks.LEAF_LITTER) || pile.getBlock() instanceof FallenLeavesBlock;
    }

    /** One more segment on this pile, keeping the rest of its state (its facing, a placed pile's persistence). */
    private static boolean addSegment(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof SegmentableBlock segmentable)) return false;
        IntegerProperty amount = segmentable.getSegmentAmountProperty();
        int segments = state.getValue(amount);
        if (segments >= SegmentableBlock.MAX_SEGMENT) return false;
        level.setBlockAndUpdate(pos, state.setValue(amount, segments + 1));
        return true;
    }
}
