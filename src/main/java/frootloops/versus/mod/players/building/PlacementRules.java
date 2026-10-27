package frootloops.versus.mod.players.building;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.GlazedTerracottaBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * How a placed block is turned. Vanilla's state is the default; only what vanilla takes from noisy input changes: the
 * face clicked (a log's axis), the height clicked on a side (a slab's or stair's half, a trapdoor's), the way the player
 * faces (stairs, furnaces and the like, fence gates, signs and heads). What the click means is never touched: what a
 * block hangs on, waterlogging, a slab merged into a double slab, the halves of doors and beds.
 * <p>
 * Each property is decided by the first of these that applies:
 * <ol>
 * <li>Sneaking: vanilla's (the client doesn't ask).</li>
 * <li>The stroke: a block placed while right-click is still held since the first of the stroke was placed takes that
 * first block's turn, half and rotation, so a row or a floor placed in one go stays one way.</li>
 * <li>The last placement: the same block placed just before keeps its direction for the next one placed beside it in
 * the same line. A log, pillar or chain keeps its axis next to it on any side; stairs keep their facing and half
 * along their row or up and down their slope (not back to back, where a roof turns); a slab keeps its half beside it
 * on its level; a trapdoor its facing and half along its row.</li>
 * <li>The neighbours: a block extending a line of its own kind takes the line's turn (logs, stairs, furnaces and the
 * like); a slab, stair or trapdoor placed on the side of a full block, where the height clicked is a guess, takes the
 * half of the ones beside it; a fence gate faces across its fence; a sign or head turns like the ones beside it when
 * they're within two steps.</li>
 * <li>Vanilla's.</li>
 * </ol>
 * Pistons, observers, dispensers, droppers, crafters, hoppers, repeaters and comparators only follow a stroke: their
 * turn is what they do.
 * <p>
 * Everything here is the client's: it decides, then clicks so that vanilla places the state chosen (the face and the
 * height clicked, and which way the player looks), which any server takes as an ordinary placement.
 */
public final class PlacementRules {
    private PlacementRules() {}

    /** A block placed, and where. */
    public record Placed(BlockPos pos, BlockState state) {}

    /** The blocks around a placement. */
    public interface View {
        BlockState state(BlockPos pos);
        boolean sturdy(BlockPos pos, Direction face);

        static View of(BlockGetter level) {
            return new View() {
                @Override public BlockState state(BlockPos pos) { return level.getBlockState(pos); }
                @Override public boolean sturdy(BlockPos pos, Direction face) { return level.getBlockState(pos).isFaceSturdy(level, pos, face); }
            };
        }
    }

    /** The properties a placement may change; anything else must stay vanilla's (see {@link #acceptable}). */
    private static final Set<Property<?>> CHANGEABLE = Set.of(BlockStateProperties.AXIS, BlockStateProperties.HORIZONTAL_FACING,
            BlockStateProperties.FACING, BlockStateProperties.HALF, BlockStateProperties.SLAB_TYPE, BlockStateProperties.ROTATION_16,
            // Recomputed from the neighbours once the block is turned
            BlockStateProperties.STAIRS_SHAPE, BlockStateProperties.IN_WALL);

