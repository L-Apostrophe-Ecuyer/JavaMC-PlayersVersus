package frootloops.versus.mixin.client.items.inventory;

import frootloops.versus.mod.items.inventory.HotbarCycling;
import frootloops.versus.mod.items.inventory.InventorySorting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.ScreenPos;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Environment(EnvType.CLIENT)
@Mixin(RecipeBookScreen.class)
public abstract class InventoryScreenSurvivalMixin<T extends AbstractRecipeScreenHandler> extends HandledScreen<T> implements RecipeBookProvider {

    private TexturedButtonWidget buttonHotbarSwap = null;
    private TexturedButtonWidget buttonSortInventory = null;

    @Shadow
    protected abstract ScreenPos getRecipeBookButtonPos();

    public InventoryScreenSurvivalMixin(T handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }


    @Inject(method = "init", at = @At("TAIL"), cancellable = false)
    private void addInventoryButtons(CallbackInfo info) {

        this.buttonHotbarSwap = new TexturedButtonWidget(this.x + 104 + 22, this.height / 2 - 22, 20, 18, InventorySorting.TEXTURE_HOTBAR_SWAP_BUTTON, button -> {
            HotbarCycling.doHotbarSwap(client.player.getInventory());
            if (buttonHotbarSwap != null) buttonHotbarSwap.setFocused(false);
        });

        this.buttonSortInventory = new TexturedButtonWidget(buttonHotbarSwap.getX() + 22, buttonHotbarSwap.getY(), 20, 18, InventorySorting.TEXTURE_INVENTORY_SORT_BUTTON, button -> {
            if (client.player != null)
                InventorySorting.sortInventory(this.handler, client, client.player.getInventory(), InventorySorting.InventoryToSort.SURVIVAL_INVENTORY);
            if (buttonSortInventory != null) buttonSortInventory.setFocused(false);
        });

        this.addDrawableChild(buttonHotbarSwap);
        this.addDrawableChild(buttonSortInventory);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if(button == 2) {
            HotbarCycling.doHotbarSwap(client.player.getInventory());
        }
        else {
            ScreenPos recipeBookButtonPos = this.getRecipeBookButtonPos();
            boolean clickedOnRecipeBook = (mouseY >= recipeBookButtonPos.y() && mouseY <= recipeBookButtonPos.y() + 18) && (mouseX >= recipeBookButtonPos.x() && mouseX <= recipeBookButtonPos.x() + 20);
            if (clickedOnRecipeBook) {
                if(buttonHotbarSwap != null) {
                    buttonHotbarSwap.setPosition(this.x + 104 + 22, this.height / 2 - 22);
                    buttonHotbarSwap.setFocused(false);
                }
                if(buttonSortInventory != null) {
                    buttonSortInventory.setPosition(buttonSortInventory == null ? this.x + 104 + 22 : buttonHotbarSwap.getX() + 22, this.height / 2 - 22);
                    buttonSortInventory.setFocused(false);
                }
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Inject(method = "mouseClicked",at = @At("RETURN"), cancellable = false)
    public void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable info) {

    }
}
