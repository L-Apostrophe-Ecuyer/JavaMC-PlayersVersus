package frootloops.versus.mixin.client.mobs.hostile;

import com.mojang.blaze3d.vertex.PoseStack;
import frootloops.versus.mod.mobs.hostile.overworld.climbing.ClimbingRender;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Turns a climbing spider's model to its wall or ceiling in place of the usual turn to its yaw. */
@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererClimbMixin {

    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("HEAD"), cancellable = true)
    private void playersVersus$turnToTheSurface(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float scale, CallbackInfo info) {
        if (ClimbingRender.rotate(state, poseStack)) info.cancel();
    }
}