    /**
     * The state for a placement at pos, clicked on clickedFace, whose vanilla state is vanilla; stroke is the first
     * block of the stroke, recent the block placed just before, if any (see the class); viewer the player's eyes.
     */
    public static BlockState resolve(BlockState vanilla, BlockPos pos, Direction clickedFace, @Nullable BlockState stroke,
                                     @Nullable Placed recent, @Nullable Vec3 viewer, View view) {
        Block block = vanilla.getBlock();
        if (stroke != null && stroke.getBlock() == block && isTurnable(vanilla)) return copy(vanilla, stroke);
        BlockState followed = follow(vanilla, pos, recent);
        if (followed != null) return followed;
        if (isFunction(block)) return vanilla;

        if (vanilla.hasProperty(BlockStateProperties.AXIS)) return pillar(vanilla, pos, view);
        if (block instanceof StairBlock) return half(stairs(vanilla, pos, view), pos, clickedFace, view, StairBlock.class);
        if (block instanceof SlabBlock) return slab(vanilla, pos, clickedFace, view);
        if (block instanceof TrapDoorBlock) return half(vanilla, pos, clickedFace, view, TrapDoorBlock.class);
        if (block instanceof FenceGateBlock) return gate(vanilla, pos, viewer, view);
        if (isFront(block)) return front(vanilla, pos, view);
        if (vanilla.hasProperty(BlockStateProperties.ROTATION_16)) return rotation(vanilla, pos, view);
        return vanilla;
    }

    /** The state that keeps the direction of the same block placed just before beside it in a line, or null. */
    @Nullable
    private static BlockState follow(BlockState state, BlockPos pos, @Nullable Placed recent) {
        if (recent == null || recent.state.getBlock() != state.getBlock()) return null;
        BlockState last = recent.state;
        int dx = pos.getX() - recent.pos.getX(), dy = pos.getY() - recent.pos.getY(), dz = pos.getZ() - recent.pos.getZ();
        int steps = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
        boolean beside = steps == 1 && dy == 0;

        // Logs, pillars, chains: next to it on any side, a wall, a floor or a line of them
        if (state.hasProperty(BlockStateProperties.AXIS)) return steps == 1 ? copyValue(state, last, BlockStateProperties.AXIS) : null;
        if (state.getBlock() instanceof StairBlock) {
            Direction facing = last.getValue(BlockStateProperties.HORIZONTAL_FACING);
            boolean row = beside && (dx != 0 ? Direction.Axis.X : Direction.Axis.Z) != facing.getAxis();
            // A step up the way it climbs, or down the other way: the next row of a roof
            boolean slope = Math.abs(dy) == 1 && dx == facing.getStepX() * dy && dz == facing.getStepZ() * dy;
            return row || slope ? copy(state, last) : null;
        }
        if (state.getBlock() instanceof SlabBlock) {
            SlabType type = last.getValue(BlockStateProperties.SLAB_TYPE);
            if (!beside || type == SlabType.DOUBLE || state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE) return null;
            return state.setValue(BlockStateProperties.SLAB_TYPE, type);
        }
        if (state.getBlock() instanceof TrapDoorBlock) {
            Direction facing = last.getValue(BlockStateProperties.HORIZONTAL_FACING);
            boolean row = beside && (dx != 0 ? Direction.Axis.X : Direction.Axis.Z) != facing.getAxis();
            return row ? copy(state, last) : null;
        }
        return null;
    }

    /**
     * The way a slab goes from one the player just placed, when a click would double that one (vanilla merges a slab
     * clicked on its open face, or on the open half of a side): out of the side clicked; clicked on the open face, on
     * along the way the row has been going, else the way the player faces. Same half, beside it, as a row or a bridge
     * goes on.
     */
    public static Direction slabStep(Direction clickedFace, @Nullable Direction travel, Direction facing) {
        if (clickedFace.getAxis().isHorizontal()) return clickedFace;
        return travel != null && travel.getAxis().isHorizontal() ? travel : facing;
    }

    /** The way a row went from one block to the next placed beside it on its level, or null if not beside. */
    @Nullable
    public static Direction travel(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX(), dy = to.getY() - from.getY(), dz = to.getZ() - from.getZ();
        if (dy != 0 || Math.abs(dx) + Math.abs(dz) != 1) return null;
        return Direction.fromDelta(dx, 0, dz);
    }

