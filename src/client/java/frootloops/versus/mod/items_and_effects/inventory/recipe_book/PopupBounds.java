package frootloops.versus.mod.items_and_effects.inventory.recipe_book;

import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import org.jetbrains.annotations.Nullable;

/** What the recipe book's group popup (OverlayRecipeComponent) tells, which vanilla's own methods don't. */
public interface PopupBounds {
    /** Whether the point is inside the popup, its border included (vanilla's isMouseOver is always false). */
    boolean playersVersus$popupContains(double x, double y);

    /** The recipe whose button is under the point, or null. */
    @Nullable
    RecipeDisplayEntry playersVersus$entryAt(double x, double y);
}
