package frootloops.versus.mixin.environment.blocks;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.block.*;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SugarCaneBlock.class)
public abstract class SugarCaneMixin extends Block {

    public SugarCaneMixin(Settings settings) {
        super(settings);
    }

    @Override @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        if(ctx.getPlayer() == null) {
            BlockPos blockPosDown = ctx.getBlockPos().down();
            BlockState blockStateDown = ctx.getWorld().getBlockState(blockPosDown);
            if (blockStateDown.isOf(Blocks.SUGAR_CANE) && blockStateDown.get(Properties.AGE_15) != 15) {
                ctx.getWorld().setBlockState(blockPosDown, Blocks.SUGAR_CANE.getDefaultState().with(Properties.AGE_15, 15));
            }

            BlockPos blockPosUp = ctx.getBlockPos().up();
            BlockState blockStateUp = ctx.getWorld().getBlockState(blockPosUp);
            if (blockStateUp.isOf(Blocks.SUGAR_CANE)) {
                return Blocks.SUGAR_CANE.getDefaultState().with(Properties.AGE_15, 15);
            }
        }
        return Blocks.SUGAR_CANE.getDefaultState();
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int age = state.get(Properties.AGE_15);
        if (world.isAir(pos.up())) {
            int numSugarCaneBelow = 0;
            while (numSugarCaneBelow < 3 && world.getBlockState(pos.down(numSugarCaneBelow)).isOf(this)) {
                numSugarCaneBelow++;
            }
            if(numSugarCaneBelow == 2 && age == 14) return;
            if (age == 15 && (world.getLightLevel(LightType.SKY, pos) > (1 + numSugarCaneBelow) * (5 + random.nextInt(4)))) {
                world.setBlockState(pos.up(), state.with(Properties.AGE_15, 0));
            }
            else if(age < 15){
                world.setBlockState(pos, state.with(Properties.AGE_15, age + 1));
            }
        }
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos blockPosDown = pos.down();
        BlockState blockState = world.getBlockState(blockPosDown);
        if(blockState.isOf(Blocks.SUGAR_CANE)) {
            return blockState.get(Properties.AGE_15) == 15;
        }
        if (blockState.isOf(Blocks.MUD) || blockState.isOf(Blocks.CLAY) || blockState.isOf(CustomBlocks.BROWN_MUD)) {
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
}
