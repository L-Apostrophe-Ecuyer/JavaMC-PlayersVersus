
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
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

public class SugarCaneTopBlock extends Block implements Fertilizable {

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
        this.setDefaultState(this.stateManager.getDefaultState().with(CAN_GROW, true).with(AGE, Integer.valueOf(0)));
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
        BlockState groundState = world.getBlockState(blockPosDown);
        if (groundState.isOf(Blocks.SUGAR_CANE)) return true;
        if (world.getLightLevel(LightType.SKY, pos) < 13) return false;
        if (groundState.isOf(Blocks.MUD) || groundState.isOf(CustomBlocks.GRAY_MUD) || groundState.isOf(CustomBlocks.BROWN_MUD)) {
            return true;
        } else {
            if (groundState.isIn(BlockTags.DIRT) || groundState.isIn(BlockTags.SAND) || groundState.isOf(Blocks.CLAY) || groundState.isOf(CustomBlocks.GRAY_CLAY)) {
                FluidState fluidState;
                BlockState neighborState;
                for (Direction direction : Direction.Type.HORIZONTAL) {
                    BlockPos offsetedPos = blockPosDown.offset(direction);
                    neighborState = world.getBlockState(offsetedPos);
                    fluidState = world.getFluidState(offsetedPos);
                    if (fluidState.isIn(FluidTags.WATER) || neighborState.isOf(Blocks.FROSTED_ICE)) return true;
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
        if (world.getLightLevel(LightType.SKY, pos) < 13) {
            world.setBlockState(pos.up(), CustomBlocks.SUGAR_CANE_TOP.getStuntedState());
            return;
        }
        if(state.get(CAN_GROW) && this.canGrow(world, random, pos, state)) this.grow(world, random, pos, state);
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

    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state) {
        return state.get(CAN_GROW) && world.getLightLevel(LightType.SKY, pos) > 13;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        int chanceToGrow = world.getBlockState(pos.down()).isOf(Blocks.SUGAR_CANE) ? 8 : 16;
        if(!state.get(CAN_GROW)) chanceToGrow /= 4;
        if (random.nextInt(chanceToGrow) == 1) return false;
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        BlockState blockStateBelow = world.getBlockState(pos.down());
        int numSugarCaneBelow = 0;
        while (blockStateBelow.isOf(Blocks.SUGAR_CANE) && numSugarCaneBelow < 3) numSugarCaneBelow++;
        if (world.isAir(pos.up())) {
            int age = state.get(AGE);
            if(age < 15) {
                world.setBlockState(pos, state.with(AGE, Integer.valueOf(age + 1)).with(CAN_GROW, true));
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
                else world.setBlockState(pos.up(), CustomBlocks.SUGAR_CANE_TOP.getStuntedState());
            }
        }
    }
}
