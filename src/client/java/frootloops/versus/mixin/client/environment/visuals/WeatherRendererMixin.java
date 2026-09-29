package frootloops.versus.mixin.client.environment.visuals;

import frootloops.versus.mod.environment.CustomSpecialEffects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

@Environment(EnvType.CLIENT)
// 26.3 moved getPrecipitationAt and the rain particles (tickRainParticles, now tickWeatherEffects) to ClientLevel,
// which ClientWorldSkyMixin changes; what is drawn is still read here.
@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherRendererMixin {

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"))
    private float getRainAmount(ClientLevel world, float delta) {
        float stormAmount =  world.getThunderLevel(delta)/1.5f;
        float precipitationAmount = world.getPrecipitationAt(Minecraft.getInstance().player.blockPosition()) == Biome.Precipitation.SNOW ? Math.max(stormAmount, world.getRainLevel(delta)) : stormAmount;
        return precipitationAmount;
    }
}
