package frootloops.versus.mixin.client.environment.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;


@Environment(EnvType.CLIENT)
@Mixin(targets = "net.minecraft.client.render.BackgroundRenderer$DarknessFogModifier")
public abstract class DarknessFogRendererMixin {

    @ModifyConstant(method = "applyStartEndModifier", constant = @Constant(floatValue = 15.0F))
    private float darknessEffectNoLongerCompletelyBlindsPlayers(float fogEnd) {
        return 32.0f;
    }

    @ModifyConstant(method = "applyStartEndModifier", constant = @Constant(floatValue = 0.75F))
    private float darknessEffectFogStart(float fogStartOffset) {
        return 0.0f;
    }
}
