package frootloops.versus.mixin.client.items_and_effects.inventory.recipe_book;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.items_and_effects.inventory.recipe_book.PopupBounds;
import frootloops.versus.mod.items_and_effects.inventory.recipe_book.RecipeBookSorting;
import frootloops.versus.mod.items_and_effects.recipes.RecipeGroupHover;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.input.MouseButtonEvent;
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
 * The recipe book's page of group buttons:
 * <ul>
 * <li>a click outside an open group's popup closes it and goes through (vanilla closes it and swallows the click);</li>
 * <li>a group opens when the mouse stays on its button (RecipeGroupHover) and closes when the mouse is outside the button
 * and the popup; left-click still places the recipe it shows, right-click still opens the popup at once;</li>
 * <li>the groups are listed craftable first, then the rest, each ordered like the inventory sort.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
@Mixin(RecipeBookPage.class)
public abstract class RecipeBookPageMixin {
    /** Where vanilla's right-click anchors the popup: the middle of the book's page, 13 down (RecipeBookPage.mouseClicked). */
    @Unique private static final int ANCHOR_DOWN = 13;

    @Shadow @Final private OverlayRecipeComponent overlay;
    @Shadow @Final private List<RecipeButton> buttons;
    @Shadow private Minecraft minecraft;
    @Shadow private boolean isFiltering;

    @Unique private final RecipeGroupHover<RecipeButton> playersVersus$hover = new RecipeGroupHover<>();

    @ModifyVariable(method = "updateCollections", at = @At("HEAD"), argsOnly = true)
    private List<RecipeCollection> playersVersus$orderGroups(List<RecipeCollection> collections) {
        if (!VersusSettings.QOL.DO_SMART_RECIPE_ORDER || this.minecraft == null || this.minecraft.level == null) return collections;
        return RecipeBookSorting.sortCollections(collections, SlotDisplayContext.fromLevel(this.minecraft.level));
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void playersVersus$clickThrough(MouseButtonEvent event, int x, int y, int width, int height, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!VersusSettings.QOL.DO_RECIPE_BOOK_CLICK_THROUGH || !this.overlay.isVisible()) return;
        if (((PopupBounds) (Object) this.overlay).playersVersus$popupContains(event.x(), event.y())) return;
        // Closed here, vanilla goes on as if no popup was open: the click reaches whatever is under it
        this.overlay.setVisible(false);
        this.playersVersus$hover.suppress(this.playersVersus$buttonAt(event.x(), event.y()));
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void playersVersus$hoverGroups(GuiGraphicsExtractor graphics, int x, int y, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!VersusSettings.QOL.DO_RECIPE_BOOK_HOVER_GROUPS || this.minecraft == null || this.minecraft.level == null) return;
        RecipeButton hovered = this.playersVersus$buttonAt(mouseX, mouseY);
        if (hovered != null && hovered.isOnlyOption()) hovered = null;
        boolean open = this.overlay.isVisible();
        boolean overOpenGroup = open && (((PopupBounds) (Object) this.overlay).playersVersus$popupContains(mouseX, mouseY) || this.playersVersus$isOverOpenGroupsButton(mouseX, mouseY));
        switch (this.playersVersus$hover.update(hovered, open, overOpenGroup, Util.getMillis())) {
            case CLOSE -> this.overlay.setVisible(false);
            case OPEN -> this.overlay.init(hovered.getCollection(), SlotDisplayContext.fromLevel(this.minecraft.level), this.isFiltering,
                    hovered.getX(), hovered.getY(), x + RecipeBookComponent.IMAGE_WIDTH / 2, y + ANCHOR_DOWN + RecipeBookComponent.IMAGE_HEIGHT / 2, (float) hovered.getWidth());
            case NONE -> {}
        }
    }

    @Unique
    @Nullable
    private RecipeButton playersVersus$buttonAt(double mouseX, double mouseY) {
        for (RecipeButton button : this.buttons) {
            if (button.isVisible() && button.isMouseOver(mouseX, mouseY)) return button;
        }
        return null;
    }

    @Unique
    private boolean playersVersus$isOverOpenGroupsButton(double mouseX, double mouseY) {
        RecipeButton under = this.playersVersus$buttonAt(mouseX, mouseY);
        return under != null && under.getCollection() == this.overlay.getRecipeCollection();
    }
}
