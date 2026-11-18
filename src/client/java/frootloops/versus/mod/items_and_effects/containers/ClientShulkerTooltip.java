package frootloops.versus.mod.items_and_effects.containers;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

/** A shulker box's tooltip image: its slots (ShulkerGrid) in the box's colour, the selected item named above, as the bundle's is. */
public class ClientShulkerTooltip implements ClientTooltipComponent {

    private static final int MARGIN_Y = 4;

    private final ShulkerTooltip tooltip;

    public ClientShulkerTooltip(ShulkerTooltip tooltip) {
        this.tooltip = tooltip;
    }

    @Override
    public int getHeight(Font font) {
        return ShulkerGrid.HEIGHT + MARGIN_Y;
    }

    @Override
    public int getWidth(Font font) {
        return ShulkerGrid.WIDTH;
    }

    @Override
    public boolean showTooltipWithItemInHand() {
        return true;
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        ShulkerGrid.draw(graphics, font, x + (w - ShulkerGrid.WIDTH) / 2, y, this.tooltip.slots(), this.tooltip.selected(), this.tooltip.color());
        ShulkerGrid.selectedName(graphics, font, this.tooltip.slots(), this.tooltip.selected(), x + w / 2, y);
    }
}
