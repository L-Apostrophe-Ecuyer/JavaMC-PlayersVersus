package frootloops.versus.mod.environment.blocks;

import frootloops.versus.VersusMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class LadderBlockItem extends BlockItem {
    public LadderBlockItem(LadderBlock block, Properties settings) {
        super(block, settings);
    }

    private BlockPos.MutableBlockPos searchForLadderPlacement(Level world, BlockPos startPos, Direction ladderFacing, boolean searchUpwardsFirst) {
        for(int iter = 0; iter < 2; iter++) {

            BlockPos.MutableBlockPos currentPos = startPos.mutable();
            for(int y = 1; y < 6; y++) {
                currentPos = currentPos.move(searchUpwardsFirst ? Direction.UP : Direction.DOWN);
                if(!world.isInWorldBounds(currentPos)) break;

                BlockState state = world.getBlockState(currentPos);
                if(state.is(this.getBlock())) {
                    if(state.getValue(LadderBlock.FACING) == ladderFacing) continue;
                    else break;
                }
                else {
                    if(state.canBeReplaced()) {
                        if(!searchUpwardsFirst) return currentPos; // If we were looking downwards, that guarantees the ladder will have support
                        BlockPos supportingBlockPos = currentPos.relative(ladderFacing.getOpposite()); // Otherwise, only accept pos if solid block behind it
                        BlockState supportingBlock = world.getBlockState(supportingBlockPos);
                        if(supportingBlock.isFaceSturdy(world, currentPos.relative(ladderFacing), ladderFacing.getOpposite())) return currentPos;
                        else break;
                    }
                    else break;
                }
            }
            searchUpwardsFirst = !searchUpwardsFirst;
        }
        return null;
    }

    @Nullable
    @Override
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        Level world = context.getLevel();
        BlockPos blockPos = context.getClickedPos().relative(context.getClickedFace().getOpposite());
        BlockState blockState = world.getBlockState(blockPos);
        if (!blockState.is(this.getBlock())) return context;

        BlockPos.MutableBlockPos newPos = searchForLadderPlacement(world, blockPos, blockState.getValue(LadderBlock.FACING), context.getNearestLookingVerticalDirection() == Direction.UP);
        if(newPos == null) return context;

        Direction directionNewPos = newPos.getY() <= blockPos.getY() ? Direction.DOWN : Direction.UP;
        return BlockPlaceContext.at(context, newPos, directionNewPos);
    }
}