    /**
     * Whether a state chosen for a placement may replace vanilla's: the same block, and only its turn, half or rotation
     * changed (never a double slab either way).
     */
    public static boolean acceptable(BlockState vanilla, BlockState chosen) {
        if (chosen.getBlock() != vanilla.getBlock()) return false;
        for (Property<?> property : vanilla.getProperties()) {
            if (vanilla.getValue(property).equals(chosen.getValue(property))) continue;
            if (!CHANGEABLE.contains(property)) return false;
        }
        if (vanilla.hasProperty(BlockStateProperties.SLAB_TYPE) && vanilla.getValue(BlockStateProperties.SLAB_TYPE) != chosen.getValue(BlockStateProperties.SLAB_TYPE)) {
            return vanilla.getValue(BlockStateProperties.SLAB_TYPE) != SlabType.DOUBLE && chosen.getValue(BlockStateProperties.SLAB_TYPE) != SlabType.DOUBLE;
        }
        return true;
    }

    /** The properties that make a block's turn: what a placement chooses. */
    private static final Set<Property<?>> TURN = Set.of(BlockStateProperties.AXIS, BlockStateProperties.HORIZONTAL_FACING,
            BlockStateProperties.FACING, BlockStateProperties.HALF, BlockStateProperties.SLAB_TYPE, BlockStateProperties.ROTATION_16);

    /** Whether two states are the same block turned the same way (whatever their shape from the neighbours). */
    public static boolean sameTurn(BlockState a, BlockState b) {
        if (a.getBlock() != b.getBlock()) return false;
        for (Property<?> property : TURN) {
            if (a.hasProperty(property) && !a.getValue(property).equals(b.getValue(property))) return false;
        }
        return true;
    }

    /** Blocks whose turn is what they do: they only follow a stroke. */
    public static boolean isFunction(Block block) {
        return block instanceof PistonBaseBlock || block instanceof ObserverBlock || block instanceof DispenserBlock
                || block instanceof CrafterBlock || block instanceof HopperBlock || block instanceof DiodeBlock;
    }

    /**
     * Blocks whose turn is free to follow a stroke: not one that comes from what they hang on (wall signs, torches,
     * buttons, ladders and the like keep vanilla's).
     */
    public static boolean isTurnable(BlockState state) {
        Block block = state.getBlock();
        return state.hasProperty(BlockStateProperties.AXIS) || state.hasProperty(BlockStateProperties.ROTATION_16)
                || block instanceof StairBlock || block instanceof SlabBlock || block instanceof TrapDoorBlock
                || block instanceof FenceGateBlock || block instanceof GlazedTerracottaBlock || isFront(block) || isFunction(block);
    }

    /** Blocks with a front, which line up with their own kind in rows and columns. */
    public static boolean isFront(Block block) {
        return block instanceof AbstractFurnaceBlock || block instanceof BarrelBlock || block instanceof CarvedPumpkinBlock
                || block instanceof BeehiveBlock || block instanceof ChiseledBookShelfBlock || block instanceof LoomBlock
                || block instanceof StonecutterBlock || block instanceof LecternBlock || block instanceof CampfireBlock
                || block instanceof AnvilBlock;
    }

