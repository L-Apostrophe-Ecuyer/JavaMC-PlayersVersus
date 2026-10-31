package frootloops.versus.mod.items_and_effects.inventory.recipe_book;

import frootloops.versus.mod.items_and_effects.recipes.RecipeWindowLayout;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static frootloops.versus.mod.items_and_effects.recipes.RecipeWindowLayout.CELL;
import static frootloops.versus.mod.items_and_effects.recipes.RecipeWindowLayout.CLOSE;
import static frootloops.versus.mod.items_and_effects.recipes.RecipeWindowLayout.ICON;
import static frootloops.versus.mod.items_and_effects.recipes.RecipeWindowLayout.PADDING;

/**
 * A recipe book collection's popup: its items' icons, small, under a title bar. Hovering the collection's button shows
 * it beside the button until the mouse leaves both (a peek); a click keeps it (pinned), a window that its title bar
 * drags anywhere on the screen (the book's drag events, from a press on it to the next press) and its close box, or
 * Esc, closes. One at a time: another collection takes its place.
 * What a click on an icon does is the page's (RecipeBookPageMixin).
 */
@Environment(EnvType.CLIENT)
public final class FamilyWindow {
    private static final int BACKGROUND = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, SHADOW = 0xFF555555, EDGE = 0xFF000000;
    private static final int TITLE_COLOUR = 0xFF404040, CLOSE_HOVERED = 0xFFC03030;
    private static final int CELL_CRAFTABLE = 0x30000000, CELL_UNCRAFTABLE = 0x90C04040, CELL_HOVERED = 0x80FFFFFF;
    private static final int SEVERAL_MARK = 0xFFFFFFFF;

    @Nullable private FamilyRecipes family;
    private RecipeWindowLayout layout = new RecipeWindowLayout(0, 0, 0, 0);
    private boolean pinned, dragging;
    private double grabX, grabY;

    public boolean isVisible() {
        return this.family != null;
    }

    public boolean isPinned() {
        return this.family != null && this.pinned;
    }

    @Nullable
    public FamilyRecipes family() {
        return this.family;
    }

    /** Whether it shows this collection (the same family, whichever refresh made it). */
    public boolean shows(@Nullable RecipeCollection collection) {
        return this.family != null && collection instanceof FamilyRecipes other && other.family().equals(this.family.family());
    }

    /** Shows the collection beside its button, below it, without keeping it. */
    public void peek(FamilyRecipes family, int buttonX, int buttonBottom) {
        this.show(family, buttonX - PADDING, buttonBottom - 1);
        this.pinned = false;
    }

    /** Keeps the collection open: where it is if shown already, else beside its button. */
    public void pin(FamilyRecipes family, int buttonX, int buttonBottom) {
        if (!this.shows(family)) this.show(family, buttonX - PADDING, buttonBottom - 1);
        this.pinned = true;
    }

    /** Keeps what it shows open. */
    public void pin() {
        this.pinned = this.family != null;
    }

    public void close() {
        this.family = null;
        this.pinned = this.dragging = false;
    }

    /** After the book refreshes: the same collection made anew, in place; closed if the page doesn't list it now. */
    public void refresh(List<RecipeCollection> listed) {
        if (this.family == null) return;
        for (RecipeCollection collection : listed) {
            if (collection instanceof FamilyRecipes other && other.family().equals(this.family.family())) {
                this.show(other, this.layout.x(), this.layout.y());
                return;
            }
        }
        this.close();
    }

    public boolean contains(double x, double y) {
        return this.family != null && this.layout.contains(x, y);
    }

    public boolean inCloseBox(double x, double y) {
        return this.family != null && this.layout.inCloseBox(x, y);
    }

    public boolean inTitleBar(double x, double y) {
        return this.family != null && this.layout.inTitleBar(x, y);
    }

    /** The item whose icon is under the point, or null. */
    @Nullable
    public ItemRecipes itemAt(double x, double y) {
        if (this.family == null) return null;
        int index = this.layout.cellAt(x, y);
        return index < 0 ? null : this.family.items().get(index);
    }

