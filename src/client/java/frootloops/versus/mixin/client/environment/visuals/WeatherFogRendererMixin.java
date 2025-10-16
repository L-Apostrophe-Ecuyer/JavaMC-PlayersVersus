package frootloops.versus.mixin.client.environment.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.AtmosphericFogModifier;
import net.minecraft.client.render.fog.FogData;
import net.minecraft.client.render.fog.StandardFogModifier;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Environment(EnvType.CLIENT)
@Mixin(AtmosphericFogModifier.class)
public abstract class WeatherFogRendererMixin extends StandardFogModifier {

    @Shadow private float fogMultiplier;

    @Inject(method = "applyStartEndModifier", at = @At("TAIL"), cancellable = false)
    private void rainFogNotAsIntense(FogData data, Entity cameraEntity, BlockPos cameraPos, ClientWorld world, float viewDistance, RenderTickCounter tickCounter, CallbackInfo info) {
        data.environmentalStart = Math.max(-40f, data.environmentalStart);
        data.environmentalEnd = viewDistance * 1.0f/(fogMultiplier + 1.0f);
    }
}
