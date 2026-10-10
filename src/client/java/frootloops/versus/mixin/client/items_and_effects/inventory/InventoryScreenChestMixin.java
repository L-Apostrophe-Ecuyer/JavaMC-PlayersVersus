package frootloops.versus.mixin.client.items_and_effects.inventory;

import frootloops.versus.mod.items_and_effects.inventory.ContainerDumping;
import frootloops.versus.mod.items_and_effects.inventory.InventorySorting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import org.spongepowered.asm.mixin.Mixin;


@Environment(EnvType.CLIENT)
@Mixin(ContainerScreen.class)
public abstract class InventoryScreenChestMixin extends AbstractContainerScreen<ChestMenu> implements MenuAccess<ChestMenu> {

    private ImageButton buttonSortChest = null;
    private ImageButton buttonSortInventory = null;

    private ImageButton buttonQuickMoveToStorage = null;
    private ImageButton buttonQuickMoveToPlayer = null;

    public InventoryScreenChestMixin(ChestMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();

        this.buttonSortChest = new ImageButton(this.leftPos + 155, this.topPos + 5, 13, 11, InventorySorting.TEXTURE_CHEST_SORT_BUTTON, button -> {
            if(minecraft.player != null) InventorySorting.sortInventory(this.menu, minecraft,  this.menu.getContainer(), InventorySorting.InventoryToSort.CONTAINER_INVENTORY);
            if(buttonSortChest != null) buttonSortChest.setFocused(false);
        });

        this.buttonSortInventory = new ImageButton(this.leftPos + 155, this.topPos + 21 + (17 * menu.getRowCount()) + (menu.getRowCount() > 4 ? 3 : 0), 13, 11, InventorySorting.TEXTURE_SMALL_INVENTORY_SORT_BUTTON, button -> {
            if(minecraft.player != null) InventorySorting.sortInventory(this.menu, minecraft, minecraft.player.getInventory(), InventorySorting.InventoryToSort.PLAYER_INVENTORY);
            if(buttonSortInventory != null) buttonSortInventory.setFocused(false);
        });

        this.buttonQuickMoveToPlayer = new ImageButton(buttonSortInventory.getX() - 15, buttonSortInventory.getY(), 13, 11, ContainerDumping.TEXTURE_DUMP_TO_PLAYER_BUTTON, button -> {
            if(minecraft.player != null) ContainerDumping.quickDumpIntoPlayerInventory(this.menu, minecraft, minecraft.player.getInventory(), this.menu.getContainer());
            if(buttonQuickMoveToPlayer != null) buttonQuickMoveToPlayer.setFocused(false);
        });

        this.buttonQuickMoveToStorage = new ImageButton(buttonQuickMoveToPlayer.getX() - 15, buttonSortInventory.getY(), 13, 11, ContainerDumping.TEXTURE_DUMP_TO_STORAGE_BUTTON, button -> {
            if(minecraft.player != null) ContainerDumping.quickDumpIntoContainer(this.menu, minecraft, minecraft.player.getInventory(), this.menu.getContainer());
            if(buttonQuickMoveToPlayer != null) buttonQuickMoveToPlayer.setFocused(false);
        });

        this.addRenderableWidget(buttonSortChest);
        this.addRenderableWidget(buttonSortInventory);
        this.addRenderableWidget(buttonQuickMoveToPlayer);
        this.addRenderableWidget(buttonQuickMoveToStorage);
    }
}
