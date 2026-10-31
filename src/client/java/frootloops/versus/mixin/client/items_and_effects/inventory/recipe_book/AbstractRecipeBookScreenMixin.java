package frootloops.versus.mixin.client.items_and_effects.inventory.recipe_book;

import frootloops.versus.mod.items_and_effects.inventory.recipe_book.RecipeBookPopups;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A slot under the recipe book's window (a collection's, FamilyWindow) isn't under the mouse: no highlight, no tooltip. */
@Environment(EnvType.CLIENT)
@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {
    @Shadow @Final private RecipeBookComponent<?> recipeBookComponent;

    @Inject(method = "isHovering", at = @At("HEAD"), cancellable = true)
    private void playersVersus$windowCoversSlots(int x, int y, int width, int height, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (!this.recipeBookComponent.isVisible()) return;
        RecipeBookPopups popups = (RecipeBookPopups) ((RecipeBookComponentAccessor) this.recipeBookComponent).playersVersus$page();
        if (popups.playersVersus$windowContains(mouseX, mouseY)) cir.setReturnValue(false);
    }
}
