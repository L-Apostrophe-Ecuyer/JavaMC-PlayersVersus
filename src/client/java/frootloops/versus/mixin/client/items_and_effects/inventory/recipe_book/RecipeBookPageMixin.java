package frootloops.versus.mixin.client.items_and_effects.inventory.recipe_book;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.items_and_effects.inventory.recipe_book.FamilyRecipes;
import frootloops.versus.mod.items_and_effects.inventory.recipe_book.FamilyWindow;
import frootloops.versus.mod.items_and_effects.inventory.recipe_book.ItemRecipes;
import frootloops.versus.mod.items_and_effects.inventory.recipe_book.PopupBounds;
import frootloops.versus.mod.items_and_effects.inventory.recipe_book.RecipeBookCollections;
import frootloops.versus.mod.items_and_effects.inventory.recipe_book.RecipeBookPopups;
import frootloops.versus.mod.items_and_effects.inventory.recipe_book.RecipeBookSorting;
import frootloops.versus.mod.items_and_effects.recipes.RecipeGroupHover;
import frootloops.versus.mod.items_and_effects.recipes.RecipeWindowLayout;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * The recipe book's page:
 * <ul>
 * <li>It lists groups, an item's recipes, and collections of similar items' groups (RecipeBookCollections), craftable
 * first, then in the inventory sort's order.</li>
 * <li>A group of several recipes opens its popup (vanilla's: the recipes' shapes) when the mouse stays on its button
 * (RecipeGroupHover), and closes it when the mouse is outside the button and the popup. A left click places the recipe
 * the button shows and keeps the popup open, as does a right click or a click in the popup, until a click outside it or
 * Esc.</li>
 * <li>A collection opens its popup of icons (FamilyWindow) the same way, and a click keeps it as a window. In it, an
 * icon is used as a group's button.</li>
 * <li>A click outside an open group popup closes it and goes through (vanilla closes it and swallows the click).</li>
 * </ul>
 * The popups are drawn by the book, above the rest of it (RecipeBookComponentMixin), the window first.
 */
@Environment(EnvType.CLIENT)
@Mixin(RecipeBookPage.class)
public abstract class RecipeBookPageMixin implements RecipeBookPopups {
    /** Where vanilla's right-click anchors the popup: the middle of the book's page, 13 down (RecipeBookPage.mouseClicked). */
    @Unique private static final int ANCHOR_DOWN = 13;
    /** How far from a window's icon its group popup keeps (vanilla keeps it within 50 of the book's middle). */
    @Unique private static final int NEAR_CELL = 50;

    @Shadow @Final private OverlayRecipeComponent overlay;
    @Shadow @Final private List<RecipeButton> buttons;
    @Shadow private Minecraft minecraft;
    @Shadow private boolean isFiltering;
    @Shadow @Final private RecipeBookComponent<?> parent;

    @Unique private final RecipeGroupHover<RecipeCollection> playersVersus$groupHover = new RecipeGroupHover<>();
    @Unique private final RecipeGroupHover<RecipeCollection> playersVersus$familyHover = new RecipeGroupHover<>();
    @Unique private final FamilyWindow playersVersus$window = new FamilyWindow();
    /** Where the group popup was opened: the left, top and width of what it opened from, and the point it keeps near. */
    @Unique @Nullable private int[] playersVersus$popupAnchor;

    // What the page lists

    @ModifyVariable(method = "updateCollections", at = @At("HEAD"), argsOnly = true)
    private List<RecipeCollection> playersVersus$listGroupsAndCollections(List<RecipeCollection> collections, @Local(argsOnly = true, ordinal = 1) boolean filtering) {
        if (this.minecraft == null || this.minecraft.level == null) return collections;
        ContextMap context = SlotDisplayContext.fromLevel(this.minecraft.level);
        List<RecipeCollection> listed = collections;
        if (VersusSettings.QOL.DO_RECIPE_BOOK_COLLECTIONS) {
            EditBox search = ((RecipeBookComponentAccessor) this.parent).playersVersus$searchBox();
            listed = RecipeBookCollections.regroup(collections, context, search == null ? "" : search.getValue(), filtering);
        } else if (VersusSettings.QOL.DO_SMART_RECIPE_ORDER) {
            listed = RecipeBookSorting.sortCollections(collections, context);
        }
        this.playersVersus$window.refresh(listed);
        this.playersVersus$refreshGroupPopup(listed);
        return listed;
    }

