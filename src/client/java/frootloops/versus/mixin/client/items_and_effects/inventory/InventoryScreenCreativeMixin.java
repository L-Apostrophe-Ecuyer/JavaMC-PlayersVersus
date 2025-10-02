package frootloops.versus.mixin.client.items_and_effects.inventory;

import frootloops.versus.mod.items_and_effects.CustomBrewingItems;
import frootloops.versus.mod.items_and_effects.inventory.HotbarCycling;
import frootloops.versus.mod.items_and_effects.inventory.InventorySorting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.*;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(CreativeInventoryScreen.class)
public abstract class InventoryScreenCreativeMixin  extends HandledScreen<CreativeInventoryScreen.CreativeScreenHandler> implements FabricCreativeInventoryScreen {

    @Shadow private static ItemGroup selectedTab;

    private TexturedButtonWidget buttonHotbarSwap = null;
    private TexturedButtonWidget buttonSortInventory = null;
    private boolean areButtonsVisible = false;

    public InventoryScreenCreativeMixin(CreativeInventoryScreen.CreativeScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
        super(screenHandler, playerInventory, text);
    }

    private static final Item[] itemArray = new Item[]{
            Items.QUARTZ_BLOCK, Items.CALCITE, Items.CLAY, Items.ANDESITE, Items.STONE, Items.COBBLESTONE, Items.TUFF, Items.DEEPSLATE, Items.BLACKSTONE,
            Items.RED_WOOL, Items.ORANGE_WOOL, Items.YELLOW_WOOL, Items.LIME_WOOL, Items.CYAN_WOOL, Items.BLUE_WOOL, Items.PURPLE_WOOL, Items.PINK_WOOL, Items.MAGENTA_WOOL,
            Items.RED_TERRACOTTA, Items.ORANGE_TERRACOTTA, Items.YELLOW_TERRACOTTA, Items.LIME_TERRACOTTA, Items.CYAN_TERRACOTTA, Items.BLUE_TERRACOTTA, Items.PURPLE_TERRACOTTA, Items.PINK_TERRACOTTA, Items.MAGENTA_TERRACOTTA,
            Items.APPLE, Items.GOLDEN_APPLE, Items.CARROT, Items.GOLDEN_CARROT, Items.BEETROOT, CustomBrewingItems.GLISTERING_BEETROOT, Items.MELON_SLICE, Items.GLISTERING_MELON_SLICE, Items.GOLD_INGOT};

    @Inject(method = "init",at = @At("TAIL"), cancellable = false)
    private void addInventoryButtons(CallbackInfo info) {
        if (this.client.currentScreen instanceof CreativeInventoryScreen) {

            this.buttonHotbarSwap = new TexturedButtonWidget(this.x + 104 + 24, this.height / 2 - 36, 20, 18, InventorySorting.TEXTURE_HOTBAR_SWAP_BUTTON, button -> {
                HotbarCycling.doHotbarSwap(client.player.getInventory());
                if(buttonHotbarSwap != null) buttonHotbarSwap.setFocused(false);
            });

            this.buttonSortInventory = new TexturedButtonWidget(this.x + 104 + 46, this.height / 2 - 36, 20, 18, InventorySorting.TEXTURE_INVENTORY_SORT_BUTTON, button -> {
                if(client.player != null) InventorySorting.sortInventory(this.handler, client, client.player.getInventory(), InventorySorting.InventoryToSort.CREATIVE_INVENTORY);
                if(buttonSortInventory != null) buttonSortInventory.setFocused(false);
            });
        }
    }

    @Inject(method = "populateDisplay",at = @At("RETURN"), cancellable = false)
    private void populateDisplay(CallbackInfoReturnable cir) {
        this.updateButtonVisibility();
    }



    @Inject(method = "setSelectedTab",at = @At("TAIL"), cancellable = false)
    private void addInventoryButtons(ItemGroup group, CallbackInfo info) {
        this.updateButtonVisibility();
    }

    @Inject(method = "mouseReleased",at = @At("RETURN"), cancellable = false)
    public void mouseClicked(Click click, CallbackInfoReturnable info) {
        if (selectedTab.getType() == ItemGroup.Type.INVENTORY && click.button() == 2 && this.handler.getCursorStack().isEmpty()) {
            HotbarCycling.doHotbarSwap(client.player.getInventory());
        }
    }

    private void updateButtonVisibility() {
        if (selectedTab.getType() == ItemGroup.Type.INVENTORY) {
            if(buttonHotbarSwap != null && !areButtonsVisible) {
                this.addDrawableChild(buttonHotbarSwap);
                this.addDrawableChild(buttonSortInventory);
                areButtonsVisible = true;
            }
        }
        else {
            if(buttonHotbarSwap != null && areButtonsVisible) {
                this.remove(buttonHotbarSwap);
                this.remove(buttonSortInventory);
                areButtonsVisible = false;
            }
        }
    }
}
