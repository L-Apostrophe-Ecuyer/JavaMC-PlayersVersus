package frootloops.versus.mod.environment.blocks;

import frootloops.versus.VersusMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.LadderBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class LadderBlockItem extends BlockItem {
    public LadderBlockItem(LadderBlock block, Settings settings) {
        super(block, settings);
    }

    private BlockPos.Mutable searchForLadderPlacement(World world, BlockPos startPos, Direction ladderFacing, boolean searchUpwardsFirst) {
        for(int iter = 0; iter < 2; iter++) {

            BlockPos.Mutable currentPos = startPos.mutableCopy();
            for(int y = 1; y < 6; y++) {
                currentPos = currentPos.move(searchUpwardsFirst ? Direction.UP : Direction.DOWN);
                if(!world.isInBuildLimit(currentPos)) break;

                BlockState state = world.getBlockState(currentPos);
                if(state.isOf(this.getBlock())) {
                    if(state.get(LadderBlock.FACING) == ladderFacing) continue;
                    else break;
                }
                else {
                    if(state.isReplaceable()) {
                        if(!searchUpwardsFirst) return currentPos; // If we were looking downwards, that guarantees the ladder will have support
                        BlockPos supportingBlockPos = currentPos.offset(ladderFacing.getOpposite()); // Otherwise, only accept pos if solid block behind it
                        BlockState supportingBlock = world.getBlockState(supportingBlockPos);
                        if(supportingBlock.isSideSolidFullSquare(world, currentPos.offset(ladderFacing), ladderFacing.getOpposite())) return currentPos;
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
    public ItemPlacementContext getPlacementContext(ItemPlacementContext context) {
        World world = context.getWorld();
        BlockPos blockPos = context.getBlockPos().offset(context.getSide().getOpposite());
        BlockState blockState = world.getBlockState(blockPos);
        if (!blockState.isOf(this.getBlock())) return context;

        BlockPos.Mutable newPos = searchForLadderPlacement(world, blockPos, blockState.get(LadderBlock.FACING), context.getVerticalPlayerLookDirection() == Direction.UP);
        if(newPos == null) return context;

        Direction directionNewPos = newPos.getY() <= blockPos.getY() ? Direction.DOWN : Direction.UP;
        return ItemPlacementContext.offset(context, newPos, directionNewPos);
    }
}
