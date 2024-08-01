package frootloops.versus.mixin.client.items.inventory;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.items.inventory.InventorySorting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.search.SearchManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(CreativeInventoryScreen.class)
public abstract class InventoryScreenCreativeMixin extends AbstractInventoryScreen<CreativeInventoryScreen.CreativeScreenHandler>  {

    @Shadow private static ItemGroup selectedTab;

    private static final ButtonTextures TEXTURE_HOTBAR_SWAP_BUTTON = new ButtonTextures(Identifier.of("players-versus", "container/hotbar_swap_down"), Identifier.of("players-versus", "container/hotbar_swap_down_highlighted"));
    private static final ButtonTextures TEXTURE_SORT_BUTTON = new ButtonTextures(Identifier.of("players-versus", "container/sort_inventory"), Identifier.of("players-versus", "container/sort_inventory_highlighted"));

    private TexturedButtonWidget buttonHotbarSwap = null;
    private TexturedButtonWidget buttonSortInventory = null;
    private boolean areButtonsVisible = false;

    public InventoryScreenCreativeMixin(CreativeInventoryScreen.CreativeScreenHandler screenHandler, PlayerInventory playerInventory, Text text) {
        super(screenHandler, playerInventory, text);
    }

    @Inject(method = "init",at = @At("TAIL"), cancellable = false)
    private void addInventoryButtons(CallbackInfo info) {
        if (this.client.interactionManager.hasCreativeInventory()) {
            this.buttonHotbarSwap = new TexturedButtonWidget(this.x + 104 + 24, this.height / 2 - 36, 20, 18, TEXTURE_HOTBAR_SWAP_BUTTON, button -> {
                InventorySorting.doHotbarSwap(client, client.player.getInventory());
                if(buttonHotbarSwap != null) buttonHotbarSwap.setFocused(false);
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

    @Inject(method = "mouseClicked",at = @At("RETURN"), cancellable = false)
    public void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable info) {
        if (selectedTab.getType() == ItemGroup.Type.INVENTORY && button == 2 && this.handler.getCursorStack().isEmpty()) {
            InventorySorting.doHotbarSwap(client, client.player.getInventory());
        }
    }

    private void updateButtonVisibility() {
        if (selectedTab.getType() == ItemGroup.Type.INVENTORY) {
            if(buttonHotbarSwap != null && !areButtonsVisible) {
                this.addDrawableChild(buttonHotbarSwap);
                areButtonsVisible = true;
            }
        }
        else {
            if(buttonHotbarSwap != null && areButtonsVisible) {
                this.remove(buttonHotbarSwap);
                areButtonsVisible = false;
            }
        }
    }
}
