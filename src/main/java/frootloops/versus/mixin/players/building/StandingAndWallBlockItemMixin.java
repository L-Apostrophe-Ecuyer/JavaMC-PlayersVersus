package frootloops.versus.mixin.players.building;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.players.building.PlacementRules;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Signs, banners, heads and torches pick their standing or wall block themselves: the same rules as other blocks. */
@Mixin(StandingAndWallBlockItem.class)
public abstract class StandingAndWallBlockItemMixin extends BlockItem {
    public StandingAndWallBlockItemMixin(Block block, Properties properties) {
        super(block, properties);
    }

    @Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true)
    private void playersVersus$turnToMatch(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState vanilla = cir.getReturnValue();
        if (vanilla == null || !VersusSettings.QOL.DO_SMARTER_BLOCK_PLACING) return;
        BlockState state = PlacementRules.adjust(context, vanilla, (c, s) -> this.canPlace(c, s));
        if (state != vanilla) cir.setReturnValue(state);
    }
}
