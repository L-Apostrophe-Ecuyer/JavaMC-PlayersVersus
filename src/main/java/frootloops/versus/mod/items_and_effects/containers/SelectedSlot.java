package frootloops.versus.mod.items_and_effects.containers;

import net.minecraft.world.item.component.ItemContainerContents;

/**
 * The slot selected in a shulker box's contents (ItemContainerContents, by a mixin), kept as vanilla keeps a bundle's
 * selected item: on the contents of each side's copy of the box, neither saved nor sent nor compared.
 */
public interface SelectedSlot {

    int playersVersus$selected();

    /** Whether this slot of the contents holds an item. */
    boolean playersVersus$holds(int slot);

    /** The same contents, this slot selected (-1 for none). */
    ItemContainerContents playersVersus$withSelected(int slot);

    static SelectedSlot of(ItemContainerContents contents) {
        return (SelectedSlot) (Object) contents;
    }
}
