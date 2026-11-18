package frootloops.versus.mixin.client.items_and_effects.containers;

import frootloops.versus.mod.items_and_effects.containers.ShulkerPeek;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Using a shulker box with an item selected in its peek takes that item instead of opening the box. */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModePeekMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void playersVersus$peekTake(LocalPlayer player, InteractionHand hand, BlockHitResult blockHit, CallbackInfoReturnable<InteractionResult> cir) {
        InteractionResult taken = ShulkerPeek.use(blockHit.getBlockPos());
        if (taken != null) cir.setReturnValue(taken);
    }
}
