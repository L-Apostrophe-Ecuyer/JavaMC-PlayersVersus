package frootloops.versus.mixin.items_and_effects.containers;

import frootloops.versus.mod.items_and_effects.containers.SelectedSlot;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.Optional;

/** A shulker box's selected slot, on its contents, as BundleContents keeps a bundle's (not saved, sent or compared). */
@Mixin(ItemContainerContents.class)
public abstract class ItemContainerContentsMixin implements SelectedSlot {

    @Shadow
    @Final
    private List<Optional<ItemStackTemplate>> items;

    @Unique
    private int playersVersus$selected = -1;

    @Override
    public int playersVersus$selected() {
        return this.playersVersus$selected;
    }

    @Override
    public boolean playersVersus$holds(int slot) {
        return slot >= 0 && slot < this.items.size() && this.items.get(slot).isPresent();
    }

    @Override
    public ItemContainerContents playersVersus$withSelected(int slot) {
        ItemContainerContents copy = ItemContainerContentsAccessor.playersVersus$create(this.items);
        ((ItemContainerContentsMixin) (Object) copy).playersVersus$selected = slot;
        return copy;
    }
}
