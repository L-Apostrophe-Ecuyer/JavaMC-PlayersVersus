package frootloops.versus.mixin.client.items_and_effects.inventory;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.items_and_effects.inventory.HotbarCycling;
import frootloops.versus.mod.items_and_effects.inventory.InventorySorting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Environment(EnvType.CLIENT)
@Mixin(AbstractRecipeBookScreen.class)
public abstract class InventoryScreenSurvivalMixin<T extends RecipeBookMenu> extends AbstractContainerScreen<T> implements RecipeUpdateListener {

    private ImageButton buttonHotbarSwap = null;
    private ImageButton buttonSortInventory = null;

    @Shadow private final RecipeBookComponent<?> recipeBookComponent;
    @Shadow private boolean widthTooNarrow;

    @Shadow
    protected abstract ScreenPosition getRecipeBookButtonPosition();

    public InventoryScreenSurvivalMixin(T handler, Inventory inventory, Component title, RecipeBookComponent<?> recipeBook) {
        super(handler, inventory, title);
        this.recipeBookComponent = recipeBook;
    }


    @Inject(method = "init", at = @At("TAIL"), cancellable = false)
    private void addInventoryButtons(CallbackInfo info) {

        this.buttonHotbarSwap = new ImageButton(this.leftPos + 104 + 22, this.height / 2 - 22, 20, 18, InventorySorting.TEXTURE_HOTBAR_SWAP_BUTTON, button -> {
            HotbarCycling.doHotbarSwap(minecraft.player.getInventory());
            if (buttonHotbarSwap != null) buttonHotbarSwap.setFocused(false);
        });

        this.buttonSortInventory = new ImageButton(buttonHotbarSwap.getX() + 22, buttonHotbarSwap.getY(), 20, 18, InventorySorting.TEXTURE_INVENTORY_SORT_BUTTON, button -> {
            if (minecraft.player != null) {
                int numSlots = 36;
                int indexFirstRow = this.menu.findSlot(minecraft.player.getInventory(), 9).getAsInt();
                int indexHotbar = this.menu.findSlot(minecraft.player.getInventory(), 0).getAsInt();
                int indexStart = Math.min(indexFirstRow, indexHotbar);
                VersusMod.MOD_LOGGER.warn("Start of player inventory is " + indexStart);
                InventorySorting.InventoryToSort type = indexHotbar == 0 ? InventorySorting.InventoryToSort.SURVIVAL_INVENTORY : InventorySorting.InventoryToSort.INVENTORY_WHITH_SLOTS_ABOVE;
                InventorySorting.sortInventory(this.menu, minecraft, minecraft.player.getInventory(), type, indexStart, numSlots);
            }
            if (buttonSortInventory != null) buttonSortInventory.setFocused(false);
        });

        this.addRenderableWidget(buttonHotbarSwap);
        this.addRenderableWidget(buttonSortInventory);
    }

    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if(click.button() == 2) {
            HotbarCycling.doHotbarSwap(minecraft.player.getInventory());
            return false;
        }
        else {
            boolean returnValue;
            if (this.recipeBookComponent.mouseClicked(click, doubled)) {
                this.setFocused(this.recipeBookComponent);
                returnValue = true;
            } else {
                boolean outsideClickValue = super.mouseClicked(click, doubled);
                returnValue = this.widthTooNarrow && this.recipeBookComponent.isVisible() ? true : outsideClickValue;
            }

            if(buttonHotbarSwap != null) {
                buttonHotbarSwap.setPosition(this.leftPos + 104 + 22, this.height / 2 - 22);
                buttonHotbarSwap.setFocused(false);
            }
            if(buttonSortInventory != null) {
                buttonSortInventory.setPosition(buttonSortInventory == null ? this.leftPos + 104 + 22 : buttonHotbarSwap.getX() + 22, this.height / 2 - 22);
                buttonSortInventory.setFocused(false);
            }

            return returnValue;
        }
    }
}
