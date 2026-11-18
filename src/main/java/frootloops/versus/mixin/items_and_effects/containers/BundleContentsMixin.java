package frootloops.versus.mixin.items_and_effects.containers;

import frootloops.versus.mod.items_and_effects.containers.BundleGrid;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The bundle's grid is 5 by 4: as many of its stacks shown and selectable as BundleGrid says. */
@Mixin(BundleContents.class)
public abstract class BundleContentsMixin {

    @Shadow
    public abstract int size();

    @Inject(method = "getNumberOfItemsToShow", at = @At("HEAD"), cancellable = true)
    private void playersVersus$wideGrid(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(BundleGrid.itemsToShow(this.size()));
    }
}
