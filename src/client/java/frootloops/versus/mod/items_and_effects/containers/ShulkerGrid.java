package frootloops.versus.mod.items_and_effects.containers;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * A shulker box's 27 slots as a compact grid, 9 by 3 in the box's own order, in the box's colour (ShulkerColors): cells of
 * 9 pixels a pixel apart, in a one-pixel frame, with the items at half size (8 pixels). An empty cell is empty, the
 * selected one lightened and outlined.
 */
public final class ShulkerGrid {

    public static final int COLUMNS = 9, ROWS = 3, PITCH = 10, CELL = PITCH - 1;
    public static final int WIDTH = COLUMNS * PITCH + 1, HEIGHT = ROWS * PITCH + 1;
    private static final float ICON_SCALE = 0.5F;
    private static final int OUTLINE = 0xFFFFFFFF;

    private ShulkerGrid() {
    }

    /** The grid with its top left corner at x, y; color is the box's RGB (ShulkerColors.of). */
    public static void draw(GuiGraphicsExtractor graphics, Font font, int x, int y, List<ItemStack> slots, int selected, int color) {
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, ShulkerColors.frame(color));
        for (int slot = 0; slot < Math.min(slots.size(), COLUMNS * ROWS); slot++) {
            int cellX = x + 1 + slot % COLUMNS * PITCH, cellY = y + 1 + slot / COLUMNS * PITCH;
            boolean highlighted = slot == selected;
            graphics.fill(cellX, cellY, cellX + CELL, cellY + CELL, highlighted ? ShulkerColors.highlight(color) : ShulkerColors.cell(color));
            if (highlighted) graphics.outline(cellX, cellY, CELL, CELL, OUTLINE);
            ItemStack stack = slots.get(slot);
            if (stack.isEmpty()) continue;
            // The 16-pixel item at half size, in the middle of its cell
            graphics.pose().pushMatrix();
            graphics.pose().translate(cellX + (CELL - 16 * ICON_SCALE) / 2.0F, cellY + (CELL - 16 * ICON_SCALE) / 2.0F);
            graphics.pose().scale(ICON_SCALE, ICON_SCALE);
            graphics.item(stack, 0, 0, slot);
            graphics.itemDecorations(font, stack, 0, 0);
            graphics.pose().popMatrix();
        }
    }

    /** The selected item's name in a small tooltip of its own, centred on this x, its bottom just above this y. */
    public static void selectedName(GuiGraphicsExtractor graphics, Font font, List<ItemStack> slots, int selected, int centreX, int y) {
        if (selected < 0 || selected >= slots.size() || slots.get(selected).isEmpty()) return;
        ItemStack stack = slots.get(selected);
        Component name = stack.getStyledHoverName();
        int textWidth = font.width(name.getVisualOrderText());
        graphics.tooltip(font, List.of(ClientTooltipComponent.create(name.getVisualOrderText())), centreX - textWidth / 2, y - 15,
                DefaultTooltipPositioner.INSTANCE, stack.get(DataComponents.TOOLTIP_STYLE), false);
    }
}
