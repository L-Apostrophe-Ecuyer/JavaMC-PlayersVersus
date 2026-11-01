package frootloops.versus.mixin.client.mobs.hostile;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import frootloops.versus.mod.mobs.hostile.end.GazeRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Glowing eyes fade with their body (GazeTranslucency): an enderman's, a watchling's or a phantom's eyes are drawn as
 * bright as it's seen. (The eyes' glow adds light, so a dimmer colour is a fainter glow.)
 */
@Environment(EnvType.CLIENT)
@Mixin(EyesLayer.class)
public abstract class EyesLayerGazeMixin {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @WrapOperation(method = "submit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;III)V"))
    private void playersVersus$fadeEyes(OrderedSubmitNodeCollector collector, Model model, Object state, PoseStack pose, RenderType type,
                                        int light, int overlay, int outline, Operation<Void> original) {
        float seen = state instanceof GazeRenderState gaze ? gaze.playersVersus$seen() : 1.0F;
        if (seen >= 0.999F) {
            original.call(collector, model, state, pose, type, light, overlay, outline);
            return;
        }
        int channel = Math.round(255.0F * seen);
        collector.submitModel(model, state, pose, type, light, overlay, ARGB.color(channel, channel, channel, channel), (UvMapping) null, outline);
    }
}