    // Clicks

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void playersVersus$clicks(MouseButtonEvent event, int x, int y, int width, int height, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (VersusSettings.QOL.DO_RECIPE_BOOK_CLICK_THROUGH && this.overlay.isVisible() && !this.playersVersus$popupContains(event.x(), event.y())) {
            // Closed here, vanilla goes on as if no popup was open: the click reaches whatever is under it
            this.overlay.setVisible(false);
            RecipeButton under = this.playersVersus$buttonAt(event.x(), event.y());
            this.playersVersus$groupHover.suppress(under == null ? null : under.getCollection());
        }
        if (this.overlay.isVisible()) return;
        RecipeButton button = this.playersVersus$buttonAt(event.x(), event.y());
        if (button != null && button.getCollection() instanceof FamilyRecipes family) {
            // A collection's button keeps its window open, and places nothing
            this.playersVersus$window.pin(family, button.getX(), button.getY() + button.getHeight());
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("RETURN"))
    private void playersVersus$keepGroupOpen(MouseButtonEvent event, int x, int y, int width, int height, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!VersusSettings.QOL.DO_RECIPE_BOOK_HOVER_GROUPS || !Boolean.TRUE.equals(cir.getReturnValue())) return;
        if (this.overlay.isVisible()) {
            // Opened by a right click, or clicked in: it stays
            this.playersVersus$groupHover.pin();
            return;
        }
        RecipeButton button = this.playersVersus$buttonAt(event.x(), event.y());
        if (!event.isLeftMouse() || button == null || button.isOnlyOption()) return;
        // A left click on a group of several recipes places the one shown (vanilla), and opens their shapes, kept open
        this.playersVersus$openGroupPopup(button.getCollection(), button.getX(), button.getY(), button.getWidth(), x + width / 2, y + ANCHOR_DOWN + height / 2);
        this.playersVersus$groupHover.pin();
    }

    @Override
    public boolean playersVersus$windowClicked(MouseButtonEvent event, boolean doubleClick) {
        FamilyWindow window = this.playersVersus$window;
        window.release();
        double x = event.x(), y = event.y();
        if (!window.contains(x, y) || this.overlay.isVisible() && this.playersVersus$popupContains(x, y)) return false;
        if (window.inCloseBox(x, y)) {
            this.playersVersus$closeWindow();
            return true;
        }
        if (window.inTitleBar(x, y)) {
            window.grab(x, y);
            return true;
        }
        window.pin();
        ItemRecipes item = window.itemAt(x, y);
        if (item == null || !event.isLeftMouse() && !event.isRightMouse()) return true;
        if (this.overlay.isVisible()) this.overlay.setVisible(false);
        // An icon is used as a group's button: a left click places its recipe, and one of several opens their shapes
        if (event.isLeftMouse()) {
            ((RecipeBookComponentAccessor) this.parent).playersVersus$tryPlaceRecipe(item, item.recipe().id(), event.hasShiftDown() || doubleClick);
        }
        if (item.hasSeveralRecipes()) {
            this.playersVersus$openAtCell(item);
            this.playersVersus$groupHover.pin();
        }
        return true;
    }

    @Override
    public boolean playersVersus$windowDragged(double x, double y) {
        return this.playersVersus$window.drag(x, y);
    }

    @Override
    public boolean playersVersus$closePopup() {
        if (this.overlay.isVisible()) {
            this.overlay.setVisible(false);
            return true;
        }
        if (this.playersVersus$window.isVisible()) {
            this.playersVersus$closeWindow();
            return true;
        }
        return false;
    }

    @Override
    public boolean playersVersus$windowContains(double x, double y) {
        return this.playersVersus$window.contains(x, y);
    }

    // Hovering and drawing

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void playersVersus$hover(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!VersusSettings.QOL.DO_RECIPE_BOOK_HOVER_GROUPS || this.minecraft == null || this.minecraft.level == null) return;
        long now = System.nanoTime() / 1_000_000L;
        FamilyWindow window = this.playersVersus$window;
        boolean overWindow = window.contains(mouseX, mouseY);
        RecipeButton button = overWindow ? null : this.playersVersus$buttonAt(mouseX, mouseY);
        ItemRecipes cell = overWindow ? window.itemAt(mouseX, mouseY) : null;
        RecipeCollection hovered = cell != null ? cell : button != null ? button.getCollection() : null;

        // Collections: their popup of icons, until it's kept
        if (!window.isPinned()) {
            FamilyRecipes family = hovered instanceof FamilyRecipes collection ? collection : null;
            switch (this.playersVersus$familyHover.update(family, window.isVisible(), overWindow || window.shows(hovered), now)) {
                case OPEN -> window.peek(family, button.getX(), button.getY() + button.getHeight());
                case CLOSE -> window.close();
                case NONE -> {}
            }
        }

