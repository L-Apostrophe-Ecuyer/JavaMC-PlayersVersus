package frootloops.versus.mixin.environment.blocks;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SugarCaneBlock.class)
public abstract class SugarCaneMixin extends Block {

    public SugarCaneMixin(Properties settings) {
        super(settings);
    }

    @Override @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if(ctx.getPlayer() == null) {
            BlockPos blockPosDown = ctx.getClickedPos().below();
            BlockState blockStateDown = ctx.getLevel().getBlockState(blockPosDown);
            if (blockStateDown.is(Blocks.SUGAR_CANE) && blockStateDown.getValue(BlockStateProperties.AGE_15) != 15) {
                ctx.getLevel().setBlockAndUpdate(blockPosDown, Blocks.SUGAR_CANE.defaultBlockState().setValue(BlockStateProperties.AGE_15, 15));
            }

            BlockPos blockPosUp = ctx.getClickedPos().above();
            BlockState blockStateUp = ctx.getLevel().getBlockState(blockPosUp);
            if (blockStateUp.is(Blocks.SUGAR_CANE)) {
                return Blocks.SUGAR_CANE.defaultBlockState().setValue(BlockStateProperties.AGE_15, 15);
            }
        }
        return Blocks.SUGAR_CANE.defaultBlockState();
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        int age = state.getValue(BlockStateProperties.AGE_15);
        if (world.isEmptyBlock(pos.above())) {
            int numSugarCaneBelow = 0;
            while (numSugarCaneBelow < 3 && world.getBlockState(pos.below(numSugarCaneBelow)).is(this)) {
                numSugarCaneBelow++;
            }
            if(numSugarCaneBelow == 2 && age == 14) return;
            if (age == 15 && (world.getBrightness(LightLayer.SKY, pos) > (1 + numSugarCaneBelow) * (5 + random.nextInt(4)))) {
                world.setBlockAndUpdate(pos.above(), state.setValue(BlockStateProperties.AGE_15, 0));
            }
            else if(age < 15){
                world.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.AGE_15, age + 1));
            }
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        BlockPos blockPosDown = pos.below();
        BlockState blockState = world.getBlockState(blockPosDown);
        if(blockState.is(Blocks.SUGAR_CANE)) {
            return blockState.getValue(BlockStateProperties.AGE_15) == 15;
        }
        if (blockState.is(Blocks.MUD) || blockState.is(Blocks.CLAY) || blockState.is(CustomBlocks.BROWN_MUD)) {
            return true;
        } else {
            if (blockState.is(BlockTags.DIRT) || blockState.is(BlockTags.SAND)) {
                FluidState fluidState;
                BlockState blockStateDown;
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    BlockPos offsetedPos = blockPosDown.relative(direction);
                    blockStateDown = world.getBlockState(offsetedPos);
                    fluidState = world.getFluidState(offsetedPos);
                    if (fluidState.is(FluidTags.WATER) || blockStateDown.is(Blocks.FROSTED_ICE)) return true;
                    if (world.getFluidState(offsetedPos.relative(direction.getClockWise(Direction.Axis.X))).is(FluidTags.WATER) ) return true;
                    if (world.getFluidState(offsetedPos.relative(direction.getClockWise(Direction.Axis.Z))).is(FluidTags.WATER) ) return true;
                    if (world.getFluidState(blockPosDown.relative(direction, 2)).is(FluidTags.WATER) ) return true;
                }
            }
            return false;
        }
    }
}