    private static BlockState copy(BlockState state, BlockState reference) {
        state = copyValue(state, reference, BlockStateProperties.AXIS);
        state = copyValue(state, reference, BlockStateProperties.HORIZONTAL_FACING);
        state = copyValue(state, reference, BlockStateProperties.FACING);
        state = copyValue(state, reference, BlockStateProperties.HALF);
        state = copyValue(state, reference, BlockStateProperties.ROTATION_16);
        if (state.hasProperty(BlockStateProperties.SLAB_TYPE) && state.getValue(BlockStateProperties.SLAB_TYPE) != SlabType.DOUBLE
                && reference.getValue(BlockStateProperties.SLAB_TYPE) != SlabType.DOUBLE) {
            state = copyValue(state, reference, BlockStateProperties.SLAB_TYPE);
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState copyValue(BlockState to, BlockState from, Property<T> property) {
        return to.hasProperty(property) && from.hasProperty(property) ? to.setValue(property, from.getValue(property)) : to;
    }

    // Pillars (logs, stems, basalt, chains and the like): the axis of the neighbours lined up with it, the same block
    // counting most; a neighbour across the axis, or a close score, leaves vanilla's.

    private static BlockState pillar(BlockState state, BlockPos pos, View view) {
        Direction.Axis placed = state.getValue(BlockStateProperties.AXIS);
        int x = axisScore(Direction.Axis.X, placed, state.getBlock(), pos, view);
        if (x >= 50) return state;
        int y = axisScore(Direction.Axis.Y, placed, state.getBlock(), pos, view);
        if (y >= 50) return state;
        int z = axisScore(Direction.Axis.Z, placed, state.getBlock(), pos, view);
        if (z >= 50) return state;

        int max = Math.max(Math.max(x, y), z);
        int current = placed == Direction.Axis.X ? x : placed == Direction.Axis.Y ? y : z;
        if (max <= current + 5) return state; // Too close to warrant turning it
        Direction.Axis best = max == x ? Direction.Axis.X : max == y ? Direction.Axis.Y : Direction.Axis.Z;
        return state.setValue(BlockStateProperties.AXIS, best);
    }

    private static int axisScore(Direction.Axis axis, Direction.Axis placed, Block block, BlockPos pos, View view) {
        int score = 0;
        for (int i = -1; i < 2; i += 2) {
            BlockState neighbor = view.state(pos.relative(axis, i));
            Optional<Direction.Axis> neighborAxis = neighbor.getOptionalValue(BlockStateProperties.AXIS);
            boolean same = neighbor.is(block);
            if (neighborAxis.isEmpty()) continue;
            if (neighborAxis.get() == axis) {
                if (placed == axis && same) return 50;
                score += same ? 10 : 4;
            } else if (!neighbor.isAir()) {
                score -= same ? 2 : 1;
            }
        }
        return score;
    }

    // Stairs: the facing of the stairs beside them in a row, then of the ones diagonally above and below (a roof), then
    // with their flat back against a full block; vanilla's when it's close.

    private static BlockState stairs(BlockState state, BlockPos pos, View view) {
        Direction placed = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        Pair<Integer, Direction> x = bestFacing(Direction.Axis.X, placed, state.getBlock(), pos, view);
        if (x.getFirst() >= 50) return state;
        Pair<Integer, Direction> z = bestFacing(Direction.Axis.Z, placed, state.getBlock(), pos, view);
        if (z.getFirst() >= 50) return state;

        if (x.getFirst() <= 3 && z.getFirst() <= 3) return state;
        if ((x.getSecond() == placed || z.getSecond() == placed) && Math.abs(x.getFirst() - z.getFirst()) < 4) return state;
        Direction best = x.getFirst() > z.getFirst() ? x.getSecond() : z.getSecond();
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING, best);
    }

    private static Pair<Integer, Direction> bestFacing(Direction.Axis axis, Direction placed, Block block, BlockPos pos, View view) {
        Direction clockwise = axis == Direction.Axis.X ? Direction.NORTH : Direction.EAST;
        Direction counterClockwise = clockwise.getOpposite();
        int scoreClockwise = 0, scoreCounterClockwise = 0;

        // The stairs beside it along the axis, facing across it: a row
        for (int i = -1; i < 2; i += 2) {
            BlockState neighbor = view.state(pos.relative(axis, i));
            Optional<Direction> facing = neighbor.getOptionalValue(BlockStateProperties.HORIZONTAL_FACING);
            boolean same = neighbor.is(block);
            if (facing.isEmpty()) continue;
            if (facing.get() == clockwise) {
                if (placed == clockwise && same) return Pair.of(50, clockwise);
                scoreClockwise += same ? 10 : 4;
            } else if (facing.get() == counterClockwise) {
                if (placed == counterClockwise && same) return Pair.of(50, counterClockwise);
                scoreCounterClockwise += same ? 10 : 4;
            } else if (!neighbor.isAir()) {
                scoreClockwise -= same ? 3 : 1;
                scoreCounterClockwise -= same ? 3 : 1;
            }
        }

        // Stairs diagonally above or below facing the same way: a roof
        if (isStairFacing(view.state(pos.relative(counterClockwise).below()), clockwise)) scoreClockwise += 3;
        if (isStairFacing(view.state(pos.relative(clockwise).above()), clockwise)) scoreClockwise += 3;
        if (isStairFacing(view.state(pos.relative(clockwise).below()), counterClockwise)) scoreCounterClockwise += 3;
        if (isStairFacing(view.state(pos.relative(counterClockwise).above()), counterClockwise)) scoreCounterClockwise += 3;

        // The flat side resting on a full block
        BlockPos side = pos.relative(clockwise);
        if (!view.state(side).isAir() && view.sturdy(side, counterClockwise)) scoreClockwise += 2;
        side = pos.relative(counterClockwise);
        if (!view.state(side).isAir() && view.sturdy(side, clockwise)) scoreCounterClockwise += 2;

        // Less likely the opposite of where the player faces, which would feel arbitrary
        if (clockwise == placed.getOpposite()) scoreClockwise -= 1;
        if (counterClockwise == placed.getOpposite()) scoreCounterClockwise -= 1;

        if (scoreClockwise == scoreCounterClockwise && (placed == clockwise || placed == counterClockwise)) return Pair.of(scoreClockwise, placed);
        if (scoreCounterClockwise > scoreClockwise) return Pair.of(scoreCounterClockwise, counterClockwise);
        if (scoreClockwise > scoreCounterClockwise) return Pair.of(scoreClockwise, clockwise);
        return Pair.of(scoreClockwise, placed);
    }

    private static boolean isStairFacing(BlockState state, Direction facing) {
        return state.getBlock() instanceof StairBlock && state.getValue(BlockStateProperties.HORIZONTAL_FACING) == facing;
    }

    // Halves: placed on the side of another kind of block, where the height clicked is a guess, a slab, stair or
    // trapdoor takes the half most of the ones of its kind beside it have. On the side of its own kind (or a slab or
    // stair), the height clicked is exact, and so is a click on a top or bottom face.

    private static BlockState slab(BlockState state, BlockPos pos, Direction clickedFace, View view) {
        SlabType type = state.getValue(BlockStateProperties.SLAB_TYPE);
        if (type == SlabType.DOUBLE || !guessedHeight(pos, clickedFace, view)) return state;
        int top = 0, bottom = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState neighbor = view.state(pos.relative(direction));
            if (!(neighbor.getBlock() instanceof SlabBlock)) continue;
            SlabType neighborType = neighbor.getValue(BlockStateProperties.SLAB_TYPE);
            if (neighborType == SlabType.TOP) top++;
            else if (neighborType == SlabType.BOTTOM) bottom++;
        }
        if (top > bottom) return state.setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP);
        if (bottom > top) return state.setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM);
        return state;
    }

    private static BlockState half(BlockState state, BlockPos pos, Direction clickedFace, View view, Class<? extends Block> kind) {
        if (!guessedHeight(pos, clickedFace, view)) return state;
        int top = 0, bottom = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState neighbor = view.state(pos.relative(direction));
            if (!kind.isInstance(neighbor.getBlock())) continue;
            if (neighbor.getValue(BlockStateProperties.HALF) == Half.TOP) top++;
            else bottom++;
        }
        if (top > bottom) return state.setValue(BlockStateProperties.HALF, Half.TOP);
        if (bottom > top) return state.setValue(BlockStateProperties.HALF, Half.BOTTOM);
        return state;
    }

    /** Whether the half came from the height clicked on the side of a full block, a guess rather than an aim. */
    private static boolean guessedHeight(BlockPos pos, Direction clickedFace, View view) {
        if (clickedFace.getAxis() == Direction.Axis.Y) return false;
        Block against = view.state(pos.relative(clickedFace.getOpposite())).getBlock();
        return !(against instanceof SlabBlock || against instanceof StairBlock || against instanceof TrapDoorBlock);
    }

    // Fence gates: across the fence or wall they're placed in, facing the way the player looks across it.

    private static BlockState gate(BlockState state, BlockPos pos, @Nullable Vec3 viewer, View view) {
        boolean lineX = isFence(view.state(pos.east())) || isFence(view.state(pos.west()));
        boolean lineZ = isFence(view.state(pos.north())) || isFence(view.state(pos.south()));
        if (lineX == lineZ) return state;
        Direction.Axis across = lineX ? Direction.Axis.Z : Direction.Axis.X;
        Direction placed = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (placed.getAxis() == across) return state;
        Direction facing;
        if (across == Direction.Axis.Z) facing = viewer == null || viewer.z < pos.getZ() + 0.5 ? Direction.SOUTH : Direction.NORTH;
        else facing = viewer == null || viewer.x < pos.getX() + 0.5 ? Direction.EAST : Direction.WEST;
        return state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
    }

    private static boolean isFence(BlockState state) {
        Block block = state.getBlock();
        return block instanceof FenceBlock || block instanceof WallBlock || block instanceof FenceGateBlock;
    }

    // Blocks with a front: the facing of their own kind beside them in a row or a column, facing across the line (a
    // furnace beside a smoker counts for less than one beside a furnace); vanilla's on a tie.

    private static BlockState front(BlockState state, BlockPos pos, View view) {
        Property<Direction> property = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? BlockStateProperties.HORIZONTAL_FACING
                : state.hasProperty(BlockStateProperties.FACING) ? BlockStateProperties.FACING : null;
        if (property == null) return state;
        Map<Direction, Integer> votes = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            BlockState neighbor = view.state(pos.relative(direction));
            if (!isFront(neighbor.getBlock())) continue;
            Direction facing = neighbor.getOptionalValue(BlockStateProperties.HORIZONTAL_FACING)
                    .or(() -> neighbor.getOptionalValue(BlockStateProperties.FACING)).orElse(null);
            if (facing == null || facing.getAxis() == direction.getAxis() || !property.getPossibleValues().contains(facing)) continue;
            votes.merge(facing, neighbor.getBlock() == state.getBlock() ? 2 : 1, Integer::sum);
        }
        Direction best = null;
        int bestVotes = 0;
        boolean tie = false;
        for (Map.Entry<Direction, Integer> vote : votes.entrySet()) {
            if (vote.getValue() > bestVotes) {
                best = vote.getKey();
                bestVotes = vote.getValue();
                tie = false;
            } else if (vote.getValue() == bestVotes) {
                tie = true;
            }
        }
        if (best == null || tie || bestVotes < 2) return state;
        return state.setValue(property, best);
    }

    // Signs, banners and heads: the rotation of the same block beside them, when it's within two steps (45 degrees) of
    // vanilla's; the nearest one wins, vanilla's on a tie.

    private static BlockState rotation(BlockState state, BlockPos pos, View view) {
        int placed = state.getValue(BlockStateProperties.ROTATION_16);
        int best = placed, bestDistance = Integer.MAX_VALUE;
        boolean tie = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState neighbor = view.state(pos.relative(direction));
            if (!neighbor.is(state.getBlock())) continue;
            int rotation = neighbor.getValue(BlockStateProperties.ROTATION_16);
            int distance = Math.min(Math.abs(rotation - placed), 16 - Math.abs(rotation - placed));
            if (distance > 2) continue;
            if (distance < bestDistance) {
                best = rotation;
                bestDistance = distance;
                tie = false;
            } else if (distance == bestDistance && rotation != best) {
                tie = true;
            }
        }
        return tie ? state : state.setValue(BlockStateProperties.ROTATION_16, best);
    }
}
