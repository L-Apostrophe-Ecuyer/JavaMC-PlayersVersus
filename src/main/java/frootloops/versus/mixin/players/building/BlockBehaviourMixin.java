package frootloops.versus.mixin.players.building;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.players.building.RecentPlacements;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A block the player just placed breaks much faster ({@link RecentPlacements}). Client and server both compute the
 * progress here (the server checks a break against it), so they agree.
 */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {
    @Inject(method = "getDestroyProgress", at = @At("RETURN"), cancellable = true)
    private void playersVersus$takeBackPlaced(BlockState state, Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (!VersusSettings.QOL.DO_FAST_UNDO_PLACING || player == null) return;
        float progress = cir.getReturnValue();
        // Only with the tool it drops with: a misplaced block is to be taken back, not lost
        if (progress <= 0.0F || progress >= 1.0F || !player.hasCorrectToolForDrops(state)) return;
        if (RecentPlacements.recent(player, pos, state)) cir.setReturnValue(RecentPlacements.faster(progress));
    }
}
