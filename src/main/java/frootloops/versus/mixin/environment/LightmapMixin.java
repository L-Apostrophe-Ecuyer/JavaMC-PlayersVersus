package frootloops.versus.mixin.environment;

import frootloops.versus.VersusMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapMixin {

    @Shadow private final MinecraftClient client;
    @Shadow private final GameRenderer renderer;

    protected LightmapMixin(MinecraftClient client, GameRenderer renderer) {
        this.client = client;
        this.renderer = renderer;
    }

    @ModifyVariable(method = "update", at = @At("STORE"), ordinal = 3)
    private float alwaysSomeDarkness(float h) {
        return (h + 2.0f)/3.0f;
    }

    @ModifyVariable(method = "update", at = @At("STORE"), ordinal = 6)
    private float reducedNightVision(float l) {
        return l/1.25f;
    }

    @ModifyVariable(method = "update", at = @At("STORE"), ordinal = 15)
    private float gammaReduced(float gamma) {
        return -0.2f + gamma/2.0f;
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/LightmapTextureManager;getBrightness(Lnet/minecraft/world/dimension/DimensionType;I)F"))
    private float getBrightness(DimensionType type, int lightLevel) {
        float f = (float)lightLevel / 14.0f;
        return MathHelper.lerp(type.ambientLight() - 0.02f, f / (4.0f - 3.0f * f), 1.1f);
    }

}
