package frootloops.versus.mixin.client.items_and_effects.inventory.recipe_book;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.items_and_effects.inventory.recipe_book.PopupBounds;
import frootloops.versus.mod.items_and_effects.inventory.recipe_book.RecipeBookSorting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/**
 * The popup of a recipe group: its two sections (what can be crafted, then what can't, as vanilla lists them) each ordered
 * like the inventory sort; and where it is, for the page to know when the mouse leaves it (vanilla's isMouseOver is always
 * false).
 */
@Environment(EnvType.CLIENT)
@Mixin(OverlayRecipeComponent.class)
public abstract class OverlayRecipeComponentMixin implements PopupBounds {
    /** The sprite is drawn this far outside the buttons: 4 left and right, 5 above, 3 below (its size is 25 per button and 8). */
    @Unique private static final int BORDER_LEFT_RIGHT = 4, BORDER_TOP = 5, BORDER_BOTTOM = 3;

    @Shadow @Final private List<? extends AbstractWidget> recipeButtons;
    @Shadow public abstract boolean isVisible();

    /** The recipes the popup was made with, as vanilla makes its buttons: the craftable ones, then the others. */
    @Unique private List<RecipeDisplayEntry> playersVersus$craftableEntries = List.of();
    @Unique private List<RecipeDisplayEntry> playersVersus$otherEntries = List.of();

    @WrapOperation(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/recipebook/RecipeCollection;getSelectedRecipes(Lnet/minecraft/client/gui/screens/recipebook/RecipeCollection$CraftableStatus;)Ljava/util/List;"))
    private List<RecipeDisplayEntry> playersVersus$orderSection(RecipeCollection collection, RecipeCollection.CraftableStatus status,
                                                               Operation<List<RecipeDisplayEntry>> original, @Local(argsOnly = true) ContextMap context) {
        List<RecipeDisplayEntry> entries = original.call(collection, status);
        if (VersusSettings.QOL.DO_SMART_RECIPE_ORDER) entries = RecipeBookSorting.sortEntries(entries, context);
        if (status == RecipeCollection.CraftableStatus.CRAFTABLE) this.playersVersus$craftableEntries = entries;
        else if (status == RecipeCollection.CraftableStatus.NOT_CRAFTABLE) this.playersVersus$otherEntries = entries;
        return entries;
    }

    @Override
    public boolean playersVersus$popupContains(double x, double y) {
        if (this.recipeButtons.isEmpty()) return false;
        int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = Integer.MIN_VALUE, bottom = Integer.MIN_VALUE;
        for (AbstractWidget button : this.recipeButtons) {
            left = Math.min(left, button.getX());
            top = Math.min(top, button.getY());
            right = Math.max(right, button.getX() + button.getWidth());
            bottom = Math.max(bottom, button.getY() + button.getHeight());
        }
        return x >= left - BORDER_LEFT_RIGHT && x < right + BORDER_LEFT_RIGHT && y >= top - BORDER_TOP && y < bottom + BORDER_BOTTOM;
    }

    @Override
    @Nullable
    public RecipeDisplayEntry playersVersus$entryAt(double x, double y) {
        for (int i = 0; i < this.recipeButtons.size(); i++) {
            AbstractWidget button = this.recipeButtons.get(i);
            if (x >= button.getX() && x < button.getX() + button.getWidth() && y >= button.getY() && y < button.getY() + button.getHeight()) return this.playersVersus$entryOfButton(i);
        }
        return null;
    }

    @Unique
    @Nullable
    private RecipeDisplayEntry playersVersus$entryOfButton(int index) {
        List<RecipeDisplayEntry> all = new ArrayList<>(this.playersVersus$craftableEntries);
        all.addAll(this.playersVersus$otherEntries);
        return index < all.size() ? all.get(index) : null;
    }
}
