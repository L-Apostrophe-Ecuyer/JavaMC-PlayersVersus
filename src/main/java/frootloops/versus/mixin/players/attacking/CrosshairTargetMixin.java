package frootloops.versus.mixin.players.attacking;

import frootloops.versus.mod.Combat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(GameRenderer.class)
public abstract class CrosshairTargetMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    private double playerAttackRange = 0.0d;
    @ModifyVariable(method = "updateCrosshairTarget", at = @At("STORE"), ordinal = 1)
    private double getActualAttackRange(double e) {
        return Combat.getAttackRange(client.player);
    }
}
