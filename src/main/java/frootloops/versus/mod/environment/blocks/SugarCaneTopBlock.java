
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
    public static final IntProperty GROUND_TYPE = IntProperty.of("ground_type", 0, 6);
    private static final int TYPE_REGULAR = 0;
    private static final int TYPE_SAND = 1;
    private static final int TYPE_RED_SAND = 2;
    private static final int TYPE_BROWN_MUD = 3;
    private static final int TYPE_GREY_MUD = 4;
    private static final int TYPE_CLAY = 5;
    private static final int TYPE_MUD_CAVE = 6;

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(GROUND_TYPE).add(AGE);
    }

    @Override
    public MapCodec<SugarCaneTopBlock> getCodec() {
        return CODEC;
    }

    public SugarCaneTopBlock(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(GROUND_TYPE, Integer.valueOf(TYPE_REGULAR)).with(AGE, Integer.valueOf(0)));
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        return Blocks.SUGAR_CANE.getPickStack(world, pos, state);
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
        if (blockState.isOf(Blocks.SUGAR_CANE) || blockState.isOf(Blocks.MUD) || blockState.isOf(Blocks.CLAY) || blockState.isOf(CustomBlocks.BROWN_MUD)) {
            return true;
        } else {
            if (blockState.isIn(BlockTags.DIRT) || blockState.isIn(BlockTags.SAND)) {
                FluidState fluidState;
                BlockState blockStateNeighbor, blockStateDown;
                for (Direction direction : Direction.Type.HORIZONTAL) {
                    BlockPos offsetedPos = blockPosDown.offset(direction);
                    blockStateDown = world.getBlockState(offsetedPos);
                    fluidState = world.getFluidState(offsetedPos);
                    if (fluidState.isIn(FluidTags.WATER) || blockStateDown.isOf(Blocks.FROSTED_ICE)) return true;
                    if (world.getFluidState(offsetedPos.offset(direction.rotateClockwise(Direction.Axis.X))).isIn(FluidTags.WATER) ) return true;

                    blockStateNeighbor = world.getBlockState(pos.offset(direction));
                    if (blockStateNeighbor.isOf(Blocks.SUGAR_CANE) || blockStateNeighbor.isOf(CustomBlocks.SUGAR_CANE_TOP)) {
                        offsetedPos = blockPosDown.offset(direction, 1);
                        if (world.getFluidState(offsetedPos).isIn(FluidTags.WATER) ) return true;
                        if (world.getFluidState(offsetedPos.offset(direction.rotateClockwise(Direction.Axis.X))).isIn(FluidTags.WATER) ) return true;
                        if (world.getFluidState(offsetedPos.offset(direction.rotateCounterclockwise(Direction.Axis.X))).isIn(FluidTags.WATER) ) return true;
                    }
                }
            }
            return false;
        }
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        BlockState blockStateBelow = world.getBlockState(pos.down());
        int numSugarCaneBelow = blockStateBelow.isOf(Blocks.SUGAR_CANE) ? 1 : 0;
        int chanceToGrow = numSugarCaneBelow > 0 ? 128 : blockStateBelow.isIn(BlockTags.SAND) ? 64 : 16;

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
            if (numSugarCaneBelow < 3) world.setBlockState(pos, Blocks.SUGAR_CANE.getDefaultState());
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
        else if(blockStateDown.isOf(Blocks.SAND)) return this.getDefaultState().with(GROUND_TYPE, TYPE_SAND);
        else if(blockStateDown.isOf(Blocks.RED_SAND)) return this.getDefaultState().with(GROUND_TYPE, TYPE_RED_SAND);
        else if(blockStateDown.isOf(Blocks.MUD)) return this.getDefaultState().with(GROUND_TYPE, TYPE_GREY_MUD);
        else if(blockStateDown.isOf(Blocks.CLAY)) return this.getDefaultState().with(GROUND_TYPE, TYPE_CLAY);
        else if(blockStateDown.isOf(CustomBlocks.BROWN_MUD)) {
            if(ctx.getWorld().getLightLevel(LightType.SKY, ctx.getBlockPos()) > 4) return this.getDefaultState().with(GROUND_TYPE, TYPE_BROWN_MUD);
            else return this.getDefaultState().with(GROUND_TYPE, TYPE_MUD_CAVE);
        }
        return CustomBlocks.SUGAR_CANE_TOP.getDefaultState();
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
        return true;
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        if(random.nextInt(2) == 0) {
            world.setBlockState(pos, Blocks.SUGAR_CANE.getDefaultState());
            if(random.nextInt(8) == 0 && world.isAir(pos.up())) {
                world.setBlockState(pos.up(), this.getDefaultState());
            }
        }
    }
}
