package frootloops.versus.mod.items_and_effects.containers;

import com.mojang.serialization.DataResult;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Vanilla's bundle tooltip (ClientBundleTooltip) at 5 columns and up to 4 rows (BundleGrid): the same sprites, the same
 * order (the newest stack top left, the "+N" cell bottom right), the fullness bar as wide as the grid.
 */
public class WideBundleTooltip implements ClientTooltipComponent {

    private static final Identifier PROGRESSBAR_BORDER_SPRITE = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_border");
    private static final Identifier PROGRESSBAR_FILL_SPRITE = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_fill");
    private static final Identifier PROGRESSBAR_FULL_SPRITE = Identifier.withDefaultNamespace("container/bundle/bundle_progressbar_full");
    private static final Identifier SLOT_HIGHLIGHT_BACK_SPRITE = Identifier.withDefaultNamespace("container/bundle/slot_highlight_back");
    private static final Identifier SLOT_HIGHLIGHT_FRONT_SPRITE = Identifier.withDefaultNamespace("container/bundle/slot_highlight_front");
    private static final Identifier SLOT_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("container/bundle/slot_background");
    private static final int SLOT_SIZE = 24, GRID_WIDTH = BundleGrid.COLUMNS * SLOT_SIZE;
    private static final int PROGRESSBAR_HEIGHT = 13, PROGRESSBAR_FILL_MAX = GRID_WIDTH - 2, PROGRESSBAR_MARGIN_Y = 4;
    private static final Component BUNDLE_FULL_TEXT = Component.translatable("item.minecraft.bundle.full");
    private static final Component BUNDLE_EMPTY_TEXT = Component.translatable("item.minecraft.bundle.empty");
    private static final Component BUNDLE_EMPTY_DESCRIPTION = Component.translatable("item.minecraft.bundle.empty.description");

    private final BundleContents contents;

    public WideBundleTooltip(BundleContents contents) {
        this.contents = contents;
    }

    @Override
    public int getHeight(Font font) {
        return this.contents.isEmpty() ? emptyDescriptionHeight(font) + PROGRESSBAR_HEIGHT + 2 * PROGRESSBAR_MARGIN_Y
                : this.gridHeight() + PROGRESSBAR_HEIGHT + 2 * PROGRESSBAR_MARGIN_Y;
    }

    @Override
    public int getWidth(Font font) {
        return GRID_WIDTH;
    }

    @Override
    public boolean showTooltipWithItemInHand() {
        return true;
    }

    private int gridHeight() {
        return this.rows() * SLOT_SIZE;
    }

    private int rows() {
        return Mth.positiveCeilDiv(Math.min(BundleGrid.CELLS, this.contents.size()), BundleGrid.COLUMNS);
    }

    private static int contentOffset(int tooltipWidth) {
        return (tooltipWidth - GRID_WIDTH) / 2;
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        DataResult<Fraction> weight = this.contents.weight();
        if (weight.isError()) return;
        int left = x + contentOffset(w);
        if (this.contents.isEmpty()) {
            graphics.textWithWordWrap(font, BUNDLE_EMPTY_DESCRIPTION, left, y, GRID_WIDTH, -5592406);
            progressbar(left, y + emptyDescriptionHeight(font) + PROGRESSBAR_MARGIN_Y, font, graphics, Fraction.ZERO);
            return;
        }
        boolean overflowing = this.contents.size() > BundleGrid.CELLS;
        List<ItemStackTemplate> shown = this.contents.items().subList(0, Math.min(this.contents.size(), this.contents.getNumberOfItemsToShow()));
        int right = left + GRID_WIDTH, bottom = y + this.gridHeight();
        int slot = 1;
        for (int row = 1; row <= this.rows(); row++) {
            for (int column = 1; column <= BundleGrid.COLUMNS; column++) {
                int drawX = right - column * SLOT_SIZE, drawY = bottom - row * SLOT_SIZE;
                if (overflowing && row == 1 && column == 1) {
                    int hidden = this.contents.items().stream().skip(shown.size()).mapToInt(ItemInstance::count).sum();
                    graphics.centeredText(font, "+" + hidden, drawX + SLOT_SIZE / 2, drawY + 10, -1);
                } else if (shown.size() >= slot) {
                    this.slot(slot, drawX, drawY, shown, font, graphics);
                    slot++;
                }
            }
        }
        this.selectedName(font, graphics, x, y, w);
        progressbar(left, y + this.gridHeight() + PROGRESSBAR_MARGIN_Y, font, graphics, weight.getOrThrow());
    }

    private void slot(int slotNumber, int drawX, int drawY, List<ItemStackTemplate> shown, Font font, GuiGraphicsExtractor graphics) {
        int index = shown.size() - slotNumber;
        boolean selected = index == this.contents.getSelectedItemIndex();
        ItemStack item = shown.get(index).create();
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, selected ? SLOT_HIGHLIGHT_BACK_SPRITE : SLOT_BACKGROUND_SPRITE, drawX, drawY, SLOT_SIZE, SLOT_SIZE);
        graphics.item(item, drawX + 4, drawY + 4, slotNumber);
        graphics.itemDecorations(font, item, drawX + 4, drawY + 4);
        if (selected) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_HIGHLIGHT_FRONT_SPRITE, drawX, drawY, SLOT_SIZE, SLOT_SIZE);
    }

    private void selectedName(Font font, GuiGraphicsExtractor graphics, int x, int y, int w) {
        ItemStackTemplate selected = this.contents.getSelectedItem();
        if (selected == null) return;
        ItemStack stack = selected.create();
        Component name = stack.getStyledHoverName();
        int textWidth = font.width(name.getVisualOrderText());
        graphics.tooltip(font, List.of(ClientTooltipComponent.create(name.getVisualOrderText())), x + w / 2 - 12 - textWidth / 2, y - 15,
                DefaultTooltipPositioner.INSTANCE, stack.get(DataComponents.TOOLTIP_STYLE), false);
    }

    private static void progressbar(int x, int y, Font font, GuiGraphicsExtractor graphics, Fraction weight) {
        int fill = Math.clamp(Mth.mulAndTruncate(weight, PROGRESSBAR_FILL_MAX), 0, PROGRESSBAR_FILL_MAX);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, weight.compareTo(Fraction.ONE) >= 0 ? PROGRESSBAR_FULL_SPRITE : PROGRESSBAR_FILL_SPRITE, x + 1, y, fill, PROGRESSBAR_HEIGHT);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESSBAR_BORDER_SPRITE, x, y, GRID_WIDTH, PROGRESSBAR_HEIGHT);
        Component text = fillText(weight);
        if (text != null) graphics.centeredText(font, text, x + GRID_WIDTH / 2, y + 3, -1);
    }

    private static @Nullable Component fillText(Fraction weight) {
        if (weight.compareTo(Fraction.ZERO) == 0) return BUNDLE_EMPTY_TEXT;
        return weight.compareTo(Fraction.ONE) >= 0 ? BUNDLE_FULL_TEXT : null;
    }

    private static int emptyDescriptionHeight(Font font) {
        return font.split(BUNDLE_EMPTY_DESCRIPTION, GRID_WIDTH).size() * 9;
    }
}
