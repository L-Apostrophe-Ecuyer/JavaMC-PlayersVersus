package frootloops.versus.mixin.environment;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BackgroundRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Environment(EnvType.CLIENT)
@Mixin(BackgroundRenderer.class)
public abstract class FogRendererMixin {

    @ModifyVariable(method = "applyFog", at = @At("HEAD"), ordinal = 0)
    private static BackgroundRenderer.FogType modifyFogType(BackgroundRenderer.FogType fogType) {
        if(fogType == BackgroundRenderer.FogType.FOG_TERRAIN) return BackgroundRenderer.FogType.FOG_SKY;
        return fogType;
    }
}
