package frootloops.versus.mixin.items_and_effects.containers;

import frootloops.versus.mod.items_and_effects.containers.ShulkerSelection;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A menu closing unselects the shulker boxes in it: the client's unselect can come after the close (vanilla sends the
 * close first) or not at all (the server closing it), and a selection left on a box would make a later right click take
 * a stack instead of picking the box up.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuShulkerMixin {

    @Inject(method = "removed", at = @At("HEAD"))
    private void playersVersus$unselectBoxes(Player player, CallbackInfo ci) {
        ShulkerSelection.clearIn((AbstractContainerMenu) (Object) this);
    }
}
