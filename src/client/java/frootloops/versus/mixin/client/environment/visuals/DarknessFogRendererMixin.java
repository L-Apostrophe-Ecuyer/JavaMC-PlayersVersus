package frootloops.versus.mixin.client.environment.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.fog.DarknessEffectFogModifier;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;


@Environment(EnvType.CLIENT)
@Mixin(DarknessEffectFogModifier.class)
public abstract class DarknessFogRendererMixin {

    @ModifyConstant(method = "applyStartEndModifier", constant = @Constant(floatValue = 15.0F))
    private float darknessEffectNoLongerCompletelyBlindsPlayers(float fogEnd) {
        StatusEffectInstance darkness = MinecraftClient.getInstance().player.getStatusEffect(StatusEffects.DARKNESS);
        float blindnessAmount = darkness == null ? 0.0f : 8f + darkness.getAmplifier() * 16.0f;
        return 48.0f - blindnessAmount;
    }

    @ModifyConstant(method = "applyStartEndModifier", constant = @Constant(floatValue = 0.75F))
    private float darknessEffectFogStart(float fogStartOffset) {
        return 0.0f;
    }
}
