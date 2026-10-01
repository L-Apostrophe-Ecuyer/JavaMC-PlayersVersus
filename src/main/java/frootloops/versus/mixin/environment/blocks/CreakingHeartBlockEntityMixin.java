package frootloops.versus.mixin.environment.blocks;

import frootloops.versus.mod.environment.WorldTime;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.entity.CreakingHeartBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreakingHeartBlockEntity.class)
public abstract class CreakingHeartBlockEntityMixin {

    @Inject(method = "updateCreakingState", at = @At("HEAD"), cancellable = true)
    private static void playersVersus$activateAtNightOrUnderground(Level level, BlockState state, BlockPos pos,
                                                                    CreakingHeartBlockEntity entity,
                                                                    CallbackInfoReturnable<BlockState> cir) {
        long timeOfDay = Math.floorMod(WorldTime.dayTime(level), 24000L);
        boolean isNight = timeOfDay >= 13000L && timeOfDay < 23000L;
        boolean isDeepAndSheltered = pos.getY() < 48 && !level.canSeeSky(pos);
        if ((isNight || isDeepAndSheltered) && CreakingHeartBlock.hasRequiredLogs(state, level, pos)) {
            cir.setReturnValue(state.setValue(CreakingHeartBlock.STATE, CreakingHeartState.AWAKE));
        }
    }
}
