package frootloops.versus.mixin.players.attacking;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(InGameHud.class)
public class CrosshairSizeMixin {

    @Shadow
    private final MinecraftClient client;

    public CrosshairSizeMixin(MinecraftClient client) {
        this.client = client;
    }

    @ModifyArg(method = "renderCrosshair", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;renderCrosshair(I)V"), index = 0)
    private int crosshairSize(int x) {
        if(client.targetedEntity != null && client.targetedEntity.isAlive()) return 10;
        return 0;
    }
}
