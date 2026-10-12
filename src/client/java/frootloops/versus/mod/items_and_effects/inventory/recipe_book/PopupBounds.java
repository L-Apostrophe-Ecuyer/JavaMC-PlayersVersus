package frootloops.versus.mod.items_and_effects.inventory.recipe_book;

/** What the recipe book's group popup (OverlayRecipeComponent) tells, which vanilla's isMouseOver doesn't (it is always false). */
public interface PopupBounds {
    /** Whether the point is inside the popup, its border included. */
    boolean playersVersus$popupContains(double x, double y);
}
