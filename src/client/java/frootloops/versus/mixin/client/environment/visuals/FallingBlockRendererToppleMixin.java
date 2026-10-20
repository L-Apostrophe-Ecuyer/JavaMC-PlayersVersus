package frootloops.versus.mixin.client.environment.visuals;

import com.mojang.blaze3d.vertex.PoseStack;
import frootloops.versus.mod.environment.ClientTopples;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A felled tree's falling block is drawn where its tree has turned it to this frame, turned over with it (ClientTopples). */
@Environment(EnvType.CLIENT)
@Mixin(FallingBlockRenderer.class)
public abstract class FallingBlockRendererToppleMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/item/FallingBlockEntity;Lnet/minecraft/client/renderer/entity/state/FallingBlockRenderState;F)V", at = @At("TAIL"))
    private void playersVersus$turnWithItsTree(FallingBlockEntity block, FallingBlockRenderState state, float partialTick, CallbackInfo info) {
        ClientTopples.extract(block, state, partialTick);
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/FallingBlockRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V"))
    private void playersVersus$turnOver(FallingBlockRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo info) {
        ClientTopples.turn(state, poseStack);
    }
}
