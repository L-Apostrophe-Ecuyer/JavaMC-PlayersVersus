package frootloops.versus.mixin.items.inventory;

import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractInventoryScreen<PlayerScreenHandler>  implements RecipeBookProvider {
    public InventoryScreenMixin(PlayerScreenHandler screenHandler, PlayerInventory playerInventory, Text text, boolean mouseDown) {
        super(screenHandler, playerInventory, text);
    }
    private static final Identifier HOTBAR_BUTTON_TEXTURE = new Identifier("players-versus", "textures/gui/sprites/container/hotbar_swap_down_combined.png");

    private TexturedButtonWidget buttonHotbarSwap = null;

    @Inject(method = "init",at = @At("TAIL"), cancellable = false)
    private void addInventoryButtons(CallbackInfo info) {
        this.buttonHotbarSwap = new TexturedButtonWidget(this.x + 104 + 22, this.height / 2 - 22, 20, 18, 0, 0, 18, HOTBAR_BUTTON_TEXTURE, button -> {
            PlayerInventory playerInventory = client.player.getInventory();
            for (int i = 0; i < 9; i++) {
                swapItemsFromSlots(playerInventory, i, i + 9);
                swapItemsFromSlots(playerInventory, i, i + 18);
                swapItemsFromSlots(playerInventory, i, i + 27);
            }
        });
        this.addDrawableChild(buttonHotbarSwap);
    }

    private void swapItemsFromSlots(PlayerInventory inventory, int hotbarSlot, int slotTwo) {
        ItemStack stackOne = inventory.getStack(hotbarSlot);
        ItemStack stackTwo = inventory.getStack(slotTwo);
        if(stackOne.isEmpty() && stackTwo.isEmpty()) return;
        client.interactionManager.clickSlot(0, slotTwo, hotbarSlot, SlotActionType.SWAP, client.player);
    }

    @Inject(method = "mouseClicked",at = @At("RETURN"), cancellable = false)
    public void mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable info) {
        int posButtonsMinY = this.height/2 - 22 - 1;
        if(buttonHotbarSwap != null && mouseY > posButtonsMinY && mouseY < posButtonsMinY + 18 + 1) {
            buttonHotbarSwap.setPosition(this.x + 104 + 22, this.height / 2 - 22);
            buttonHotbarSwap.setFocused(false);
        }
    }
}