    /** The cell of the item, its left and top edges, or null if it doesn't show it. */
    @Nullable
    public int[] cellOf(ItemRecipes item) {
        if (this.family == null) return null;
        int index = this.family.items().indexOf(item);
        return index < 0 ? null : new int[] {this.layout.cellX(index), this.layout.cellY(index)};
    }

    /** Starts dragging by the title bar from this point, which keeps the window. */
    public void grab(double x, double y) {
        this.pinned = true;
        this.dragging = true;
        this.grabX = x - this.layout.x();
        this.grabY = y - this.layout.y();
    }

    /** The mouse dragged to this point: the window follows it if it was grabbed (true). */
    public boolean drag(double mouseX, double mouseY) {
        if (!this.dragging || this.family == null) return false;
        this.moveTo((int) Math.round(mouseX - this.grabX), (int) Math.round(mouseY - this.grabY));
        return true;
    }

    /** A new press of a mouse button: whatever drag there was has ended. */
    public void release() {
        this.dragging = false;
    }

    public void render(GuiGraphicsExtractor graphics, Font font, int mouseX, int mouseY) {
        if (this.family == null) return;
        RecipeWindowLayout at = this.layout;
        int x = at.x(), y = at.y(), right = x + at.width(), bottom = y + at.height();
        graphics.nextStratum();
        // A panel as the inventory's: an edge, lit at the top left, shaded at the bottom right
        graphics.fill(x, y, right, bottom, EDGE);
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, BACKGROUND);
        graphics.fill(x + 1, y + 1, right - 2, y + 2, LIGHT);
        graphics.fill(x + 1, y + 1, x + 2, bottom - 2, LIGHT);
        graphics.fill(x + 2, bottom - 2, right - 1, bottom - 1, SHADOW);
        graphics.fill(right - 2, y + 2, right - 1, bottom - 1, SHADOW);
        graphics.text(font, this.family.title(), x + PADDING, y + 3, TITLE_COLOUR, false);
        graphics.text(font, "x", right - CLOSE + 2, y + 2, at.inCloseBox(mouseX, mouseY) ? CLOSE_HOVERED : TITLE_COLOUR, false);

        List<ItemRecipes> items = this.family.items();
        int hovered = at.cellAt(mouseX, mouseY);
        for (int i = 0; i < items.size(); i++) {
            ItemRecipes item = items.get(i);
            int cellX = at.cellX(i), cellY = at.cellY(i);
            graphics.fill(cellX, cellY, cellX + CELL, cellY + CELL, item.hasCraftable() ? CELL_CRAFTABLE : CELL_UNCRAFTABLE);
            if (i == hovered) graphics.fill(cellX, cellY, cellX + CELL, cellY + CELL, CELL_HOVERED);
            int inset = (CELL - ICON) / 2;
            graphics.pose().pushMatrix();
            graphics.pose().translate(cellX + inset, cellY + inset);
            graphics.pose().scale(ICON / 16.0F, ICON / 16.0F);
            graphics.fakeItem(item.icon(), 0, 0);
            graphics.pose().popMatrix();
            // Several recipes: a mark in the corner, as the book's own stack of slots for a group
            if (item.hasSeveralRecipes()) graphics.fill(cellX + CELL - 3, cellY + 1, cellX + CELL - 1, cellY + 3, SEVERAL_MARK);
        }
        if (hovered >= 0) graphics.setTooltipForNextFrame(font, items.get(hovered).icon(), mouseX, mouseY);
    }

    private void show(FamilyRecipes family, int x, int y) {
        this.family = family;
        this.layout = new RecipeWindowLayout(x, y, family.items().size(), Minecraft.getInstance().font.width(family.title()));
        this.moveTo(x, y);
    }

    /** Moves it there, kept on the screen. */
    private void moveTo(int x, int y) {
        Screen screen = Minecraft.getInstance().screen;
        int width = screen == null ? Integer.MAX_VALUE : screen.width, height = screen == null ? Integer.MAX_VALUE : screen.height;
        int clampedX = Math.max(0, Math.min(x, width - this.layout.width()));
        int clampedY = Math.max(0, Math.min(y, height - this.layout.height()));
        this.layout = this.layout.at(clampedX, clampedY);
    }
}
