package frootloops.versus.mod.environment.blocks.trees;

import frootloops.versus.VersusSettings;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A log as a tree grows it (TreeBlocks), one per wood: the wood's log with six faces, each showing bark (true) or the
 * cut wood under it (false), as a huge mushroom block's show skin or pores. Set once, as the tree grows or as one is
 * placed in creative, and never after: cut against another tree block, a log or a sturdy face (the ground, rock, roots,
 * a bee nest), bark against anything else. Breaking one gives its wood's vanilla log (its loot table), so whatever a
 * player builds is logs, and only trees are made of these.
 */
public class TreeBlock extends RotatedPillarBlock {

    public static final Map<Direction, BooleanProperty> FACES = PipeBlock.PROPERTY_BY_DIRECTION;

    private final Block log;

    public TreeBlock(Block log, Properties properties) {
        super(properties);
        this.log = log;
        BlockState state = this.defaultBlockState();
        for (BooleanProperty face : FACES.values()) state = state.setValue(face, true);
        this.registerDefaultState(state);
    }

    /** The vanilla log this tree block stands for. */
    public Block log() {
        return this.log;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACES.values().toArray(BooleanProperty[]::new));
    }

    /** Cutting one takes longer the more of the tree would fall (Felling), worked out alike on the client and the server. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float progress = super.getDestroyProgress(state, player, level, pos);
        if (!VersusSettings.Trees.DO_FELLING || !(level instanceof LevelReader reader)) return progress;
        return (float) (progress / Felling.cutTimeFactor(reader, pos));
    }

    /** Whoever breaks one made the cut, for which way the tree falls (TreeFelling). */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server) TreeFelling.cutBy(server, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Gone, whatever took it: what it held up may come down (TreeFelling). */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        TreeFelling.removed(level, pos);
    }

    /** Placed in creative: turned as a log is, by the face clicked, its faces set by what is next to it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : withFaces(context.getLevel(), context.getClickedPos(), state);
    }

    /** This tree block with each face set by what is next to it at this spot now. */
    public static BlockState withFaces(LevelReader level, BlockPos pos, BlockState state) {
        for (Direction side : Direction.values()) state = state.setValue(FACES.get(side), !isCutAgainst(level, pos, side));
        return state;
    }

    /**
     * Whether a tree block's face on this side is cut: against a tree block, a log or a sturdy face, never leaves (a
     * crown's leaves sit on bark), and bark where the neighbour isn't loaded.
     */
    private static boolean isCutAgainst(LevelReader level, BlockPos pos, Direction side) {
        BlockPos next = pos.relative(side);
        if (!level.hasChunkAt(next)) return false;
        BlockState neighbour = level.getBlockState(next);
        if (neighbour.getBlock() instanceof TreeBlock || neighbour.is(BlockTags.LOGS)) return true;
        return !neighbour.is(BlockTags.LEAVES) && neighbour.isFaceSturdy(level, next, side.getOpposite());
    }

    /** Turned in a structure, its faces turn with it, as a huge mushroom block's. */
    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = super.rotate(state, rotation);
        for (Direction side : Direction.Plane.HORIZONTAL) rotated = rotated.setValue(FACES.get(rotation.rotate(side)), state.getValue(FACES.get(side)));
        return rotated;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = state;
        for (Direction side : Direction.Plane.HORIZONTAL) mirrored = mirrored.setValue(FACES.get(mirror.mirror(side)), state.getValue(FACES.get(side)));
        return mirrored;
    }
}
