
package frootloops.versus.mod.environment.blocks;

import com.mojang.serialization.MapCodec;
import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.block.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

public class SugarCaneTopBlock extends Block {

    public static final MapCodec<SugarCaneTopBlock> CODEC = createCodec(SugarCaneTopBlock::new);
    protected static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 10.0, 14.0);
    public static final IntProperty AGE = Properties.AGE_15;
    public static final BooleanProperty CAN_GROW = BooleanProperty.of("can_grow");

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(CAN_GROW).add(AGE);
    }

    @Override
    public MapCodec<SugarCaneTopBlock> getCodec() {
        return CODEC;
    }

    public SugarCaneTopBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(CAN_GROW, false).with(AGE, Integer.valueOf(0)));
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return Blocks.SUGAR_CANE.getPickStack(world, pos, state);
    }

    public BlockState getStuntedState() {
        return this.getDefaultState().with(CAN_GROW, false);
    }


    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!state.canPlaceAt(world, pos)) {
            world.breakBlock(pos, true);
        }
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos blockPosDown = pos.down();
        BlockState blockState = world.getBlockState(blockPosDown);
        if(blockState.isAir() || !blockState.isOpaqueFullCube()) return false;
        if (blockState.isOf(Blocks.SUGAR_CANE) || blockState.isOf(Blocks.MUD) || blockState.isOf(Blocks.CLAY) || blockState.isOf(CustomBlocks.BROWN_MUD)) {
            return true;
        } else {
            if (blockState.isIn(BlockTags.DIRT) || blockState.isIn(BlockTags.SAND)) {
                FluidState fluidState;
                BlockState blockStateDown;
                for (Direction direction : Direction.Type.HORIZONTAL) {
                    BlockPos offsetedPos = blockPosDown.offset(direction);
                    blockStateDown = world.getBlockState(offsetedPos);
                    fluidState = world.getFluidState(offsetedPos);
                    if (fluidState.isIn(FluidTags.WATER) || blockStateDown.isOf(Blocks.FROSTED_ICE)) return true;
                    if (world.getFluidState(offsetedPos.offset(direction.rotateClockwise(Direction.Axis.X))).isIn(FluidTags.WATER) ) return true;
                    if (world.getFluidState(offsetedPos.offset(direction.rotateClockwise(Direction.Axis.Z))).isIn(FluidTags.WATER) ) return true;
                    if (world.getFluidState(blockPosDown.offset(direction, 2)).isIn(FluidTags.WATER) ) return true;
                }
            }
            return false;
        }
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {

        BlockState blockStateBelow = world.getBlockState(pos.down());
        int numSugarCaneBelow = blockStateBelow.isOf(Blocks.SUGAR_CANE) ? 1 : 0;
        int chanceToGrow = numSugarCaneBelow == 0 ? 128 : numSugarCaneBelow > 0 ? 64 : blockStateBelow.isIn(BlockTags.SAND) ? 32 : 16;
        if(!state.get(CAN_GROW)) {
            if(numSugarCaneBelow > 0) return;
            else chanceToGrow /= 4;
        }

        if (random.nextInt(chanceToGrow) == 1) return;
        if (world.isAir(pos.up())) {
            int age = state.get(AGE);
            if(age < 15) {
                world.setBlockState(pos, state.with(AGE, Integer.valueOf(age + 1)), Block.NO_REDRAW);
                return;
            }
            if(numSugarCaneBelow == 1) {
                while (numSugarCaneBelow < 4 && world.getBlockState(pos.down(numSugarCaneBelow)).isOf(Blocks.SUGAR_CANE)) {
                    numSugarCaneBelow++;
                }
            }
            if (numSugarCaneBelow < 3) {
                world.setBlockState(pos, Blocks.SUGAR_CANE.getDefaultState());
                if(random.nextInt(6) > 2 * numSugarCaneBelow) world.setBlockState(pos.up(), CustomBlocks.SUGAR_CANE_TOP.getDefaultState());
            }
        }
    }

    @Override @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos blockPosDown = ctx.getBlockPos().down();
        BlockState blockStateDown = ctx.getWorld().getBlockState(blockPosDown);
        if(blockStateDown.isOf(CustomBlocks.SUGAR_CANE_TOP)) {
            ctx.getWorld().setBlockState(blockPosDown, Blocks.SUGAR_CANE.getDefaultState());
            return this.getDefaultState();
        }
        else if(blockStateDown.isOf(Blocks.SUGAR_CANE) && ctx.getWorld().getRandom().nextInt(4) == 0) return this.getStuntedState();
        return this.getDefaultState();
    }

    @Override
    protected BlockState getStateForNeighborUpdate(
            BlockState state,
            WorldView world,
            ScheduledTickView tickView,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            Random random
    ) {
        if (!state.canPlaceAt(world, pos)) {
            tickView.scheduleBlockTick(pos, this, 1);
        }

        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }
}
