package frootloops.versus.mixin.players.building;

import frootloops.versus.mod.players.building.RecentPlacements;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The blocks a player places, remembered a moment for taking a misplaced one back quickly ({@link RecentPlacements}). */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(method = "placeBlock", at = @At("RETURN"))
    private void playersVersus$rememberPlaced(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && context.getPlayer() != null) RecentPlacements.record(context.getPlayer(), context.getClickedPos(), state);
    }
}
