package frootloops.versus.mixin.client.environment.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.fog.environment.DimensionOrBossFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;


@Environment(EnvType.CLIENT)
@Mixin(DimensionOrBossFogEnvironment.class)
public abstract class NetherFogRendererMixin {

    @ModifyConstant(method = "setupFog", constant = @Constant(floatValue = 192.0f))
    private static float lessDenseNetherFog(float maxRenderDistance) {
        return 512.0f;
    }

}
