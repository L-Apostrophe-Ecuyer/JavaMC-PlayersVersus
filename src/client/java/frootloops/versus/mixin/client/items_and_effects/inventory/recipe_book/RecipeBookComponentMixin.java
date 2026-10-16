package frootloops.versus.mixin.client.items_and_effects.inventory.recipe_book;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import frootloops.versus.VersusSettings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Double-click a recipe in the book to place the most crafts it can (what shift-click does), and double-click it again to
 * go back to one. The first click of the double-click places one, as always; the second decides: the most, unless the
 * recipe was already placed for the most before the first click, then one.
 */
@Environment(EnvType.CLIENT)
@Mixin(RecipeBookComponent.class)
public abstract class RecipeBookComponentMixin {
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
}
