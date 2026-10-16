package frootloops.versus.mixin.environment.blocks;


import frootloops.versus.VersusMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LadderBlock.class)
public abstract class LadderMixin extends BlockBehaviour {

    public LadderMixin(Properties settings) {
        super(settings);
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            LevelReader world,
            ScheduledTickAccess tickView,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random
    ) {
        if (!state.canSurvive(world, pos)) { // Now
            return Blocks.AIR.defaultBlockState();
        } else {
            if (state.getValue(LadderBlock.WATERLOGGED)) {
                tickView.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
            }

            return super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        }
    }

    @Inject(method = "canSurvive", at = @At("RETURN"), cancellable = true)
    protected void canPlaceLaddersAttachedToOtherLadder(BlockState state, LevelReader world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if(!cir.getReturnValue()) {
            if(world.getBlockState(pos.above()).is(Blocks.LADDER)) cir.setReturnValue(true);
        }
    }



    @Inject(method = "getStateForPlacement", at = @At("RETURN"), cancellable = true)
    public void getPlacementState(BlockPlaceContext ctx, CallbackInfoReturnable<BlockState> cir) {
        BlockState ladder = cir.getReturnValue();
        if(ladder != null) {

            // If placed block has support, and was placed by crouching player, don't reorient:
            if(ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown() && ctx.getPlayer().onGround()) {
                BlockPos supportPos = ctx.getClickedPos().relative(ladder.getValue(LadderBlock.FACING).getOpposite());
                BlockState supportState  = ctx.getLevel().getBlockState(supportPos);
                if(supportState.isFaceSturdy(ctx.getLevel(), supportPos, ladder.getValue(LadderBlock.FACING))) return;
            }

            // Else if block above is a ladder, match its orientation:
            BlockState stateOther = ctx.getLevel().getBlockState(ctx.getClickedPos().above());
            if (stateOther.is(Blocks.LADDER)) {
                ladder = ladder.setValue(LadderBlock.FACING, stateOther.getValue(LadderBlock.FACING));
                cir.setReturnValue(ladder);
                return;
            }

            // Else if block below is a ladder, match its orientation:
            stateOther = ctx.getLevel().getBlockState(ctx.getClickedPos().below());
            if (stateOther.is(Blocks.LADDER)) {
                ladder = ladder.setValue(LadderBlock.FACING, stateOther.getValue(LadderBlock.FACING));
                if (ladder.canSurvive(ctx.getLevel(), ctx.getClickedPos())) {
                    cir.setReturnValue(ladder);
                }
            }
        }
    }
}
