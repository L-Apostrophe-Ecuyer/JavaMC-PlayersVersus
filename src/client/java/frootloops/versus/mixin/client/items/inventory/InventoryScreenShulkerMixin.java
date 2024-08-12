package frootloops.versus.mixin.client.items.inventory;

import frootloops.versus.mod.items.inventory.InventorySorting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ShulkerBoxScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;


@Environment(EnvType.CLIENT)
@Mixin(ShulkerBoxScreen.class)
public abstract class InventoryScreenShulkerMixin extends HandledScreen<ShulkerBoxScreenHandler> {

    private TexturedButtonWidget buttonSortShulker = null;
    private TexturedButtonWidget buttonSortInventory = null;

    public InventoryScreenShulkerMixin(ShulkerBoxScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();

        this.buttonSortShulker = new TexturedButtonWidget(this.x + 155, this.y + 5, 13, 11, InventorySorting.TEXTURE_SHULKER_SORT_BUTTON, button -> {
            if(client.player != null) InventorySorting.sortInventory(this.handler, client,  ((InventoryScreenShulkerAccessor)this.handler).getInventory(), InventorySorting.InventoryToSort.CONTAINER_INVENTORY, 0, 27);
            if(buttonSortShulker != null) buttonSortShulker.setFocused(false);
        });

        this.buttonSortInventory = new TexturedButtonWidget(this.x + 155, this.y + 20 + (17 * 3), 13, 11, InventorySorting.TEXTURE_SMALL_INVENTORY_SORT_BUTTON, button -> {
            if(client.player != null) InventorySorting.sortInventory(this.handler, client, client.player.getInventory(), InventorySorting.InventoryToSort.INVENTORY_WHILE_CHEST_OPEN, 27, 36);
            if(buttonSortInventory != null) buttonSortInventory.setFocused(false);
        });

        this.addDrawableChild(buttonSortShulker);
        this.addDrawableChild(buttonSortInventory);
    }
}
