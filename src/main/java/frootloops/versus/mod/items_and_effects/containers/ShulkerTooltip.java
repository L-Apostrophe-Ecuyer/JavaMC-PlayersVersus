package frootloops.versus.mod.items_and_effects.containers;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** A shulker box's tooltip image: its 27 slots in order, empty ones empty, the slot selected (-1 for none), the box's colour (RGB). */
public record ShulkerTooltip(List<ItemStack> slots, int selected, int color) implements TooltipComponent {
}
