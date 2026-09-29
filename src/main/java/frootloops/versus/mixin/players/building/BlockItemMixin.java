package frootloops.versus.mixin.players.building;

import frootloops.versus.VersusSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.StairsShape;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin extends Item {
    public BlockItemMixin(Properties settings, Block block) {
        super(settings);
        this.block = block;
    }

    @Shadow private final Block block;

    @Shadow protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        return true;
    }



    @Inject(method = "getPlacementState", at = @At(value = "RETURN"), cancellable = true)
    public void getPlacementState(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        if(!VersusSettings.QOL.DO_SMARTER_BLOCK_PLACING) return;

        BlockState blockState = this.block.getStateForPlacement(context);
        if(blockState == null || !canPlace(context, blockState)) return;
        if(context.getPlayer().isShiftKeyDown()) return;

        // Pillar Blocks: Try to match the neighboring blocks' axis
        Optional<Direction.Axis> placementAxisOptional = blockState.getOptionalValue(BlockStateProperties.AXIS);
        if(placementAxisOptional.isPresent()) {
            Direction.Axis placementAxis = placementAxisOptional.get();
            BlockPos placementPos = context.getClickedPos();
            Level world = context.getLevel();

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
                if(placementAxis != Direction.Axis.X) cir.setReturnValue(blockState.setValue(BlockStateProperties.AXIS, Direction.Axis.X));
            }
            else if(maxScore == scoreAxisY) {
                if(placementAxis != Direction.Axis.Y) cir.setReturnValue(blockState.setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
            }
            else if(maxScore == scoreAxisZ) {
                if(placementAxis != Direction.Axis.Z) cir.setReturnValue(blockState.setValue(BlockStateProperties.AXIS, Direction.Axis.Z));
            }
            return;
        }


        // Other Directional Blocks: Try to match the neighboring blocks' axis
        Optional<Direction> placementDirectionOptional = blockState.getOptionalValue(BlockStateProperties.HORIZONTAL_FACING);
        if(placementDirectionOptional.isPresent() && block instanceof StairBlock) {
            Direction placementDirection = placementDirectionOptional.get();
            BlockPos placementPos = context.getClickedPos();
            Level world = context.getLevel();
            boolean isPlacingStairs = blockState.getBlock() instanceof StairBlock;

            Tuple<Integer, Direction> scoreAxisX = this.getBestFacingScore(Direction.Axis.X, placementDirection, placementPos, world, isPlacingStairs);
            if(scoreAxisX.getA() >= 50) return;

            Tuple<Integer, Direction> scoreAxisZ = this.getBestFacingScore(Direction.Axis.Z, placementDirection, placementPos, world, isPlacingStairs);
            if(scoreAxisZ.getA() >= 50) return;

            if(scoreAxisX.getA() <= 3 && scoreAxisZ.getA() <= 3) return;
            else if((scoreAxisX.getB() == placementDirection || scoreAxisZ.getB() == placementDirection) && Math.abs(scoreAxisX.getA() - scoreAxisZ.getA()) < 4) return;
            else if(scoreAxisX.getA() > scoreAxisZ.getA()){
                if(placementDirection != scoreAxisX.getB()) cir.setReturnValue(blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, scoreAxisX.getB()).setValue(StairBlock.SHAPE, StairsShape.STRAIGHT));
            }
            else {
                if(placementDirection != scoreAxisZ.getB()) cir.setReturnValue(blockState.setValue(BlockStateProperties.HORIZONTAL_FACING, scoreAxisZ.getB()).setValue(StairBlock.SHAPE, StairsShape.STRAIGHT));
            }
            return;
        }
    }

    private int getAxisScore(Direction.Axis axisToEvaluate, Direction.Axis placementAxis, BlockPos placementPos, Level world) {
        int score = 0;
        boolean isSameBlock;
        BlockState neighborState;
        Optional<Direction.Axis> neighborAxis;

        for(int i = -1; i < 2; i += 2) {
            neighborState = world.getBlockState(placementPos.relative(axisToEvaluate, i));
            neighborAxis = neighborState.getOptionalValue(BlockStateProperties.AXIS);
            isSameBlock = neighborState.is(this.block);
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

    private Tuple<Integer, Direction> getBestFacingScore(Direction.Axis axisToEvaluate, Direction placementDirection, BlockPos placementPos, Level world, boolean isPlacingStairs) {
        Direction clockwise = (axisToEvaluate == Direction.Axis.X) ? Direction.NORTH : Direction.EAST;
        Direction counterClockwise = (axisToEvaluate == Direction.Axis.X) ? Direction.SOUTH : Direction.WEST;
        int scoreClockwise = 0, scoreCounterClockwise = 0;
        boolean isSameBlock;
        BlockPos pos;
        BlockState neighborState;
        Optional<Direction> neighborFacing;

        // Check neighboring directional blocks and align with them:
        for(int i = -1; i < 2; i += 2) {
            neighborState = world.getBlockState(placementPos.relative(axisToEvaluate, i));
            neighborFacing = neighborState.getOptionalValue(BlockStateProperties.HORIZONTAL_FACING);
            isSameBlock = neighborState.is(this.block);

            if(!neighborFacing.isPresent())  continue;
            else if (neighborFacing.get() == clockwise) {
                if (placementDirection == clockwise && isSameBlock) return new Tuple<>(50, clockwise);
                else scoreClockwise += (isSameBlock) ? 10 : 4;
            }
            else if (neighborFacing.get() == counterClockwise) {
                if (placementDirection == counterClockwise && isSameBlock) return new Tuple<>(50, counterClockwise);
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
            pos = placementPos.relative(counterClockwise).below();
            neighborState = world.getBlockState(pos);
            if(neighborState.getBlock() instanceof StairBlock && neighborState.getValue(BlockStateProperties.HORIZONTAL_FACING) == clockwise) scoreClockwise += 3;

            pos = placementPos.relative(clockwise).above();
            neighborState = world.getBlockState(pos);
            if(neighborState.getBlock() instanceof StairBlock && neighborState.getValue(BlockStateProperties.HORIZONTAL_FACING) == clockwise) scoreClockwise += 3;

            pos = placementPos.relative(clockwise).below();
            neighborState = world.getBlockState(pos);
            if(neighborState.getBlock() instanceof StairBlock && neighborState.getValue(BlockStateProperties.HORIZONTAL_FACING) == counterClockwise) scoreCounterClockwise += 3;

            pos = placementPos.relative(counterClockwise).above();
            neighborState = world.getBlockState(pos);
            if(neighborState.getBlock() instanceof StairBlock && neighborState.getValue(BlockStateProperties.HORIZONTAL_FACING) == counterClockwise) scoreCounterClockwise += 3;
        }

        // If placing stairs, check opposing blocks: we'd usually want the flat side of the stairs to be resting on a full block:
        if(isPlacingStairs) {
            pos = placementPos.relative(clockwise);
            neighborState = world.getBlockState(pos);
            if(!neighborState.isAir() && neighborState.isFaceSturdy(world, pos, counterClockwise)) scoreClockwise += 2;

            pos = placementPos.relative(counterClockwise);
            neighborState = world.getBlockState(pos);
            if(!neighborState.isAir() && neighborState.isFaceSturdy(world, pos, clockwise)) scoreCounterClockwise += 2;
        }

        // Try to lower the chances of blocks getting placed the complete opposite of where the player is facing, since that might feel arbitrary
        if(clockwise == placementDirection.getOpposite()) scoreClockwise -= 1;
        if(counterClockwise == placementDirection.getOpposite()) scoreCounterClockwise -= 1;

        // Check which direction has the best score and return it:
        if(scoreClockwise == scoreCounterClockwise &&  (placementDirection == clockwise || placementDirection == counterClockwise)) return new Tuple<>(scoreClockwise, placementDirection);
        else if(scoreCounterClockwise > scoreClockwise) return new Tuple<>(scoreCounterClockwise, counterClockwise);
        else if(scoreClockwise > scoreCounterClockwise) return new Tuple<>(scoreClockwise, clockwise);
        else return new Tuple<>(scoreClockwise, placementDirection);
    }
}
