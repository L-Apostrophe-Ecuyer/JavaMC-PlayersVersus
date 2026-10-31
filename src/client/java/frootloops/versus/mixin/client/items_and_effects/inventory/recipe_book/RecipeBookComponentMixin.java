package frootloops.versus.mixin.client.items_and_effects.inventory.recipe_book;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import frootloops.versus.VersusSettings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import frootloops.versus.mod.items_and_effects.inventory.recipe_book.RecipeBookPopups;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Double-click a recipe in the book to place the most crafts it can (what shift-click does), and double-click it again to
 * go back to one. The first click of the double-click places one, as always; the second decides: the most, unless the
 * recipe was already placed for the most before the first click, then one.
 * <p>
 * The page's popups (RecipeBookPopups: a collection's window, a group's recipe shapes) are drawn last, above the rest of
 * the book, and the window takes the clicks and drags on it before the book does; Esc closes an open popup before the
 * screen.
 */
@Environment(EnvType.CLIENT)
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {
    @Shadow @Final private RecipeBookPage recipeBookPage;

    @Shadow public abstract boolean isVisible();

    /** The recipe whose last placement was for the most crafts, until something else is placed or the grid is emptied. */
    @Unique @Nullable private RecipeDisplayId playersVersus$placedForMost;
    /** The recipe that was placed for the most when the current click (or double-click) began. */
    @Unique @Nullable private RecipeDisplayId playersVersus$placedForMostBeforeClick;

    @WrapOperation(method = "mouseClicked", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeBookComponent;tryPlaceRecipe(Lnet/minecraft/client/gui/screens/recipebook/RecipeCollection;Lnet/minecraft/world/item/crafting/display/RecipeDisplayId;Z)Z"))
    private boolean playersVersus$doubleClickForMost(RecipeBookComponent<?> self, RecipeCollection collection, RecipeDisplayId id, boolean forMost,
                                                     Operation<Boolean> original, @Local(argsOnly = true) boolean doubleClick) {
        if (!VersusSettings.QOL.DO_RECIPE_DOUBLE_CLICK_MAX) return original.call(self, collection, id, forMost);
        if (!doubleClick) {
            this.playersVersus$placedForMostBeforeClick = id.equals(this.playersVersus$placedForMost) ? id : null;
            boolean placed = original.call(self, collection, id, forMost);
            this.playersVersus$placedForMost = placed && forMost ? id : null;
            return placed;
        }
        boolean most = forMost || !id.equals(this.playersVersus$placedForMostBeforeClick);
        boolean placed = original.call(self, collection, id, most);
        this.playersVersus$placedForMost = placed && most ? id : null;
        this.playersVersus$placedForMostBeforeClick = null;
        return placed;
    }

    /** Taking the crafted items (or clicking any crafting slot) ends what was placed: the next double-click places the most. */
    @Inject(method = "slotClicked", at = @At("HEAD"))
    private void playersVersus$forgetPlacement(Slot slot, CallbackInfo ci) {
        this.playersVersus$placedForMost = null;
    }

    @Unique
    private RecipeBookPopups playersVersus$popups() {
        return (RecipeBookPopups) this.recipeBookPage;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void playersVersus$windowClicks(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (this.isVisible() && this.playersVersus$popups().playersVersus$windowClicked(event, doubleClick)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void playersVersus$dragWindow(MouseButtonEvent event, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        if (this.isVisible() && this.playersVersus$popups().playersVersus$windowDragged(event.x(), event.y())) cir.setReturnValue(true);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void playersVersus$escapeClosesPopup(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.isEscape() && this.isVisible() && this.playersVersus$popups().playersVersus$closePopup()) cir.setReturnValue(true);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void playersVersus$drawPopups(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (this.isVisible()) this.playersVersus$popups().playersVersus$renderPopups(graphics, mouseX, mouseY, partialTick);
    }

    /** A click on the window is inside the screen: it doesn't drop what the mouse carries. */
    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void playersVersus$windowIsInside(double x, double y, int left, int top, int width, int height, CallbackInfoReturnable<Boolean> cir) {
        if (this.isVisible() && this.playersVersus$popups().playersVersus$windowContains(x, y)) cir.setReturnValue(false);
    }
}
