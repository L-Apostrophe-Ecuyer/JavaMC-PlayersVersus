package frootloops.versus.mixin.client.mobs.hostile;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import frootloops.versus.mod.mobs.hostile.end.GazeRenderState;
import frootloops.versus.mod.mobs.hostile.end.GazeTranslucency;
import frootloops.versus.mod.mobs.hostile.overworld.PlayerZombieRenderState;
import frootloops.versus.mod.mobs.hostile.overworld.PlayerZombieRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Draws the warped and phantoms as opaque as the camera sees them (GazeTranslucency): a see-through body, from a
 * shimmer to whole, their shadows fading with them. (Their eyes fade in EyesLayerGazeMixin.)
 */
@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererGazeMixin {

    @Shadow
    public abstract Identifier getTextureLocation(LivingEntityRenderState state);

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void playersVersus$howWellSeen(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo info) {
        float seen = GazeTranslucency.faded(entity) ? GazeTranslucency.opacity(entity, partialTick) : 1.0F;
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
        info.setReturnValue(RenderTypes.entityTranslucent(this.getTextureLocation(state)));
    }

    @ModifyReturnValue(method = "getModelTint", at = @At("RETURN"))
    private int playersVersus$fade(int tint, LivingEntityRenderState state) {
        // A player zombie's skin, gone grey-green
        if (state instanceof PlayerZombieRenderState) tint = ARGB.multiply(tint, PlayerZombieRenderer.TINT);
        float seen = ((GazeRenderState) state).playersVersus$seen();
        if (seen >= 0.999F) return tint;
        int alpha = Math.round(((tint >>> 24) & 0xFF) * seen);
        return (alpha << 24) | (tint & 0xFFFFFF);
    }
}
