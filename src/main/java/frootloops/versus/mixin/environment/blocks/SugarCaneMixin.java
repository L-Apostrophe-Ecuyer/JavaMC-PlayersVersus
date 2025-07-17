package frootloops.versus.mixin.environment.blocks;

import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.blocks.SugarCaneTopBlock;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static frootloops.versus.mod.environment.blocks.SugarCaneTopBlock.*;

@Mixin(SugarCaneBlock.class)
public abstract class SugarCaneMixin extends Block {

    public SugarCaneMixin(Settings settings) {
        super(settings);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        int age = (Integer)state.get(Properties.AGE_15);
        if (world.isAir(pos.up())) {
            int numSugarCaneBelow = 0;
            while (numSugarCaneBelow < 3 && world.getBlockState(pos.down(numSugarCaneBelow)).isOf(this)) {
                numSugarCaneBelow++;
            }
            if (numSugarCaneBelow > 0) {
                if (age >= 14) {
                    if(numSugarCaneBelow > 2 || world.getLightLevel(LightType.SKY, pos) < 13 || random.nextInt(4) ==  1)
                        world.setBlockState(pos.up(), CustomBlocks.SUGAR_CANE_TOP.getStuntedState());
                    else world.setBlockState(pos.up(), CustomBlocks.SUGAR_CANE_TOP.getDefaultState());
                    world.setBlockState(pos, state.with(Properties.AGE_15, Integer.valueOf(8)), Block.NO_REDRAW); // Aged down, but only half way! Incentives picking only the top
                } else {
                    world.setBlockState(pos, state.with(Properties.AGE_15, Integer.valueOf(age + 1)), Block.NO_REDRAW);
                }
            }
            else {
                world.setBlockState(pos.up(), CustomBlocks.SUGAR_CANE_TOP.getDefaultState());
            }
        }
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos blockPosDown = pos.down();
        BlockState blockState = world.getBlockState(blockPosDown);
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

    @Override @Nullable
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos blockPosDown = ctx.getBlockPos().down();
        BlockState blockStateDown = ctx.getWorld().getBlockState(blockPosDown);
        if(blockStateDown.isOf(CustomBlocks.SUGAR_CANE_TOP)) {
            ctx.getWorld().setBlockState(blockPosDown, Blocks.SUGAR_CANE.getDefaultState());
            return CustomBlocks.SUGAR_CANE_TOP.getDefaultState();
        }
        else if(blockStateDown.isOf(Blocks.SUGAR_CANE)) return Blocks.SUGAR_CANE.getDefaultState();
        else return CustomBlocks.SUGAR_CANE_TOP.getPlacementState(ctx);
    }
}
