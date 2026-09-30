package frootloops.versus.mixin.client.items_and_effects.inventory;

import frootloops.versus.mod.items_and_effects.CustomBrewingItems;
import frootloops.versus.mod.items_and_effects.inventory.HotbarCycling;
import frootloops.versus.mod.items_and_effects.inventory.InventorySorting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.creativetab.v1.FabricCreativeModeInventoryScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(CreativeModeInventoryScreen.class)
public abstract class InventoryScreenCreativeMixin  extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> implements FabricCreativeModeInventoryScreen {

    @Shadow private static CreativeModeTab selectedTab;

    private ImageButton buttonHotbarSwap = null;
    private ImageButton buttonSortInventory = null;
    private boolean areButtonsVisible = false;

    public InventoryScreenCreativeMixin(CreativeModeInventoryScreen.ItemPickerMenu screenHandler, Inventory playerInventory, Component text) {
        super(screenHandler, playerInventory, text);
    }

    private static final Item[] itemArray = new Item[]{
            Items.QUARTZ_BLOCK, Items.CALCITE, Items.CLAY, Items.ANDESITE, Items.STONE, Items.COBBLESTONE, Items.TUFF, Items.DEEPSLATE, Items.BLACKSTONE,
            Items.WOOL.red(), Items.WOOL.orange(), Items.WOOL.yellow(), Items.WOOL.lime(), Items.WOOL.cyan(), Items.WOOL.blue(), Items.WOOL.purple(), Items.WOOL.pink(), Items.WOOL.magenta(),
            Items.DYED_TERRACOTTA.red(), Items.DYED_TERRACOTTA.orange(), Items.DYED_TERRACOTTA.yellow(), Items.DYED_TERRACOTTA.lime(), Items.DYED_TERRACOTTA.cyan(), Items.DYED_TERRACOTTA.blue(), Items.DYED_TERRACOTTA.purple(), Items.DYED_TERRACOTTA.pink(), Items.DYED_TERRACOTTA.magenta(),
            Items.APPLE, Items.GOLDEN_APPLE, Items.CARROT, Items.GOLDEN_CARROT, Items.BEETROOT, CustomBrewingItems.GLISTERING_BEETROOT, Items.MELON_SLICE, Items.GLISTERING_MELON_SLICE, Items.GOLD_INGOT};

    @Inject(method = "init",at = @At("TAIL"), cancellable = false)
    private void addInventoryButtons(CallbackInfo info) {
        if (this.minecraft.gui.screen() instanceof CreativeModeInventoryScreen) {

            this.buttonHotbarSwap = new ImageButton(this.leftPos + 104 + 24, this.height / 2 - 36, 20, 18, InventorySorting.TEXTURE_HOTBAR_SWAP_BUTTON, button -> {
                HotbarCycling.doHotbarSwap(minecraft.player.getInventory());
                if(buttonHotbarSwap != null) buttonHotbarSwap.setFocused(false);
            });

            this.buttonSortInventory = new ImageButton(this.leftPos + 104 + 46, this.height / 2 - 36, 20, 18, InventorySorting.TEXTURE_INVENTORY_SORT_BUTTON, button -> {
                if(minecraft.player != null) InventorySorting.sortInventory(this.menu, minecraft, minecraft.player.getInventory(), InventorySorting.InventoryToSort.CREATIVE_INVENTORY);
                if(buttonSortInventory != null) buttonSortInventory.setFocused(false);
            });
        }
    }

    @Inject(method = "tryRebuildTabContents",at = @At("RETURN"), cancellable = false)
    private void populateDisplay(CallbackInfoReturnable cir) {
        this.updateButtonVisibility();
    }



    @Inject(method = "selectTab",at = @At("TAIL"), cancellable = false)
    private void addInventoryButtons(CreativeModeTab group, CallbackInfo info) {
        this.updateButtonVisibility();
    }

    @Inject(method = "mouseReleased",at = @At("RETURN"), cancellable = false)
    public void mouseClicked(MouseButtonEvent click, CallbackInfoReturnable info) {
        if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY && click.button() == 2 && this.menu.getCarried().isEmpty()) {
            HotbarCycling.doHotbarSwap(minecraft.player.getInventory());
        }
    }

    private void updateButtonVisibility() {
        if (selectedTab.getType() == CreativeModeTab.Type.INVENTORY) {
            if(buttonHotbarSwap != null && !areButtonsVisible) {
                this.addRenderableWidget(buttonHotbarSwap);
                this.addRenderableWidget(buttonSortInventory);
                areButtonsVisible = true;
            }
        }
        else {
            if(buttonHotbarSwap != null && areButtonsVisible) {
                this.removeWidget(buttonHotbarSwap);
                this.removeWidget(buttonSortInventory);
                areButtonsVisible = false;
            }
        }
    }
}
