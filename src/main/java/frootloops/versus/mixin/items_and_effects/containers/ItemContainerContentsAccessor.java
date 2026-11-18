package frootloops.versus.mixin.items_and_effects.containers;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.Optional;

@Mixin(ItemContainerContents.class)
public interface ItemContainerContentsAccessor {

    @Invoker("<init>")
    static ItemContainerContents playersVersus$create(List<Optional<ItemStackTemplate>> items) {
        throw new AssertionError();
    }
}
