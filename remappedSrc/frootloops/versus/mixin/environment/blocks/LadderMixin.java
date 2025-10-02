package frootloops.versus.mixin.environment.blocks;


import frootloops.versus.VersusMod;
import net.minecraft.block.*;
import net.minecraft.fluid.Fluids;
import net.minecraft.fluid.WaterFluid;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LadderBlock.class)
public abstract class LadderMixin extends AbstractBlock {

    public LadderMixin(Settings settings) {
        super(settings);
    }

    @Override
    public BlockState getStateForNeighborUpdate(
            BlockState state,
            WorldView world,
            ScheduledTickView tickView,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            Random random
    ) {
        if (!state.canPlaceAt(world, pos)) { // Now
            return Blocks.AIR.getDefaultState();
        } else {
            if (state.get(LadderBlock.WATERLOGGED)) {
                tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
            }

            return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        }
    }

    @Inject(method = "canPlaceAt", at = @At("RETURN"), cancellable = true)
    protected void canPlaceLaddersAttachedToOtherLadder(BlockState state, WorldView world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if(!cir.getReturnValue()) {
            if(world.getBlockState(pos.up()).isOf(Blocks.LADDER)) cir.setReturnValue(true);
        }
    }



    @Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true)
    public void getPlacementState(ItemPlacementContext ctx, CallbackInfoReturnable<BlockState> cir) {
        BlockState ladder = cir.getReturnValue();
        if(ladder != null) {

            // If placed block has support, and was placed by crouching player, don't reorient:
            if(ctx.getPlayer() != null && ctx.getPlayer().isSneaking() && ctx.getPlayer().isOnGround()) {
                BlockPos supportPos = ctx.getBlockPos().offset(ladder.get(LadderBlock.FACING).getOpposite());
                BlockState supportState  = ctx.getWorld().getBlockState(supportPos);
                if(supportState.isSideSolidFullSquare(ctx.getWorld(), supportPos, ladder.get(LadderBlock.FACING))) return;
            }

            // Else if block above is a ladder, match its orientation:
            BlockState stateOther = ctx.getWorld().getBlockState(ctx.getBlockPos().up());
            if (stateOther.isOf(Blocks.LADDER)) {
                ladder = ladder.with(LadderBlock.FACING, stateOther.get(LadderBlock.FACING));
                cir.setReturnValue(ladder);
                return;
            }

            // Else if block below is a ladder, match its orientation:
            stateOther = ctx.getWorld().getBlockState(ctx.getBlockPos().down());
            if (stateOther.isOf(Blocks.LADDER)) {
                ladder = ladder.with(LadderBlock.FACING, stateOther.get(LadderBlock.FACING));
                if (ladder.canPlaceAt(ctx.getWorld(), ctx.getBlockPos())) {
                    cir.setReturnValue(ladder);
                }
            }
        }
    }
}
