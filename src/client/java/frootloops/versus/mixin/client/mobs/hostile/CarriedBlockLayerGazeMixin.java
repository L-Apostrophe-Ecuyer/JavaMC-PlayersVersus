package frootloops.versus.mixin.client.mobs.hostile;

import com.mojang.blaze3d.vertex.PoseStack;
import frootloops.versus.mod.mobs.hostile.end.GazeRenderState;
import frootloops.versus.mod.mobs.hostile.end.GazeTranslucency;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CarriedBlockLayer;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The block an enderman carries can't be drawn see-through: it shows only once the enderman is mostly seen. */
@Environment(EnvType.CLIENT)
@Mixin(CarriedBlockLayer.class)
public abstract class CarriedBlockLayerGazeMixin {

    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/EndermanRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void playersVersus$onlyOnceSeen(PoseStack pose, SubmitNodeCollector collector, int light, EndermanRenderState state, float yRot, float xRot, CallbackInfo info) {
        if (((GazeRenderState) state).playersVersus$seen() < GazeTranslucency.CARRIED_BLOCK_FROM) info.cancel();
    }
}
