package frootloops.versus.mod.items_and_effects.inventory.recipe_book;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * The recipe book page's popups, as the book and the screen reach them: the collections' window (FamilyWindow) and the
 * group popup (vanilla's overlay of recipe shapes), drawn above the rest of the book, the window first.
 */
@Environment(EnvType.CLIENT)
public interface RecipeBookPopups {
    /** Whether the point is on the collections' window. */
    boolean playersVersus$windowContains(double x, double y);

    /** A click that the window takes (true), before the book sees it. */
    boolean playersVersus$windowClicked(MouseButtonEvent event, boolean doubleClick);

    /** The mouse dragged to this point: the window follows it if its title bar was grabbed (true). */
    boolean playersVersus$windowDragged(double x, double y);

    /** Esc: the open popup closes (true), the group popup first, then the window. */
    boolean playersVersus$closePopup();

    /** The window, then the group popup above it, over everything else in the book. */
    void playersVersus$renderPopups(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick);
}
