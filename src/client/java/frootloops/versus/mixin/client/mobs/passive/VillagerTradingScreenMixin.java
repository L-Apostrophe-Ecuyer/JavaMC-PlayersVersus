package frootloops.versus.mixin.client.mobs.passive;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class VillagerTradingScreenMixin extends HandledScreen<MerchantScreenHandler>  {

    public VillagerTradingScreenMixin(MerchantScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "drawForeground", at = @At("TAIL"), cancellable = false)
    public void renderMain(DrawContext context, int mouseX, int mouseY, CallbackInfo info) {
        if(handler.getExperience() > 0) {
            if(mouseX > x + 136 && mouseX < x + 234 && mouseY > y + 14 && mouseY < y + 23) {
                Text text = Text.translatable("players-versus.tradeScreen.experienceBarHover");
                if (text != null) context.drawOrderedTooltip(textRenderer, textRenderer.wrapLines(text, Math.max(context.getScaledWindowWidth() / 3, 200)), mouseX, mouseY);
            }
        }
    }
}
