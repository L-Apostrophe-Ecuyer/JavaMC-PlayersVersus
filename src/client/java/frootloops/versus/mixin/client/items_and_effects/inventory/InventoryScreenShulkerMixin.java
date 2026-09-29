package frootloops.versus.mixin.client.items_and_effects.inventory;

import frootloops.versus.mod.items_and_effects.inventory.ContainerDumping;
import frootloops.versus.mod.items_and_effects.inventory.InventorySorting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import org.spongepowered.asm.mixin.Mixin;


@Environment(EnvType.CLIENT)
@Mixin(ShulkerBoxScreen.class)
public abstract class InventoryScreenShulkerMixin extends AbstractContainerScreen<ShulkerBoxMenu> {

    private ImageButton buttonSortShulker = null;
    private ImageButton buttonSortInventory = null;
    private ImageButton buttonQuickMoveToStorage = null;
    private ImageButton buttonQuickMoveToPlayer = null;

    public InventoryScreenShulkerMixin(ShulkerBoxMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();

        this.buttonSortShulker = new ImageButton(this.leftPos + 155, this.topPos + 5, 13, 11, InventorySorting.TEXTURE_SHULKER_SORT_BUTTON, button -> {
            if(minecraft.player != null) InventorySorting.sortInventory(this.menu, minecraft,  ((InventoryScreenShulkerAccessor)this.menu).getContainer(), InventorySorting.InventoryToSort.CONTAINER_INVENTORY, 0, 27);
            if(buttonSortShulker != null) buttonSortShulker.setFocused(false);
        });

        this.buttonSortInventory = new ImageButton(this.leftPos + 155, this.topPos + 20 + (17 * 3), 13, 11, InventorySorting.TEXTURE_SMALL_INVENTORY_SORT_BUTTON, button -> {
            if(minecraft.player != null) InventorySorting.sortInventory(this.menu, minecraft, minecraft.player.getInventory(), InventorySorting.InventoryToSort.INVENTORY_WHITH_SLOTS_ABOVE, 27, 36);
            if(buttonSortInventory != null) buttonSortInventory.setFocused(false);
        });

        this.buttonQuickMoveToPlayer = new ImageButton(buttonSortInventory.getX() - 15, buttonSortInventory.getY(), 13, 11, ContainerDumping.TEXTURE_DUMP_TO_PLAYER_BUTTON, button -> {
            if(minecraft.player != null) ContainerDumping.quickDumpIntoPlayerInventory(this.menu, minecraft, minecraft.player.getInventory(), ((InventoryScreenShulkerAccessor)this.menu).getContainer());
            if(buttonQuickMoveToPlayer != null) buttonQuickMoveToPlayer.setFocused(false);
        });

        this.buttonQuickMoveToStorage = new ImageButton(buttonQuickMoveToPlayer.getX() - 15, buttonSortInventory.getY(), 13, 11, ContainerDumping.TEXTURE_DUMP_TO_STORAGE_BUTTON, button -> {
            if(minecraft.player != null) ContainerDumping.quickDumpIntoContainer(this.menu, minecraft, minecraft.player.getInventory(), ((InventoryScreenShulkerAccessor)this.menu).getContainer());
            if(buttonQuickMoveToPlayer != null) buttonQuickMoveToPlayer.setFocused(false);
        });

        this.addRenderableWidget(buttonSortShulker);
        this.addRenderableWidget(buttonSortInventory);
        this.addRenderableWidget(buttonQuickMoveToPlayer);
        this.addRenderableWidget(buttonQuickMoveToStorage);
    }
}
