package frootloops.versus.mixin.client.players.attacking;

import frootloops.versus.mod.Combat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
// GameRenderer#pick moved to LocalPlayer (static) in 26.3.
@Mixin(LocalPlayer.class)
public abstract class CrosshairTargetMixin {
    @ModifyVariable(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At("HEAD"), ordinal = 1)
    private static double getActualAttackRange(double entityInteractionRange) {
        return Combat.getAttackRange(Minecraft.getInstance().player);
    }
}
