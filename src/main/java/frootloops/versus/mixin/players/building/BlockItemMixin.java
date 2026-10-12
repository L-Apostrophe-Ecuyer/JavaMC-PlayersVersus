package frootloops.versus.mixin.players.building;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.players.building.PlacementRules;
import frootloops.versus.mod.players.building.RecentPlacements;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Placed blocks turned to match their stroke and neighbours ({@link PlacementRules}), and remembered a moment. */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin extends Item {
    public BlockItemMixin(Properties settings) {
        super(settings);
    }

    @Shadow protected abstract boolean canPlace(BlockPlaceContext context, BlockState state);

    @Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true)
    private void playersVersus$turnToMatch(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState vanilla = cir.getReturnValue();
        if (vanilla == null || !VersusSettings.QOL.DO_SMARTER_BLOCK_PLACING) return;
        BlockState state = PlacementRules.adjust(context, vanilla, (c, s) -> this.canPlace(c, s));
        if (state != vanilla) cir.setReturnValue(state);
    }

    @Inject(method = "placeBlock", at = @At("RETURN"))
    private void playersVersus$rememberPlaced(BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || context.getPlayer() == null) return;
        RecentPlacements.record(context.getPlayer(), context.getClickedPos(), state);
        if (context.getLevel().isClientSide() && PlacementRules.client != null) PlacementRules.client.placed(context, state);
    }
}
