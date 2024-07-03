package frootloops.versus.mixin.client.items.inventory;

import frootloops.versus.mod.items.inventory.InventorySorting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenSurvivalMixin extends AbstractInventoryScreen<PlayerScreenHandler>  implements RecipeBookProvider {
    public InventoryScreenSurvivalMixin(PlayerScreenHandler screenHandler, PlayerInventory playerInventory, Text text, boolean mouseDown, RecipeBookWidget recipeBook) {
        super(screenHandler, playerInventory, text);
        this.recipeBook = recipeBook;
    }

    private static final ButtonTextures TEXTURE_HOTBAR_SWAP_BUTTON = new ButtonTextures(Identifier.of("players-versus", "container/hotbar_swap_down"), Identifier.of("players-versus", "container/hotbar_swap_down_highlighted"));
    private static final ButtonTextures TEXTURE_SORT_BUTTON = new ButtonTextures(Identifier.of("players-versus", "container/sort_inventory"), Identifier.of("players-versus", "container/sort_inventory_highlighted"));

    @Shadow private final RecipeBookWidget recipeBook;

    private TexturedButtonWidget buttonHotbarSwap = null;
    private TexturedButtonWidget buttonSortInventory = null;

    @Inject(method = "init",at = @At("TAIL"), cancellable = false)
    private void addInventoryButtons(CallbackInfo info) {
        this.buttonHotbarSwap = new TexturedButtonWidget(this.x + 104 + 22, this.height / 2 - 22, 20, 18, TEXTURE_HOTBAR_SWAP_BUTTON, button -> {
            InventorySorting.doHotbarSwap(client, client.player.getInventory());
            if(buttonHotbarSwap != null) buttonHotbarSwap.setFocused(false);
        });
        this.addDrawableChild(buttonHotbarSwap);
    }

    @Inject(method = "mouseClicked",at = @At("RETURN"), cancellable = false)
    public void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable info) {
        if(button == 2) {
            InventorySorting.doHotbarSwap(client, client.player.getInventory());
        }
        else {
            int posButtonsMinY = this.height / 2 - 22 - 1;
            if (buttonHotbarSwap != null && mouseY > posButtonsMinY && mouseY < posButtonsMinY + 18 + 1) {
                buttonHotbarSwap.setPosition(this.x + 104 + 22, this.height / 2 - 22);
                buttonHotbarSwap.setFocused(false);
            }
        }
    }
}
