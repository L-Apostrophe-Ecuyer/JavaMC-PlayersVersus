package frootloops.versus.mixin.environment;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(WorldRenderer.class)
public abstract class RainRendererMixin {

    @Shadow private final MinecraftClient client;

    protected RainRendererMixin(MinecraftClient client) {
        this.client = client;
    }

    @Inject(method = "tickRainSplashing", at = @At("HEAD"), cancellable = true)
    public void tickRainSplashing(Camera camera, CallbackInfo info) {
        if(this.client.world.getThunderGradient(1.0f) == 0.0f) {
            if(this.client.world.getTime() % 5 != 0) info.cancel();
        }
    }
}
