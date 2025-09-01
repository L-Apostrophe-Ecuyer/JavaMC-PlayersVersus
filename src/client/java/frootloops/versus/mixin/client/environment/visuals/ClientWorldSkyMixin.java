package frootloops.versus.mixin.client.environment.visuals;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Environment(EnvType.CLIENT)
@Mixin(ClientWorld.class)
public abstract class ClientWorldSkyMixin {

    @ModifyConstant(method = "getSkyBrightness", constant = @Constant(floatValue = 16.0F))
    private static float lessDepressingWeather(float f) {
        return 24.0f;
    }

    @ModifyVariable(method = "getSkyColor", at = @At("STORE"), ordinal = 3)
    private float moreColorfulSkyDuringRain(float rainGradient) {return rainGradient/1.25f;}
}
