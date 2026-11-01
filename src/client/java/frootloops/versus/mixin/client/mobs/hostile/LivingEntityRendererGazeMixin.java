package frootloops.versus.mixin.client.mobs.hostile;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import frootloops.versus.mod.mobs.hostile.end.GazeRenderState;
import frootloops.versus.mod.mobs.hostile.end.GazeTranslucency;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Draws the warped as well as the camera sees them (GazeTranslucency): a see-through body faded by how well seen, not
 * drawn at all when not seen, their layers (eyes, carried block) only once mostly seen, their shadows fading with them.
 */
@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererGazeMixin {

    @Shadow
    public abstract Identifier getTextureLocation(LivingEntityRenderState state);

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void playersVersus$howWellSeen(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo info) {
        float seen = GazeTranslucency.warped(entity) ? GazeTranslucency.seen(entity, partialTick) : 1.0F;
        ((GazeRenderState) state).playersVersus$setSeen(seen);
        if (seen < 1.0F) {
            state.shadowRadius *= seen;
            if (seen < 0.5F) state.shadowPieces.clear();
        }
    }

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void playersVersus$seeThrough(LivingEntityRenderState state, boolean bodyVisible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> info) {
        float seen = ((GazeRenderState) state).playersVersus$seen();
        if (seen >= 0.999F || glowing) return;
        info.setReturnValue(seen <= 0.01F ? null : RenderTypes.entityTranslucent(this.getTextureLocation(state)));
    }

    @ModifyReturnValue(method = "getModelTint", at = @At("RETURN"))
    private int playersVersus$fade(int tint, LivingEntityRenderState state) {
        float seen = ((GazeRenderState) state).playersVersus$seen();
        if (seen >= 0.999F) return tint;
        int alpha = Math.round(((tint >>> 24) & 0xFF) * seen);
        return (alpha << 24) | (tint & 0xFFFFFF);
    }

    @ModifyReturnValue(method = "shouldRenderLayers", at = @At("RETURN"))
    private boolean playersVersus$layersOnceSeen(boolean render, LivingEntityRenderState state) {
        return render && ((GazeRenderState) state).playersVersus$seen() >= GazeTranslucency.LAYERS_FROM;
    }
}
