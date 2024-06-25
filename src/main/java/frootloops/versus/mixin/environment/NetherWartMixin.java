package frootloops.versus.mixin.environment;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NetherWartBlock.class)
public abstract class NetherWartMixin extends PlantBlock {

    protected NetherWartMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo info) {
        if (!world.getDimension().ultrawarm()) {
            if(world.getLightLevel(LightType.BLOCK, pos) < 10) {
                world.setBlockState(pos, CustomBlocks.WITHERED_WART_PLANT.getStateWithProperties(state), Block.NOTIFY_LISTENERS);
            }
            else if(state.isOf(Blocks.NETHER_WART)) {
                world.setBlockState(pos, CustomBlocks.CORRUPTED_WART_PLANT.getDefaultState(), Block.NOTIFY_LISTENERS);
                info.cancel();
            }
        }
    }

    @Inject(method = "hasRandomTicks", at = @At("HEAD"), cancellable = true)
    protected void hasRandomTicks(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(!state.isOf(CustomBlocks.WITHERED_WART_PLANT));
    }
}
