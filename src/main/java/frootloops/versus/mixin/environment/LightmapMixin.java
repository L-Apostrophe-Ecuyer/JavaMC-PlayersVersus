package frootloops.versus.mixin.environment;

import frootloops.versus.VersusMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapMixin {

    @Shadow private final MinecraftClient client;

    protected LightmapMixin(MinecraftClient client, GameRenderer renderer) {
        this.client = client;
    }

    @ModifyVariable(method = "update", at = @At("STORE"), ordinal = 3)
    private float alwaysSomeDarkness(float h) {
        return (h + 2.0f)/3.0f;
    }

    @ModifyVariable(method = "update", at = @At("STORE"), ordinal = 6)
    private float reducedNightVision(float l) {return Math.min(0.8f, l);}

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/LightmapTextureManager;getBrightness(Lnet/minecraft/world/dimension/DimensionType;I)F"))
    private float getBrightness(DimensionType type, int lightLevel) {
        float f = (float)lightLevel / 14.0f;
        return MathHelper.lerp(type.ambientLight() - 0.015f, f / (4.0f - 3.0f * f), 1.5f);
    }

    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getSkyBrightness(F)F"))
    private float getSkyBrightness(ClientWorld world, float tickDelta) {
        float result = world.getSkyBrightness(tickDelta);
        if(result < 0.7f && world.getDimension().hasSkyLight()) {
            float moonPhaseDarkness = Math.abs(4.0f - (float)((world.getLunarTime() + 6000L + (24000L * 3L)) % (24000L * 8L))/24000.0f);
            float moonHeight = 0.3f/(0.25f + Math.abs(((float)(world.getTimeOfDay()%12000L) - 6000f)/12000f));
            result -= moonHeight * (moonPhaseDarkness/56f + (moonPhaseDarkness * moonPhaseDarkness)/96f);
        }
        return result;
    }
}
