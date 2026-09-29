package frootloops.versus.mixin.client.mobs.passive;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class VillagerTradingScreenMixin extends AbstractContainerScreen<MerchantMenu>  {

    public VillagerTradingScreenMixin(MerchantMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    @Inject(method = "renderLabels", at = @At("TAIL"), cancellable = false)
    public void renderMain(GuiGraphics context, int mouseX, int mouseY, CallbackInfo info) {
        if(menu.getTraderXp() > 0) {
            if(mouseX > leftPos + 136 && mouseX < leftPos + 234 && mouseY > topPos + 14 && mouseY < topPos + 23) {
                Component text = Component.translatable("players-versus.tradeScreen.experienceBarHover");
                if (text != null) context.setTooltipForNextFrame(font, font.split(text, Math.max(context.guiWidth() / 3, 200)), mouseX, mouseY);
            }
        }
    }
}
