package frootloops.versus.mixin.client.items_and_effects.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShulkerBoxMenu.class)
public interface InventoryScreenShulkerAccessor {

    @Accessor
    Container getContainer();
}