package frootloops.versus.mixin.players;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrawableHelper.class)
public abstract class HungerBarRendererDefaultMixin {

    @Inject(method = "drawTexture(Lnet/minecraft/client/util/math/MatrixStack;IIIIII)V", at = @At(value = "HEAD"), cancellable = true)
    private static void renderHungerBar(MatrixStack matrices, int x, int y, int u, int v, int width, int height, CallbackInfo info) {
        if(VersusSettings.DO_FOOD_OVERHAUL && width == 9 && height == 9 && v == 27) {
            boolean isFoodContainerIcon = (u == 16) || (u == 16 + 13);
            boolean isFoodHaunchIcon = (u == 16 + 36) || (u == 16 + 45) || (u == 29 + 36) || (u == 29 + 45);
            if(isFoodContainerIcon || isFoodHaunchIcon) {
                info.cancel();
                return;
            }
        }
        DrawableHelper.drawTexture(matrices, x, y, 0, u, v, width, height, 256, 256);
    }
}
