package frootloops.versus.mixin.client.mobs.hostile;

import com.mojang.blaze3d.vertex.PoseStack;
import frootloops.versus.mod.mobs.hostile.overworld.crawling.SurfaceTilt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tilts a crawling spider's model onto its wall or ceiling in place of the usual turn to its yaw. */
@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererTiltMixin {

    @Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("HEAD"), cancellable = true)
    private void playersVersus$turnToTheSurface(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float scale, CallbackInfo info) {
        if (SurfaceTilt.rotate(state, poseStack)) info.cancel();
    }
}
