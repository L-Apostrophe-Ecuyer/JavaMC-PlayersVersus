package frootloops.versus.mixin.environment.blocks;

import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.WorldTime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NetherWartBlock.class)
public abstract class NetherWartMixin extends VegetationBlock {

    protected NetherWartMixin(Properties settings) {
        super(settings);
    }

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    protected void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random, CallbackInfo info) {
        if (!WorldTime.ultraWarm(world)) {
            if(world.getBrightness(LightLayer.BLOCK, pos) < 10) {
                world.setBlock(pos, CustomBlocks.WITHERED_WART_PLANT.withPropertiesOf(state), Block.UPDATE_CLIENTS);
            }
            else if(state.is(Blocks.NETHER_WART) && random.nextBoolean()) {
                world.setBlock(pos, CustomBlocks.CORRUPTED_WART_PLANT.defaultBlockState(), Block.UPDATE_CLIENTS);
                info.cancel();
            }
        }
    }

    @Inject(method = "isRandomlyTicking", at = @At("HEAD"), cancellable = true)
    protected void hasRandomTicks(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(!state.is(CustomBlocks.WITHERED_WART_PLANT));
    }
}