        // Groups of several recipes: their shapes
        RecipeCollection group = hovered != null && !(hovered instanceof FamilyRecipes) && playersVersus$severalRecipes(hovered) ? hovered : null;
        boolean open = this.overlay.isVisible();
        boolean overOpenGroup = open && (this.playersVersus$popupContains(mouseX, mouseY) || hovered == this.overlay.getRecipeCollection());
        switch (this.playersVersus$groupHover.update(group, open, overOpenGroup, now)) {
            case CLOSE -> this.overlay.setVisible(false);
            case OPEN -> {
                if (cell != null) this.playersVersus$openAtCell(cell);
                else this.playersVersus$openGroupPopup(group, button.getX(), button.getY(), button.getWidth(),
                        x + RecipeBookComponent.IMAGE_WIDTH / 2, y + ANCHOR_DOWN + RecipeBookComponent.IMAGE_HEIGHT / 2);
            }
            case NONE -> {}
        }
    }

    /** The group popup is drawn by the book after the window (playersVersus$renderPopups), above it. */
    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/recipebook/OverlayRecipeComponent;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void playersVersus$drawGroupPopupLater(OverlayRecipeComponent overlay, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, Operation<Void> original) {
    }

    @Override
    public void playersVersus$renderPopups(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft != null) this.playersVersus$window.render(graphics, this.minecraft.font, mouseX, mouseY);
        if (this.overlay.isVisible()) {
            graphics.nextStratum();
            this.overlay.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
    }

    /** No tooltip for a button under the window: the window's icon has its own. */
    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void playersVersus$noTooltipUnderTheWindow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (this.playersVersus$window.contains(mouseX, mouseY)) ci.cancel();
    }

    // Helpers

    @Unique
    private void playersVersus$openGroupPopup(RecipeCollection collection, int x, int y, int width, int nearX, int nearY) {
        if (this.minecraft == null || this.minecraft.level == null) return;
        this.playersVersus$popupAnchor = new int[] {x, y, width, nearX, nearY};
        this.overlay.init(collection, SlotDisplayContext.fromLevel(this.minecraft.level), this.isFiltering, x, y, nearX, nearY, (float) width);
    }

    /** The group popup of a window's icon, opened beside it. */
    @Unique
    private void playersVersus$openAtCell(ItemRecipes item) {
        int[] cell = this.playersVersus$window.cellOf(item);
        if (cell != null) this.playersVersus$openGroupPopup(item, cell[0], cell[1], RecipeWindowLayout.CELL, cell[0] + NEAR_CELL, cell[1] + NEAR_CELL);
    }

    /** After the book refreshes: the open group popup made anew from the same item's group, where it was; closed if gone. */
    @Unique
    private void playersVersus$refreshGroupPopup(List<RecipeCollection> listed) {
        int[] anchor = this.playersVersus$popupAnchor;
        if (!this.overlay.isVisible() || anchor == null || !(this.overlay.getRecipeCollection() instanceof ItemRecipes shown)) return;
        ItemRecipes now = null;
        for (RecipeCollection collection : listed) {
            if (collection instanceof ItemRecipes item && item.icon().getItem() == shown.icon().getItem()) now = item;
            if (collection instanceof FamilyRecipes family) {
                for (ItemRecipes item : family.items()) if (item.icon().getItem() == shown.icon().getItem()) now = item;
            }
        }
        if (now == null) this.overlay.setVisible(false);
        else this.playersVersus$openGroupPopup(now, anchor[0], anchor[1], anchor[2], anchor[3], anchor[4]);
    }

    @Unique
    private void playersVersus$closeWindow() {
        FamilyRecipes family = this.playersVersus$window.family();
        // The group popup of one of its icons goes with it
        if (family != null && this.overlay.isVisible() && family.items().contains(this.overlay.getRecipeCollection())) this.overlay.setVisible(false);
        this.playersVersus$window.close();
    }

    @Unique
    private static boolean playersVersus$severalRecipes(RecipeCollection collection) {
        if (collection instanceof ItemRecipes item) return item.hasSeveralRecipes();
        return collection.getSelectedRecipes(RecipeCollection.CraftableStatus.ANY).size() > 1;
    }

    @Unique
    private boolean playersVersus$popupContains(double x, double y) {
        return ((PopupBounds) (Object) this.overlay).playersVersus$popupContains(x, y);
    }

    @Unique
    @Nullable
    private RecipeButton playersVersus$buttonAt(double mouseX, double mouseY) {
        for (RecipeButton button : this.buttons) {
            if (button.isVisible() && button.isMouseOver(mouseX, mouseY)) return button;
        }
        return null;
    }
}
