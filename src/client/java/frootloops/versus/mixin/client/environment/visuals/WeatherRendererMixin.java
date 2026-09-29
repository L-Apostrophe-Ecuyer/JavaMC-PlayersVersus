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
@Mixin(WeatherEffectRenderer.class)
public abstract class WeatherRendererMixin {

    @Shadow private int rainSoundTime;

    @Overwrite
    private Biome.Precipitation getPrecipitationAt(Level world, BlockPos pos) {
        if (!world.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()))) {
            return Biome.Precipitation.NONE;
        }
        else if(world.getThunderLevel(1.0F) == 0.0F) {
            return Biome.Precipitation.NONE;
        }
        else {
            Biome biome = (Biome)world.getBiome(pos).value();
            return biome.getPrecipitationAt(pos, world.getSeaLevel());
        }
    }

    @Redirect(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getRainLevel(F)F"))
    private float getRainAmount(Level world, float delta) {
        float stormAmount =  world.getThunderLevel(delta)/1.5f;
        float precipitationAmount = this.getPrecipitationAt(world, Minecraft.getInstance().player.blockPosition()) == Biome.Precipitation.SNOW ? Math.max(stormAmount, world.getRainLevel(delta)) : stormAmount;
        return precipitationAmount;
    }

    @ModifyVariable(method = "tickRainParticles", at = @At("STORE"), ordinal = 0)
    private float noRainOnFoggyWeather(float fogAmount, ClientLevel world, Camera camera, int ticks, ParticleStatus particlesMode) {
        if(fogAmount < 0.1f) return 0.0f;

        BlockPos cameraPos = BlockPos.containing(camera.getPosition());
        RandomSource random = RandomSource.create((long)ticks * 312987231L);
        if(world.getBrightness(LightLayer.SKY, cameraPos) > 4 && random.nextInt(600) < this.rainSoundTime++) {
            this.rainSoundTime = 0;
            world.playLocalSound(cameraPos.above(3), CustomSpecialEffects.FOG_WIND_SOUND, SoundSource.WEATHER, 0.15F, 1.0F, false);
        }

        float stormAmount =  world.getThunderLevel(1.0F);
        float precipitationAmount = this.getPrecipitationAt(world, Minecraft.getInstance().player.blockPosition()) == Biome.Precipitation.SNOW ? Math.max(stormAmount, fogAmount) : stormAmount;
        return precipitationAmount;
    }
}
