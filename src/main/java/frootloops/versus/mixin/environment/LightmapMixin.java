package frootloops.versus.mixin.environment;

import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LightmapTextureManager.class)
public class LightmapMixin {

    @ModifyVariable(method = "update", at = @At("STORE"), ordinal = 3)
    private float alwaysSomeDarkness(float h) {
        return (h + 2.5f)/3.0f;
    }

}
