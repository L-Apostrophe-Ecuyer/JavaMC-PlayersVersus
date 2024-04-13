package frootloops.versus.mixin.players.attacking;

import frootloops.versus.mod.Combat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class CrosshairTargetMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @ModifyVariable(method = "findCrosshairTarget", at = @At("HEAD"), ordinal = 1)
    private double getActualAttackRange(double entityInteractionRange) {
        return Combat.getAttackRange(client.player);
    }
}
