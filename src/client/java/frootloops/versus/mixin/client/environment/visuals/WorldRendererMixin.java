package frootloops.versus.mixin.client.environment.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {

    @Shadow private final MinecraftClient client;

    protected WorldRendererMixin(MinecraftClient client) {
        this.client = client;
    }

    /*
    @Inject(method = "tickRainSplashing", at = @At("HEAD"), cancellable = true)
    public void tickRainSplashing(Camera camera, CallbackInfo info) {
        if(this.client.world.getThunderGradient(1.0f) == 0.0f) {
            if(this.client.world.getTime() % 5 != 0) info.cancel();
        }
    }*/

    @Inject(method = "addParticle", at = @At("HEAD"), cancellable = true)
    public void tickRainSplashing(ParticleEffect parameters, boolean shouldAlwaysSpawn, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfo info) {
        ParticleType type = parameters.getType();
        if((type == ParticleTypes.ENTITY_EFFECT) && this.client.getCameraEntity().getEyePos().squaredDistanceTo(x,y + 0.1,z) < 2.5) {
            info.cancel();
        }
        else if((type == ParticleTypes.RAIN) && this.client.getCameraEntity().getEyePos().squaredDistanceTo(x,y,z) < 80.0) {
            info.cancel();
        }
    }
}
