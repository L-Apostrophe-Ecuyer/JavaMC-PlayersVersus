package frootloops.versus.mixin.client.items_and_effects.containers;

import frootloops.versus.mod.items_and_effects.containers.ShulkerMouseActions;
import net.minecraft.client.gui.ItemSlotMouseAction;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every container screen lets the wheel select in a shulker box, as it does in a bundle. */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenShulkerMixin {

    @Shadow
    protected abstract void addItemSlotMouseAction(ItemSlotMouseAction itemSlotMouseAction);

    @Inject(method = "init", at = @At("TAIL"))
    private void playersVersus$shulkerWheel(CallbackInfo ci) {
        this.addItemSlotMouseAction(new ShulkerMouseActions());
    }
}
