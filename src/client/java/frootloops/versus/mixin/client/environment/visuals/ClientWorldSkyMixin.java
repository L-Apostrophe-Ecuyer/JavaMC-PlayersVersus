package frootloops.versus.mixin.client.environment.visuals;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import frootloops.versus.mod.environment.CustomSpecialEffects;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.*;

@Environment(EnvType.CLIENT)
@Mixin(ClientLevel.class)
public abstract class ClientWorldSkyMixin extends Level {
    protected ClientWorldSkyMixin(WritableLevelData properties, ResourceKey<Level> registryRef, RegistryAccess registryManager, Holder<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, maxChainedNeighborUpdates);
    }

    // 26.3 moved the weather ticking (rain particles and sounds, tickWeatherEffects) and the precipitation check
    // (getPrecipitationAt) from WeatherEffectRenderer into ClientLevel, unchanged in what they do.
    // Not ported yet: 1.21.10's getSkyDarken (rain darkening 5/16 -> 5/24) and getSkyColor (rain gradient / 1.25)
    // tweaks. 26.x computes the sky from environment attributes (SKY_COLOR, SKY_LIGHT_FACTOR) instead.
    @Shadow private int rainSoundTime;

    // Rain only falls in thunderstorms; plain rain is fog.
    @Inject(method = "getPrecipitationAt", at = @At("HEAD"), cancellable = true)
    private void onlyStormsPrecipitate(BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
        if (this.getThunderLevel(1.0F) == 0.0F) cir.setReturnValue(Biome.Precipitation.NONE);
    }

    // The rain level tickWeatherEffects stores first (tickRainParticles' in 1.21.10): fog blows instead of rain.
    @ModifyVariable(method = "tickWeatherEffects", at = @At("STORE"), ordinal = 0)
    private float noRainOnFoggyWeather(float fogAmount) {
        if(fogAmount < 0.1f) return 0.0f;

        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
        BlockPos cameraPos = BlockPos.containing(camera.position());
        RandomSource random = RandomSource.create(this.getGameTime() * 312987231L);
        if(this.getBrightness(LightLayer.SKY, cameraPos) > 4 && random.nextInt(600) < this.rainSoundTime++) {
            this.rainSoundTime = 0;
            this.playLocalSound(cameraPos.above(3), CustomSpecialEffects.FOG_WIND_SOUND, SoundSource.WEATHER, 0.15F, 1.0F, false);
        }

        float stormAmount = this.getThunderLevel(1.0F);
        float precipitationAmount = ((ClientLevel)(Object)this).getPrecipitationAt(Minecraft.getInstance().player.blockPosition()) == Biome.Precipitation.SNOW ? Math.max(stormAmount, fogAmount) : stormAmount;
        return precipitationAmount;
    }

    @Override
    public boolean isRainingAt(BlockPos pos) {
        if (!this.isThundering()) return false;
        else return super.isRainingAt(pos);
    }
}
