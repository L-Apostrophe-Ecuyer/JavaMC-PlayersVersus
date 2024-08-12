package frootloops.versus.mixin.client.items.inventory;

import frootloops.versus.mod.items.inventory.ContainerDumping;
import frootloops.versus.mod.items.inventory.InventorySorting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.*;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;


@Environment(EnvType.CLIENT)
@Mixin(GenericContainerScreen.class)
public abstract class InventoryScreenChestMixin extends HandledScreen<GenericContainerScreenHandler> implements ScreenHandlerProvider<GenericContainerScreenHandler> {

    private TexturedButtonWidget buttonSortChest = null;
    private TexturedButtonWidget buttonSortInventory = null;

    private TexturedButtonWidget buttonQuickMoveToStorage = null;
    private TexturedButtonWidget buttonQuickMoveToPlayer = null;

    public InventoryScreenChestMixin(GenericContainerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();

        this.buttonSortChest = new TexturedButtonWidget(this.x + 155, this.y + 5, 13, 11, InventorySorting.TEXTURE_CHEST_SORT_BUTTON, button -> {
            if(client.player != null) InventorySorting.sortInventory(this.handler, client,  this.handler.getInventory(), InventorySorting.InventoryToSort.CONTAINER_INVENTORY, 0, handler.getRows() * 9);
            if(buttonSortChest != null) buttonSortChest.setFocused(false);
        });

        this.buttonSortInventory = new TexturedButtonWidget(this.x + 155, this.y + 21 + (17 * handler.getRows()) + (handler.getRows() > 4 ? 3 : 0), 13, 11, InventorySorting.TEXTURE_SMALL_INVENTORY_SORT_BUTTON, button -> {
            if(client.player != null) InventorySorting.sortInventory(this.handler, client, client.player.getInventory(), InventorySorting.InventoryToSort.INVENTORY_WHILE_CHEST_OPEN, handler.getRows() * 9, 36);
            if(buttonSortInventory != null) buttonSortInventory.setFocused(false);
        });

        this.buttonQuickMoveToPlayer = new TexturedButtonWidget(buttonSortInventory.getX() - 15, buttonSortInventory.getY(), 13, 11, ContainerDumping.TEXTURE_DUMP_TO_PLAYER_BUTTON, button -> {
            if(client.player != null) ContainerDumping.quickDumpIntoPlayerInventory(this.handler, client, client.player.getInventory(), ((InventoryScreenShulkerAccessor)this.handler).getInventory());
            if(buttonQuickMoveToPlayer != null) buttonQuickMoveToPlayer.setFocused(false);
        });

        this.buttonQuickMoveToStorage = new TexturedButtonWidget(buttonSortChest.getX() - 15, buttonSortChest.getY(), 13, 11, ContainerDumping.TEXTURE_DUMP_TO_STORAGE_BUTTON, button -> {
            if(client.player != null) ContainerDumping.quickDumpIntoContainer(this.handler, client, client.player.getInventory(), ((InventoryScreenShulkerAccessor)this.handler).getInventory());
            if(buttonQuickMoveToPlayer != null) buttonQuickMoveToPlayer.setFocused(false);
        });

        this.addDrawableChild(buttonSortChest);
        this.addDrawableChild(buttonSortInventory);
        this.addDrawableChild(buttonQuickMoveToPlayer);
        this.addDrawableChild(buttonQuickMoveToStorage);
    }
}
