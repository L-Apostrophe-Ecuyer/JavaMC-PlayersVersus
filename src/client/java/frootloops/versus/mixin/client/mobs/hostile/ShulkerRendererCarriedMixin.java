package frootloops.versus.mixin.client.mobs.hostile;

import frootloops.versus.VersusSettings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.ShulkerRenderer;
import net.minecraft.client.renderer.entity.state.ShulkerRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Shulker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A shulker an enderman carries (EndermanShulkerCarrier) is drawn in its arms: SHULKER_HELD_FORWARD before it (it really
 * is inside the enderman's box, at its chest), turned as the enderman's body turns.
 */
@Environment(EnvType.CLIENT)
@Mixin(ShulkerRenderer.class)
public abstract class ShulkerRendererCarriedMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/monster/Shulker;Lnet/minecraft/client/renderer/entity/state/ShulkerRenderState;F)V", at = @At("TAIL"))
    private void playersVersus$inItsArms(Shulker shulker, ShulkerRenderState state, float partialTick, CallbackInfo info) {
        if (!(shulker.getVehicle() instanceof Enderman carrier)) return;
        float yaw = Mth.rotLerp(partialTick, carrier.yBodyRotO, carrier.yBodyRot);
        double forward = VersusSettings.Endermen.SHULKER_HELD_FORWARD, radians = Math.toRadians(yaw);
        state.renderOffset = state.renderOffset.add(-Math.sin(radians) * forward, 0.0, Math.cos(radians) * forward);
        state.bodyRot = yaw;
        state.yBodyRot = yaw;
    }
}
