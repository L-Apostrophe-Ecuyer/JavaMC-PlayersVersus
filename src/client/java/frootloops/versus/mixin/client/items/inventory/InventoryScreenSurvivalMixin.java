package frootloops.versus.mixin.client.items.inventory;

import frootloops.versus.mod.items.inventory.HotbarCycling;
import frootloops.versus.mod.items.inventory.InventorySorting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Environment(EnvType.CLIENT)
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenSurvivalMixin extends RecipeBookScreen<PlayerScreenHandler> {

    private TexturedButtonWidget buttonHotbarSwap = null;
    private TexturedButtonWidget buttonSortInventory = null;

    public InventoryScreenSurvivalMixin(PlayerScreenHandler handler, RecipeBookWidget<?> recipeBook, PlayerInventory inventory, Text title) {
        super(handler, recipeBook, inventory, title);
    }

    @Inject(method = "init",at = @At("TAIL"), cancellable = false)
    private void addInventoryButtons(CallbackInfo info) {
        this.buttonHotbarSwap = new TexturedButtonWidget(this.x + 104 + 22, this.height / 2 - 22, 20, 18, InventorySorting.TEXTURE_HOTBAR_SWAP_BUTTON, button -> {
            HotbarCycling.doHotbarSwap(client, client.player.getInventory());
            if(buttonHotbarSwap != null) buttonHotbarSwap.setFocused(false);
        });

        this.buttonSortInventory = new TexturedButtonWidget(this.x + 104 + 44, this.height / 2 - 22, 20, 18, InventorySorting.TEXTURE_INVENTORY_SORT_BUTTON, button -> {
            if(client.player != null) InventorySorting.sortInventory(this.handler, client, client.player.getInventory(), InventorySorting.InventoryToSort.SURVIVAL_INVENTORY);
            if(buttonSortInventory != null) buttonSortInventory.setFocused(false);
        });

        this.addDrawableChild(buttonHotbarSwap);
        this.addDrawableChild(buttonSortInventory);
    }

    @Inject(method = "mouseReleased",at = @At("RETURN"), cancellable = false)
    public void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable info) {
        if(button == 2) {
            HotbarCycling.doHotbarSwap(client, client.player.getInventory());
        }
        else {
            int posButtonsMinY = this.height / 2 - 22 - 1;
            if (mouseY > posButtonsMinY && mouseY < posButtonsMinY + 18 + 1) {
                if(buttonHotbarSwap != null) {
                    buttonHotbarSwap.setPosition(this.x + 104 + 22, this.height / 2 - 22);
                    buttonHotbarSwap.setFocused(false);
                }
                if(buttonHotbarSwap != null) {
                    buttonSortInventory.setPosition(this.x + 104 + 44, this.height / 2 - 22);
                    buttonSortInventory.setFocused(false);
                }
            }
        }
    }
}
