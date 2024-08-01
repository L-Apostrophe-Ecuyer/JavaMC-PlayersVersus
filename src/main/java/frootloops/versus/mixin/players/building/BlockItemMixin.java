package frootloops.versus.mixin.players.building;

import frootloops.versus.VersusMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.StairShape;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin extends Item {
    public BlockItemMixin(Settings settings, Block block) {
        super(settings);
        this.block = block;
    }

    @Shadow private final Block block;

    @Shadow protected boolean canPlace(ItemPlacementContext context, BlockState state) {
        return true;
    }



    @Inject(method = "getPlacementState", at = @At(value = "RETURN"), cancellable = true)
    public void getPlacementState(ItemPlacementContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState blockState = this.block.getPlacementState(context);
        if(blockState == null || !canPlace(context, blockState)) return;
        if(context.getPlayer().isSneaking()) return;

        // Pillar Blocks: Try to match the neighboring blocks' axis
        Optional<Direction.Axis> placementAxisOptional = blockState.getOrEmpty(Properties.AXIS);
        if(placementAxisOptional.isPresent()) {
            Direction.Axis placementAxis = placementAxisOptional.get();
            BlockPos placementPos = context.getBlockPos();
            World world = context.getWorld();

            int scoreAxisX = this.getAxisScore(Direction.Axis.X, placementAxis, placementPos, world);
            if(scoreAxisX >= 50) return;

            int scoreAxisY = this.getAxisScore(Direction.Axis.Y, placementAxis, placementPos, world);
            if(scoreAxisY >= 50) return;

            int scoreAxisZ = this.getAxisScore(Direction.Axis.Z, placementAxis, placementPos, world);
            if(scoreAxisZ >= 50) return;

            int maxScore = Math.max(Math.max(scoreAxisX, scoreAxisY), scoreAxisZ);
            int placementAxisScore = (placementAxis == Direction.Axis.X) ? scoreAxisX : (placementAxis == Direction.Axis.Y) ? scoreAxisY : scoreAxisZ;
            if(maxScore <= placementAxisScore + 5) return; // Scores are too similar to warrant changing the block's orientation
            else if(maxScore == scoreAxisX) {
                if(placementAxis != Direction.Axis.X) cir.setReturnValue(blockState.with(Properties.AXIS, Direction.Axis.X));
            }
            else if(maxScore == scoreAxisY) {
                if(placementAxis != Direction.Axis.Y) cir.setReturnValue(blockState.with(Properties.AXIS, Direction.Axis.Y));
            }
            else if(maxScore == scoreAxisZ) {
                if(placementAxis != Direction.Axis.Z) cir.setReturnValue(blockState.with(Properties.AXIS, Direction.Axis.Z));
            }
            return;
        }


        // Other Directional Blocks: Try to match the neighboring blocks' axis
        Optional<Direction> placementDirectionOptional = blockState.getOrEmpty(Properties.HORIZONTAL_FACING);
        if(placementDirectionOptional.isPresent()) {
            Direction placementDirection = placementDirectionOptional.get();
            BlockPos placementPos = context.getBlockPos();
            World world = context.getWorld();
            boolean isPlacingStairs = blockState.getBlock() instanceof StairsBlock;

            Pair<Integer, Direction> scoreAxisX = this.getBestFacingScore(Direction.Axis.X, placementDirection, placementPos, world, isPlacingStairs);
            if(scoreAxisX.getLeft() >= 50) return;

            Pair<Integer, Direction> scoreAxisZ = this.getBestFacingScore(Direction.Axis.Z, placementDirection, placementPos, world, isPlacingStairs);
            if(scoreAxisZ.getLeft() >= 50) return;

            if(scoreAxisX.getLeft() <= 3 && scoreAxisZ.getLeft() <= 3) return;
            else if((scoreAxisX.getRight() == placementDirection || scoreAxisZ.getRight() == placementDirection) && Math.abs(scoreAxisX.getLeft() - scoreAxisZ.getLeft()) < 4) return;
            else if(scoreAxisX.getLeft() > scoreAxisZ.getLeft()){
                if(placementDirection != scoreAxisX.getRight()) cir.setReturnValue(blockState.with(Properties.HORIZONTAL_FACING, scoreAxisX.getRight()).with(StairsBlock.SHAPE, StairShape.STRAIGHT));
            }
            else {
                if(placementDirection != scoreAxisZ.getRight()) cir.setReturnValue(blockState.with(Properties.HORIZONTAL_FACING, scoreAxisZ.getRight()).with(StairsBlock.SHAPE, StairShape.STRAIGHT));
            }
            return;
        }
    }

    private int getAxisScore(Direction.Axis axisToEvaluate, Direction.Axis placementAxis, BlockPos placementPos, World world) {
        int score = 0;
        boolean isSameBlock;
        BlockState neighborState;
        Optional<Direction.Axis> neighborAxis;

        for(int i = -1; i < 2; i += 2) {
            neighborState = world.getBlockState(placementPos.offset(axisToEvaluate, i));
            neighborAxis = neighborState.getOrEmpty(Properties.AXIS);
            isSameBlock = neighborState.isOf(this.block);
            if(!neighborAxis.isPresent()) continue;
            else if (neighborAxis.get() == axisToEvaluate) {
                if (placementAxis == axisToEvaluate && isSameBlock) return 50;
                else score += (isSameBlock) ? 10 : 4;
            }
            else if(!neighborState.isAir()) {
                if(isSameBlock) score -= 2;
                else score -= 1;
            }
        }
        return score;
    }

    private Pair<Integer, Direction> getBestFacingScore(Direction.Axis axisToEvaluate, Direction placementDirection, BlockPos placementPos, World world, boolean isPlacingStairs) {
        Direction clockwise = (axisToEvaluate == Direction.Axis.X) ? Direction.NORTH : Direction.EAST;
        Direction counterClockwise = (axisToEvaluate == Direction.Axis.X) ? Direction.SOUTH : Direction.WEST;
        int scoreClockwise = 0, scoreCounterClockwise = 0;
        boolean isSameBlock;
        BlockPos pos;
        BlockState neighborState;
        Optional<Direction> neighborFacing;

        // Check neighboring directional blocks and align with them:
        for(int i = -1; i < 2; i += 2) {
            neighborState = world.getBlockState(placementPos.offset(axisToEvaluate, i));
            neighborFacing = neighborState.getOrEmpty(Properties.HORIZONTAL_FACING);
            isSameBlock = neighborState.isOf(this.block);

            if(!neighborFacing.isPresent())  continue;
            else if (neighborFacing.get() == clockwise) {
                if (placementDirection == clockwise && isSameBlock) return new Pair<>(50, clockwise);
                else scoreClockwise += (isSameBlock) ? 10 : 4;
            }
            else if (neighborFacing.get() == counterClockwise) {
                if (placementDirection == counterClockwise && isSameBlock) return new Pair<>(50, counterClockwise);
                else scoreCounterClockwise += (isSameBlock) ? 10 : 4;
            }
            else if(!neighborState.isAir()) {
                if(isSameBlock) {
                    scoreClockwise -= 3;
                    scoreCounterClockwise -= 3;
                }
                else {
                    scoreClockwise -= 1;
                    scoreCounterClockwise -= 1;
                }
            }
        }

        // If placing stairs, check diagonal blocks: this can usually happen when roofing, where you would want your stairs to line up diagonally
        if(isPlacingStairs) {
            pos = placementPos.offset(counterClockwise).down();
            neighborState = world.getBlockState(pos);
            if(neighborState.getBlock() instanceof StairsBlock && neighborState.get(Properties.HORIZONTAL_FACING) == clockwise) scoreClockwise += 3;

            pos = placementPos.offset(clockwise).up();
            neighborState = world.getBlockState(pos);
            if(neighborState.getBlock() instanceof StairsBlock && neighborState.get(Properties.HORIZONTAL_FACING) == clockwise) scoreClockwise += 3;

            pos = placementPos.offset(clockwise).down();
            neighborState = world.getBlockState(pos);
            if(neighborState.getBlock() instanceof StairsBlock && neighborState.get(Properties.HORIZONTAL_FACING) == counterClockwise) scoreCounterClockwise += 3;

            pos = placementPos.offset(counterClockwise).up();
            neighborState = world.getBlockState(pos);
            if(neighborState.getBlock() instanceof StairsBlock && neighborState.get(Properties.HORIZONTAL_FACING) == counterClockwise) scoreCounterClockwise += 3;
        }

        // If placing stairs, check opposing blocks: we'd usually want the flat side of the stairs to be resting on a full block:
        if(isPlacingStairs) {
            pos = placementPos.offset(clockwise);
            neighborState = world.getBlockState(pos);
            if(!neighborState.isAir() && neighborState.isSideSolidFullSquare(world, pos, counterClockwise)) scoreClockwise += 2;

            pos = placementPos.offset(counterClockwise);
            neighborState = world.getBlockState(pos);
            if(!neighborState.isAir() && neighborState.isSideSolidFullSquare(world, pos, clockwise)) scoreCounterClockwise += 2;
        }

        // Try to lower the chances of blocks getting placed the complete opposite of where the player is facing, since that might feel arbitrary
        if(clockwise == placementDirection.getOpposite()) scoreClockwise -= 1;
        if(counterClockwise == placementDirection.getOpposite()) scoreCounterClockwise -= 1;

        // Check which direction has the best score and return it:
        if(scoreClockwise == scoreCounterClockwise &&  (placementDirection == clockwise || placementDirection == counterClockwise)) return new Pair<>(scoreClockwise, placementDirection);
        else if(scoreCounterClockwise > scoreClockwise) return new Pair<>(scoreCounterClockwise, counterClockwise);
        else if(scoreClockwise > scoreCounterClockwise) return new Pair<>(scoreClockwise, clockwise);
        else return new Pair<>(scoreClockwise, placementDirection);
    }
}
