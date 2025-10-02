package frootloops.versus.mixin.client.environment.visuals;

import frootloops.versus.mod.environment.CustomSpecialEffects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

@Environment(EnvType.CLIENT)
@Mixin(WeatherRendering.class)
public abstract class WeatherRendererMixin {

    @Shadow private int soundChance;

    @Overwrite
    private Biome.Precipitation getPrecipitationAt(World world, BlockPos pos) {
        if (!world.getChunkManager().isChunkLoaded(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()))) {
            return Biome.Precipitation.NONE;
        }
        else if(world.getThunderGradient(1.0F) == 0.0F) {
            return Biome.Precipitation.NONE;
        }
        else {
            Biome biome = (Biome)world.getBiome(pos).value();
            return biome.getPrecipitation(pos, world.getSeaLevel());
        }
    }

    @Redirect(method = "buildPrecipitationPieces", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getRainGradient(F)F"))
    private float getRainAmount(World world, float delta) {
        float stormAmount =  world.getThunderGradient(delta)/1.5f;
        float precipitationAmount = this.getPrecipitationAt(world, MinecraftClient.getInstance().player.getBlockPos()) == Biome.Precipitation.SNOW ? Math.max(stormAmount, world.getRainGradient(delta)) : stormAmount;
        return precipitationAmount;
    }

    @ModifyVariable(method = "addParticlesAndSound", at = @At("STORE"), ordinal = 0)
    private float noRainOnFoggyWeather(float fogAmount, ClientWorld world, Camera camera, int ticks, ParticlesMode particlesMode) {
        if(fogAmount < 0.1f) return 0.0f;

        BlockPos cameraPos = BlockPos.ofFloored(camera.getPos());
        Random random = Random.create((long)ticks * 312987231L);
        if(world.getLightLevel(LightType.SKY, cameraPos) > 4 && random.nextInt(600) < this.soundChance++) {
            this.soundChance = 0;
            world.playSoundAtBlockCenterClient(cameraPos.up(3), CustomSpecialEffects.FOG_WIND_SOUND, SoundCategory.WEATHER, 0.15F, 1.0F, false);
        }

        float stormAmount =  world.getThunderGradient(1.0F);
        float precipitationAmount = this.getPrecipitationAt(world, MinecraftClient.getInstance().player.getBlockPos()) == Biome.Precipitation.SNOW ? Math.max(stormAmount, fogAmount) : stormAmount;
        return precipitationAmount;
    }
}
