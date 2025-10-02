package frootloops.versus.mixin.client.items_and_effects.inventory;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.items_and_effects.inventory.HotbarCycling;
import frootloops.versus.mod.items_and_effects.inventory.InventorySorting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.ScreenPos;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
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

    @Shadow private final RecipeBookWidget<?> recipeBook;
    @Shadow private boolean narrow;

    @Shadow
    protected abstract ScreenPos getRecipeBookButtonPos();

    public InventoryScreenSurvivalMixin(T handler, PlayerInventory inventory, Text title, RecipeBookWidget<?> recipeBook) {
        super(handler, inventory, title);
        this.recipeBook = recipeBook;
    }


    @Inject(method = "init", at = @At("TAIL"), cancellable = false)
    private void addInventoryButtons(CallbackInfo info) {

        this.buttonHotbarSwap = new TexturedButtonWidget(this.x + 104 + 22, this.height / 2 - 22, 20, 18, InventorySorting.TEXTURE_HOTBAR_SWAP_BUTTON, button -> {
            HotbarCycling.doHotbarSwap(client.player.getInventory());
            if (buttonHotbarSwap != null) buttonHotbarSwap.setFocused(false);
        });

        this.buttonSortInventory = new TexturedButtonWidget(buttonHotbarSwap.getX() + 22, buttonHotbarSwap.getY(), 20, 18, InventorySorting.TEXTURE_INVENTORY_SORT_BUTTON, button -> {
            if (client.player != null) {
                int numSlots = 36;
                int indexFirstRow = this.handler.getSlotIndex(client.player.getInventory(), 9).getAsInt();
                int indexHotbar = this.handler.getSlotIndex(client.player.getInventory(), 0).getAsInt();
                int indexStart = Math.min(indexFirstRow, indexHotbar);
                VersusMod.MOD_LOGGER.warn("Start of player inventory is " + indexStart);
                InventorySorting.InventoryToSort type = indexHotbar == 0 ? InventorySorting.InventoryToSort.SURVIVAL_INVENTORY : InventorySorting.InventoryToSort.INVENTORY_WHITH_SLOTS_ABOVE;
                InventorySorting.sortInventory(this.handler, client, client.player.getInventory(), type, indexStart, numSlots);
            }
            if (buttonSortInventory != null) buttonSortInventory.setFocused(false);
        });

        this.addDrawableChild(buttonHotbarSwap);
        this.addDrawableChild(buttonSortInventory);
    }

    public boolean mouseClicked(Click click, boolean doubled) {
        if(click.button() == 2) {
            HotbarCycling.doHotbarSwap(client.player.getInventory());
            return false;
        }
        else {
            boolean returnValue;
            if (this.recipeBook.mouseClicked(click, doubled)) {
                this.setFocused(this.recipeBook);
                returnValue = true;
            } else {
                boolean outsideClickValue = super.mouseClicked(click, doubled);
                returnValue = this.narrow && this.recipeBook.isOpen() ? true : outsideClickValue;
            }

            if(buttonHotbarSwap != null) {
                buttonHotbarSwap.setPosition(this.x + 104 + 22, this.height / 2 - 22);
                buttonHotbarSwap.setFocused(false);
            }
            if(buttonSortInventory != null) {
                buttonSortInventory.setPosition(buttonSortInventory == null ? this.x + 104 + 22 : buttonHotbarSwap.getX() + 22, this.height / 2 - 22);
                buttonSortInventory.setFocused(false);
            }

            return returnValue;
        }
    }
}
